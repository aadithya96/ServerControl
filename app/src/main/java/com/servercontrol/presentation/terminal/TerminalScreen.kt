package com.servercontrol.presentation.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.terminal.SessionState
import com.servercontrol.terminal.TerminalColorTheme
import com.servercontrol.terminal.TerminalSession
import com.servercontrol.terminal.TerminalThemes
import kotlinx.coroutines.launch

private val TabActiveColor = Color(0xFF238636)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val outputs by viewModel.outputs.collectAsState()
    val sessionStates by viewModel.sessionStates.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val colorTheme by viewModel.colorTheme.collectAsState()

    val colors = remember(colorTheme) { TerminalThemes.forTheme(colorTheme) }
    val terminalBackground = colors.background
    val terminalText = colors.foreground

    var inputText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        containerColor = terminalBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161B22),
                    titleContentColor = terminalText,
                    actionIconContentColor = terminalText,
                    navigationIconContentColor = terminalText
                ),
                title = {
                    TabRow(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        sessionStates = sessionStates,
                        terminalText = terminalText,
                        onSelectTab = viewModel::selectTab,
                        onCloseTab = { id ->
                            if (tabs.size > 1) viewModel.closeTab(id)
                        },
                        onAddTab = viewModel::addTab
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (showSettings) {
                        DropdownMenu(
                            expanded = showSettings,
                            onDismissRequest = { showSettings = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Font Size: ${fontSize}sp") },
                                onClick = {}
                            )
                            DropdownMenuItem(
                                text = { Text("Font +") },
                                onClick = {
                                    viewModel.increaseFontSize()
                                    showSettings = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Font -") },
                                onClick = {
                                    viewModel.decreaseFontSize()
                                    showSettings = false
                                }
                            )
                            HorizontalDivider()
                            Text(
                                "Theme",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            TerminalColorTheme.values().forEach { theme ->
                                DropdownMenuItem(
                                    text = { Text(theme.displayName) },
                                    onClick = {
                                        viewModel.setColorTheme(theme)
                                        showSettings = false
                                    },
                                    trailingIcon = {
                                        if (colorTheme == theme) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color(0xFF161B22))) {
                ExtendedKeyRow(
                    terminalText = terminalText,
                    onKey = { key -> viewModel.sendInput(key) },
                    onControlChar = { char -> viewModel.sendControlChar(char) },
                    onCopy = {
                        val allText = activeTabId?.let { outputs[it]?.text } ?: ""
                        clipboardManager.setText(AnnotatedString(allText))
                    },
                    onPaste = {
                        val text = clipboardManager.getText()?.text ?: ""
                        if (text.isNotEmpty()) viewModel.sendInput(text)
                    }
                )
                HorizontalDivider(color = Color(0xFF30363D))
                InputRow(
                    inputText = inputText,
                    terminalText = terminalText,
                    terminalBackground = terminalBackground,
                    onInputChange = { inputText = it },
                    onSend = {
                        viewModel.sendInput(inputText + "\n")
                        inputText = ""
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(terminalBackground)
        ) {
            val activeState = activeTabId?.let { sessionStates[it] }

            when (activeState) {
                SessionState.CONNECTING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = TabActiveColor)
                            Text(
                                "Connecting...",
                                color = terminalText,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                SessionState.DISCONNECTED, SessionState.ERROR -> {
                    TerminalOutput(
                        output = activeTabId?.let { outputs[it] } ?: AnnotatedString(""),
                        fontSize = fontSize,
                        terminalBackground = terminalBackground,
                        terminalText = terminalText,
                        modifier = Modifier.fillMaxSize(),
                        onFontSizeChange = { viewModel.setFontSize(it) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (activeState == SessionState.ERROR) "Connection Error" else "Disconnected",
                                    color = Color(0xFFFF5555),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Button(
                                    onClick = { activeTabId?.let { viewModel.reconnect(it) } },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TabActiveColor
                                    )
                                ) {
                                    Text("Reconnect")
                                }
                            }
                        }
                    }
                }
                else -> {
                    TerminalOutput(
                        output = activeTabId?.let { outputs[it] } ?: AnnotatedString(""),
                        fontSize = fontSize,
                        terminalBackground = terminalBackground,
                        terminalText = terminalText,
                        modifier = Modifier.fillMaxSize(),
                        onFontSizeChange = { viewModel.setFontSize(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabRow(
    tabs: List<TerminalSession>,
    activeTabId: String?,
    sessionStates: Map<String, SessionState>,
    terminalText: Color,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddTab: () -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == activeTabId
            val state = sessionStates[tab.id]
            val dotColor = when (state) {
                SessionState.CONNECTED -> Color(0xFF4CAF50)
                SessionState.CONNECTING -> Color(0xFFFFC107)
                SessionState.DISCONNECTED, SessionState.ERROR -> Color(0xFFF44336)
                null -> Color(0xFF777777)
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .background(
                        if (isActive) Color(0xFF238636).copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dotColor, shape = androidx.compose.foundation.shape.CircleShape)
                )
                TextButton(
                    onClick = { onSelectTab(tab.id) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = tab.title,
                        color = if (isActive) terminalText else terminalText.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                if (tabs.size > 1) {
                    IconButton(
                        onClick = { onCloseTab(tab.id) },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close tab",
                            tint = terminalText.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (tabs.size < 8) {
            IconButton(onClick = onAddTab, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add tab",
                    tint = terminalText.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TerminalOutput(
    output: AnnotatedString,
    fontSize: Int,
    terminalBackground: Color,
    terminalText: Color,
    modifier: Modifier = Modifier,
    onFontSizeChange: (Int) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Pinch-to-zoom state
    var scale by remember { mutableStateOf(1f) }
    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 2.5f)
        val newSize = (13 * scale).toInt().coerceIn(8, 32)
        onFontSizeChange(newSize)
    }

    // Split output into lines for display in LazyColumn. Each line EXCLUDES its
    // trailing '\n' — keeping the newline in the slice makes every Text render an
    // extra blank row, which doubled the line spacing and mangled the output.
    val lines = remember(output) {
        val text = output.text
        if (text.isEmpty()) return@remember listOf(AnnotatedString(""))
        val result = mutableListOf<AnnotatedString>()
        var start = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                result.add(output.subSequence(start, i))
                start = i + 1
            }
        }
        // Trailing segment after the last newline (empty if text ends with '\n').
        result.add(output.subSequence(start, text.length))
        result
    }

    // Auto-scroll to bottom when new output arrives
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(lines.size - 1)
            }
        }
    }

    SelectionContainer(
        modifier = modifier
            .background(terminalBackground)
            .transformable(state = transformableState)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(lines) { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    color = terminalText,
                    lineHeight = (fontSize * 1.4).sp
                )
            }
        }
    }
}

@Composable
private fun ExtendedKeyRow(
    terminalText: Color,
    onKey: (String) -> Unit,
    onControlChar: (Char) -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit
) {
    // Control/navigation keys must include the ESC () prefix so the remote
    // PTY recognises them as escape sequences rather than literal "[A" text.
    val keys = listOf(
        "Tab" to "\t",
        "Esc" to "",
        "↑" to "[A",
        "↓" to "[B",
        "←" to "[D",
        "→" to "[C",
        "PgUp" to "[5~",
        "PgDn" to "[6~",
        "Home" to "[H",
        "End" to "[F",
        "|" to "|",
        "~" to "~",
        "/" to "/"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Ctrl+C
        OutlinedButton(
            onClick = { onControlChar('\u0003') },
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5555)),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Color(0xFFFF5555).copy(alpha = 0.5f)
            )
        ) {
            Text("Ctrl+C", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        // Ctrl+D
        OutlinedButton(
            onClick = { onControlChar('\u0004') },
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFC107)),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Color(0xFFFFC107).copy(alpha = 0.5f)
            )
        ) {
            Text("Ctrl+D", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        // Copy button
        OutlinedButton(
            onClick = onCopy,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = terminalText),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Color(0xFF30363D)
            )
        ) {
            Text("Copy", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        // Paste button
        OutlinedButton(
            onClick = onPaste,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = terminalText),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Color(0xFF30363D)
            )
        ) {
            Text("Paste", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        keys.forEach { (label, seq) ->
            OutlinedButton(
                onClick = { onKey(seq) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = terminalText),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color(0xFF30363D)
                )
            ) {
                Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun InputRow(
    inputText: String,
    terminalText: Color,
    terminalBackground: Color,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .background(terminalBackground, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            textStyle = TextStyle(
                color = terminalText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(TabActiveColor),
            decorationBox = { innerTextField ->
                Box {
                    if (inputText.isEmpty()) {
                        Text(
                            "type command here...",
                            color = terminalText.copy(alpha = 0.3f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(40.dp)
                .background(TabActiveColor, RoundedCornerShape(8.dp))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
