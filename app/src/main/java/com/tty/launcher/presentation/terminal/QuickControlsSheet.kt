package com.tty.launcher.presentation.terminal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tty.launcher.presentation.components.SectionLabel
import com.tty.launcher.presentation.components.TerminalButton
import com.tty.launcher.presentation.components.TerminalDivider
import com.tty.launcher.presentation.theme.LocalTerminalPalette
import com.tty.launcher.services.system.SettingsPanel

@Composable
fun QuickControlsSheet(
    isDefaultLauncher: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMonitor: () -> Unit,
    onOpenApplications: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenPanel: (SettingsPanel) -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestHomeRole: () -> Unit,
    onRequestBluetoothPermission: () -> Unit
) {
    val palette = LocalTerminalPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "QUICK CONTROLS",
            color = palette.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "Triple-tap the header to open this panel.",
            color = palette.secondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        SectionLabel("TERMINAL")
        TerminalDivider()
        SheetRow("RESUME TERMINAL", onDismiss)
        SheetRow("TTY SETTINGS", onOpenSettings)
        SheetRow("SYSTEM MONITOR", onOpenMonitor)
        SheetRow("APPLICATIONS", onOpenApplications)
        SheetRow("CALCULATOR", onOpenCalculator)

        SectionLabel("SYSTEM SETTINGS")
        TerminalDivider()
        SheetRow("WI-FI", { onOpenPanel(SettingsPanel.WIFI) })
        SheetRow("BLUETOOTH", { onOpenPanel(SettingsPanel.BLUETOOTH) })
        SheetRow("DISPLAY", { onOpenPanel(SettingsPanel.DISPLAY) })
        SheetRow("SOUND", { onOpenPanel(SettingsPanel.SOUND) })
        SheetRow("BATTERY", { onOpenPanel(SettingsPanel.BATTERY) })
        SheetRow("STORAGE", { onOpenPanel(SettingsPanel.STORAGE) })
        SheetRow("APPS", { onOpenPanel(SettingsPanel.APPS) })

        SectionLabel("PERMISSIONS")
        TerminalDivider()
        SheetRow(
            "NOTIFICATION ACCESS",
            onOpenNotificationAccess
        )
        SheetRow(
            if (isDefaultLauncher) "DEFAULT HOME: ENABLED" else "SET TTY AS DEFAULT HOME",
            onRequestHomeRole
        )
        SheetRow("BLUETOOTH PERMISSION", onRequestBluetoothPermission)

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Nothing leaves this device. No analytics, no account.",
                color = palette.secondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun SheetRow(label: String, onClick: () -> Unit) {
    val palette = LocalTerminalPalette.current
    TerminalButton(
        label = "  " + label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentDescription = label.lowercase()
    )
    TerminalDivider(color = palette.border.copy(alpha = 0.5f))
}
