package main

import (
	"context"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"sync/atomic"
	"syscall"
	"time"

	"github.com/aadithya96/ServerControl/agent/handlers"
	"github.com/aadithya96/ServerControl/agent/middleware"
)

const version = "1.0.0"

func main() {
	// Preliminary logger so config errors are emitted as structured JSON too.
	setupLogger("info")
	cfg := loadConfig()
	setupLogger(cfg.LogLevel)

	slog.Info("starting agent", "port", cfg.Port, "log_level", cfg.LogLevel)
	slog.Info("config loaded",
		"metrics_enabled", cfg.MetricsEnabled,
		"rate_limit_per_sec", cfg.RateLimitPerSec,
		"tls", cfg.TLSCert != "",
	)

	mux := http.NewServeMux()

	// Health endpoint — no auth
	mux.HandleFunc("/health", healthHandler)

	// Optional Prometheus metrics endpoint — no auth, off by default
	if cfg.MetricsEnabled {
		mux.HandleFunc("/metrics", metricsHandler)
		slog.Info("prometheus metrics endpoint enabled", "path", "/metrics")
	}

	// Auth-protected API router
	apiMux := http.NewServeMux()
	apiMux.HandleFunc("/api/v1/stats", handlers.StatsHandler)
	apiMux.HandleFunc("/api/v1/processes", handlers.ProcessesHandler)
	apiMux.HandleFunc("/api/v1/disk", handlers.DiskHandler)
	apiMux.HandleFunc("/api/v1/connections", handlers.ConnectionsHandler)
	apiMux.HandleFunc("/api/v1/bandwidth", handlers.BandwidthHandler)
	// Services endpoints
	apiMux.HandleFunc("/api/v1/services/", func(w http.ResponseWriter, r *http.Request) {
		path := r.URL.Path
		// /api/v1/services/{name}/action  -> POST
		// /api/v1/services/{name}/logs    -> GET
		trimmed := strings.TrimPrefix(path, "/api/v1/services/")
		parts := strings.Split(trimmed, "/")
		if len(parts) == 2 && parts[1] == "action" {
			if r.Method != http.MethodPost {
				http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
				return
			}
			handlers.ServiceActionHandler(w, r)
		} else if len(parts) == 2 && parts[1] == "logs" {
			if r.Method != http.MethodGet {
				http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
				return
			}
			handlers.ServiceLogsHandler(w, r)
		} else {
			http.NotFound(w, r)
		}
	})
	apiMux.HandleFunc("/api/v1/services", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
			return
		}
		handlers.ServicesHandler(w, r)
	})
	// Logs endpoint
	apiMux.HandleFunc("/api/v1/logs", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
			return
		}
		handlers.LogsHandler(w, r)
	})
	apiMux.HandleFunc("/api/v1/firewall/toggle", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
			return
		}
		handlers.FirewallToggleHandler(w, r)
	})
	apiMux.HandleFunc("/api/v1/firewall", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
			return
		}
		handlers.FirewallHandler(w, r)
	})
	apiMux.HandleFunc("/api/v1/process/", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodDelete {
			http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
			return
		}
		// Ensure there's a PID in the path
		path := r.URL.Path
		trimmed := strings.TrimPrefix(path, "/api/v1/process/")
		if trimmed == "" || strings.Contains(trimmed, "/") {
			http.Error(w, `{"error":"invalid path"}`, http.StatusBadRequest)
			return
		}
		handlers.KillProcessHandler(w, r)
	})

	// Docker endpoints
	apiMux.HandleFunc("/api/v1/docker/containers/", func(w http.ResponseWriter, r *http.Request) {
		if strings.HasSuffix(r.URL.Path, "/action") {
			handlers.DockerContainerActionHandler(w, r)
		} else if strings.HasSuffix(r.URL.Path, "/logs") {
			handlers.DockerContainerLogsHandler(w, r)
		} else {
			http.NotFound(w, r)
		}
	})
	apiMux.HandleFunc("/api/v1/docker/containers", handlers.DockerContainersHandler)
	apiMux.HandleFunc("/api/v1/docker/images", handlers.DockerImagesHandler)

	// Exec endpoint
	apiMux.HandleFunc("/api/v1/exec", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
			return
		}
		handlers.ExecHandler(w, r)
	})

	// Security endpoints
	apiMux.HandleFunc("/api/v1/security/failed-logins", handlers.FailedLoginsHandler)
	apiMux.HandleFunc("/api/v1/security/ssl", handlers.SslCertHandler)
	apiMux.HandleFunc("/api/v1/security/block-ip", handlers.BlockIpHandler)

	// Self-update endpoint — opt-in, off by default (remote-code-update surface).
	if cfg.SelfUpdateEnabled {
		updateHandler := selfUpdateHandler(cfg.SelfUpdateURL)
		apiMux.HandleFunc("/api/v1/agent/update", func(w http.ResponseWriter, r *http.Request) {
			if r.Method != http.MethodPost {
				http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
				return
			}
			updateHandler(w, r)
		})
		slog.Warn("self-update endpoint enabled", "url", cfg.SelfUpdateURL)
	}

	// Token is held atomically so it can be hot-swapped on SIGHUP.
	var tokenHolder atomic.Pointer[string]
	tokenHolder.Store(&cfg.Token)

	// Wrap API with auth + CORS + logging
	protectedAPI := middleware.Chain(apiMux,
		middleware.CORS,
		middleware.BearerAuthFunc(func() string { return *tokenHolder.Load() }),
		middleware.Logging,
	)

	// Main mux: health is unprotected, everything else goes through auth
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if strings.HasPrefix(r.URL.Path, "/api/") {
			protectedAPI.ServeHTTP(w, r)
			return
		}
		http.NotFound(w, r)
	})

	// Outermost handler: CORS for all requests, plus optional per-IP rate limiting.
	var rootHandler http.Handler = mux
	if cfg.RateLimitPerSec > 0 {
		rate := float64(cfg.RateLimitPerSec)
		// Allow a short burst of 2x the sustained rate to absorb screen-load spikes.
		limiter := middleware.NewRateLimiter(rate, rate*2)
		limiter.StartCleanup(5*time.Minute, 10*time.Minute)
		rootHandler = limiter.Middleware(rootHandler)
		slog.Info("rate limiting enabled", "req_per_sec", cfg.RateLimitPerSec)
	}

	srv := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      middleware.CORS(rootHandler),
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 60 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	// Graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	// Graceful config reload on SIGHUP: re-read the token and log level from the
	// config file/env and hot-apply them without restarting the listener.
	hup := make(chan os.Signal, 1)
	signal.Notify(hup, syscall.SIGHUP)
	go func() {
		for range hup {
			rc := loadReloadableConfig(configFilePath)
			setupLogger(rc.LogLevel)
			tokenChanged := false
			if rc.Token != "" && rc.Token != *tokenHolder.Load() {
				tokenHolder.Store(&rc.Token)
				tokenChanged = true
			}
			slog.Info("config reloaded via SIGHUP",
				"log_level", rc.LogLevel,
				"token_changed", tokenChanged,
			)
		}
	}()

	go func() {
		var err error
		if cfg.TLSCert != "" && cfg.TLSKey != "" {
			slog.Info("TLS enabled", "cert", cfg.TLSCert)
			err = srv.ListenAndServeTLS(cfg.TLSCert, cfg.TLSKey)
		} else {
			err = srv.ListenAndServe()
		}
		if err != nil && err != http.ErrServerClosed {
			slog.Error("server error", "error", err)
			os.Exit(1)
		}
	}()

	slog.Info("agent listening", "addr", ":"+cfg.Port)
	<-quit
	slog.Info("shutting down")

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		slog.Error("forced shutdown", "error", err)
		os.Exit(1)
	}
	slog.Info("agent stopped")
}
