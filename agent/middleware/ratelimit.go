package middleware

import (
	"net"
	"net/http"
	"sync"
	"time"
)

// RateLimiter is a simple per-client-IP token-bucket limiter. It is intended to
// protect the agent against accidental request floods (e.g. a misbehaving app
// polling loop), not as a security control.
type RateLimiter struct {
	mu       sync.Mutex
	buckets  map[string]*tokenBucket
	rate     float64 // tokens added per second
	capacity float64 // maximum burst size
}

type tokenBucket struct {
	tokens   float64
	lastSeen time.Time
}

// NewRateLimiter creates a limiter allowing ratePerSec sustained requests with a
// burst of up to capacity, per client IP.
func NewRateLimiter(ratePerSec, capacity float64) *RateLimiter {
	return &RateLimiter{
		buckets:  make(map[string]*tokenBucket),
		rate:     ratePerSec,
		capacity: capacity,
	}
}

// allow reports whether a request from key is permitted at time now, consuming a
// token if so. Exposed (lowercase) for testing within the package.
func (rl *RateLimiter) allow(key string, now time.Time) bool {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	b, ok := rl.buckets[key]
	if !ok {
		rl.buckets[key] = &tokenBucket{tokens: rl.capacity - 1, lastSeen: now}
		return true
	}

	// Refill based on elapsed time, capped at capacity.
	elapsed := now.Sub(b.lastSeen).Seconds()
	b.tokens = min(rl.capacity, b.tokens+elapsed*rl.rate)
	b.lastSeen = now

	if b.tokens >= 1 {
		b.tokens--
		return true
	}
	return false
}

// Middleware wraps a handler, returning 429 when a client exceeds its rate.
func (rl *RateLimiter) Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if !rl.allow(clientIP(r), time.Now()) {
			w.Header().Set("Retry-After", "1")
			http.Error(w, `{"error":"rate limit exceeded"}`, http.StatusTooManyRequests)
			return
		}
		next.ServeHTTP(w, r)
	})
}

// StartCleanup launches a background goroutine that evicts buckets idle for
// longer than maxIdle, so the map does not grow unbounded. Runs until the
// process exits.
func (rl *RateLimiter) StartCleanup(interval, maxIdle time.Duration) {
	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()
		for range ticker.C {
			cutoff := time.Now().Add(-maxIdle)
			rl.mu.Lock()
			for key, b := range rl.buckets {
				if b.lastSeen.Before(cutoff) {
					delete(rl.buckets, key)
				}
			}
			rl.mu.Unlock()
		}
	}()
}

// clientIP extracts the remote IP from a request, falling back to RemoteAddr
// verbatim if it has no port. We intentionally do not trust X-Forwarded-For, as
// it is client-controllable and would let callers evade the limiter.
func clientIP(r *http.Request) string {
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}
