package main

import (
	"crypto/sha256"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestParseChecksums(t *testing.T) {
	data := "abc123  servercontrol-agent-linux-amd64\n" +
		"DEF456 *servercontrol-agent-linux-arm64\n" +
		"\n" +
		"garbage-line\n"
	sums := parseChecksums(data)

	if sums["servercontrol-agent-linux-amd64"] != "abc123" {
		t.Errorf("amd64 = %q, want abc123", sums["servercontrol-agent-linux-amd64"])
	}
	// '*' prefix stripped, hex lowercased.
	if sums["servercontrol-agent-linux-arm64"] != "def456" {
		t.Errorf("arm64 = %q, want def456", sums["servercontrol-agent-linux-arm64"])
	}
	if len(sums) != 2 {
		t.Errorf("expected 2 entries, got %d: %+v", len(sums), sums)
	}
}

func TestAssetNameCurrentPlatform(t *testing.T) {
	// Tests run on linux/amd64 in CI; just assert it resolves without error and
	// is non-empty on a supported platform.
	name, err := assetName()
	if err != nil {
		t.Skipf("unsupported test platform: %v", err)
	}
	if name == "" {
		t.Error("assetName returned empty name with no error")
	}
}

// updateTestServer serves a fake release: SHA256SUMS.txt + the asset bytes.
func updateTestServer(t *testing.T, asset string, content []byte, tamperSum bool) *httptest.Server {
	t.Helper()
	sum := fmt.Sprintf("%x", sha256.Sum256(content))
	if tamperSum {
		sum = "0000000000000000000000000000000000000000000000000000000000000000"
	}
	mux := http.NewServeMux()
	mux.HandleFunc("/SHA256SUMS.txt", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "%s  %s\n", sum, asset)
	})
	mux.HandleFunc("/"+asset, func(w http.ResponseWriter, r *http.Request) {
		w.Write(content)
	})
	return httptest.NewServer(mux)
}

func TestDownloadVerifiedBinarySuccess(t *testing.T) {
	asset := "servercontrol-agent-linux-amd64"
	content := []byte("fake-binary-bytes")
	srv := updateTestServer(t, asset, content, false)
	defer srv.Close()

	data, checksum, err := downloadVerifiedBinary(srv.Client(), srv.URL, asset)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if string(data) != string(content) {
		t.Errorf("data mismatch: got %q", data)
	}
	want := fmt.Sprintf("%x", sha256.Sum256(content))
	if checksum != want {
		t.Errorf("checksum = %q, want %q", checksum, want)
	}
}

func TestDownloadVerifiedBinaryChecksumMismatch(t *testing.T) {
	asset := "servercontrol-agent-linux-amd64"
	srv := updateTestServer(t, asset, []byte("payload"), true) // tampered checksum
	defer srv.Close()

	if _, _, err := downloadVerifiedBinary(srv.Client(), srv.URL, asset); err == nil {
		t.Fatal("expected checksum mismatch error, got nil")
	}
}

func TestDownloadVerifiedBinaryMissingEntry(t *testing.T) {
	srv := updateTestServer(t, "some-other-asset", []byte("x"), false)
	defer srv.Close()

	_, _, err := downloadVerifiedBinary(srv.Client(), srv.URL, "servercontrol-agent-linux-amd64")
	if err == nil {
		t.Fatal("expected error for asset missing from checksums, got nil")
	}
}

func TestSelfUpdateHandlerRejectsNonHTTPS(t *testing.T) {
	h := selfUpdateHandler("http://example.com/releases")
	req := httptest.NewRequest(http.MethodPost, "/api/v1/agent/update", nil)
	rec := httptest.NewRecorder()

	h(rec, req)

	if rec.Code != http.StatusPreconditionFailed {
		t.Errorf("non-https source: got %d, want 412", rec.Code)
	}
}
