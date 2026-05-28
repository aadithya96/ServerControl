# ServerControl — Development TODO

## Phase 1: Project Foundation ✅ (scaffolded)
- [x] Project structure & Gradle setup
- [x] Hilt DI wiring
- [x] Room DB + ServerProfile entity
- [x] Navigation graph skeleton
- [x] Material3 theme

## Phase 2: Agent Daemon (server-side)
- [ ] Write lightweight Go or Python agent daemon
  - [ ] GET /stats — cpu, mem, swap, uptime, load avg
  - [ ] GET /processes — pid, name, cpu%, mem%, user, command
  - [ ] GET /disk — mounts, used, total, read_bps, write_bps
  - [ ] GET /connections — proto, local_addr, remote_addr, state, pid
  - [ ] GET /firewall — chain, target, proto, source, destination, options
  - [ ] DELETE /process/{pid} — kill process
  - [ ] POST /firewall/toggle — enable/disable rule
  - [ ] Bearer token auth for all endpoints
  - [ ] TLS support (self-signed cert option)
- [ ] Write install script (curl | bash) for the daemon
- [ ] Systemd service unit file for daemon

## Phase 3: Data Layer — Agent
- [ ] Finalize all DTOs to match agent JSON schema
- [ ] AgentApi Retrofit interface — all endpoints
- [ ] AgentDataSource with error handling
- [ ] OkHttp interceptor for Bearer token
- [ ] Self-signed cert TLS support in OkHttp
- [ ] StatsRepositoryImpl — agent path
- [ ] Unit tests for repository (mock AgentApi)

## Phase 4: Data Layer — SSH Fallback
- [ ] SshDataSource using JSch
  - [ ] Connect with password or private key
  - [ ] Execute shell commands, parse stdout
  - [ ] CPU stats via /proc/stat
  - [ ] RAM stats via /proc/meminfo
  - [ ] Disk stats via df -h + iostat
  - [ ] Processes via ps aux
  - [ ] Connections via ss -tunap
  - [ ] Firewall via iptables -L -n -v
  - [ ] Kill process via kill -9 {pid}
- [ ] StatsRepositoryImpl — SSH fallback path
- [ ] Connection type detection (try agent → fallback to SSH)

## Phase 5: Domain Layer
- [ ] All model classes finalized
- [ ] All repository interfaces
- [ ] GetSystemStatsUseCase — emits Flow<Resource<SystemStats>>
- [ ] GetProcessesUseCase — with sort param
- [ ] GetDiskInfoUseCase
- [ ] GetConnectionsUseCase
- [ ] GetFirewallRulesUseCase
- [ ] KillProcessUseCase — returns Flow<Resource<Unit>>
- [ ] ToggleFirewallRuleUseCase

## Phase 6: Server Management UI
- [ ] ServerListScreen
  - [ ] Empty state illustration
  - [ ] Server cards (name, host, online/offline badge)
  - [ ] Swipe-to-delete with undo snackbar
  - [ ] FAB → AddServerScreen
- [ ] AddServerScreen
  - [ ] Fields: display name, hostname/IP, agent port (default 9876), auth type toggle
  - [ ] Auth type: Agent Token (text field) / SSH Password / SSH Key (file picker)
  - [ ] Test connection button → shows latency or error
  - [ ] Save to Room DB

## Phase 7: Dashboard Screen
- [ ] Real-time CPU % gauge (Vico / custom Canvas composable)
- [ ] RAM usage bar with used/total label
- [ ] Swap usage bar
- [ ] Load average (1m/5m/15m) row
- [ ] Uptime chip
- [ ] Auto-refresh (configurable interval: 2s/5s/10s/30s)
- [ ] Pull-to-refresh
- [ ] CPU history sparkline (last 60 data points)
- [ ] Color thresholds: green <60%, yellow <80%, red ≥80%

## Phase 8: Processes Screen
- [ ] Paginated process list (LazyColumn)
- [ ] Sort bar: CPU% | MEM% | PID | Name
- [ ] Search bar (filter by name)
- [ ] Process row: name, PID, CPU%, MEM%, user
- [ ] Long-press → confirmation dialog → kill process
- [ ] Success/error toast after kill
- [ ] Auto-refresh toggle

## Phase 9: Disk & I/O Screen
- [ ] Per-mount linear progress bars
- [ ] Used / Total / Free labels
- [ ] Read MB/s + Write MB/s per device
- [ ] I/O wait % indicator
- [ ] Filesystem type chip (ext4, btrfs, tmpfs…)

## Phase 10: Network Connections Screen
- [ ] Connection list grouped by state (ESTABLISHED, LISTEN, etc.)
- [ ] Columns: proto, local address:port, remote address:port, state, PID/process
- [ ] Filter by protocol (TCP/UDP)
- [ ] Filter by state
- [ ] Search by address

## Phase 11: Firewall Screen
- [ ] List rules grouped by chain (INPUT / OUTPUT / FORWARD)
- [ ] Rule row: target, proto, source, destination, options
- [ ] Toggle switch per rule (with confirmation)
- [ ] Warning banner: "Changes take effect immediately"
- [ ] Add rule (stretch goal)
- [ ] Delete rule (stretch goal)

## Phase 12: Settings Screen
- [ ] Default refresh interval
- [ ] Theme (System / Light / Dark)
- [ ] Notification threshold config (CPU %, disk %)
- [ ] Background monitoring toggle
- [ ] About / version info

## Phase 13: Notifications & Background Monitoring
- [ ] WorkManager periodic task
- [ ] Alert: CPU > threshold for N minutes
- [ ] Alert: Disk usage > threshold
- [ ] Alert: Server unreachable
- [ ] Notification channel setup
- [ ] Notification tap → opens relevant screen

## Phase 14: Polish & Release
- [ ] App icon (adaptive icon)
- [ ] Splash screen
- [ ] Onboarding flow (first launch)
- [ ] Error states with retry buttons
- [ ] Offline/no-connection state handling
- [ ] ProGuard / R8 rules
- [ ] Screenshots for Play Store
- [ ] Release build signing config
