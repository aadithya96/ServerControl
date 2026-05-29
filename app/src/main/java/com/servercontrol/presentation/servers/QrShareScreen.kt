package com.servercontrol.presentation.servers

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.util.QrCodeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShareScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ServerListViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val server = servers.find { it.id == serverId }
    val context = LocalContext.current

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(server) {
        server?.let { s ->
            withContext(Dispatchers.Default) {
                val encoded = QrCodeUtil.encodeServerProfile(s)
                qrBitmap = QrCodeUtil.generateQrBitmap(encoded, size = 512)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share as QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            server ?: return@IconButton
                            val bmp = qrBitmap ?: return@IconButton
                            val file = File(context.cacheDir, "qr_${server.name.replace(" ", "_")}.png")
                            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Server QR Code"))
                        },
                        enabled = qrBitmap != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            server?.let { s ->
                Text(
                    text = s.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${s.host}:${s.agentPort}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (qrBitmap != null) {
                Card(modifier = Modifier.size(280.dp)) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Credentials not included — you'll need to re-enter your token or SSH password when importing.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
