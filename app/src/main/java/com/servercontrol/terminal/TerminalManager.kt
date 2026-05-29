package com.servercontrol.terminal

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalManager @Inject constructor() {

    private data class ActiveSession(
        val jschSession: com.jcraft.jsch.Session,
        val channel: ChannelShell,
        val outputStream: OutputStream,
        val inputStream: InputStream,
        val outputFlow: MutableSharedFlow<String>,
        val readerJob: Job,
        val keepAliveJob: Job
    )

    private val sessions = ConcurrentHashMap<String, ActiveSession>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun openSession(profile: ServerProfile, sessionId: String): Result<Unit> {
        return runCatching {
            val jsch = JSch()
            val jschSession = when (profile.authType) {
                AuthType.SSH_KEY -> {
                    profile.sshPrivateKey?.let { key ->
                        jsch.addIdentity("key_$sessionId", key.toByteArray(), null, null)
                    }
                    jsch.getSession(
                        profile.sshUsername ?: "root",
                        profile.host,
                        profile.sshPort
                    )
                }
                AuthType.SSH_PASSWORD -> {
                    jsch.getSession(
                        profile.sshUsername ?: "root",
                        profile.host,
                        profile.sshPort
                    ).also {
                        it.setPassword(profile.sshPassword ?: "")
                    }
                }
                else -> throw IllegalArgumentException("SSH auth not configured for this server")
            }
            jschSession.setConfig("StrictHostKeyChecking", "no")
            jschSession.setConfig("ServerAliveInterval", "30")
            jschSession.connect(15_000)

            val channel = jschSession.openChannel("shell") as ChannelShell
            channel.setPtyType("xterm-256color")
            channel.setPtySize(80, 24, 640, 480)
            channel.isAgentForwarding = false

            val outputStream = channel.outputStream
            val inputStream = channel.inputStream

            channel.connect(10_000)

            val sharedFlow = MutableSharedFlow<String>(
                replay = 0,
                extraBufferCapacity = 512
            )

            val readerJob = scope.launch {
                val buffer = ByteArray(4096)
                try {
                    while (isActive) {
                        val available = inputStream.available()
                        if (available > 0) {
                            val n = inputStream.read(buffer, 0, minOf(available, buffer.size))
                            if (n > 0) {
                                val chunk = String(buffer, 0, n, Charsets.UTF_8)
                                sharedFlow.emit(chunk)
                            }
                        } else if (channel.isClosed) {
                            break
                        } else {
                            delay(20)
                        }
                    }
                } catch (_: Exception) {
                    // stream closed
                }
            }

            val keepAliveJob = scope.launch {
                while (isActive && jschSession.isConnected) {
                    delay(30_000)
                    try {
                        jschSession.sendKeepAliveMsg()
                    } catch (_: Exception) {
                        break
                    }
                }
            }

            sessions[sessionId] = ActiveSession(
                jschSession = jschSession,
                channel = channel,
                outputStream = outputStream,
                inputStream = inputStream,
                outputFlow = sharedFlow,
                readerJob = readerJob,
                keepAliveJob = keepAliveJob
            )
        }
    }

    fun sendInput(sessionId: String, text: String) {
        val session = sessions[sessionId] ?: return
        scope.launch {
            try {
                session.outputStream.write(text.toByteArray(Charsets.UTF_8))
                session.outputStream.flush()
            } catch (_: Exception) {
                // channel closed
            }
        }
    }

    fun outputFlow(sessionId: String): Flow<String> {
        return sessions[sessionId]?.outputFlow ?: MutableSharedFlow()
    }

    fun closeSession(sessionId: String) {
        val session = sessions.remove(sessionId) ?: return
        session.readerJob.cancel()
        session.keepAliveJob.cancel()
        try { session.channel.disconnect() } catch (_: Exception) {}
        try { session.jschSession.disconnect() } catch (_: Exception) {}
    }

    fun resizePTY(sessionId: String, cols: Int, rows: Int) {
        val session = sessions[sessionId] ?: return
        try {
            session.channel.setPtySize(cols, rows, cols * 8, rows * 16)
        } catch (_: Exception) {}
    }

    fun isConnected(sessionId: String): Boolean {
        val session = sessions[sessionId] ?: return false
        return session.jschSession.isConnected && !session.channel.isClosed
    }

    fun closeAllSessions() {
        sessions.keys.toList().forEach { closeSession(it) }
    }
}
