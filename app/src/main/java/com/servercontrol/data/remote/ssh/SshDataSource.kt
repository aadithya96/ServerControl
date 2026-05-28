package com.servercontrol.data.remote.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.servercontrol.domain.model.*
import com.servercontrol.util.Resource
import java.io.ByteArrayOutputStream

class SshDataSource {

    private fun createSession(server: ServerProfile): Session {
        val jsch = JSch()
        val session: Session

        when (server.authType) {
            AuthType.SSH_KEY -> {
                server.sshPrivateKey?.let { key ->
                    jsch.addIdentity("key", key.toByteArray(), null, null)
                }
                session = jsch.getSession(server.sshUsername ?: "root", server.host, server.sshPort)
            }
            AuthType.SSH_PASSWORD -> {
                session = jsch.getSession(server.sshUsername ?: "root", server.host, server.sshPort)
                session.setPassword(server.sshPassword ?: "")
            }
            else -> throw IllegalArgumentException("SSH auth not configured")
        }

        session.setConfig("StrictHostKeyChecking", "no")
        session.connect(10_000)
        return session
    }

    private fun executeCommand(session: Session, command: String): String {
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        val output = ByteArrayOutputStream()
        channel.outputStream = output
        channel.connect(5_000)
        while (!channel.isClosed) Thread.sleep(50)
        channel.disconnect()
        return output.toString(Charsets.UTF_8.name())
    }

    suspend fun getSystemStats(server: ServerProfile): Resource<SystemStats> = try {
        val session = createSession(server)
        val cpuOutput = executeCommand(session, "cat /proc/stat | head -1")
        val memOutput = executeCommand(session, "cat /proc/meminfo")
        val uptimeOutput = executeCommand(session, "cat /proc/uptime")
        val loadOutput = executeCommand(session, "cat /proc/loadavg")
        session.disconnect()

        val stats = parseProcStats(cpuOutput, memOutput, uptimeOutput, loadOutput)
        Resource.Success(stats)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "SSH stats failed", e)
    }

    suspend fun getProcesses(server: ServerProfile): Resource<List<Process>> = try {
        val session = createSession(server)
        val output = executeCommand(session, "ps aux --no-headers")
        session.disconnect()
        Resource.Success(parsePsOutput(output))
    } catch (e: Exception) {
        Resource.Error(e.message ?: "SSH processes failed", e)
    }

    suspend fun getDiskInfo(server: ServerProfile): Resource<List<DiskInfo>> = try {
        val session = createSession(server)
        val output = executeCommand(session, "df -B1 --output=source,target,fstype,size,used,avail")
        session.disconnect()
        Resource.Success(parseDfOutput(output))
    } catch (e: Exception) {
        Resource.Error(e.message ?: "SSH disk info failed", e)
    }

    suspend fun getConnections(server: ServerProfile): Resource<List<NetworkConnection>> = try {
        val session = createSession(server)
        val output = executeCommand(session, "ss -tunap")
        session.disconnect()
        Resource.Success(parseSsOutput(output))
    } catch (e: Exception) {
        Resource.Error(e.message ?: "SSH connections failed", e)
    }

    suspend fun getFirewallRules(server: ServerProfile): Resource<List<FirewallRule>> = try {
        val session = createSession(server)
        val output = executeCommand(session, "iptables -L -n -v --line-numbers 2>/dev/null || echo 'ERROR'")
        session.disconnect()
        Resource.Success(parseIptablesOutput(output))
    } catch (e: Exception) {
        Resource.Error(e.message ?: "SSH firewall failed", e)
    }

    suspend fun killProcess(server: ServerProfile, pid: Int): Resource<Unit> = try {
        val session = createSession(server)
        executeCommand(session, "kill -9 $pid")
        session.disconnect()
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "SSH kill process failed", e)
    }

    // --- Parsers ---

    private fun parseProcStats(
        cpuLine: String, memInfo: String, uptime: String, loadAvg: String
    ): SystemStats {
        val cpuParts = cpuLine.trim().split("\\s+".toRegex())
        val user = cpuParts.getOrElse(1) { "0" }.toLongOrNull() ?: 0L
        val nice = cpuParts.getOrElse(2) { "0" }.toLongOrNull() ?: 0L
        val system = cpuParts.getOrElse(3) { "0" }.toLongOrNull() ?: 0L
        val idle = cpuParts.getOrElse(4) { "0" }.toLongOrNull() ?: 0L
        val total = user + nice + system + idle
        val cpuPercent = if (total > 0) (total - idle).toDouble() / total * 100 else 0.0

        val memMap = memInfo.lines()
            .mapNotNull { line ->
                val parts = line.split(":\\s+".toRegex())
                if (parts.size >= 2) parts[0].trim() to (parts[1].trim().split(" ")[0].toLongOrNull() ?: 0L) * 1024
                else null
            }.toMap()

        val memTotal = memMap["MemTotal"] ?: 0L
        val memAvail = memMap["MemAvailable"] ?: memMap["MemFree"] ?: 0L
        val swapTotal = memMap["SwapTotal"] ?: 0L
        val swapFree = memMap["SwapFree"] ?: 0L

        val uptimeSecs = uptime.trim().split(" ")[0].toDoubleOrNull()?.toLong() ?: 0L

        val loadParts = loadAvg.trim().split("\\s+".toRegex())
        val load1 = loadParts.getOrElse(0) { "0.0" }.toDoubleOrNull() ?: 0.0
        val load5 = loadParts.getOrElse(1) { "0.0" }.toDoubleOrNull() ?: 0.0
        val load15 = loadParts.getOrElse(2) { "0.0" }.toDoubleOrNull() ?: 0.0

        return SystemStats(
            cpuPercent = cpuPercent,
            memTotalBytes = memTotal,
            memUsedBytes = memTotal - memAvail,
            swapTotalBytes = swapTotal,
            swapUsedBytes = swapTotal - swapFree,
            uptimeSeconds = uptimeSecs,
            loadAvg1m = load1,
            loadAvg5m = load5,
            loadAvg15m = load15
        )
    }

    private fun parsePsOutput(output: String): List<Process> {
        return output.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex(), limit = 11)
                if (parts.size < 11) return@mapNotNull null
                Process(
                    user = parts[0],
                    pid = parts[1].toIntOrNull() ?: return@mapNotNull null,
                    cpuPercent = parts[2].toDoubleOrNull() ?: 0.0,
                    memPercent = parts[3].toDoubleOrNull() ?: 0.0,
                    memRss = (parts[5].toLongOrNull() ?: 0L) * 1024,
                    status = parts[7],
                    name = parts[10].substringAfterLast("/").split(" ")[0],
                    command = parts[10]
                )
            }
    }

    private fun parseDfOutput(output: String): List<DiskInfo> {
        return output.lines()
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size < 6) return@mapNotNull null
                DiskInfo(
                    device = parts[0],
                    mountPoint = parts[1],
                    fsType = parts[2],
                    totalBytes = parts[3].toLongOrNull() ?: 0L,
                    usedBytes = parts[4].toLongOrNull() ?: 0L,
                    freeBytes = parts[5].toLongOrNull() ?: 0L
                )
            }
    }

    private fun parseSsOutput(output: String): List<NetworkConnection> {
        return output.lines()
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size < 5) return@mapNotNull null
                val localParts = parts[4].substringBeforeLast(":") to (parts[4].substringAfterLast(":").toIntOrNull() ?: 0)
                val remoteParts = parts.getOrElse(5) { "0.0.0.0:0" }.let {
                    it.substringBeforeLast(":") to (it.substringAfterLast(":").toIntOrNull() ?: 0)
                }
                val pidInfo = parts.lastOrNull { it.startsWith("pid=") }
                NetworkConnection(
                    protocol = parts[0],
                    localAddress = localParts.first,
                    localPort = localParts.second,
                    remoteAddress = remoteParts.first,
                    remotePort = remoteParts.second,
                    state = if (parts.size > 3) parts[1] else "UNKNOWN",
                    pid = pidInfo?.substringAfter("pid=")?.substringBefore(",")?.toIntOrNull(),
                    processName = null
                )
            }
    }

    private fun parseIptablesOutput(output: String): List<FirewallRule> {
        if (output.startsWith("ERROR")) return emptyList()
        var currentChain = ""
        val rules = mutableListOf<FirewallRule>()
        var ruleIndex = 0

        for (line in output.lines()) {
            when {
                line.startsWith("Chain ") -> {
                    currentChain = line.split(" ")[1]
                }
                line.trim().firstOrNull()?.isDigit() == true -> {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 9) {
                        rules.add(
                            FirewallRule(
                                id = "${currentChain}_${ruleIndex++}",
                                chain = currentChain,
                                target = parts[3],
                                protocol = parts[4],
                                source = parts[7],
                                destination = parts[8],
                                options = parts.drop(9).joinToString(" "),
                                enabled = true,
                                packetsCount = parts[1].toLongOrNull() ?: 0L,
                                bytesCount = parts[2].toLongOrNull() ?: 0L
                            )
                        )
                    }
                }
            }
        }
        return rules
    }
}
