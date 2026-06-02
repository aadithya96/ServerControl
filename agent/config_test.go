package main

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoadReloadableConfigFromFile(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "agent.conf")
	content := "# comment\nTOKEN=\"file-token\"\nLOG_LEVEL=debug\nPORT=1234\n"
	if err := os.WriteFile(path, []byte(content), 0o600); err != nil {
		t.Fatalf("write temp config: %v", err)
	}

	// Ensure env does not override the file for this test.
	t.Setenv("SC_TOKEN", "")
	t.Setenv("SC_LOG_LEVEL", "")

	rc := loadReloadableConfig(path)
	if rc.Token != "file-token" {
		t.Errorf("Token = %q, want file-token", rc.Token)
	}
	if rc.LogLevel != "debug" {
		t.Errorf("LogLevel = %q, want debug", rc.LogLevel)
	}
}

func TestLoadReloadableConfigEnvOverridesFile(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "agent.conf")
	if err := os.WriteFile(path, []byte("TOKEN=file-token\n"), 0o600); err != nil {
		t.Fatalf("write temp config: %v", err)
	}

	t.Setenv("SC_TOKEN", "env-token")

	rc := loadReloadableConfig(path)
	if rc.Token != "env-token" {
		t.Errorf("Token = %q, want env-token (env should win over file)", rc.Token)
	}
}

func TestLoadReloadableConfigMissingFileDefaults(t *testing.T) {
	t.Setenv("SC_TOKEN", "")
	t.Setenv("SC_LOG_LEVEL", "")

	rc := loadReloadableConfig(filepath.Join(t.TempDir(), "does-not-exist.conf"))
	if rc.Token != "" {
		t.Errorf("Token = %q, want empty when no source provides it", rc.Token)
	}
	if rc.LogLevel != "info" {
		t.Errorf("LogLevel = %q, want default info", rc.LogLevel)
	}
}
