package middleware

import (
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
)

func doAuthReq(t *testing.T, h http.Handler, token string) int {
	t.Helper()
	req := httptest.NewRequest(http.MethodGet, "/api/v1/stats", nil)
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	return rec.Code
}

func okHandler() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})
}

func TestBearerAuthStatic(t *testing.T) {
	h := BearerAuth("secret")(okHandler())

	if code := doAuthReq(t, h, "secret"); code != http.StatusOK {
		t.Errorf("valid token: got %d, want 200", code)
	}
	if code := doAuthReq(t, h, "wrong"); code != http.StatusUnauthorized {
		t.Errorf("wrong token: got %d, want 401", code)
	}
	if code := doAuthReq(t, h, ""); code != http.StatusUnauthorized {
		t.Errorf("missing header: got %d, want 401", code)
	}
}

func TestBearerAuthFuncHotReload(t *testing.T) {
	var holder atomic.Pointer[string]
	initial := "old-token"
	holder.Store(&initial)

	h := BearerAuthFunc(func() string { return *holder.Load() })(okHandler())

	if code := doAuthReq(t, h, "old-token"); code != http.StatusOK {
		t.Fatalf("old token should be accepted, got %d", code)
	}

	// Simulate a SIGHUP reload swapping the token.
	updated := "new-token"
	holder.Store(&updated)

	if code := doAuthReq(t, h, "old-token"); code != http.StatusUnauthorized {
		t.Errorf("old token should be rejected after reload, got %d", code)
	}
	if code := doAuthReq(t, h, "new-token"); code != http.StatusOK {
		t.Errorf("new token should be accepted after reload, got %d", code)
	}
}
