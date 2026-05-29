package handlers

import (
	"context"
	"encoding/json"
	"net/http"
	"os/exec"
	"strings"
	"time"
)

type execRequest struct {
	Command        string `json:"command"`
	TimeoutSeconds int    `json:"timeout_seconds"`
}

type execResponse struct {
	Output   string `json:"output"`
	ExitCode int    `json:"exit_code"`
	Success  bool   `json:"success"`
}

// ExecHandler handles POST /api/v1/exec
// Body: {"command":"df -h","timeout_seconds":30}
func ExecHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")

	var req execRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid JSON: " + err.Error()})
		return
	}

	if req.Command == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "command is required"})
		return
	}

	timeoutSecs := req.TimeoutSeconds
	if timeoutSecs <= 0 {
		timeoutSecs = 30
	}
	if timeoutSecs > 300 {
		timeoutSecs = 300
	}

	ctx, cancel := context.WithTimeout(r.Context(), time.Duration(timeoutSecs)*time.Second)
	defer cancel()

	cmd := exec.CommandContext(ctx, "sh", "-c", req.Command)
	outBytes, err := cmd.CombinedOutput()
	output := strings.TrimRight(string(outBytes), "\n")

	exitCode := 0
	success := true

	if err != nil {
		success = false
		if exitErr, ok := err.(*exec.ExitError); ok {
			exitCode = exitErr.ExitCode()
		} else if ctx.Err() == context.DeadlineExceeded {
			exitCode = 124 // standard "timed out" exit code
			output += "\n[command timed out]"
		} else {
			exitCode = 1
		}
	}

	json.NewEncoder(w).Encode(execResponse{
		Output:   output,
		ExitCode: exitCode,
		Success:  success,
	})
}
