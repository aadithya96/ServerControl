package main

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"os"
	"strings"
)

type Config struct {
	Port     string
	Token    string
	TLSCert  string
	TLSKey   string
	LogLevel string
}

func loadConfig() *Config {
	cfg := &Config{}

	// Load from config file first
	fileConfig := loadConfigFile("/etc/servercontrol/agent.conf")

	// CLI flags (highest priority)
	flag.StringVar(&cfg.Port, "port", "", "Port to listen on")
	flag.StringVar(&cfg.Token, "token", "", "Bearer token for authentication")
	flag.StringVar(&cfg.TLSCert, "tls-cert", "", "Path to TLS certificate")
	flag.StringVar(&cfg.TLSKey, "tls-key", "", "Path to TLS key")
	flag.StringVar(&cfg.LogLevel, "log-level", "", "Log level (debug, info, warn, error)")
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

	if cfg.Token == "" {
		log.Fatal("ERROR: No auth token configured. Set --token, SC_TOKEN env var, or TOKEN= in /etc/servercontrol/agent.conf")
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
	return fmt.Sprintf("port=%s logLevel=%s tlsCert=%s", c.Port, c.LogLevel, c.TLSCert)
}
