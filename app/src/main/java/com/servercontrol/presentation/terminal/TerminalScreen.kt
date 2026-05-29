package com.servercontrol.presentation.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.terminal.SessionState
import com.servercontrol.terminal.TerminalSession
import kotlinx.coroutines.launch

private val TerminalBackground = Color(0xFF0D1117)
private val TerminalText = Color(0xFFE6EDF3)
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

    var inputText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TerminalBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161B22),
                    titleContentColor = TerminalText,
                    actionIconContentColor = TerminalText,
                    navigationIconContentColor = TerminalText
                ),
                title = {
                    TabRow(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        sessionStates = sessionStates,
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
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color(0xFF161B22))) {
                ExtendedKeyRow(
                    onKey = { key -> viewModel.sendInput(key) },
                    onControlChar = { char -> viewModel.sendControlChar(char) }
                )
                HorizontalDivider(color = Color(0xFF30363D))
                InputRow(
                    inputText = inputText,
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
                .background(TerminalBackground)
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
                                color = TerminalText,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                SessionState.DISCONNECTED, SessionState.ERROR -> {
                    TerminalOutput(
                        output = activeTabId?.let { outputs[it] } ?: AnnotatedString(""),
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxSize()
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
                        modifier = Modifier.fillMaxSize()
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
                        color = if (isActive) TerminalText else TerminalText.copy(alpha = 0.6f),
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
                            tint = TerminalText.copy(alpha = 0.6f),
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
                    tint = TerminalText.copy(alpha = 0.7f),
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
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Split output into lines for display in LazyColumn
    val lines = remember(output) {
        // We split on newline boundaries while keeping ANSI spans intact
        val text = output.text
        val lineRanges = mutableListOf<IntRange>()
        var start = 0
        text.forEachIndexed { i, c ->
            if (c == '\n') {
                lineRanges.add(start..i)
                start = i + 1
            }
        }
        if (start <= text.length) {
            lineRanges.add(start until text.length)
        }
        lineRanges.map { range ->
            if (range.first <= range.last && range.last <= output.length) {
                output.subSequence(range.first, range.last + 1)
            } else {
                AnnotatedString("")
            }
        }
    }

    // Auto-scroll to bottom when new output arrives
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(lines.size - 1)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(TerminalBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(lines.size) { index ->
            Text(
                text = lines[index],
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = TerminalText,
                lineHeight = (fontSize * 1.4).sp
            )
        }
    }
}

@Composable
private fun ExtendedKeyRow(
    onKey: (String) -> Unit,
    onControlChar: (Char) -> Unit
) {
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
            onClick = { onControlChar('') },
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
            onClick = { onControlChar('') },
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFC107)),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, Color(0xFFFFC107).copy(alpha = 0.5f)
            )
        ) {
            Text("Ctrl+D", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        keys.forEach { (label, seq) ->
            OutlinedButton(
                onClick = { onKey(seq) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalText),
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
                .background(Color(0xFF0D1117), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            textStyle = TextStyle(
                color = TerminalText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(TabActiveColor),
            decorationBox = { innerTextField ->
                Box {
                    if (inputText.isEmpty()) {
                        Text(
                            "type command here...",
                            color = TerminalText.copy(alpha = 0.3f),
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
