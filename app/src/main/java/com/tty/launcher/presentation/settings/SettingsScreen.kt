package com.tty.launcher.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tty.launcher.BuildConfig
import com.tty.launcher.domain.models.AppSettings
import com.tty.launcher.domain.models.CursorStyle
import com.tty.launcher.domain.models.FontChoice
import com.tty.launcher.domain.models.HomeScreen
import com.tty.launcher.domain.models.ThemeId
import com.tty.launcher.presentation.components.SectionLabel
import com.tty.launcher.presentation.components.TerminalButton
import com.tty.launcher.presentation.components.TerminalChip
import com.tty.launcher.presentation.components.TerminalDivider
import com.tty.launcher.presentation.components.TerminalRow
import com.tty.launcher.presentation.theme.LocalTerminalPalette
import com.tty.launcher.services.notifications.NotificationAccess
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationAccess by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    var writeSettings by remember { mutableStateOf(com.tty.launcher.services.system.BrightnessController(context).canWrite()) }

    // Permissions are granted in system UI; refresh whenever the user returns to this screen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationAccess = NotificationAccess.isGranted(context)
        writeSettings = com.tty.launcher.services.system.BrightnessController(context).canWrite()
        viewModel.refreshLauncherStatus()
    }

    SettingsContent(
        settings = settings,
        isDefaultLauncher = isDefaultLauncher,
        notificationAccess = notificationAccess,
        writeSettings = writeSettings,
        onBack = onBack,
        onRefreshPermissions = {
            notificationAccess = NotificationAccess.isGranted(context)
            writeSettings = com.tty.launcher.services.system.BrightnessController(context).canWrite()
            viewModel.refreshLauncherStatus()
        },
        viewModel = viewModel
    )
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    isDefaultLauncher: Boolean,
    notificationAccess: Boolean,
    writeSettings: Boolean,
    onBack: () -> Unit,
    onRefreshPermissions: () -> Unit,
    viewModel: SettingsViewModel
) {
    val palette = LocalTerminalPalette.current
    var opacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity * 100f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .systemBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalButton(label = "< BACK", onClick = onBack, contentDescription = "Back")
            Text(
                text = "SETTINGS",
                color = palette.accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        TerminalDivider(modifier = Modifier.padding(top = 6.dp))

        SectionLabel("IDENTITY")
        SettingsTextField(
            label = "USERNAME",
            value = settings.username,
            onCommit = viewModel::setUsername
        )
        SettingsTextField(
            label = "DEVICE ALIAS (HOSTNAME)",
            value = settings.hostname,
            onCommit = viewModel::setHostname
        )
        SettingsTextField(
            label = "STARTUP MESSAGE",
            value = settings.startupMessage,
            onCommit = viewModel::setStartupMessage
        )
        Text(
            text = "PROMPT PREVIEW: ${settings.prompt}",
            color = palette.secondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        SectionLabel("APPEARANCE")
        Text("THEME", color = palette.secondary, fontSize = 11.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ThemeId.entries.forEach { theme ->
                TerminalChip(
                    label = theme.key.uppercase(),
                    selected = settings.theme == theme,
                    onClick = { viewModel.setTheme(theme) }
                )
            }
        }

        Text("FONT", color = palette.secondary, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TerminalChip(
                label = "JETBRAINS MONO",
                selected = settings.font == FontChoice.JETBRAINS_MONO,
                onClick = { viewModel.setFont(FontChoice.JETBRAINS_MONO) }
            )
            TerminalChip(
                label = "SYSTEM MONO",
                selected = settings.font == FontChoice.MONOSPACE,
                onClick = { viewModel.setFont(FontChoice.MONOSPACE) }
            )
        }

        StepperRow(
            label = "FONT SIZE",
            value = "${settings.fontSizeSp} sp",
            onDecrease = { viewModel.setFontSize(settings.fontSizeSp - 1) },
            onIncrease = { viewModel.setFontSize(settings.fontSizeSp + 1) }
        )

        Text("CURSOR", color = palette.secondary, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CursorStyle.entries.forEach { style ->
                TerminalChip(
                    label = style.key.uppercase(),
                    selected = settings.cursor == style,
                    onClick = { viewModel.setCursor(style) }
                )
            }
        }

        Text(
            text = "TERMINAL OPACITY: ${opacity.toInt()}%",
            color = palette.secondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        Slider(
            value = opacity,
            onValueChange = { opacity = it },
            onValueChangeFinished = { viewModel.setOpacity(opacity / 100f) },
            valueRange = 40f..100f,
            colors = SliderDefaults.colors(
                thumbColor = palette.primary,
                activeTrackColor = palette.primary,
                inactiveTrackColor = palette.border
            )
        )

        ToggleRow("ANIMATIONS", settings.animations) { viewModel.setAnimations(it) }
        ToggleRow("BOOT SEQUENCE", settings.bootSequence) { viewModel.setBootSequence(it) }
        ToggleRow("HAPTIC FEEDBACK", settings.haptics) { viewModel.setHaptics(it) }

        SectionLabel("TERMINAL BEHAVIOUR")
        StepperRow(
            label = "HISTORY LIMIT",
            value = settings.historyLimit.toString(),
            onDecrease = { viewModel.setHistoryLimit(settings.historyLimit - 25) },
            onIncrease = { viewModel.setHistoryLimit(settings.historyLimit + 25) }
        )
        SettingsTextField(
            label = "DATE FORMAT",
            value = settings.dateFormat,
            onCommit = viewModel::setDateFormat
        )
        SettingsTextField(
            label = "TIME FORMAT",
            value = settings.timeFormat,
            onCommit = viewModel::setTimeFormat
        )

        Text("DEFAULT SCREEN", color = palette.secondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HomeScreen.entries.forEach { screen ->
                TerminalChip(
                    label = screen.key.uppercase(),
                    selected = settings.defaultScreen == screen,
                    onClick = { viewModel.setDefaultScreen(screen) }
                )
            }
        }

        ToggleRow("TRIPLE-TAP QUICK CONTROLS", settings.tripleTapEnabled) { viewModel.setTripleTap(it) }
        ToggleRow("SHOW SYSTEM STATUS PANEL", settings.showSystemPanel) { viewModel.setSystemPanel(it) }

        SectionLabel("LAUNCHER & PERMISSIONS")
        TerminalRow("DEFAULT HOME", if (isDefaultLauncher) "TTY" else "OTHER APP")
        if (!isDefaultLauncher) {
            Text(
                text = "Android only changes the home app with your explicit approval.",
                color = palette.secondary,
                fontSize = 11.sp
            )
            TerminalButton(
                label = "[ SET TTY AS DEFAULT HOME ]",
                onClick = viewModel::requestHomeRole,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        TerminalDivider(modifier = Modifier.padding(vertical = 6.dp))
        TerminalRow("NOTIFICATION ACCESS", if (notificationAccess) "GRANTED" else "NOT GRANTED")
        TerminalButton(
            label = if (notificationAccess) "[ MANAGE NOTIFICATION ACCESS ]" else "[ GRANT NOTIFICATION ACCESS ]",
            onClick = {
                viewModel.openNotificationAccess()
                onRefreshPermissions()
            }
        )
        TerminalDivider(modifier = Modifier.padding(vertical = 6.dp))
        TerminalRow("WRITE SETTINGS", if (writeSettings) "GRANTED" else "NOT GRANTED")
        Text(
            text = "Used only by the 'brightness' command.",
            color = palette.secondary,
            fontSize = 11.sp
        )
        TerminalButton(
            label = "[ MANAGE WRITE SETTINGS ]",
            onClick = {
                viewModel.openWriteSettings()
                onRefreshPermissions()
            }
        )

        SectionLabel("ABOUT")
        TerminalRow("VERSION", BuildConfig.VERSION_NAME)
        TerminalRow("PACKAGE", BuildConfig.APPLICATION_ID)
        TerminalRow("SDK", "${android.os.Build.VERSION.RELEASE} / ${android.os.Build.VERSION.SDK_INT}")
        Text(
            text = "All data stays on this device. No account, no analytics, no cloud sync.",
            color = palette.secondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        TerminalButton(
            label = "[ RESET ALL SETTINGS TO DEFAULTS ]",
            onClick = viewModel::resetToDefaults,
            modifier = Modifier.padding(top = 18.dp, bottom = 24.dp)
        )
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onCommit: (String) -> Unit
) {
    val palette = LocalTerminalPalette.current
    var text by remember(value) { mutableStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 64) text = it },
            modifier = Modifier.weight(1f),
            label = { Text(label, fontSize = 10.sp, color = palette.secondary) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = palette.primary,
                fontSize = 13.sp
            ),
            shape = RoundedCornerShape(2.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(onDone = { onCommit(text) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.primary,
                unfocusedBorderColor = palette.border,
                cursorColor = palette.primary,
                focusedContainerColor = palette.surface.copy(alpha = 0.4f),
                unfocusedContainerColor = palette.surface.copy(alpha = 0.2f)
            )
        )
        TerminalButton(
            label = "SET",
            onClick = { onCommit(text) },
            contentDescription = "Apply $label",
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalTerminalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.secondary,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = palette.secondary,
                uncheckedTrackColor = palette.surface,
                uncheckedBorderColor = palette.border
            )
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val palette = LocalTerminalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.secondary,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        TerminalButton(label = "-", onClick = onDecrease, contentDescription = "Decrease $label")
        Text(text = value, color = palette.primary, fontSize = 12.sp)
        TerminalButton(label = "+", onClick = onIncrease, contentDescription = "Increase $label")
    }
}
