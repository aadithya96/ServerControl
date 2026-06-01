package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"runtime"
	"time"
)

// startTime records process start so /health and /metrics can report uptime.
var startTime = time.Now()

// HealthResponse is the JSON returned by the unauthenticated /health endpoint.
// It is intentionally lightweight but includes enough runtime detail to be
// useful behind a reverse proxy / for liveness probes.
type HealthResponse struct {
	Status        string `json:"status"`
	Version       string `json:"version"`
	UptimeSeconds int64  `json:"uptime_seconds"`
	GoVersion     string `json:"go_version"`
	NumGoroutine  int    `json:"num_goroutine"`
	NumCPU        int    `json:"num_cpu"`
	AllocBytes    uint64 `json:"alloc_bytes"`
	SysBytes      uint64 `json:"sys_bytes"`
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	var mem runtime.MemStats
	runtime.ReadMemStats(&mem)

	resp := HealthResponse{
		Status:        "ok",
		Version:       version,
		UptimeSeconds: int64(time.Since(startTime).Seconds()),
		GoVersion:     runtime.Version(),
		NumGoroutine:  runtime.NumGoroutine(),
		NumCPU:        runtime.NumCPU(),
		AllocBytes:    mem.Alloc,
		SysBytes:      mem.Sys,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}

// metricsHandler exposes a minimal Prometheus text-format exposition. It is
// dependency-free (no client_golang) and gated behind cfg.MetricsEnabled so it
// is off by default. Intended to be scraped on the same port as /health.
func metricsHandler(w http.ResponseWriter, r *http.Request) {
	var mem runtime.MemStats
	runtime.ReadMemStats(&mem)

	w.Header().Set("Content-Type", "text/plain; version=0.0.4; charset=utf-8")

	fmt.Fprintf(w, "# HELP servercontrol_agent_info Agent build information.\n")
	fmt.Fprintf(w, "# TYPE servercontrol_agent_info gauge\n")
	fmt.Fprintf(w, "servercontrol_agent_info{version=%q,go_version=%q} 1\n", version, runtime.Version())

	fmt.Fprintf(w, "# HELP servercontrol_agent_uptime_seconds Time since the agent started, in seconds.\n")
	fmt.Fprintf(w, "# TYPE servercontrol_agent_uptime_seconds gauge\n")
	fmt.Fprintf(w, "servercontrol_agent_uptime_seconds %d\n", int64(time.Since(startTime).Seconds()))

	fmt.Fprintf(w, "# HELP servercontrol_agent_goroutines Number of currently running goroutines.\n")
	fmt.Fprintf(w, "# TYPE servercontrol_agent_goroutines gauge\n")
	fmt.Fprintf(w, "servercontrol_agent_goroutines %d\n", runtime.NumGoroutine())

	fmt.Fprintf(w, "# HELP servercontrol_agent_memory_alloc_bytes Bytes of allocated heap objects.\n")
	fmt.Fprintf(w, "# TYPE servercontrol_agent_memory_alloc_bytes gauge\n")
	fmt.Fprintf(w, "servercontrol_agent_memory_alloc_bytes %d\n", mem.Alloc)

	fmt.Fprintf(w, "# HELP servercontrol_agent_memory_sys_bytes Total bytes of memory obtained from the OS.\n")
	fmt.Fprintf(w, "# TYPE servercontrol_agent_memory_sys_bytes gauge\n")
	fmt.Fprintf(w, "servercontrol_agent_memory_sys_bytes %d\n", mem.Sys)
}
