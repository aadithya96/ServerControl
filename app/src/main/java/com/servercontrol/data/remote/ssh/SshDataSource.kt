package com.servercontrol.data.remote.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.servercontrol.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class SshDataSource @Inject constructor() {

    private fun openSession(profile: ServerProfile): Session {
        val jsch = JSch()
        val session: Session = when (profile.authType) {
            AuthType.SSH_KEY -> {
                profile.sshPrivateKey?.let { key ->
                    jsch.addIdentity("key", key.toByteArray(), null, null)
                }
                jsch.getSession(profile.sshUsername ?: "root", profile.host, profile.sshPort)
            }
            AuthType.SSH_PASSWORD -> {
                jsch.getSession(profile.sshUsername ?: "root", profile.host, profile.sshPort).also {
                    it.setPassword(profile.sshPassword ?: "")
                }
            }
            else -> throw IllegalArgumentException("SSH auth not configured for this server")
        }
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect(10_000)
        return session
    }

    private fun exec(session: Session, cmd: String): String {
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(cmd)
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        channel.outputStream = out
        channel.setErrStream(err)
        channel.connect(5_000)
        while (!channel.isClosed) Thread.sleep(50)
        channel.disconnect()
        return out.toString(Charsets.UTF_8.name())
    }

    suspend fun getSystemStats(profile: ServerProfile): Result<SystemStats> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession(profile)
            try {
                // CPU: read /proc/stat twice 500ms apart
                val stat1 = exec(session, "cat /proc/stat | head -1")
                Thread.sleep(500)
                val stat2 = exec(session, "cat /proc/stat | head -1")

                val memInfo = exec(session, "cat /proc/meminfo")
                val uptimeOut = exec(session, "cat /proc/uptime")
                val loadOut = exec(session, "cat /proc/loadavg")
                val hostnameOut = exec(session, "hostname")
                val cpuCoresOut = exec(session, "nproc 2>/dev/null || grep -c '^processor' /proc/cpuinfo")

                fun parseCpuLine(line: String): LongArray {
                    val parts = line.trim().split("\\s+".toRegex())
                    return LongArray(7) { i -> parts.getOrElse(i + 1) { "0" }.toLongOrNull() ?: 0L }
                }

                val c1 = parseCpuLine(stat1)
                val c2 = parseCpuLine(stat2)
                // user nice system idle iowait irq softirq
                val idle1 = c1[3] + c1[4]
                val idle2 = c2[3] + c2[4]
                val total1 = c1.sum()
                val total2 = c2.sum()
                val totalDelta = total2 - total1
                val idleDelta = idle2 - idle1
                val cpuPercent = if (totalDelta > 0) (totalDelta - idleDelta).toDouble() / totalDelta * 100.0 else 0.0

                val memMap = memInfo.lines()
                    .mapNotNull { line ->
                        val parts = line.split(":\\s+".toRegex())
                        if (parts.size >= 2) parts[0].trim() to (parts[1].trim().split(" ")[0].toLongOrNull() ?: 0L) * 1024L
                        else null
                    }.toMap()

                val memTotal = memMap["MemTotal"] ?: 0L
                val memAvail = memMap["MemAvailable"] ?: memMap["MemFree"] ?: 0L
                val memFree = memMap["MemFree"] ?: 0L
                val swapTotal = memMap["SwapTotal"] ?: 0L
                val swapFree = memMap["SwapFree"] ?: 0L

                val uptimeSecs = uptimeOut.trim().split(" ")[0].toDoubleOrNull()?.toLong() ?: 0L

                val loadParts = loadOut.trim().split("\\s+".toRegex())
                val load1 = loadParts.getOrElse(0) { "0.0" }.toDoubleOrNull() ?: 0.0
                val load5 = loadParts.getOrElse(1) { "0.0" }.toDoubleOrNull() ?: 0.0
                val load15 = loadParts.getOrElse(2) { "0.0" }.toDoubleOrNull() ?: 0.0

                val cores = cpuCoresOut.trim().toIntOrNull() ?: 1

                SystemStats(
                    hostname = hostnameOut.trim(),
                    uptimeSeconds = uptimeSecs,
                    loadAvg1m = load1,
                    loadAvg5m = load5,
                    loadAvg15m = load15,
                    cpuPercent = cpuPercent,
                    cpuCores = cores,
                    memTotalBytes = memTotal,
                    memUsedBytes = memTotal - memAvail,
                    memFreeBytes = memFree,
                    swapTotalBytes = swapTotal,
                    swapUsedBytes = swapTotal - swapFree
                )
            } finally {
                session.disconnect()
            }
        }
    }

    suspend fun getProcesses(profile: ServerProfile, sort: String = "cpu"): Result<List<Process>> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession(profile)
            try {
                // Try both ps formats
                val output = exec(session,
                    "ps -eo pid,user,pcpu,pmem,rss,stat,comm,args --no-headers 2>/dev/null || ps aux --no-headers 2>/dev/null")
                parsePsOutput(output)
            } finally {
                session.disconnect()
            }
        }
    }

    suspend fun getDiskInfo(profile: ServerProfile): Result<List<DiskInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession(profile)
            try {
                val dfOut = exec(session, "df -B1 -T 2>/dev/null")
                val diskstatsOut = exec(session, "cat /proc/diskstats 2>/dev/null")
                parseDfOutput(dfOut, diskstatsOut)
            } finally {
                session.disconnect()
            }
        }
    }

    suspend fun getConnections(profile: ServerProfile, proto: String = "all"): Result<List<NetworkConnection>> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession(profile)
            try {
                val protoFlag = when (proto) {
                    "tcp" -> "-tnap"
                    "udp" -> "-unap"
                    else -> "-tunap"
                }
                val output = exec(session, "ss $protoFlag 2>/dev/null || netstat -tunap 2>/dev/null")
                parseSsOutput(output)
            } finally {
                session.disconnect()
            }
        }
    }

    suspend fun getFirewallRules(profile: ServerProfile): Result<FirewallData> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession(profile)
            try {
                val output = exec(session, "iptables -L -n -v --line-numbers -x 2>/dev/null")
                parseIptablesOutput(output)
            } finally {
                session.disconnect()
            }
        }
    }

    suspend fun killProcess(profile: ServerProfile, pid: Int, signal: Int = 9): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession(profile)
            try {
                exec(session, "kill -$signal $pid")
                "Process $pid killed with signal $signal"
            } finally {
                session.disconnect()
            }
        }
    }

    suspend fun toggleFirewallRule(profile: ServerProfile, ruleId: String, enabled: Boolean): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // ruleId format: "CHAIN-NUM" e.g. "INPUT-3"
            val parts = ruleId.split("-")
            if (parts.size < 2) throw IllegalArgumentException("Invalid ruleId format: $ruleId")
            val chain = parts.dropLast(1).joinToString("-")
            val num = parts.last().toIntOrNull() ?: throw IllegalArgumentException("Invalid rule number in: $ruleId")

            val session = openSession(profile)
            try {
                if (!enabled) {
                    // Delete the rule at that position
                    exec(session, "iptables -D $chain $num")
                    "Rule $ruleId deleted from chain $chain"
                } else {
                    // Re-inserting requires knowing the rule spec; we just return a message
                    // Since we can't easily re-insert without rule spec, we note this limitation
                    throw UnsupportedOperationException("Re-enabling rules via SSH requires rule spec. Use agent mode.")
                }
            } finally {
                session.disconnect()
            }
        }
    }

    suspend fun testConnection(profile: ServerProfile): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val session = openSession(profile)
            session.disconnect()
            System.currentTimeMillis() - start
        }
    }

    // --- Parsers ---

    private fun parsePsOutput(output: String): List<Process> {
        return output.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex(), limit = 8)
                if (parts.size < 7) return@mapNotNull null
                // Format: pid user pcpu pmem rss stat comm [args...]
                val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
                val user = parts[1]
                val cpu = parts[2].toDoubleOrNull() ?: 0.0
                val mem = parts[3].toDoubleOrNull() ?: 0.0
                val rss = (parts[4].toLongOrNull() ?: 0L) * 1024L
                val stat = parts[5]
                val comm = parts[6]
                val args = parts.getOrElse(7) { comm }
                Process(
                    pid = pid,
                    user = user,
                    cpuPercent = cpu,
                    memPercent = mem,
                    memRss = rss,
                    status = stat,
                    name = comm,
                    command = args
                )
            }
    }

    private fun parseDfOutput(dfOutput: String, diskstatsOutput: String): List<DiskInfo> {
        // Build a map of device -> (readSectors, writeSectors) from diskstats
        // /proc/diskstats columns: major minor name reads_completed ... sectors_read ... writes_completed ... sectors_written
        val diskstatsMap = mutableMapOf<String, Pair<Long, Long>>()
        diskstatsOutput.lines().filter { it.isNotBlank() }.forEach { line ->
            val p = line.trim().split("\\s+".toRegex())
            if (p.size >= 10) {
                val name = p[2]
                val sectorsRead = p[5].toLongOrNull() ?: 0L
                val sectorsWritten = p[9].toLongOrNull() ?: 0L
                diskstatsMap[name] = sectorsRead * 512L to sectorsWritten * 512L
            }
        }

        return dfOutput.lines()
            .drop(1) // skip header
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                // df -B1 -T: Filesystem Type 1B-blocks Used Available Use% Mounted
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size < 7) return@mapNotNull null
                val device = parts[0]
                val fsType = parts[1]
                val total = parts[2].toLongOrNull() ?: 0L
                val used = parts[3].toLongOrNull() ?: 0L
                val avail = parts[4].toLongOrNull() ?: 0L
                val mount = parts[6]

                val devName = device.substringAfterLast("/")
                val (readBytes, writeBytes) = diskstatsMap[devName] ?: (0L to 0L)

                DiskInfo(
                    device = device,
                    mountPoint = mount,
                    fsType = fsType,
                    totalBytes = total,
                    usedBytes = used,
                    freeBytes = avail,
                    readBytesPerSec = 0L, // diskstats are cumulative; would need two reads for rate
                    writeBytesPerSec = 0L,
                    ioWaitPercent = 0.0
                )
            }
    }

    private fun parseSsOutput(output: String): List<NetworkConnection> {
        // ss -tunap format: Netid State Recv-Q Send-Q Local-Addr:Port Peer-Addr:Port [Process]
        return output.lines()
            .filter { it.isNotBlank() && !it.startsWith("Netid") }
            .mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size < 5) return@mapNotNull null
                val proto = parts[0]
                val state = parts[1]
                // parts[2] = Recv-Q, parts[3] = Send-Q
                val localAddrPort = parts[4]
                val peerAddrPort = parts.getOrElse(5) { "0.0.0.0:0" }

                fun splitAddrPort(s: String): Pair<String, Int> {
                    val lastColon = s.lastIndexOf(':')
                    return if (lastColon < 0) s to 0
                    else s.substring(0, lastColon) to (s.substring(lastColon + 1).toIntOrNull() ?: 0)
                }

                val (localAddr, localPort) = splitAddrPort(localAddrPort)
                val (peerAddr, peerPort) = splitAddrPort(peerAddrPort)

                // Process info like: users:(("sshd",pid=1234,fd=3))
                val processInfo = parts.drop(6).joinToString(" ")
                val pid = Regex("pid=(\\d+)").find(processInfo)?.groupValues?.get(1)?.toIntOrNull()
                val procName = Regex("\"([^\"]+)\"").find(processInfo)?.groupValues?.get(1)

                NetworkConnection(
                    protocol = proto,
                    localAddress = localAddr,
                    localPort = localPort,
                    remoteAddress = peerAddr,
                    remotePort = peerPort,
                    state = state,
                    pid = pid,
                    processName = procName
                )
            }
    }

    private fun parseIptablesOutput(output: String): FirewallData {
        if (output.isBlank()) return FirewallData("iptables", emptyList())

        val chains = mutableListOf<FirewallChain>()
        var currentChain = ""
        var currentPolicy = ""
        val currentRules = mutableListOf<FirewallRule>()

        for (line in output.lines()) {
            when {
                line.startsWith("Chain ") -> {
                    // Save previous chain
                    if (currentChain.isNotEmpty()) {
                        chains.add(FirewallChain(currentChain, currentPolicy, currentRules.toList()))
                        currentRules.clear()
                    }
                    // Chain INPUT (policy ACCEPT 0 packets, 0 bytes)
                    val chainMatch = Regex("Chain (\\S+)\\s+\\(policy (\\S+)").find(line)
                    currentChain = chainMatch?.groupValues?.get(1) ?: line.split(" ")[1]
                    currentPolicy = chainMatch?.groupValues?.get(2) ?: "ACCEPT"
                }
                line.trim().firstOrNull()?.isDigit() == true -> {
                    // num pkts bytes target prot opt in out source destination [options]
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 9) {
                        val num = parts[0].toIntOrNull() ?: 0
                        val pkts = parts[1].toLongOrNull() ?: 0L
                        val bytes = parts[2].toLongOrNull() ?: 0L
                        val target = parts[3]
                        val proto = parts[4]
                        // parts[5]=opt, parts[6]=in, parts[7]=out
                        val src = parts[8]
                        val dst = parts.getOrElse(9) { "0.0.0.0/0" }
                        val opts = parts.drop(10).joinToString(" ")
                        currentRules.add(
                            FirewallRule(
                                id = "$currentChain-$num",
                                chain = currentChain,
                                target = target,
                                protocol = proto,
                                source = src,
                                destination = dst,
                                options = opts,
                                enabled = true,
                                packetsCount = pkts,
                                bytesCount = bytes
                            )
                        )
                    }
                }
            }
        }

        // Save last chain
        if (currentChain.isNotEmpty()) {
            chains.add(FirewallChain(currentChain, currentPolicy, currentRules.toList()))
        }

        return FirewallData("iptables", chains)
    }
}
