# ServerControl

**A native Android app for monitoring and managing Linux servers in real-time.**

![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.x-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)
![Go](https://img.shields.io/badge/Go-1.24-00ADD8?logo=go&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)

ServerControl is a native Android application that gives you full visibility and control over your Linux servers from your phone. It connects either through a lightweight Go agent daemon (recommended) or falls back to SSH when no agent is installed. All monitoring, process management, firewall control, terminal access, and alerting runs from a single Material3 app.

---

## Features

- **Real-time dashboard** — CPU % gauge with color thresholds (green/yellow/red), RAM bar, swap bar, load averages (1m/5m/15m), uptime chip, CPU history sparkline, configurable auto-refresh (2s/5s/10s/30s), pull-to-refresh
- **Process manager** — paginated list sortable by CPU%, MEM%, PID, or name; search by process name; long-press to kill with confirmation dialog
- **Disk & I/O monitor** — per-mount linear progress bars, used/total/free labels, read MB/s + write MB/s per device, I/O wait %, filesystem type chip (ext4, btrfs, tmpfs, …)
- **Network connections viewer** — grouped by state with sticky headers, protocol chip (TCP/UDP), filter by protocol and state, search by address or process name, connection count badge, auto-refresh every 10s
- **Firewall rule management** — rules grouped by expandable/collapsible chain cards, per-rule toggle with confirmation dialog, policy badge per chain (ACCEPT/DROP), warning banner for live changes, iptables/nftables backend chip
- **Multi-tab SSH terminal** — up to 8 simultaneous PTY sessions per server, ANSI escape code rendering (colors, bold, cursor movement), extended keyboard row (Tab, Ctrl, Alt, Esc, arrow keys, PgUp/PgDn, Home, End, pipe, ~, /), keep-alive ping every 30s, reconnect on disconnect
- **Service manager** — list systemd units, filter by state, start/stop/restart/enable/disable actions, inline journal log viewer *(Phase 16, in progress)*
- **Log viewer** — systemd journal, `/var/log/syslog`, `/var/log/auth.log`, nginx, apache, custom path; real-time tail, search with highlight, log level color coding *(Phase 17, in progress)*
- **Metrics history with charts** — CPU% and RAM area charts at 1h/6h/24h/7d time ranges using custom Canvas composable, metric sampling via background worker *(Phase 18, in progress)*
- **In-app agent installer** — SSH into a server, run the install script, retrieve the token — all without leaving the app
- **Multi-server support with profiles** — server cards with online/offline badge, swipe-to-delete with undo snackbar, test connection button showing latency
- **Background monitoring and push notifications** — WorkManager periodic worker checks all servers and fires CPU alert, disk alert, or unreachable alert via two notification channels (Server Alerts / Background Sync)
- **Onboarding flow** — 3-page HorizontalPager shown on first launch, skippable on subsequent launches

---

## Architecture

### App Architecture

```
ServerControl/
├── agent/                          # Go agent daemon (runs on server)
│   ├── main.go                     # HTTP server, routing, graceful shutdown
│   ├── config.go                   # Config from file/env/CLI flags
│   ├── handlers/                   # HTTP handlers per feature
│   ├── middleware/                  # Auth (Bearer token), logging, CORS
│   ├── install.sh                  # One-liner installer script
│   └── servercontrol.service       # systemd unit file
└── app/                            # Android app
    └── src/main/java/com/servercontrol/
        ├── data/                   # DTOs, Room DB, repositories
        │   ├── local/              # Room DB, DAOs, entities
        │   ├── remote/             # Agent API (Retrofit), SSH datasource
        │   │   ├── agent/          # AgentApi.kt (Retrofit interface)
        │   │   ├── dto/            # JSON DTOs
        │   │   └── ssh/            # SshDataSource.kt (JSch-based)
        │   └── repository/         # Repository implementations
        ├── domain/                 # Business logic
        │   ├── model/              # Domain models (SystemStats, Process, …)
        │   ├── repository/         # Repository interfaces
        │   └── usecase/            # Use cases (GetSystemStatsUseCase, …)
        ├── presentation/           # Compose UI
        │   ├── dashboard/          # CPU gauge, RAM/swap bars, sparkline
        │   ├── processes/          # Process list, sort, kill
        │   ├── disk/               # Mount progress bars, I/O stats
        │   ├── connections/        # Network connection list
        │   ├── firewall/           # Chain cards, rule toggle
        │   ├── terminal/           # Multi-tab SSH terminal
        │   ├── servers/            # Server list, add/edit server
        │   ├── settings/           # DataStore-backed settings screen
        │   ├── onboarding/         # 3-page onboarding pager
        │   └── navigation/         # NavGraph, Screen.kt
        ├── di/                     # Hilt DI modules
        ├── worker/                 # WorkManager background tasks (ServerMonitorWorker)
        └── util/                   # Helpers, extensions
```

### Connection Strategy

The app uses an **agent-first, SSH-fallback** connection model:

- **Agent mode** (recommended): the app communicates over HTTP(S) with the Go agent daemon running on the server. All responses are structured JSON. This is the fastest path and supports all features including firewall rule re-enabling.
- **SSH mode** (no server install required): the app SSHes into the server using JSch (password or private key auth) and runs shell commands (`/proc/stat`, `ps aux`, `df -B1 -T`, `ss -tunap`, `iptables -L -n -v`, etc.), parsing stdout directly.
- **Detection**: on each data fetch the app first attempts to reach the agent's `/health` endpoint. If the request fails or times out, it automatically falls back to SSH. The connection type is transparent to the UI.

> Note: some features (such as re-enabling a firewall rule) are only available in agent mode because SSH cannot reconstruct an iptables rule from its position number alone.

### Agent API Endpoints

All endpoints except `/health` require an `Authorization: Bearer <token>` header.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check — returns `{"status":"ok","version":"1.0.0"}` (no auth) |
| GET | `/api/v1/stats` | System stats: CPU%, RAM, swap, uptime, load avg, hostname |
| GET | `/api/v1/processes` | Process list; query params: `sort` (cpu/mem/pid/name), `limit` (default 100) |
| DELETE | `/api/v1/process/{pid}` | Kill process by PID; query param: `signal` (default 9) |
| GET | `/api/v1/disk` | Disk usage and I/O per mount |
| GET | `/api/v1/connections` | Network connections; query param: `proto` (all/tcp/udp) |
| GET | `/api/v1/firewall` | Firewall rules grouped by chain (iptables) |
| POST | `/api/v1/firewall/toggle` | Toggle (enable/disable) a firewall rule by ID |
| GET | `/api/v1/services` | Systemd unit list; query params: `type`, `state` |
| POST | `/api/v1/services/{name}/action` | Start/stop/restart/enable/disable a service |
| GET | `/api/v1/services/{name}/logs` | Journal logs for a service; query param: `lines` (default 100) |
| GET | `/api/v1/logs` | Log viewer; query params: `source` (journal/syslog/auth/nginx/apache), `unit`, `lines` (default 200) |

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or later
- Android SDK 35 (compile), min SDK 26 (Android 8.0+)
- JDK 17
- Go 1.24+ (only needed to build the agent from source)
- A Linux server (Ubuntu 20.04+, Debian 11+, or similar systemd-based distro)

### Server Setup — Install the Agent

**Option 1: One-liner (recommended)**

```bash
curl -fsSL https://raw.githubusercontent.com/servercontrol/agent/main/install.sh | sudo bash
```

The installer:
1. Detects your OS and CPU architecture (amd64, arm64, armv7)
2. Builds the agent binary from source (if Go is available) or downloads a pre-built binary
3. Generates a cryptographically random 48-character token
4. Writes config to `/etc/servercontrol/agent.conf` (mode 640, root-owned)
5. Installs and starts the `servercontrol` systemd service
6. Prints your token — **save it, you will need it in the Android app**

You can pre-supply a token or port:

```bash
SC_TOKEN=mytoken SC_PORT=9876 bash install.sh
```

**Option 2: Build from source**

```bash
git clone https://github.com/servercontrol/agent
cd agent
go build -ldflags="-s -w" -o servercontrol-agent .
sudo ./servercontrol-agent --token=your-secret-token --port=9876
```

**Agent configuration** (`/etc/servercontrol/agent.conf`):

```ini
# ServerControl Agent Configuration
PORT=9876
TOKEN=your-secret-token
LOG_LEVEL=info
# Optional TLS:
# TLS_CERT=/etc/servercontrol/cert.pem
# TLS_KEY=/etc/servercontrol/key.pem
```

Configuration is resolved in priority order: CLI flags > environment variables (`SC_PORT`, `SC_TOKEN`, `SC_LOG_LEVEL`, `SC_TLS_CERT`, `SC_TLS_KEY`) > config file > defaults.

**Verify the agent is running:**

```bash
systemctl status servercontrol
curl http://localhost:9876/health
# Expected: {"status":"ok","version":"1.0.0"}
```

**View agent logs:**

```bash
journalctl -u servercontrol -f
```

### Android App Setup

**Clone and open:**

```bash
git clone https://github.com/servercontrol/servercontrol
```

Open the repo root in Android Studio. Gradle will sync automatically.

**Build variants:**

| Variant | Application ID | Notes |
|---------|---------------|-------|
| `debug` | `com.servercontrol.debug` | Debuggable, full logging |
| `release` | `com.servercontrol` | Minified (R8), requires signing config |

**Install on device or emulator:**

```bash
./gradlew installDebug
```

### Adding Your First Server in the App

1. Launch ServerControl
2. Complete the 3-page onboarding (or skip)
3. Tap **+** on the Server List screen
4. Fill in:
   - **Display name** — e.g. `Home Lab`
   - **Host / IP** — e.g. `192.168.1.100`
   - **Agent port** — default `9876`
   - **Auth type** — choose **Agent Token** and paste the token printed by the installer
5. Tap **Test Connection** — a green latency chip confirms success
6. Tap **Save**

> **No agent installed yet?** Set auth type to **SSH Password** or **SSH Key**. The app will SSH in directly and run shell commands. You can also go to the server card menu and tap **Install Agent** to run the installer in-app over SSH.

---

## Local Development Setup

### Android App

**Requirements:**

- Android Studio Hedgehog (2023.1.1) or newer
- Compile SDK 35, min SDK 26
- JDK 17 (`sourceCompatibility = JavaVersion.VERSION_17`)
- Kotlin 2.0.x
- KSP (annotation processing for Hilt + Room — replaces kapt)

**No API keys or environment variables are needed.** All server connection details are stored in Room DB in the app's private data directory.

**Running tests:**

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumentation tests (device/emulator required)
```

**Lint:**

```bash
./gradlew lint
```

**Key Gradle tasks:**

```bash
./gradlew assembleDebug           # Build debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease         # Build release APK (requires signing config)
./gradlew generateDebugSources    # Generate Hilt/Room code via KSP
```

**Project structure notes:**

- `AgentDataSource` creates a Retrofit instance **per server profile** using a dynamic base URL — it is not a global singleton injected by Hilt
- Room DB schema version is defined in `AppDatabase.kt` — increment the version and add a migration whenever you change an entity
- WorkManager is initialized manually via `HiltWorkerFactory`; the default `WorkManagerInitializer` is removed in `AndroidManifest.xml`
- `usesCleartextTraffic="true"` is set in the manifest to allow HTTP connections to local/LAN agent instances; for internet-facing servers use TLS

### Go Agent Daemon

**Requirements:**

- Go 1.24+
- Linux host (the agent reads `/proc` — it does not work natively on macOS or Windows)
- Root or `sudo` for iptables commands

**Run locally (Linux):**

```bash
cd agent
go mod tidy
go run . --token=devtoken --port=9876 --log-level=debug
```

**Test endpoints:**

```bash
# Health (no auth)
curl http://localhost:9876/health

# Stats
curl -H "Authorization: Bearer devtoken" http://localhost:9876/api/v1/stats | jq .

# Processes (sort by CPU, top 10)
curl -H "Authorization: Bearer devtoken" \
  "http://localhost:9876/api/v1/processes?sort=cpu&limit=10" | jq .

# Kill process
curl -X DELETE -H "Authorization: Bearer devtoken" \
  http://localhost:9876/api/v1/process/1234

# Disk
curl -H "Authorization: Bearer devtoken" http://localhost:9876/api/v1/disk | jq .

# Network connections (TCP only)
curl -H "Authorization: Bearer devtoken" \
  "http://localhost:9876/api/v1/connections?proto=tcp" | jq .
```

**Cross-compile for production:**

```bash
cd agent

# x86_64 servers
GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o servercontrol-agent .

# ARM64 (Raspberry Pi 4, AWS Graviton, etc.)
GOOS=linux GOARCH=arm64 go build -ldflags="-s -w" -o servercontrol-agent-arm64 .

# ARMv7 (Raspberry Pi 2/3 32-bit)
GOOS=linux GOARCH=arm GOARM=7 go build -ldflags="-s -w" -o servercontrol-agent-armv7 .
```

**Install as systemd service manually:**

```bash
sudo cp servercontrol-agent /usr/local/bin/
sudo cp agent/servercontrol.service /etc/systemd/system/
sudo mkdir -p /etc/servercontrol
printf 'PORT=9876\nTOKEN=yourtoken\nLOG_LEVEL=info\n' | sudo tee /etc/servercontrol/agent.conf
sudo chmod 640 /etc/servercontrol/agent.conf
sudo systemctl daemon-reload
sudo systemctl enable --now servercontrol
```

The service file includes systemd security hardening: `NoNewPrivileges=yes`, `ProtectSystem=strict`, `ProtectHome=read-only`, with `ReadWritePaths=/etc/servercontrol`.

### TLS Setup (recommended for production)

Generate a self-signed certificate:

```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes \
  -subj "/CN=myserver" \
  -addext "subjectAltName=IP:192.168.1.100"
sudo cp cert.pem key.pem /etc/servercontrol/
sudo chmod 640 /etc/servercontrol/key.pem
```

Add to `/etc/servercontrol/agent.conf`:

```ini
TLS_CERT=/etc/servercontrol/cert.pem
TLS_KEY=/etc/servercontrol/key.pem
```

Restart the service: `sudo systemctl restart servercontrol`

The Android app accepts self-signed certificates (trust-all TLS configured in `AgentDataSource`). For production deployments consider a CA-signed cert via Let's Encrypt.

---

## Tech Stack

### Android App

| Component | Library / Detail |
|-----------|-----------------|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose |
| DI | Hilt (Dagger) via KSP |
| Networking | Retrofit + OkHttp (Bearer token interceptor) |
| SSH | JSch (`com.jcraft:jsch`) |
| Local DB | Room (KSP code generation) |
| Settings | DataStore Preferences |
| Background tasks | WorkManager + HiltWorkerFactory |
| Charts | Vico (`vico-compose`, `vico-compose-m3`, `vico-core`) + custom Canvas composables |
| Async | Kotlin Coroutines + Flow |
| Min SDK | 26 (Android 8.0 Oreo) |
| Compile SDK | 35 |

### Agent

| Component | Detail |
|-----------|--------|
| Language | Go 1.24 (stdlib only — zero external dependencies) |
| Metrics source | `/proc` filesystem (`/proc/stat`, `/proc/meminfo`, `/proc/uptime`, `/proc/loadavg`, `/proc/diskstats`, `/proc/net/tcp`, `/proc/net/udp`, `/proc/*/stat`) |
| Auth | Bearer token (`Authorization` header) |
| TLS | Optional — stdlib `crypto/tls` via `ListenAndServeTLS` |
| Process manager | `/proc/*/stat` + `/proc/*/status` |
| Network connections | `/proc/net/tcp` + `/proc/net/udp` |
| Firewall | `iptables` subprocess |
| HTTP timeouts | Read: 30s, Write: 60s, Idle: 120s |
| Graceful shutdown | `SIGINT` / `SIGTERM` with 10s drain timeout |
| Default port | `9876` |

---

## Security Considerations

- **Token storage**: the agent token is stored in `/etc/servercontrol/agent.conf` with mode 640 (root-readable only). On Android it lives in the app-private Room database. Consider enabling biometric lock in Settings once Phase 21 ships.
- **TLS**: strongly recommended for any server not on your local LAN. The app currently accepts self-signed certificates — do not expose port 9876 to the public internet without TLS and firewall rules restricting source IPs.
- **Firewall API**: the agent runs as root (required for `iptables`). It only reads or toggles specific rules via the API — there is no arbitrary command execution endpoint.
- **SSH credentials**: stored in the Room database (app-private storage), never written to logs.
- **`StrictHostKeyChecking`**: the SSH client currently sets this to `no` for convenience. In production you should pin the host key.
- **Cleartext traffic**: `android:usesCleartextTraffic="true"` is enabled to allow HTTP to LAN agent instances. Use TLS for anything outside your local network.

---

## Roadmap

Completed phases (1–15 core, 16–18 in progress) bring the full monitoring and terminal feature set. Planned phases:

- **Phase 19 — Docker Integration**: container and image management, container log viewer, start/stop/restart/remove actions
- **Phase 20 — Quick Commands / Runbooks**: saved command library, one-tap SSH exec with output in bottom sheet, built-in templates
- **Phase 21 — Security & Audit**: failed SSH login monitor, top attacking IPs with one-tap block, SSL cert expiry monitor, biometric app lock, local audit log
- **Phase 22 — Home Screen Widgets** (Glance API): 2×1 health dot widget, 4×2 metrics widget with server selector
- **Phase 23 — Multi-Server Dashboard & Groups**: tag servers by environment (production/staging/home lab), overview cards side-by-side, compare mode, tablet two-pane layout
- **Phase 24 — Integrations & Export**: Slack/Discord/Telegram webhook alerts, server profile export (encrypted JSON), QR code server import, per-interface bandwidth graphs

---

## Contributing

```
1. Fork the repo
2. Create a feature branch:  git checkout -b feature/my-feature
3. Commit your changes:      git commit -m "Add my feature"
4. Push the branch:          git push origin feature/my-feature
5. Open a Pull Request
```

Please keep commits focused, match the existing code style (Kotlin official style guide; Go `gofmt`), and add tests for new behaviour.

---

## License

MIT License — see [LICENSE](LICENSE) file.
