package com.tty.launcher.domain.models

import android.content.Intent

/**
 * Side effects a command can request from the UI layer. Keeping them declarative means the
 * dispatcher stays testable and the Activity owns permissions and navigation.
 */
sealed interface TerminalAction {
    data object OpenSettings : TerminalAction
    data object OpenSystemMonitor : TerminalAction
    data object OpenApplications : TerminalAction
    data object OpenCalculator : TerminalAction
    data object GoHome : TerminalAction
    data object NavigateBack : TerminalAction
    data object FinishActivity : TerminalAction
    data object RequestHomeRole : TerminalAction
    data object RequestNotificationAccess : TerminalAction
    data object RequestWriteSettings : TerminalAction
    data object GrantBluetoothPermission : TerminalAction
    data class LaunchIntent(val intent: Intent) : TerminalAction
}
