package com.tty.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tty.launcher.domain.models.AppSettings
import com.tty.launcher.domain.models.LineStyle
import com.tty.launcher.presentation.terminal.TerminalLineUi
import com.tty.launcher.presentation.terminal.TerminalScreen
import com.tty.launcher.presentation.terminal.TerminalUiState
import com.tty.launcher.presentation.theme.TTYTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TerminalScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setTerminal(
        isBooting: Boolean = false,
        onInputChange: (String) -> Unit = {},
        onSkipBoot: () -> Unit = {}
    ) {
        composeRule.setContent {
            TTYTheme(AppSettings()) {
                TerminalScreen(
                    state = TerminalUiState(
                        lines = listOf(
                            TerminalLineUi(1, "SYSTEM READY.", LineStyle.SUCCESS),
                            TerminalLineUi(2, "Type 'help' to see available commands.", LineStyle.DIM)
                        ),
                        settings = AppSettings(),
                        isBooting = isBooting
                    ),
                    snapshot = null,
                    isDefaultLauncher = true,
                    onInputChange = onInputChange,
                    onSubmit = {},
                    onSkipBoot = onSkipBoot,
                    onHistoryPrevious = {},
                    onHistoryNext = {},
                    onComplete = {},
                    onOpenSettings = {},
                    onOpenMonitor = {},
                    onOpenApplications = {},
                    onOpenCalculator = {},
                    onOpenPanel = {},
                    onOpenNotificationAccess = {},
                    onRequestHomeRole = {},
                    onRequestBluetoothPermission = {}
                )
            }
        }
    }

    @Test
    fun rendersPromptAndOutput() {
        setTerminal()
        composeRule.onAllNodesWithText("KYENDY@ANDROID:~$").fetchSemanticsNodes().isNotEmpty()
            .let { assertTrue(it) }
        composeRule.onNodeWithText("SYSTEM READY.").assertExists()
        composeRule.onNodeWithText("Type 'help' to see available commands.").assertExists()
    }

    @Test
    fun typingInvokesInputCallback() {
        var typed = ""
        setTerminal(onInputChange = { typed = it })
        composeRule.onNode(hasSetTextAction()).performTextInput("help")
        composeRule.runOnIdle { assertEquals("help", typed) }
    }

    @Test
    fun bootSequenceCanBeSkipped() {
        var skipped = false
        setTerminal(isBooting = true, onSkipBoot = { skipped = true })
        composeRule.onNodeWithText("[ SKIP ]").performClick()
        composeRule.runOnIdle { assertTrue(skipped) }
    }
}
