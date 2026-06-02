package main

import (
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"syscall"
	"time"
)

// maxUpdateBytes caps the size of a downloaded artifact to avoid filling the disk.
const maxUpdateBytes = 100 << 20 // 100 MiB

type updateResponse struct {
	Success     bool   `json:"success"`
	Message     string `json:"message"`
	FromVersion string `json:"from_version"`
	Asset       string `json:"asset,omitempty"`
}

// assetName maps the running platform to the release asset filename produced by
// the release workflow.
func assetName() (string, error) {
	if runtime.GOOS != "linux" {
		return "", fmt.Errorf("unsupported OS %q (agent releases are linux-only)", runtime.GOOS)
	}
	switch runtime.GOARCH {
	case "amd64":
		return "servercontrol-agent-linux-amd64", nil
	case "arm64":
		return "servercontrol-agent-linux-arm64", nil
	case "arm":
		return "servercontrol-agent-linux-armv7", nil
	default:
		return "", fmt.Errorf("unsupported architecture %q", runtime.GOARCH)
	}
}

// parseChecksums parses `sha256sum` output ("<hex>  <name>", name may be
// prefixed with '*' in binary mode) into a name->hex map.
func parseChecksums(data string) map[string]string {
	result := make(map[string]string)
	for _, line := range strings.Split(data, "\n") {
		fields := strings.Fields(line)
		if len(fields) >= 2 {
			name := strings.TrimPrefix(fields[len(fields)-1], "*")
			result[name] = strings.ToLower(fields[0])
		}
	}
	return result
}

func httpGetLimited(client *http.Client, url string) ([]byte, error) {
	resp, err := client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("GET %s: status %d", url, resp.StatusCode)
	}
	return io.ReadAll(io.LimitReader(resp.Body, maxUpdateBytes))
}

// downloadVerifiedBinary downloads SHA256SUMS.txt and the named asset from base,
// verifies the asset's SHA-256 against the checksums file, and returns the
// verified bytes plus the expected checksum. Split out from the HTTP handler so
// it can be unit-tested without performing a binary swap.
func downloadVerifiedBinary(client *http.Client, base, asset string) (data []byte, checksum string, err error) {
	base = strings.TrimRight(base, "/")

	sumsRaw, err := httpGetLimited(client, base+"/SHA256SUMS.txt")
	if err != nil {
		return nil, "", fmt.Errorf("download checksums: %w", err)
	}
	sums := parseChecksums(string(sumsRaw))
	want, ok := sums[asset]
	if !ok {
		return nil, "", fmt.Errorf("checksums file has no entry for %s", asset)
	}

	bin, err := httpGetLimited(client, base+"/"+asset)
	if err != nil {
		return nil, "", fmt.Errorf("download binary: %w", err)
	}

	got := fmt.Sprintf("%x", sha256.Sum256(bin))
	if got != want {
		return nil, "", fmt.Errorf("checksum mismatch: want %s, got %s", want, got)
	}
	return bin, want, nil
}

// selfUpdateHandler returns a handler that downloads, verifies, and installs the
// latest agent release, then re-execs in place. It is gated behind config (off
// by default) and bearer auth, requires an HTTPS source, and never installs a
// binary whose checksum does not match the published SHA256SUMS.txt.
func selfUpdateHandler(baseURL string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		writeErr := func(code int, msg string) {
			w.WriteHeader(code)
			json.NewEncoder(w).Encode(updateResponse{Success: false, Message: msg, FromVersion: version})
		}

		// Fail closed on insecure sources — same-channel checksums over plain
		// HTTP give no MITM protection.
		if !strings.HasPrefix(strings.ToLower(baseURL), "https://") {
			writeErr(http.StatusPreconditionFailed, "self-update requires an https update URL")
			return
		}

		asset, err := assetName()
		if err != nil {
			writeErr(http.StatusBadRequest, err.Error())
			return
		}

		client := &http.Client{Timeout: 5 * time.Minute}
		bin, checksum, err := downloadVerifiedBinary(client, baseURL, asset)
		if err != nil {
			slog.Error("self-update download/verify failed", "asset", asset, "error", err)
			writeErr(http.StatusBadGateway, err.Error())
			return
		}

		exePath, err := os.Executable()
		if err != nil {
			writeErr(http.StatusInternalServerError, "cannot locate current executable: "+err.Error())
			return
		}
		if resolved, err := filepath.EvalSymlinks(exePath); err == nil {
			exePath = resolved
		}

		// Write to a temp file in the same directory for an atomic rename.
		dir := filepath.Dir(exePath)
		tmp, err := os.CreateTemp(dir, ".servercontrol-agent-update-*")
		if err != nil {
			writeErr(http.StatusInternalServerError, "cannot create temp file (is the install dir writable? note systemd ProtectSystem=strict makes /usr read-only): "+err.Error())
			return
		}
		tmpName := tmp.Name()
		if _, err := tmp.Write(bin); err != nil {
			tmp.Close()
			os.Remove(tmpName)
			writeErr(http.StatusInternalServerError, "failed to write update: "+err.Error())
			return
		}
		tmp.Close()
		if err := os.Chmod(tmpName, 0o755); err != nil {
			os.Remove(tmpName)
			writeErr(http.StatusInternalServerError, "failed to chmod update: "+err.Error())
			return
		}

		if err := os.Rename(tmpName, exePath); err != nil {
			os.Remove(tmpName)
			writeErr(http.StatusInternalServerError, "failed to replace binary (install dir may be read-only under systemd hardening): "+err.Error())
			return
		}

		slog.Info("self-update applied", "asset", asset, "checksum", checksum)
		json.NewEncoder(w).Encode(updateResponse{
			Success:     true,
			Message:     "update applied, restarting",
			FromVersion: version,
			Asset:       asset,
		})
		if f, ok := w.(http.Flusher); ok {
			f.Flush()
		}

		// Re-exec the freshly installed binary in place once the response drains.
		// Re-exec keeps the same PID so systemd continues tracking the service.
		go func() {
			time.Sleep(750 * time.Millisecond)
			slog.Info("re-executing updated agent", "path", exePath)
			if err := syscall.Exec(exePath, os.Args, os.Environ()); err != nil {
				slog.Error("re-exec failed; exiting so the supervisor restarts us", "error", err)
				os.Exit(1)
			}
		}()
	}
}
