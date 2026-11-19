package com.tty.launcher

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.tty.launcher.navigation.TTYApp
import com.tty.launcher.presentation.terminal.TerminalViewModel
import com.tty.launcher.presentation.theme.TTYTheme
import com.tty.launcher.services.system.SettingsPanel
import com.tty.launcher.services.system.SystemIntents
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity host. Registered for both LAUNCHER and HOME so the user can choose TTY as the
 * default home app through Android's own role selection flow.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var systemIntents: SystemIntents

    private val terminalViewModel: TerminalViewModel by viewModels()

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* State is re-read from the adapter on the next command. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val state by terminalViewModel.uiState.collectAsStateWithLifecycle()
            TTYTheme(settings = state.settings) {
                TTYApp(
                    terminalViewModel = terminalViewModel,
                    systemIntents = systemIntents,
                    onRequestBluetoothPermission = ::requestBluetoothPermission
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Returning from the home-role picker or system settings must refresh the banner.
        terminalViewModel.refreshLauncherStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                systemIntents.openPanel(SettingsPanel.BLUETOOTH)
            } else {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            systemIntents.openPanel(SettingsPanel.BLUETOOTH)
        }
    }
}
