package main

import (
	"log/slog"
	"os"
	"strings"
)

// setupLogger configures slog with a JSON handler at the given level and installs
// it as the process default. Every line includes the service name and version so
// logs are easy to filter when the agent runs behind a reverse proxy / in a
// shared log pipeline.
func setupLogger(level string) *slog.Logger {
	var lvl slog.Level
	switch strings.ToLower(strings.TrimSpace(level)) {
	case "debug":
		lvl = slog.LevelDebug
	case "warn", "warning":
		lvl = slog.LevelWarn
	case "error":
		lvl = slog.LevelError
	default:
		lvl = slog.LevelInfo
	}

	handler := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: lvl})
	logger := slog.New(handler).With(
		"service", "servercontrol-agent",
		"version", version,
	)
	slog.SetDefault(logger)
	return logger
}
