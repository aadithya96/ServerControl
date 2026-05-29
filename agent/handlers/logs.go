package handlers

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net/http"
	"os/exec"
	"strconv"
	"strings"
)

type LogsResponse struct {
	Lines  []string `json:"lines"`
	Source string   `json:"source"`
}

// GET /api/v1/logs?source=journal&unit=nginx&lines=200&follow=false
func LogsHandler(w http.ResponseWriter, r *http.Request) {
	source := r.URL.Query().Get("source")
	if source == "" {
		source = "journal"
	}
	unit := r.URL.Query().Get("unit")
	linesStr := r.URL.Query().Get("lines")
	follow := r.URL.Query().Get("follow") == "true"

	lines := 200
	if linesStr != "" {
		if v, err := strconv.Atoi(linesStr); err == nil && v > 0 {
			lines = v
		}
	}

	if follow {
		streamLogs(w, source, unit, lines)
		return
	}

	var cmd *exec.Cmd
	n := strconv.Itoa(lines)
	switch source {
	case "journal":
		if unit != "" {
			cmd = exec.Command("journalctl", "-u", unit, "-n", n, "--no-pager", "--output=short-iso")
		} else {
			cmd = exec.Command("journalctl", "-n", n, "--no-pager", "--output=short-iso")
		}
	case "syslog":
		cmd = exec.Command("tail", "-n", n, "/var/log/syslog")
	case "auth":
		cmd = exec.Command("tail", "-n", n, "/var/log/auth.log")
	case "nginx":
		cmd = exec.Command("tail", "-n", n, "/var/log/nginx/error.log")
	case "apache":
		cmd = exec.Command("tail", "-n", n, "/var/log/apache2/error.log")
	default:
		// custom: unit holds the file path
		if unit != "" {
			cmd = exec.Command("tail", "-n", n, unit)
		} else {
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(LogsResponse{Lines: []string{}, Source: source})
			return
		}
	}

	out, _ := cmd.Output()
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
	json.NewEncoder(w).Encode(LogsResponse{Lines: result, Source: source})
}

func streamLogs(w http.ResponseWriter, source, unit string, lines int) {
	w.Header().Set("Content-Type", "text/event-stream")
	w.Header().Set("Cache-Control", "no-cache")
	w.Header().Set("Connection", "keep-alive")

	flusher, ok := w.(http.Flusher)
	if !ok {
		http.Error(w, "streaming not supported", http.StatusInternalServerError)
		return
	}

	n := strconv.Itoa(lines)
	var args []string
	switch source {
	case "journal":
		args = []string{"-f", "-n", n, "--no-pager", "--output=short-iso"}
		if unit != "" {
			args = append(args, "-u", unit)
		}
	case "syslog":
		args = []string{"-f", "-n", n, "/var/log/syslog"}
	case "auth":
		args = []string{"-f", "-n", n, "/var/log/auth.log"}
	case "nginx":
		args = []string{"-f", "-n", n, "/var/log/nginx/error.log"}
	case "apache":
		args = []string{"-f", "-n", n, "/var/log/apache2/error.log"}
	default:
		if unit != "" {
			args = []string{"-f", "-n", n, unit}
		} else {
			return
		}
	}

	var cmd *exec.Cmd
	if source == "journal" {
		cmd = exec.Command("journalctl", args...)
	} else {
		cmd = exec.Command("tail", args...)
	}

	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return
	}
	if err := cmd.Start(); err != nil {
		return
	}
	defer cmd.Process.Kill()

	scanner := bufio.NewScanner(stdout)
	for scanner.Scan() {
		line := scanner.Text()
		fmt.Fprintf(w, "data: %s\n\n", line)
		flusher.Flush()
	}
}
