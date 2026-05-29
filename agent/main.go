package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/servercontrol/agent/handlers"
	"github.com/servercontrol/agent/middleware"
)

const version = "1.0.0"

type HealthResponse struct {
	Status  string `json:"status"`
	Version string `json:"version"`
}

func main() {
	cfg := loadConfig()

	log.SetOutput(os.Stdout)
	log.SetFlags(log.LstdFlags | log.Lmsgprefix)
	log.SetPrefix("[servercontrol-agent] ")
	log.Printf("Starting agent version %s on port %s", version, cfg.Port)
	log.Printf("Config: %s", cfg.String())

	mux := http.NewServeMux()

	// Health endpoint — no auth
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(HealthResponse{Status: "ok", Version: version})
	})

	// Auth-protected API router
	apiMux := http.NewServeMux()
	apiMux.HandleFunc("/api/v1/stats", handlers.StatsHandler)
	apiMux.HandleFunc("/api/v1/processes", handlers.ProcessesHandler)
	apiMux.HandleFunc("/api/v1/disk", handlers.DiskHandler)
	apiMux.HandleFunc("/api/v1/connections", handlers.ConnectionsHandler)
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

	// Wrap API with auth + CORS + logging
	protectedAPI := middleware.Chain(apiMux,
		middleware.CORS,
		middleware.BearerAuth(cfg.Token),
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

	srv := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      middleware.CORS(mux),
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 60 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	// Graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		var err error
		if cfg.TLSCert != "" && cfg.TLSKey != "" {
			log.Printf("TLS enabled (cert=%s)", cfg.TLSCert)
			err = srv.ListenAndServeTLS(cfg.TLSCert, cfg.TLSKey)
		} else {
			err = srv.ListenAndServe()
		}
		if err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server error: %v", err)
		}
	}()

	log.Printf("Agent listening on :%s", cfg.Port)
	<-quit
	log.Println("Shutting down...")

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("Forced shutdown: %v", err)
	}
	log.Println("Agent stopped")
}
