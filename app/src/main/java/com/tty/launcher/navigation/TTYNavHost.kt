package com.tty.launcher.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tty.launcher.domain.models.HomeScreen
import com.tty.launcher.domain.models.TerminalAction
import com.tty.launcher.presentation.applications.ApplicationsScreen
import com.tty.launcher.presentation.calculator.CalculatorScreen
import com.tty.launcher.presentation.components.TerminalButton
import com.tty.launcher.presentation.components.TerminalDivider
import com.tty.launcher.presentation.settings.SettingsScreen
import com.tty.launcher.presentation.system.SystemMonitorScreen
import com.tty.launcher.presentation.terminal.TerminalScreen
import com.tty.launcher.presentation.terminal.TerminalViewModel
import com.tty.launcher.presentation.theme.LocalTerminalPalette
import com.tty.launcher.services.system.SettingsPanel
import com.tty.launcher.services.system.SystemIntents

object TTYRoutes {
    const val TERMINAL = "terminal"
    const val APPLICATIONS = "applications"
    const val SYSTEM = "system"
    const val SETTINGS = "settings"
    const val CALCULATOR = "calculator"
}

@Composable
fun TTYApp(
    terminalViewModel: TerminalViewModel,
    systemIntents: SystemIntents,
    onRequestBluetoothPermission: () -> Unit
) {
    val palette = LocalTerminalPalette.current
    val navController = rememberNavController()
    val state by terminalViewModel.uiState.collectAsStateWithLifecycle()
    val snapshot by terminalViewModel.snapshot.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current
    val activity = context as? Activity
    var appliedDefaultScreen by remember { mutableStateOf(false) }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(TTYRoutes.TERMINAL) { saveState = true; inclusive = false }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(state.settingsLoaded) {
        if (state.settingsLoaded && !appliedDefaultScreen) {
            appliedDefaultScreen = true
            if (state.settings.defaultScreen == HomeScreen.APPLICATIONS) {
                navController.navigate(TTYRoutes.APPLICATIONS) { launchSingleTop = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        terminalViewModel.actions.collect { action ->
            when (action) {
                TerminalAction.OpenSettings -> navigateTo(TTYRoutes.SETTINGS)
                TerminalAction.OpenSystemMonitor -> navigateTo(TTYRoutes.SYSTEM)
                TerminalAction.OpenApplications -> navigateTo(TTYRoutes.APPLICATIONS)
                TerminalAction.OpenCalculator -> navigateTo(TTYRoutes.CALCULATOR)
                TerminalAction.GoHome -> {
                    if (terminalViewModel.uiState.value.isDefaultLauncher) {
                        navController.popBackStack(TTYRoutes.TERMINAL, inclusive = false)
                    } else {
                        systemIntents.goToSystemHome()
                    }
                }
                TerminalAction.NavigateBack -> {
                    if (!navController.popBackStack()) {
                        if (!terminalViewModel.uiState.value.isDefaultLauncher) activity?.finish()
                    }
                }
                TerminalAction.FinishActivity -> activity?.finish()
                TerminalAction.RequestHomeRole -> systemIntents.openHomeRoleSettings()
                TerminalAction.RequestNotificationAccess -> systemIntents.openNotificationAccess()
                TerminalAction.RequestWriteSettings -> systemIntents.openWriteSettings()
                TerminalAction.GrantBluetoothPermission -> onRequestBluetoothPermission()
                is TerminalAction.LaunchIntent -> runCatching { context.startActivity(action.intent) }
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            currentRoute != null && currentRoute != TTYRoutes.TERMINAL -> {
                navController.popBackStack(TTYRoutes.TERMINAL, inclusive = false)
            }
            !state.isDefaultLauncher -> activity?.finish()
            // As the default home app, back must never leave the user without a home screen.
            else -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            TTYBottomBar(
                currentRoute = currentRoute,
                onNavigate = ::navigateTo
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TTYRoutes.TERMINAL,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(TTYRoutes.TERMINAL) {
                TerminalScreen(
                    state = state,
                    snapshot = snapshot,
                    isDefaultLauncher = state.isDefaultLauncher,
                    onInputChange = terminalViewModel::onInputChange,
                    onSubmit = terminalViewModel::submit,
                    onSkipBoot = terminalViewModel::skipBoot,
                    onHistoryPrevious = terminalViewModel::historyPrevious,
                    onHistoryNext = terminalViewModel::historyNext,
                    onComplete = terminalViewModel::completeInput,
                    onOpenSettings = { navigateTo(TTYRoutes.SETTINGS) },
                    onOpenMonitor = { navigateTo(TTYRoutes.SYSTEM) },
                    onOpenApplications = { navigateTo(TTYRoutes.APPLICATIONS) },
                    onOpenCalculator = { navigateTo(TTYRoutes.CALCULATOR) },
                    onOpenPanel = { panel: SettingsPanel -> systemIntents.openPanel(panel) },
                    onOpenNotificationAccess = { systemIntents.openNotificationAccess() },
                    onRequestHomeRole = {
                        systemIntents.openHomeRoleSettings()
                        terminalViewModel.refreshLauncherStatus()
                    },
                    onRequestBluetoothPermission = onRequestBluetoothPermission
                )
            }
            composable(TTYRoutes.APPLICATIONS) {
                ApplicationsScreen()
            }
            composable(TTYRoutes.SYSTEM) {
                SystemMonitorScreen(onBack = { navigateTo(TTYRoutes.TERMINAL) })
            }
            composable(TTYRoutes.SETTINGS) {
                SettingsScreen(onBack = { navigateTo(TTYRoutes.TERMINAL) })
            }
            composable(TTYRoutes.CALCULATOR) {
                CalculatorScreen(onBack = { navigateTo(TTYRoutes.TERMINAL) })
            }
        }
    }

    LaunchedEffect(currentRoute) {
        terminalViewModel.refreshLauncherStatus()
    }
}

@Composable
private fun TTYBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val palette = LocalTerminalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .navigationBarsPadding()
    ) {
        TerminalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem("TERM", currentRoute == TTYRoutes.TERMINAL, Modifier.weight(1f)) {
                onNavigate(TTYRoutes.TERMINAL)
            }
            BottomItem("APPS", currentRoute == TTYRoutes.APPLICATIONS, Modifier.weight(1f)) {
                onNavigate(TTYRoutes.APPLICATIONS)
            }
            BottomItem("SYS", currentRoute == TTYRoutes.SYSTEM, Modifier.weight(1f)) {
                onNavigate(TTYRoutes.SYSTEM)
            }
            BottomItem("SET", currentRoute == TTYRoutes.SETTINGS, Modifier.weight(1f)) {
                onNavigate(TTYRoutes.SETTINGS)
            }
        }
    }
}

@Composable
private fun BottomItem(
    label: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    TerminalButton(
        label = label,
        onClick = onClick,
        active = active,
        modifier = modifier,
        contentDescription = label
    )
}

@Suppress("unused")
private fun NavHostController.currentRouteName(): String? =
    currentBackStackEntry?.destination?.route
