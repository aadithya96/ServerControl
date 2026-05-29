package handlers

import (
	"encoding/json"
	"net/http"
	"os/exec"
	"strconv"
	"strings"
)

type ServiceInfo struct {
	Name        string `json:"name"`
	Description string `json:"description"`
	LoadState   string `json:"load_state"`
	ActiveState string `json:"active_state"`
	SubState    string `json:"sub_state"`
	Enabled     string `json:"enabled"`
	UnitFile    string `json:"unit_file"`
	ExecStart   string `json:"exec_start"`
	Type        string `json:"type"`
}

type ServicesResponse struct {
	Services []ServiceInfo `json:"services"`
	Total    int           `json:"total"`
}

type ServiceActionRequest struct {
	Action string `json:"action"`
}

type ServiceLogsResponse struct {
	Lines []string `json:"lines"`
}

// GET /api/v1/services?type=service&state=failed
func ServicesHandler(w http.ResponseWriter, r *http.Request) {
	typeFilter := r.URL.Query().Get("type")
	stateFilter := r.URL.Query().Get("state")

	services := listServices()

	// Build enabled map from unit-files
	enabledMap := buildEnabledMap()
	for i := range services {
		if v, ok := enabledMap[services[i].Name]; ok {
			services[i].Enabled = v
		}
		// Populate ExecStart from show
		services[i].ExecStart = getExecStart(services[i].Name)
		services[i].Type = unitType(services[i].Name)
	}

	// Apply filters
	var filtered []ServiceInfo
	for _, s := range services {
		if typeFilter != "" && typeFilter != "all" && s.Type != typeFilter {
			continue
		}
		if stateFilter != "" && stateFilter != "all" && s.ActiveState != stateFilter {
			continue
		}
		filtered = append(filtered, s)
	}
	if filtered == nil {
		filtered = []ServiceInfo{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(ServicesResponse{Services: filtered, Total: len(filtered)})
}

func listServices() []ServiceInfo {
	// systemctl list-units --all --no-pager --no-legend outputs: UNIT LOAD ACTIVE SUB DESCRIPTION
	out, err := exec.Command("systemctl", "list-units", "--all", "--no-pager", "--no-legend").Output()
	if err != nil {
		return []ServiceInfo{}
	}

	var services []ServiceInfo
	lines := strings.Split(string(out), "\n")
	for _, line := range lines {
		if strings.TrimSpace(line) == "" {
			continue
		}
		// Skip lines that start with spaces (continuation / legend)
		if line[0] == ' ' || line[0] == '\t' {
			continue
		}
		// Handle "● service.name" prefix (systemctl uses ● for failed)
		line = strings.TrimPrefix(line, "● ")
		fields := strings.Fields(line)
		if len(fields) < 4 {
			continue
		}
		unit := fields[0]
		load := fields[1]
		active := fields[2]
		sub := fields[3]
		desc := ""
		if len(fields) >= 5 {
			desc = strings.Join(fields[4:], " ")
		}
		services = append(services, ServiceInfo{
			Name:        unit,
			Description: desc,
			LoadState:   load,
			ActiveState: active,
			SubState:    sub,
			Enabled:     "static",
		})
	}
	return services
}

func buildEnabledMap() map[string]string {
	out, err := exec.Command("systemctl", "list-unit-files", "--no-pager", "--no-legend").Output()
	if err != nil {
		return map[string]string{}
	}
	m := make(map[string]string)
	for _, line := range strings.Split(string(out), "\n") {
		fields := strings.Fields(line)
		if len(fields) >= 2 {
			m[fields[0]] = fields[1]
		}
	}
	return m
}

func getExecStart(unit string) string {
	out, err := exec.Command("systemctl", "show", unit, "--property=ExecStart").Output()
	if err != nil {
		return ""
	}
	// Format: ExecStart={ path=/usr/sbin/nginx ; argv[]=/usr/sbin/nginx ... }
	value := strings.TrimPrefix(strings.TrimSpace(string(out)), "ExecStart=")
	// Extract path= value
	if idx := strings.Index(value, "path="); idx >= 0 {
		rest := value[idx+5:]
		end := strings.IndexAny(rest, " ;}")
		if end > 0 {
			return rest[:end]
		}
		return rest
	}
	return ""
}

func unitType(name string) string {
	switch {
	case strings.HasSuffix(name, ".service"):
		return "service"
	case strings.HasSuffix(name, ".timer"):
		return "timer"
	case strings.HasSuffix(name, ".socket"):
		return "socket"
	case strings.HasSuffix(name, ".mount"):
		return "mount"
	case strings.HasSuffix(name, ".target"):
		return "target"
	default:
		return "other"
	}
}

// POST /api/v1/services/{name}/action
func ServiceActionHandler(w http.ResponseWriter, r *http.Request) {
	// Extract service name from URL path: /api/v1/services/{name}/action
	parts := strings.Split(strings.TrimPrefix(r.URL.Path, "/api/v1/services/"), "/")
	if len(parts) < 1 || parts[0] == "" {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "missing service name"})
		return
	}
	name := parts[0]

	var req ServiceActionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "invalid request body"})
		return
	}

	validActions := map[string]bool{
		"start": true, "stop": true, "restart": true,
		"reload": true, "enable": true, "disable": true,
	}
	if !validActions[req.Action] {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "invalid action: " + req.Action})
		return
	}

	out, err := exec.Command("systemctl", req.Action, name).CombinedOutput()
	w.Header().Set("Content-Type", "application/json")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": strings.TrimSpace(string(out)) + ": " + err.Error(),
		})
		return
	}
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "systemctl " + req.Action + " " + name + " succeeded",
	})
}

// GET /api/v1/services/{name}/logs?lines=100
func ServiceLogsHandler(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(strings.TrimPrefix(r.URL.Path, "/api/v1/services/"), "/")
	if len(parts) < 1 || parts[0] == "" {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(ServiceLogsResponse{Lines: []string{}})
		return
	}
	name := parts[0]

	linesStr := r.URL.Query().Get("lines")
	lines := 100
	if linesStr != "" {
		if v, err := strconv.Atoi(linesStr); err == nil && v > 0 {
			lines = v
		}
	}

	out, _ := exec.Command(
		"journalctl", "-u", name, "-n", strconv.Itoa(lines),
		"--no-pager", "--output=short-iso",
	).Output()

	rawLines := strings.Split(string(out), "\n")
	var result []string
	for _, l := range rawLines {
		if strings.TrimSpace(l) != "" {
			result = append(result, l)
		}
	}
	if result == nil {
		result = []string{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(ServiceLogsResponse{Lines: result})
}
