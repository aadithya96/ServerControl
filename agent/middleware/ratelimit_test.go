package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestRateLimiterAllowsBurstThenBlocks(t *testing.T) {
	rl := NewRateLimiter(1, 3) // 1/s sustained, burst of 3
	now := time.Now()

	// First 3 requests (the burst) should pass with no time advance.
	for i := 0; i < 3; i++ {
		if !rl.allow("1.2.3.4", now) {
			t.Fatalf("request %d in burst was unexpectedly blocked", i+1)
		}
	}
	// 4th immediate request must be blocked.
	if rl.allow("1.2.3.4", now) {
		t.Errorf("4th request should have been rate-limited")
	}
}

func TestRateLimiterRefills(t *testing.T) {
	rl := NewRateLimiter(2, 2) // 2 tokens/sec
	now := time.Now()

	// Drain the bucket.
	rl.allow("ip", now)
	rl.allow("ip", now)
	if rl.allow("ip", now) {
		t.Fatalf("bucket should be empty")
	}

	// After 1 second, ~2 tokens refill.
	later := now.Add(1 * time.Second)
	if !rl.allow("ip", later) {
		t.Errorf("expected a token to be available after refill")
	}
}

func TestRateLimiterPerIPIndependent(t *testing.T) {
	rl := NewRateLimiter(1, 1)
	now := time.Now()

	if !rl.allow("a", now) {
		t.Fatalf("first request for a should pass")
	}
	// Different IP must have its own bucket.
	if !rl.allow("b", now) {
		t.Errorf("first request for b should pass independently of a")
	}
}

func TestRateLimiterMiddleware429(t *testing.T) {
	rl := NewRateLimiter(1, 1)
	handler := rl.Middleware(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	newReq := func() *http.Request {
		req := httptest.NewRequest(http.MethodGet, "/health", nil)
		req.RemoteAddr = "10.0.0.1:5555"
		return req
	}

	rec1 := httptest.NewRecorder()
	handler.ServeHTTP(rec1, newReq())
	if rec1.Code != http.StatusOK {
		t.Fatalf("first request: got %d, want 200", rec1.Code)
	}

	rec2 := httptest.NewRecorder()
	handler.ServeHTTP(rec2, newReq())
	if rec2.Code != http.StatusTooManyRequests {
		t.Errorf("second request: got %d, want 429", rec2.Code)
	}
	if rec2.Header().Get("Retry-After") == "" {
		t.Errorf("429 response should include Retry-After header")
	}
}

func TestClientIP(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.RemoteAddr = "192.168.1.50:40000"
	if got := clientIP(req); got != "192.168.1.50" {
		t.Errorf("clientIP = %q, want 192.168.1.50", got)
	}

	// X-Forwarded-For must NOT be trusted.
	req.Header.Set("X-Forwarded-For", "9.9.9.9")
	if got := clientIP(req); got != "192.168.1.50" {
		t.Errorf("clientIP should ignore X-Forwarded-For, got %q", got)
	}
}
