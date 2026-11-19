package com.tty.launcher.presentation.terminal

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tty.launcher.domain.models.LineStyle
import com.tty.launcher.domain.models.SystemSnapshot
import com.tty.launcher.presentation.components.BlinkingCursor
import com.tty.launcher.presentation.components.MatrixRain
import com.tty.launcher.presentation.components.TerminalButton
import com.tty.launcher.presentation.theme.LocalTerminalPalette
import com.tty.launcher.presentation.theme.colorFor
import com.tty.launcher.presentation.theme.fontFamilyFor
import com.tty.launcher.utils.Formatters
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    state: TerminalUiState,
    snapshot: SystemSnapshot?,
    isDefaultLauncher: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkipBoot: () -> Unit,
    onHistoryPrevious: () -> Unit,
    onHistoryNext: () -> Unit,
    onComplete: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMonitor: () -> Unit,
    onOpenApplications: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenPanel: (com.tty.launcher.services.system.SettingsPanel) -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestHomeRole: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTerminalPalette.current
    val settings = state.settings
    val fontFamily = fontFamilyFor(settings.font)
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current

    val submitWithHaptic: () -> Unit = {
        if (settings.haptics) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onSubmit()
    }

    var showQuickControls by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }

    // Auto-scroll to the newest output.
    LaunchedEffect(state.lines.size) {
        if (state.lines.isEmpty()) return@LaunchedEffect
        if (settings.animations) {
            listState.animateScrollToItem(state.lines.lastIndex)
        } else {
            listState.scrollToItem(state.lines.lastIndex)
        }
    }

    LaunchedEffect(state.isBooting) {
        if (!state.isBooting) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background.copy(alpha = settings.opacity))
        ) {
            MatrixRain(enabled = palette.isMatrix && settings.animations)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            TerminalHeader(
                state = state,
                snapshot = snapshot,
                isDefaultLauncher = isDefaultLauncher,
                onRequestHomeRole = onRequestHomeRole,
                onTripleTap = {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastTapAt > 800) tapCount = 0
                    tapCount += 1
                    lastTapAt = now
                    if (tapCount >= 3 && settings.tripleTapEnabled) {
                        tapCount = 0
                        showQuickControls = true
                    }
                }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(state.isBooting) {
                        detectTapGestures(onTap = {
                            if (state.isBooting) onSkipBoot() else {
                                focusRequester.requestFocus()
                                keyboard?.show()
                            }
                        })
                    },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(items = state.lines, key = { it.id }) { line ->
                    Text(
                        text = line.text.ifEmpty { " " },
                        color = line.style.colorFor(palette),
                        fontFamily = fontFamily,
                        fontSize = settings.fontSizeSp.sp,
                        lineHeight = (settings.fontSizeSp * 1.4f).sp,
                        letterSpacing = 0.2.sp,
                        fontWeight = if (line.style == LineStyle.BANNER) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    if (state.isExecuting) {
                        Text(
                            text = "…",
                            color = palette.accent,
                            fontFamily = fontFamily,
                            fontSize = settings.fontSizeSp.sp
                        )
                    }
                }
            }

            if (state.isBooting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TerminalButton(
                        label = "[ SKIP ]",
                        onClick = onSkipBoot,
                        contentDescription = "Skip startup sequence"
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = settings.prompt,
                    color = palette.primary,
                    fontFamily = fontFamily,
                    fontSize = settings.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " ",
                    fontFamily = fontFamily,
                    fontSize = settings.fontSizeSp.sp
                )
                BasicTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "Command input" }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Enter, Key.NumPadEnter -> {
                                    focusManager.clearFocus()
                                    submitWithHaptic()
                                    true
                                }
                                Key.DirectionUp -> {
                                    onHistoryPrevious()
                                    true
                                }
                                Key.DirectionDown -> {
                                    onHistoryNext()
                                    true
                                }
                                Key.Tab -> {
                                    onComplete()
                                    true
                                }
                                else -> false
                            }
                        },
                    textStyle = TextStyle(
                        color = palette.primary,
                        fontFamily = fontFamily,
                        fontSize = settings.fontSizeSp.sp,
                        letterSpacing = 0.2.sp
                    ),
                    cursorBrush = SolidColor(palette.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = {
                        focusManager.clearFocus()
                        submitWithHaptic()
                    }),
                    singleLine = false,
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            innerTextField()
                            BlinkingCursor(
                                style = settings.cursor,
                                animations = settings.animations && !state.isBooting
                            )
                        }
                    }
                )
                TerminalButton(
                    label = "↵",
                    onClick = {
                        focusManager.clearFocus()
                        submitWithHaptic()
                    },
                    contentDescription = "Run command"
                )
            }
        }
    }

    if (showQuickControls) {
        ModalBottomSheet(
            onDismissRequest = { showQuickControls = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.surface,
            contentColor = palette.primary,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            QuickControlsSheet(
                isDefaultLauncher = isDefaultLauncher,
                onDismiss = { showQuickControls = false },
                onOpenSettings = onOpenSettings,
                onOpenMonitor = onOpenMonitor,
                onOpenApplications = onOpenApplications,
                onOpenCalculator = onOpenCalculator,
                onOpenPanel = onOpenPanel,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onRequestHomeRole = onRequestHomeRole,
                onRequestBluetoothPermission = onRequestBluetoothPermission
            )
        }
    }
}

@Composable
private fun TerminalHeader(
    state: TerminalUiState,
    snapshot: SystemSnapshot?,
    isDefaultLauncher: Boolean,
    onRequestHomeRole: () -> Unit,
    onTripleTap: () -> Unit
) {
    val palette = LocalTerminalPalette.current
    val settings = state.settings
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(5_000)
        }
    }
    val battery = snapshot?.battery
    val network = snapshot?.network
    val device = snapshot?.device

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(settings.tripleTapEnabled) {
                detectTapGestures(onTap = { onTripleTap() })
            }
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = settings.prompt,
                color = palette.primary,
                fontFamily = fontFamilyFor(settings.font),
                fontSize = settings.fontSizeSp.sp,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = Formatters.time(now, settings.timeFormat),
                color = palette.accent,
                fontFamily = fontFamilyFor(settings.font),
                fontSize = settings.fontSizeSp.sp
            )
        }

        TerminalDividerSpacer()

        if (settings.showSystemPanel) {
            Text(
                text = buildStatusLine(device?.model, battery?.percent, battery?.isCharging, network?.transport, network?.connected),
                color = palette.secondary,
                fontFamily = fontFamilyFor(settings.font),
                fontSize = (settings.fontSizeSp * 0.85f).sp,
                letterSpacing = 0.5.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (!isDefaultLauncher) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onRequestHomeRole)
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "○ TTY IS NOT THE DEFAULT HOME",
                    color = palette.warning,
                    fontFamily = fontFamilyFor(settings.font),
                    fontSize = (settings.fontSizeSp * 0.8f).sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "SET >",
                    color = palette.accent,
                    fontFamily = fontFamilyFor(settings.font),
                    fontSize = (settings.fontSizeSp * 0.8f).sp
                )
            }
        }
    }
}

@Composable
private fun TerminalDividerSpacer() {
    val palette = LocalTerminalPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(palette.border)
            .padding(top = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp)
                .background(palette.border)
        )
    }
}

private fun buildStatusLine(
    model: String?,
    batteryPercent: Int?,
    charging: Boolean?,
    transport: String?,
    connected: Boolean?
): String {
    val parts = mutableListOf<String>()
    parts += (model ?: "ANDROID").uppercase()
    parts += "BAT ${batteryPercent?.let { "$it%" } ?: "--"}${if (charging == true) " ⚡" else ""}"
    parts += if (connected == true) (transport ?: "ONLINE") else "OFFLINE"
    return parts.joinToString("  ·  ")
}
