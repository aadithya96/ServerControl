# ServerControl — Development TODO

## Phase 1: Project Foundation ✅ (scaffolded)
- [x] Project structure & Gradle setup
- [x] Hilt DI wiring
- [x] Room DB + ServerProfile entity
- [x] Navigation graph skeleton
- [x] Material3 theme

## Phase 2: Agent Daemon (server-side)
- [x] Write lightweight Go or Python agent daemon
  - [x] GET /stats — cpu, mem, swap, uptime, load avg
  - [x] GET /processes — pid, name, cpu%, mem%, user, command
  - [x] GET /disk — mounts, used, total, read_bps, write_bps
  - [x] GET /connections — proto, local_addr, remote_addr, state, pid
  - [x] GET /firewall — chain, target, proto, source, destination, options
  - [x] DELETE /process/{pid} — kill process
  - [x] POST /firewall/toggle — enable/disable rule
  - [x] Bearer token auth for all endpoints
  - [x] TLS support (self-signed cert option)
- [x] Write install script (curl | bash) for the daemon
- [x] Systemd service unit file for daemon

## Phase 3: Data Layer — Agent
- [x] Finalize all DTOs to match agent JSON schema
- [x] AgentApi Retrofit interface — all endpoints
- [x] AgentDataSource with error handling
- [x] OkHttp interceptor for Bearer token
- [x] Self-signed cert TLS support in OkHttp
- [x] StatsRepositoryImpl — agent path
- [ ] Unit tests for repository (mock AgentApi)

## Phase 4: Data Layer — SSH Fallback
- [x] SshDataSource using JSch
  - [x] Connect with password or private key
  - [x] Execute shell commands, parse stdout
  - [x] CPU stats via /proc/stat
  - [x] RAM stats via /proc/meminfo
  - [x] Disk stats via df -h + iostat
  - [x] Processes via ps aux
  - [x] Connections via ss -tunap
  - [x] Firewall via iptables -L -n -v
  - [x] Kill process via kill -9 {pid}
- [x] StatsRepositoryImpl — SSH fallback path
- [x] Connection type detection (try agent → fallback to SSH)

## Phase 5: Domain Layer
- [x] All model classes finalized
- [x] All repository interfaces
- [x] GetSystemStatsUseCase — emits Flow<Resource<SystemStats>>
- [x] GetProcessesUseCase — with sort param
- [x] GetDiskInfoUseCase
- [x] GetConnectionsUseCase
- [x] GetFirewallRulesUseCase
- [x] KillProcessUseCase — returns Flow<Resource<Unit>>
- [x] ToggleFirewallRuleUseCase

## Phase 6: Server Management UI
- [x] ServerListScreen
  - [x] Empty state illustration
  - [x] Server cards (name, host, online/offline badge)
  - [x] Swipe-to-delete with undo snackbar
  - [x] FAB → AddServerScreen
- [x] AddServerScreen
  - [x] Fields: display name, hostname/IP, agent port (default 9876), auth type toggle
  - [x] Auth type: Agent Token (text field) / SSH Password / SSH Key (file picker)
  - [x] Test connection button → shows latency or error
  - [x] Save to Room DB

## Phase 7: Dashboard Screen
- [x] Real-time CPU % gauge (Vico / custom Canvas composable)
- [x] RAM usage bar with used/total label
- [x] Swap usage bar
- [x] Load average (1m/5m/15m) row
- [x] Uptime chip
- [x] Auto-refresh (configurable interval: 2s/5s/10s/30s)
- [x] Pull-to-refresh
- [x] CPU history sparkline (last 60 data points)
- [x] Color thresholds: green <60%, yellow <80%, red ≥80%

## Phase 8: Processes Screen
- [x] Paginated process list (LazyColumn)
- [x] Sort bar: CPU% | MEM% | PID | Name
- [x] Search bar (filter by name)
- [x] Process row: name, PID, CPU%, MEM%, user
- [x] Long-press → confirmation dialog → kill process
- [x] Success/error toast after kill
- [x] Auto-refresh toggle

## Phase 9: Disk & I/O Screen
- [x] Per-mount linear progress bars
- [x] Used / Total / Free labels
- [x] Read MB/s + Write MB/s per device
- [x] I/O wait % indicator
- [x] Filesystem type chip (ext4, btrfs, tmpfs…)

## Phase 10: Network Connections Screen ✅
- [x] Connection list grouped by state (ESTABLISHED, LISTEN, etc.) with sticky headers
- [x] Protocol chip (TCP=blue, UDP=orange) + local:port → remote:port row
- [x] Filter by protocol (All/TCP/UDP)
- [x] Filter by state (All/ESTABLISHED/LISTEN/TIME_WAIT/CLOSE_WAIT)
- [x] Search by address or process name
- [x] Connection count badge in TopAppBar
- [x] Auto-refresh every 10s via SavedStateHandle

## Phase 11: Firewall Screen ✅
- [x] List rules grouped by expandable/collapsible chain cards
- [x] Rule row: number circle, target chip (colored), protocol, source → destination, options
- [x] Toggle switch per rule with AlertDialog confirmation
- [x] Warning banner: "Changes take effect immediately on the server"
- [x] Backend chip (iptables/nftables)
- [x] Policy badge per chain (ACCEPT=green, DROP=red)
- [x] Shimmer loading state
- [x] Snackbar for toggle result
- [x] Refresh button

## Phase 12: Settings Screen ✅
- [x] Default refresh interval (AlertDialog radio options: 2s/5s/10s/30s)
- [x] Dark theme toggle
- [x] Background monitoring toggle + interval slider (5-60 min)
- [x] CPU alert threshold slider (50-100%)
- [x] Disk alert threshold slider (50-100%)
- [x] About section: app version, open source link
- [x] DataStore-backed settings with typed keys

## Phase 13: Notifications & Background Monitoring ✅
- [x] NotificationHelper: createChannels, sendCpuAlert, sendDiskAlert, sendUnreachableAlert
- [x] ServerMonitorWorker (@HiltWorker) — checks all servers, sends alerts
- [x] WorkerScheduler — schedule/cancel unique periodic work
- [x] getAllServers() added to ServerRepository + DAO
- [x] ServerControlApp calls NotificationHelper.createChannels on start
- [x] WorkManager configuration via HiltWorkerFactory
- [x] Notification channels: "Server Alerts" (HIGH), "Background Sync" (LOW)

## Phase 14: Polish & Onboarding ✅
- [x] ShimmerEffect composable (ShimmerCard, ShimmerStatCard)
- [x] OnboardingScreen: 3-page HorizontalPager
  - [x] Page 1: Welcome with Dns icon
  - [x] Page 2: Install agent with copy-to-clipboard code block
  - [x] Page 3: "You're all set!" with CheckCircle icon
- [x] Onboarding integrated into NavGraph (shown on first launch, skipped if done)
- [x] Screen.kt updated with Onboarding route
- [x] strings.xml: all required string resources added
- [ ] App icon (adaptive icon)
- [ ] Splash screen
- [ ] ProGuard / R8 rules
- [ ] Screenshots for Play Store
- [ ] Release build signing config

## Phase 15: SSH Terminal
- [~] Multi-tab terminal UI (up to 8 simultaneous sessions per server)
- [~] Tab bar: add tab (+), close tab (×), tab titles (auto-named "Terminal 1", "Terminal 2", etc.)
- [~] Full interactive PTY shell via JSch shell channel
- [~] Terminal emulator: render ANSI escape codes (colors, bold, cursor movement)
- [~] Terminal composable: fixed-width font, scrollable, auto-scroll to bottom
- [~] Extended keyboard row: Tab, Ctrl, Alt, Esc, ↑ ↓ ← →, PgUp, PgDn, Home, End, pipe |, ~, /
- [ ] Copy text: long-press to select, copy to clipboard
- [ ] Paste: from clipboard via extended keyboard button
- [ ] Terminal font size adjustable (pinch to zoom or +/- buttons)
- [~] Keep-alive ping every 30s to prevent SSH timeout
- [~] Reconnect on disconnect with user prompt
- [~] Session state preserved when navigating away (ViewModel-scoped coroutines)
- [ ] Terminal color themes: Dark (default), Solarized, Dracula, Light

## Phase 16: Service Manager ✅
- [x] List all systemd units (services, timers, sockets, mounts)
- [x] Filter: All / Active / Failed / Inactive tabs
- [x] Service row: name, description, active state, sub-state, enabled/disabled badge
- [x] Actions: start / stop / restart / reload / enable / disable (with confirmation)
- [x] View service logs inline: journalctl -u <service> -n 100 --no-pager
- [x] Service detail screen: unit file path, exec start, environment, dependencies
- [x] Search/filter by name
- [x] Failed services highlighted in red at top
- [x] Agent endpoint: GET /api/v1/services, POST /api/v1/services/{name}/action

## Phase 17: Log Viewer ✅
- [x] Source selector: systemd journal, /var/log/syslog, /var/log/auth.log, nginx, apache, custom path
- [x] Real-time tail (SSE stream from agent or repeated SSH reads every 2s)
- [x] Search with highlight (regex support)
- [x] Log level color coding: ERROR=red, WARN=yellow, INFO=white, DEBUG=gray
- [x] Line count badge, auto-scroll toggle
- [x] Save/export log snippet to clipboard or share
- [x] Agent endpoint: GET /api/v1/logs?source=journal&unit=nginx&lines=200&follow=true (SSE)

## Phase 18: Metrics History ✅
- [x] Room table: MetricSample(id, serverId, timestamp, cpuPercent, memUsedBytes, diskUsedBytes)
- [x] Background worker samples every N minutes (configurable), stores to Room
- [x] Dashboard history charts: 1h / 6h / 24h / 7d time range selector
- [x] Line chart for CPU%, area chart for RAM using Canvas (no library)
- [x] Disk usage trend per mount
- [x] Export CSV of metrics

## Phase 19: Docker Integration
- [ ] Agent endpoints: GET /api/v1/docker/containers, GET /api/v1/docker/images
- [ ] POST /api/v1/docker/containers/{id}/action (start/stop/restart/remove)
- [ ] GET /api/v1/docker/containers/{id}/logs (last N lines)
- [ ] Container list: name, image, status, ports, CPU%, MEM%
- [ ] Container detail: env vars, mounts, network, resource limits
- [ ] Image list: name, tag, size, created
- [ ] Container logs viewer (reuse Log Viewer component)

## Phase 20: Quick Commands / Runbooks
- [ ] Room table: SavedCommand(id, serverId?, name, command, description)
- [ ] Command library screen: list saved commands, add/edit/delete
- [ ] Run command button: SSH exec or POST /api/v1/exec to agent
- [ ] Output shown in bottom sheet with monospace font
- [ ] Built-in command templates: "Check disk", "Restart nginx", "Tail syslog", "Top 10 CPU processes"
- [ ] Import/export commands as JSON

## Phase 21: Security & Audit
- [ ] Failed SSH login monitor: parse /var/log/auth.log for failed attempts
- [ ] Top attacking IPs with attempt count, last seen, country (ip-api.com lookup)
- [ ] One-tap block IP via iptables (with confirmation)
- [ ] SSL certificate expiry monitor for domains/certs on server
- [ ] Local audit log: every app action (kill process, toggle firewall, restart service) logged with timestamp
- [ ] Biometric lock: require fingerprint/PIN to open app

## Phase 22: Widgets & Home Screen
- [ ] 2×1 widget: server name + health dot (green/red)
- [ ] 4×2 widget: CPU bar, RAM bar, disk bar for one server
- [ ] Widget config: choose server, choose metrics to show
- [ ] Glance API implementation

## Phase 23: Multi-Server Dashboard & Groups
- [ ] Server groups: tag servers (production/staging/home lab)
- [ ] Overview screen: all servers side-by-side cards with key metrics
- [ ] Compare mode: two servers' CPU/RAM side by side
- [ ] Tablet two-pane layout

## Phase 24: Integrations & Export
- [ ] Webhook alerts: Slack/Discord/Telegram when thresholds hit
- [ ] Server profile export (encrypted JSON) via share sheet
- [ ] Import via QR code scan (encode server profile)
- [ ] Bandwidth monitor: per-interface bytes in/out graph, top processes by bandwidth
