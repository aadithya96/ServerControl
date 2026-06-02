package main

import (
	"bufio"
	"flag"
	"fmt"
	"log/slog"
	"os"
	"strconv"
	"strings"
)

type Config struct {
	Port            string
	Token           string
	TLSCert         string
	TLSKey          string
	LogLevel        string
	MetricsEnabled  bool
	RateLimitPerSec int
}

const configFilePath = "/etc/servercontrol/agent.conf"

func loadConfig() *Config {
	cfg := &Config{}

	// Load from config file first
	fileConfig := loadConfigFile(configFilePath)

	// CLI flags (highest priority)
	flag.StringVar(&cfg.Port, "port", "", "Port to listen on")
	flag.StringVar(&cfg.Token, "token", "", "Bearer token for authentication")
	flag.StringVar(&cfg.TLSCert, "tls-cert", "", "Path to TLS certificate")
	flag.StringVar(&cfg.TLSKey, "tls-key", "", "Path to TLS key")
	flag.StringVar(&cfg.LogLevel, "log-level", "", "Log level (debug, info, warn, error)")
	var metricsFlag string
	flag.StringVar(&metricsFlag, "metrics", "", "Enable Prometheus /metrics endpoint (true/false)")
	var rateLimitFlag string
	flag.StringVar(&rateLimitFlag, "rate-limit", "", "Max requests/sec per client IP (0 disables)")
	flag.Parse()

	// Apply defaults from file, then env, then CLI
	if cfg.Port == "" {
		cfg.Port = getEnvOrFileOrDefault("SC_PORT", fileConfig["PORT"], "9876")
	}
	if cfg.Token == "" {
		cfg.Token = getEnvOrFileOrDefault("SC_TOKEN", fileConfig["TOKEN"], "")
	}
	if cfg.TLSCert == "" {
		cfg.TLSCert = getEnvOrFileOrDefault("SC_TLS_CERT", fileConfig["TLS_CERT"], "")
	}
	if cfg.TLSKey == "" {
		cfg.TLSKey = getEnvOrFileOrDefault("SC_TLS_KEY", fileConfig["TLS_KEY"], "")
	}
	if cfg.LogLevel == "" {
		cfg.LogLevel = getEnvOrFileOrDefault("SC_LOG_LEVEL", fileConfig["LOG_LEVEL"], "info")
	}
	if metricsFlag == "" {
		metricsFlag = getEnvOrFileOrDefault("SC_METRICS", fileConfig["METRICS"], "false")
	}
	cfg.MetricsEnabled = parseBool(metricsFlag)
	if rateLimitFlag == "" {
		rateLimitFlag = getEnvOrFileOrDefault("SC_RATE_LIMIT", fileConfig["RATE_LIMIT"], "30")
	}
	cfg.RateLimitPerSec = parseIntOrDefault(rateLimitFlag, 30)

	if cfg.Token == "" {
		slog.Error("no auth token configured; set --token, SC_TOKEN env var, or TOKEN= in /etc/servercontrol/agent.conf")
		os.Exit(1)
	}

	return cfg
}

func loadConfigFile(path string) map[string]string {
	result := make(map[string]string)
	f, err := os.Open(path)
	if err != nil {
		return result
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		parts := strings.SplitN(line, "=", 2)
		if len(parts) == 2 {
			key := strings.TrimSpace(parts[0])
			val := strings.TrimSpace(parts[1])
			// Strip quotes
			if len(val) >= 2 && ((val[0] == '"' && val[len(val)-1] == '"') ||
				(val[0] == '\'' && val[len(val)-1] == '\'')) {
				val = val[1 : len(val)-1]
			}
			result[key] = val
		}
	}
	return result
}

// ReloadableConfig holds the subset of configuration that can be hot-applied on
// SIGHUP without restarting the HTTP listener.
type ReloadableConfig struct {
	Token    string
	LogLevel string
}

// loadReloadableConfig re-reads the hot-reloadable fields from the given config
// file path and environment. CLI flags are process-lifetime and intentionally
// not re-evaluated here. An empty Token means "no token found in file/env" and
// callers should preserve the existing one rather than clearing it.
func loadReloadableConfig(path string) ReloadableConfig {
	fileConfig := loadConfigFile(path)
	return ReloadableConfig{
		Token:    getEnvOrFileOrDefault("SC_TOKEN", fileConfig["TOKEN"], ""),
		LogLevel: getEnvOrFileOrDefault("SC_LOG_LEVEL", fileConfig["LOG_LEVEL"], "info"),
	}
}

// parseIntOrDefault parses v as an int, returning def if it is empty or invalid.
func parseIntOrDefault(v string, def int) int {
	if n, err := strconv.Atoi(strings.TrimSpace(v)); err == nil {
		return n
	}
	return def
}

// parseBool interprets common truthy strings ("true", "1", "yes", "on") case-insensitively.
func parseBool(v string) bool {
	switch strings.ToLower(strings.TrimSpace(v)) {
	case "true", "1", "yes", "on":
		return true
	default:
		return false
	}
}

func getEnvOrFileOrDefault(envKey, fileVal, defaultVal string) string {
	if v := os.Getenv(envKey); v != "" {
		return v
	}
	if fileVal != "" {
		return fileVal
	}
	return defaultVal
}

func (c *Config) String() string {
	return fmt.Sprintf("port=%s logLevel=%s tlsCert=%s metrics=%t rateLimit=%d", c.Port, c.LogLevel, c.TLSCert, c.MetricsEnabled, c.RateLimitPerSec)
}
