package handlers

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
)

type ProcessInfo struct {
	PID         int     `json:"pid"`
	Name        string  `json:"name"`
	User        string  `json:"user"`
	CPUPercent  float64 `json:"cpu_percent"`
	MemPercent  float64 `json:"mem_percent"`
	MemRSSBytes int64   `json:"mem_rss_bytes"`
	Status      string  `json:"status"`
	Command     string  `json:"command"`
}

type ProcessResponse struct {
	Processes []ProcessInfo `json:"processes"`
	Total     int           `json:"total"`
}

type KillResponse struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
}

func readProcStatus(pid int) (name, user, status string, vmRSS int64) {
	path := fmt.Sprintf("/proc/%d/status", pid)
	data, err := os.ReadFile(path)
	if err != nil {
		return
	}
	for _, line := range strings.Split(string(data), "\n") {
		parts := strings.SplitN(line, ":", 2)
		if len(parts) != 2 {
			continue
		}
		key := strings.TrimSpace(parts[0])
		val := strings.TrimSpace(parts[1])
		switch key {
		case "Name":
			name = val
		case "State":
			if len(val) > 0 {
				status = string(val[0])
			}
		case "Uid":
			uids := strings.Fields(val)
			if len(uids) > 0 {
				uid, _ := strconv.Atoi(uids[0])
				user = resolveUID(uid)
			}
		case "VmRSS":
			fields := strings.Fields(val)
			if len(fields) > 0 {
				kb, _ := strconv.ParseInt(fields[0], 10, 64)
				vmRSS = kb * 1024
			}
		}
	}
	return
}

func resolveUID(uid int) string {
	data, err := os.ReadFile("/etc/passwd")
	if err != nil {
		return strconv.Itoa(uid)
	}
	for _, line := range strings.Split(string(data), "\n") {
		fields := strings.Split(line, ":")
		if len(fields) >= 3 {
			if fields[2] == strconv.Itoa(uid) {
				return fields[0]
			}
		}
	}
	return strconv.Itoa(uid)
}

func readCmdline(pid int) string {
	path := fmt.Sprintf("/proc/%d/cmdline", pid)
	data, err := os.ReadFile(path)
	if err != nil {
		return ""
	}
	// cmdline is null-separated
	cmd := strings.ReplaceAll(string(data), "\x00", " ")
	return strings.TrimSpace(cmd)
}

// readProcStat reads /proc/<pid>/stat and returns (utime, stime, starttime)
func readProcStat(pid int) (utime, stime, starttime uint64) {
	path := fmt.Sprintf("/proc/%d/stat", pid)
	data, err := os.ReadFile(path)
	if err != nil {
		return
	}
	// The second field may contain spaces if process name has spaces; it's wrapped in ()
	s := string(data)
	end := strings.LastIndex(s, ")")
	if end < 0 {
		return
	}
	fields := strings.Fields(s[end+1:])
	if len(fields) >= 13 {
		utime, _ = strconv.ParseUint(fields[11], 10, 64)
		stime, _ = strconv.ParseUint(fields[12], 10, 64)
		starttime, _ = strconv.ParseUint(fields[19], 10, 64)
	}
	return
}

func getClockTick() uint64 {
	return 100 // typical Linux HZ
}

func getMemTotalKB() uint64 {
	f, err := os.Open("/proc/meminfo")
	if err != nil {
		return 1
	}
	defer f.Close()
	buf, _ := io.ReadAll(f)
	for _, line := range strings.Split(string(buf), "\n") {
		if strings.HasPrefix(line, "MemTotal:") {
			fields := strings.Fields(line)
			if len(fields) >= 2 {
				v, _ := strconv.ParseUint(fields[1], 10, 64)
				return v
			}
		}
	}
	return 1
}

func listProcesses() []ProcessInfo {
	entries, err := filepath.Glob("/proc/[0-9]*")
	if err != nil {
		return nil
	}

	memTotalKB := getMemTotalKB()
	uptimeF, _ := readUptime()
	clkTck := getClockTick()

	var procs []ProcessInfo
	for _, entry := range entries {
		base := filepath.Base(entry)
		pid, err := strconv.Atoi(base)
		if err != nil {
			continue
		}
		name, user, status, vmRSS := readProcStatus(pid)
		if name == "" {
			continue
		}
		cmd := readCmdline(pid)

		utime, stime, starttime := readProcStat(pid)
		totalTime := utime + stime
		elapsedSec := uint64(uptimeF) - starttime/clkTck
		var cpuPct float64
		if elapsedSec > 0 {
			cpuPct = float64(totalTime) / float64(clkTck) / float64(elapsedSec) * 100.0
		}

		var memPct float64
		if memTotalKB > 0 {
			memPct = float64(vmRSS) / float64(memTotalKB*1024) * 100.0
		}

		procs = append(procs, ProcessInfo{
			PID:         pid,
			Name:        name,
			User:        user,
			CPUPercent:  cpuPct,
			MemPercent:  memPct,
			MemRSSBytes: vmRSS,
			Status:      status,
			Command:     cmd,
		})
	}
	return procs
}

func ProcessesHandler(w http.ResponseWriter, r *http.Request) {
	sortBy := r.URL.Query().Get("sort")
	limitStr := r.URL.Query().Get("limit")
	limit := 50
	if limitStr != "" {
		if v, err := strconv.Atoi(limitStr); err == nil && v > 0 {
			limit = v
		}
	}

	procs := listProcesses()
	total := len(procs)

	switch sortBy {
	case "mem":
		sort.Slice(procs, func(i, j int) bool { return procs[i].MemPercent > procs[j].MemPercent })
	case "pid":
		sort.Slice(procs, func(i, j int) bool { return procs[i].PID < procs[j].PID })
	case "name":
		sort.Slice(procs, func(i, j int) bool { return procs[i].Name < procs[j].Name })
	default: // cpu
		sort.Slice(procs, func(i, j int) bool { return procs[i].CPUPercent > procs[j].CPUPercent })
	}

	if limit < len(procs) {
		procs = procs[:limit]
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(ProcessResponse{Processes: procs, Total: total})
}

func KillProcessHandler(w http.ResponseWriter, r *http.Request) {
	// Extract PID from URL path: /api/v1/process/{pid}
	parts := strings.Split(r.URL.Path, "/")
	pidStr := parts[len(parts)-1]
	pid, err := strconv.Atoi(pidStr)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(KillResponse{Success: false, Message: "invalid pid"})
		return
	}

	sigStr := r.URL.Query().Get("signal")
	sig := 15 // SIGTERM default
	if sigStr != "" {
		if v, err2 := strconv.Atoi(sigStr); err2 == nil {
			sig = v
		}
	}

	proc, err := os.FindProcess(pid)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(KillResponse{Success: false, Message: fmt.Sprintf("process %d not found", pid)})
		return
	}

	var sendErr error
	switch sig {
	case 9:
		sendErr = proc.Kill()
	default:
		sendErr = proc.Signal(os.Interrupt)
	}

	w.Header().Set("Content-Type", "application/json")
	if sendErr != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(KillResponse{Success: false, Message: sendErr.Error()})
		return
	}
	json.NewEncoder(w).Encode(KillResponse{Success: true, Message: fmt.Sprintf("Process %d killed", pid)})
}
