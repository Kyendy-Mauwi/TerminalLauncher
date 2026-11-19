package com.tty.launcher.presentation.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tty.launcher.domain.models.SystemSnapshot
import com.tty.launcher.presentation.components.SectionLabel
import com.tty.launcher.presentation.components.TerminalButton
import com.tty.launcher.presentation.components.TerminalDivider
import com.tty.launcher.presentation.components.TerminalRow
import com.tty.launcher.presentation.theme.LocalTerminalPalette
import com.tty.launcher.services.system.DeviceInfoProvider
import com.tty.launcher.services.system.SettingsPanel
import com.tty.launcher.services.system.SystemIntents
import com.tty.launcher.services.system.SystemMonitor
import com.tty.launcher.utils.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SystemViewModel @Inject constructor(
    private val systemMonitor: SystemMonitor,
    private val systemIntents: SystemIntents,
    deviceInfoProvider: DeviceInfoProvider
) : ViewModel() {

    val snapshot: StateFlow<SystemSnapshot> = systemMonitor.stream(15_000)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            deviceInfoProvider.snapshot()
        )

    fun openPanel(panel: SettingsPanel) {
        systemIntents.openPanel(panel)
    }

    fun openNotificationAccess() {
        systemIntents.openNotificationAccess()
    }
}

@Composable
fun SystemMonitorScreen(
    onBack: () -> Unit,
    viewModel: SystemViewModel = hiltViewModel()
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val palette = LocalTerminalPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .systemBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalButton(label = "< BACK", onClick = onBack, contentDescription = "Back")
            Text(
                text = "SYSTEM MONITOR",
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        TerminalDivider(modifier = Modifier.padding(top = 6.dp))

        SectionLabel("DEVICE")
        TerminalRow("MODEL", "${snapshot.device.manufacturer} ${snapshot.device.model}")
        TerminalRow("ANDROID", "${snapshot.device.androidRelease} (SDK ${snapshot.device.sdkInt})")
        TerminalRow("CODENAME", snapshot.device.device)
        TerminalRow("PRODUCT", snapshot.device.product)
        TerminalRow("UPTIME", Formatters.uptime(snapshot.uptimeMillis))

        SectionLabel("POWER")
        TerminalRow("BATTERY", snapshot.battery.percent?.let { "$it%" } ?: "UNKNOWN")
        TerminalRow("STATUS", snapshot.battery.status)
        TerminalRow("SOURCE", snapshot.battery.plugType)
        TerminalRow("HEALTH", snapshot.battery.health)
        TerminalRow(
            "TEMPERATURE",
            snapshot.battery.temperatureC?.let { String.format(java.util.Locale.US, "%.1f C", it) } ?: "UNKNOWN"
        )

        SectionLabel("NETWORK")
        TerminalRow("STATUS", if (snapshot.network.connected) "CONNECTED" else "OFFLINE")
        TerminalRow("TRANSPORT", snapshot.network.transport)
        TerminalRow("METERED", if (snapshot.network.isMetered) "YES" else "NO")
        TerminalRow("VALIDATED", if (snapshot.network.isValidated) "YES" else "NO")
        snapshot.network.downstreamKbps?.let { TerminalRow("DOWNSTREAM", "$it kbps") }
        snapshot.network.upstreamKbps?.let { TerminalRow("UPSTREAM", "$it kbps") }

        SectionLabel("STORAGE")
        TerminalRow("TOTAL", Formatters.bytes(snapshot.storage.totalBytes))
        TerminalRow("USED", "${Formatters.bytes(snapshot.storage.usedBytes)} (${snapshot.storage.usedPercent}%)")
        TerminalRow("FREE", Formatters.bytes(snapshot.storage.availableBytes))

        SectionLabel("MEMORY")
        TerminalRow("TOTAL", Formatters.bytes(snapshot.memory.totalBytes))
        TerminalRow("USED", "${Formatters.bytes(snapshot.memory.usedBytes)} (${snapshot.memory.usedPercent}%)")
        TerminalRow("FREE", Formatters.bytes(snapshot.memory.availableBytes))
        TerminalRow("LOW MEMORY", if (snapshot.memory.lowMemory) "YES" else "NO")

        SectionLabel("CONTROLS")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TerminalButton(label = "WI-FI", onClick = { viewModel.openPanel(SettingsPanel.WIFI) })
            TerminalButton(label = "BLUETOOTH", onClick = { viewModel.openPanel(SettingsPanel.BLUETOOTH) })
            TerminalButton(label = "DISPLAY", onClick = { viewModel.openPanel(SettingsPanel.DISPLAY) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TerminalButton(label = "SOUND", onClick = { viewModel.openPanel(SettingsPanel.SOUND) })
            TerminalButton(label = "BATTERY", onClick = { viewModel.openPanel(SettingsPanel.BATTERY) })
            TerminalButton(label = "STORAGE", onClick = { viewModel.openPanel(SettingsPanel.STORAGE) })
        }
        TerminalButton(
            label = "NOTIFICATION ACCESS",
            onClick = viewModel::openNotificationAccess,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Updates on system events with a slow heartbeat. Stops when this screen is not visible.",
            color = palette.secondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}
