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
