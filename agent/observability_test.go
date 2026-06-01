package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestHealthHandler(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rec := httptest.NewRecorder()

	healthHandler(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
	if ct := rec.Header().Get("Content-Type"); ct != "application/json" {
		t.Errorf("Content-Type = %q, want application/json", ct)
	}

	var resp HealthResponse
	if err := json.Unmarshal(rec.Body.Bytes(), &resp); err != nil {
		t.Fatalf("response is not valid JSON: %v", err)
	}
	if resp.Status != "ok" {
		t.Errorf("status = %q, want ok", resp.Status)
	}
	if resp.Version != version {
		t.Errorf("version = %q, want %q", resp.Version, version)
	}
	if resp.GoVersion == "" {
		t.Errorf("go_version should not be empty")
	}
	if resp.NumCPU < 1 {
		t.Errorf("num_cpu = %d, want >= 1", resp.NumCPU)
	}
	if resp.NumGoroutine < 1 {
		t.Errorf("num_goroutine = %d, want >= 1", resp.NumGoroutine)
	}
}

func TestMetricsHandler(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/metrics", nil)
	rec := httptest.NewRecorder()

	metricsHandler(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
	if ct := rec.Header().Get("Content-Type"); !strings.HasPrefix(ct, "text/plain") {
		t.Errorf("Content-Type = %q, want text/plain...", ct)
	}

	body := rec.Body.String()
	wantSubstrings := []string{
		"servercontrol_agent_info{",
		"servercontrol_agent_uptime_seconds ",
		"servercontrol_agent_goroutines ",
		"servercontrol_agent_memory_alloc_bytes ",
		"# TYPE servercontrol_agent_uptime_seconds gauge",
	}
	for _, s := range wantSubstrings {
		if !strings.Contains(body, s) {
			t.Errorf("metrics output missing %q\n---\n%s", s, body)
		}
	}
}

func TestParseBool(t *testing.T) {
	truthy := []string{"true", "TRUE", "1", "yes", "On", " true "}
	for _, v := range truthy {
		if !parseBool(v) {
			t.Errorf("parseBool(%q) = false, want true", v)
		}
	}
	falsy := []string{"false", "0", "no", "off", "", "nope"}
	for _, v := range falsy {
		if parseBool(v) {
			t.Errorf("parseBool(%q) = true, want false", v)
		}
	}
}
