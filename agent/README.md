# ServerControl Agent

A lightweight Go daemon that exposes a REST API for the ServerControl Android app.

## Quick Install

```bash
curl -fsSL https://raw.githubusercontent.com/servercontrol/agent/main/install.sh | sudo bash
```

With a custom token:
```bash
SC_TOKEN=my-secret-token sudo bash install.sh
```

## Manual Build

```bash
go build -o servercontrol-agent .
./servercontrol-agent --token=my-secret-token --port=9876
```

## Configuration

Priority order: CLI flags > environment variables > `/etc/servercontrol/agent.conf`

| Flag | Env Var | Default | Description |
|------|---------|---------|-------------|
| `--port` | `SC_PORT` | `9876` | Listen port |
| `--token` | `SC_TOKEN` | *(required)* | Bearer auth token |
| `--tls-cert` | `SC_TLS_CERT` | — | Path to TLS certificate |
| `--tls-key` | `SC_TLS_KEY` | — | Path to TLS key |
| `--log-level` | `SC_LOG_LEVEL` | `info` | Log verbosity |
| `--metrics` | `SC_METRICS` | `false` | Enable Prometheus `/metrics` endpoint |
| `--rate-limit` | `SC_RATE_LIMIT` | `30` | Max requests/sec per client IP (`0` disables) |
| `--self-update` | `SC_SELF_UPDATE` | `false` | Enable `POST /api/v1/agent/update` self-update endpoint |
| `--update-url` | `SC_UPDATE_URL` | GitHub latest release | Base URL for release binaries + `SHA256SUMS.txt` |

## Logging

The agent emits structured JSON logs to stdout (one object per line) via Go's
`log/slog`, including a `service`, `version`, and `level` on every line. Request
logs include `method`, `path`, `status`, and `duration_ms`. Use `--log-level`
(`debug`/`info`/`warn`/`error`) to control verbosity.

## Reloading config (SIGHUP)

Send `SIGHUP` (e.g. `systemctl reload servercontrol-agent` or `kill -HUP <pid>`)
to hot-reload the **auth token** and **log level** from the config file/env
without restarting the listener or dropping connections. Other settings (port,
TLS, metrics, rate limit) require a full restart.

## Self-update (opt-in)

When enabled with `--self-update`, an authenticated client can POST to
`/api/v1/agent/update` to upgrade the agent to the latest release:

1. Downloads `SHA256SUMS.txt` and the binary for the running OS/arch from
   `--update-url` (**HTTPS required** — plain HTTP is rejected).
2. Verifies the binary's SHA-256 against the checksums file. A mismatch aborts
   the update; nothing is installed.
3. Atomically replaces the running binary and re-execs in place (same PID, so
   systemd keeps tracking the service).

It is **disabled by default** and always sits behind bearer auth. Note: the
shipped systemd unit sets `ProtectSystem=strict`, which makes `/usr` read-only —
self-update will fail to replace a binary under `/usr/local/bin` unless you add
its directory to `ReadWritePaths=` (this trade-off weakens hardening, so enable
self-update deliberately).

## API Endpoints

All endpoints (except `/health` and `/metrics`) require `Authorization: Bearer <token>`.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check (no auth) — status, version, uptime, Go runtime stats |
| GET | `/metrics` | Prometheus metrics (no auth, only when `--metrics` enabled) |
| GET | `/api/v1/stats` | CPU, memory, load, uptime |
| GET | `/api/v1/processes?sort=cpu&limit=50` | Process list |
| GET | `/api/v1/disk` | Disk mounts and I/O stats |
| GET | `/api/v1/connections?proto=all` | Network connections |
| GET | `/api/v1/bandwidth` | Per-interface RX/TX bytes-per-second |
| GET | `/api/v1/firewall` | iptables rules |
| DELETE | `/api/v1/process/{pid}?signal=9` | Kill a process |
| POST | `/api/v1/firewall/toggle` | Toggle a firewall rule |
| POST | `/api/v1/agent/update` | Self-update to latest release (only when `--self-update` enabled) |

## Requirements

- Linux (reads `/proc` filesystem)
- Root privileges for iptables access
- systemd (for service management)
