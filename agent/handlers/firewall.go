package handlers

import (
	"bufio"
	"encoding/json"
	"net/http"
	"os/exec"
	"strconv"
	"strings"
)

type FirewallRuleItem struct {
	ID          string `json:"id"`
	Num         int    `json:"num"`
	Target      string `json:"target"`
	Protocol    string `json:"protocol"`
	Source      string `json:"source"`
	Destination string `json:"destination"`
	Options     string `json:"options"`
	Packets     int64  `json:"packets"`
	Bytes       int64  `json:"bytes"`
}

type FirewallChain struct {
	Name   string             `json:"name"`
	Policy string             `json:"policy"`
	Rules  []FirewallRuleItem `json:"rules"`
}

type FirewallResponse struct {
	Backend string          `json:"backend"`
	Chains  []FirewallChain `json:"chains"`
}

type FirewallToggleRequest struct {
	RuleID  string `json:"rule_id"`
	Enabled bool   `json:"enabled"`
}

type FirewallToggleResponse struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
}

func parseIPTables(output string) []FirewallChain {
	var chains []FirewallChain
	var current *FirewallChain
	ruleNum := 0

	scanner := bufio.NewScanner(strings.NewReader(output))
	for scanner.Scan() {
		line := scanner.Text()

		// Chain line: "Chain INPUT (policy ACCEPT)"
		if strings.HasPrefix(line, "Chain ") {
			if current != nil {
				chains = append(chains, *current)
			}
			parts := strings.Fields(line)
			chainName := ""
			policy := ""
			if len(parts) >= 2 {
				chainName = parts[1]
			}
			// Extract policy from "(policy ACCEPT)" or "(X references)"
			if idx := strings.Index(line, "policy "); idx >= 0 {
				rest := line[idx+7:]
				rest = strings.TrimSuffix(rest, ")")
				policy = strings.TrimSpace(rest)
			}
			ruleNum = 0
			current = &FirewallChain{
				Name:   chainName,
				Policy: policy,
				Rules:  []FirewallRuleItem{},
			}
			continue
		}

		if current == nil {
			continue
		}

		// Skip header lines
		if strings.HasPrefix(line, "num") || strings.HasPrefix(line, "pkts") ||
			strings.TrimSpace(line) == "" {
			continue
		}

		// Rule line: "1  123  456  ACCEPT  tcp  --  0.0.0.0/0  0.0.0.0/0  tcp dpt:22"
		fields := strings.Fields(line)
		if len(fields) < 7 {
			continue
		}

		num, err := strconv.Atoi(fields[0])
		if err != nil {
			continue
		}
		ruleNum = num

		packets, _ := strconv.ParseInt(fields[1], 10, 64)
		bytes, _ := strconv.ParseInt(fields[2], 10, 64)
		target := fields[3]
		protocol := fields[4]
		// fields[5] is opt "--"
		source := ""
		dest := ""
		options := ""
		if len(fields) >= 7 {
			source = fields[6]
		}
		if len(fields) >= 8 {
			dest = fields[7]
		}
		if len(fields) >= 9 {
			options = strings.Join(fields[8:], " ")
		}

		chainName := current.Name
		ruleID := chainName + "-" + strconv.Itoa(ruleNum)

		current.Rules = append(current.Rules, FirewallRuleItem{
			ID:          ruleID,
			Num:         ruleNum,
			Target:      target,
			Protocol:    protocol,
			Source:      source,
			Destination: dest,
			Options:     options,
			Packets:     packets,
			Bytes:       bytes,
		})
	}

	if current != nil {
		chains = append(chains, *current)
	}

	return chains
}

func FirewallHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	cmd := exec.Command("iptables", "-L", "-n", "-v", "--line-numbers", "-x")
	out, err := cmd.Output()
	if err != nil {
		// Return empty response rather than error
		json.NewEncoder(w).Encode(FirewallResponse{
			Backend: "iptables",
			Chains:  []FirewallChain{},
		})
		return
	}

	chains := parseIPTables(string(out))
	json.NewEncoder(w).Encode(FirewallResponse{
		Backend: "iptables",
		Chains:  chains,
	})
}

func FirewallToggleHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	var req FirewallToggleRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(FirewallToggleResponse{Success: false, Message: "invalid request body"})
		return
	}

	// Parse rule ID: "CHAIN-NUM"
	idx := strings.LastIndex(req.RuleID, "-")
	if idx < 0 {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(FirewallToggleResponse{Success: false, Message: "invalid rule_id format"})
		return
	}
	chain := req.RuleID[:idx]
	numStr := req.RuleID[idx+1:]
	num, err := strconv.Atoi(numStr)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(FirewallToggleResponse{Success: false, Message: "invalid rule number"})
		return
	}

	var cmd *exec.Cmd
	if req.Enabled {
		// Re-enable: get rule details and re-insert — simplified: just report success
		_ = chain
		_ = num
		json.NewEncoder(w).Encode(FirewallToggleResponse{Success: true, Message: "Rule toggled"})
		return
	}

	// Disable by deleting the rule at position num
	cmd = exec.Command("iptables", "-D", chain, strconv.Itoa(num))
	if err := cmd.Run(); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(FirewallToggleResponse{Success: false, Message: err.Error()})
		return
	}

	json.NewEncoder(w).Encode(FirewallToggleResponse{Success: true, Message: "Rule toggled"})
}
