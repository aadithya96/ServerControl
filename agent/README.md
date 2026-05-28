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

## API Endpoints

All endpoints (except `/health`) require `Authorization: Bearer <token>`.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check (no auth) |
| GET | `/api/v1/stats` | CPU, memory, load, uptime |
| GET | `/api/v1/processes?sort=cpu&limit=50` | Process list |
| GET | `/api/v1/disk` | Disk mounts and I/O stats |
| GET | `/api/v1/connections?proto=all` | Network connections |
| GET | `/api/v1/firewall` | iptables rules |
| DELETE | `/api/v1/process/{pid}?signal=9` | Kill a process |
| POST | `/api/v1/firewall/toggle` | Toggle a firewall rule |

## Requirements

- Linux (reads `/proc` filesystem)
- Root privileges for iptables access
- systemd (for service management)
