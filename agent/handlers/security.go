package handlers

import (
	"bufio"
	"crypto/tls"
	"encoding/json"
	"net"
	"net/http"
	"os"
	"os/exec"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"
)

// ─── Failed Logins ────────────────────────────────────────────────────────────

type failedLoginAttempt struct {
	SourceIP  string `json:"source_ip"`
	Username  string `json:"username"`
	Count     int    `json:"count"`
	LastSeen  string `json:"last_seen"`
}

type failedLoginsResponse struct {
	Attempts []failedLoginAttempt `json:"attempts"`
	Total    int                  `json:"total"`
}

// FailedLoginsHandler handles GET /api/v1/security/failed-logins
func FailedLoginsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")

	limitStr := r.URL.Query().Get("limit")
	limit := 50
	if limitStr != "" {
		if n, err := strconv.Atoi(limitStr); err == nil && n > 0 {
			limit = n
		}
	}

	attempts, total, err := parseFailedLogins(limit)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	json.NewEncoder(w).Encode(failedLoginsResponse{
		Attempts: attempts,
		Total:    total,
	})
}

// parseFailedLogins reads auth.log or secure, extracts "Failed password" entries,
// groups by source IP, sorts descending by count and returns up to limit rows.
func parseFailedLogins(limit int) ([]failedLoginAttempt, int, error) {
	logFiles := []string{"/var/log/auth.log", "/var/log/secure"}
	var logFile string
	for _, f := range logFiles {
		if _, err := os.Stat(f); err == nil {
			logFile = f
			break
		}
	}
	if logFile == "" {
		return []failedLoginAttempt{}, 0, nil
	}

	// Regex for: "Failed password for [invalid user] <user> from <ip> port <p> ssh2"
	re := regexp.MustCompile(`Failed password for (?:invalid user )?(\S+) from (\d+\.\d+\.\d+\.\d+)`)

	type entry struct {
		count    int
		username string
		lastLine string
	}
	ipMap := map[string]*entry{}

	f, err := os.Open(logFile)
	if err != nil {
		return nil, 0, err
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := scanner.Text()
		m := re.FindStringSubmatch(line)
		if m == nil {
			continue
		}
		user := m[1]
		ip := m[2]
		if e, ok := ipMap[ip]; ok {
			e.count++
			e.lastLine = line
		} else {
			ipMap[ip] = &entry{count: 1, username: user, lastLine: line}
		}
	}

	attempts := make([]failedLoginAttempt, 0, len(ipMap))
	totalCount := 0
	for ip, e := range ipMap {
		totalCount += e.count
		// Extract timestamp from beginning of line (first 15 chars = "Jan  1 00:00:00")
		ts := ""
		if len(e.lastLine) >= 15 {
			ts = e.lastLine[:15]
		}
		attempts = append(attempts, failedLoginAttempt{
			SourceIP: ip,
			Username: e.username,
			Count:    e.count,
			LastSeen: ts,
		})
	}

	// Sort descending by count
	sort.Slice(attempts, func(i, j int) bool {
		return attempts[i].Count > attempts[j].Count
	})

	if len(attempts) > limit {
		attempts = attempts[:limit]
	}
	return attempts, totalCount, nil
}

// ─── SSL Certificates ─────────────────────────────────────────────────────────

type sslCertInfo struct {
	Domain         string `json:"domain"`
	ExpiryUnix     int64  `json:"expiry_unix"`
	DaysUntilExpiry int   `json:"days_until_expiry"`
	Issuer         string `json:"issuer"`
	IsValid        bool   `json:"is_valid"`
}

type sslCertsResponse struct {
	Certificates []sslCertInfo `json:"certificates"`
}

// SslCertHandler handles GET /api/v1/security/ssl?domains=example.com,api.example.com
func SslCertHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")

	domainsParam := r.URL.Query().Get("domains")
	if domainsParam == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "domains parameter required"})
		return
	}

	domains := strings.Split(domainsParam, ",")
	certs := make([]sslCertInfo, 0, len(domains))
	for _, d := range domains {
		d = strings.TrimSpace(d)
		if d == "" {
			continue
		}
		info := checkSSLCert(d)
		certs = append(certs, info)
	}

	json.NewEncoder(w).Encode(sslCertsResponse{Certificates: certs})
}

func checkSSLCert(domain string) sslCertInfo {
	addr := net.JoinHostPort(domain, "443")
	dialer := &net.Dialer{Timeout: 5 * time.Second}
	conn, err := tls.DialWithDialer(dialer, "tcp", addr, &tls.Config{
		InsecureSkipVerify: false,
		ServerName:         domain,
	})
	if err != nil {
		return sslCertInfo{
			Domain:          domain,
			IsValid:         false,
			DaysUntilExpiry: 0,
		}
	}
	defer conn.Close()

	certs := conn.ConnectionState().PeerCertificates
	if len(certs) == 0 {
		return sslCertInfo{Domain: domain, IsValid: false}
	}

	cert := certs[0]
	expiry := cert.NotAfter
	days := int(time.Until(expiry).Hours() / 24)
	issuer := cert.Issuer.CommonName
	if issuer == "" && len(cert.Issuer.Organization) > 0 {
		issuer = cert.Issuer.Organization[0]
	}

	return sslCertInfo{
		Domain:          domain,
		ExpiryUnix:      expiry.Unix(),
		DaysUntilExpiry: days,
		Issuer:          issuer,
		IsValid:         days > 0,
	}
}

// ─── Block IP ─────────────────────────────────────────────────────────────────

type blockIpRequest struct {
	IP string `json:"ip"`
}

// BlockIpHandler handles POST /api/v1/security/block-ip
// Body: {"ip":"1.2.3.4"}
func BlockIpHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")

	var req blockIpRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid JSON: " + err.Error()})
		return
	}

	ip := strings.TrimSpace(req.IP)
	if ip == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "ip is required"})
		return
	}

	// Validate the IP looks like an IP address.
	if net.ParseIP(ip) == nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid IP address"})
		return
	}

	out, err := exec.Command("iptables", "-I", "INPUT", "-s", ip, "-j", "DROP").CombinedOutput()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": strings.TrimSpace(string(out)),
		})
		return
	}

	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "IP " + ip + " blocked via iptables INPUT DROP",
	})
}
