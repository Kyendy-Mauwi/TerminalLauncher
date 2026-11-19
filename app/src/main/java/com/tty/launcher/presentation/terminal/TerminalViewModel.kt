package com.tty.launcher.presentation.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tty.launcher.data.datastore.SettingsRepository
import com.tty.launcher.data.repositories.AppRepository
import com.tty.launcher.data.repositories.AliasRepository
import com.tty.launcher.data.repositories.HistoryRepository
import com.tty.launcher.domain.commands.AppResolver
import com.tty.launcher.domain.commands.CommandCatalog
import com.tty.launcher.domain.commands.CommandDispatcher
import com.tty.launcher.domain.commands.CommandParser
import com.tty.launcher.domain.models.AppSettings
import com.tty.launcher.domain.models.LineStyle
import com.tty.launcher.domain.models.OutputLine
import com.tty.launcher.domain.models.SystemSnapshot
import com.tty.launcher.domain.models.TerminalAction
import com.tty.launcher.domain.models.accent
import com.tty.launcher.domain.models.dim
import com.tty.launcher.domain.models.normal
import com.tty.launcher.domain.models.success
import com.tty.launcher.domain.models.warning
import com.tty.launcher.services.notifications.NotificationStore
import com.tty.launcher.services.system.SystemIntents
import com.tty.launcher.services.system.SystemMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class TerminalLineUi(
    val id: Long,
    val text: String,
    val style: LineStyle
)

data class TerminalUiState(
    val lines: List<TerminalLineUi> = emptyList(),
    val input: String = "",
    val settings: AppSettings = AppSettings(),
    val isBooting: Boolean = true,
    val isExecuting: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val settingsLoaded: Boolean = false
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val dispatcher: CommandDispatcher,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val systemIntents: SystemIntents,
    private val systemMonitor: SystemMonitor,
    private val appRepository: AppRepository,
    private val aliasRepository: AliasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private val _actions = Channel<TerminalAction>(Channel.BUFFERED)
    val actions: Flow<TerminalAction> = _actions.receiveAsFlow()

    /** Stops updating when no screen is collecting, so no polling happens in the background. */
    val snapshot: StateFlow<SystemSnapshot?> = systemMonitor.stream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notifications = NotificationStore.notifications

    private val history = mutableListOf<String>()
    private var historyCursor = -1
    private var nextLineId = 0L
    private var bootJob: Job? = null
    private var bootFinished = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { updated ->
                _uiState.update { it.copy(settings = updated, settingsLoaded = true) }
            }
        }
        viewModelScope.launch {
            historyRepository.observe(MAX_HISTORY_FOR_NAVIGATION).collect { entries ->
                history.clear()
                history.addAll(entries.asReversed().map { it.command })
                historyCursor = -1
            }
        }
        refreshLauncherStatus()
        startBoot()
    }

    // ------------------------------------------------------------------ boot

    private fun startBoot() {
        bootJob?.cancel()
        bootJob = viewModelScope.launch {
            val settings = settingsRepository.snapshot()
            _uiState.update { it.copy(isBooting = true, lines = emptyList()) }
            append(listOf(dim("INITIALIZING TTY..."), dim("")))

            val animate = settings.animations && settings.bootSequence
            if (settings.bootSequence) {
                val steps = listOf(
                    "LOADING SYSTEM MODULES",
                    "INITIALIZING INTERFACE",
                    "LOADING APPLICATIONS",
                    "CHECKING SYSTEM STATUS"
                )
                steps.forEach { step ->
                    if (animate) delay(STEP_DELAY_MS)
                    append(listOf(normal(step.padEnd(32, '.') + " OK")))
                }
                append(listOf(dim("")))
            }

            if (animate) delay(120)
            finishBoot(settings)
        }
    }

    /** Skips the remaining boot animation. Safe to call at any time. */
    fun skipBoot() {
        if (bootFinished) return
        bootJob?.cancel()
        viewModelScope.launch {
            finishBoot(settingsRepository.snapshot())
        }
    }

    private suspend fun finishBoot(settings: AppSettings) {
        if (bootFinished) return
        bootFinished = true
        val lines = mutableListOf<OutputLine>()
        lines += success("SYSTEM READY.")
        if (settings.startupMessage.isNotBlank()) {
            lines += accent("${settings.startupMessage}, ${settings.username.uppercase(Locale.US)}.")
        }
        lines += dim("")
        lines += dim("Type 'help' to see available commands.")
        if (!systemIntents.isDefaultLauncher()) {
            lines += dim("")
            lines += warning("TTY IS NOT THE DEFAULT HOME.")
            lines += dim("Run 'settings home' to make it your home screen.")
        }
        append(lines)
        _uiState.update { it.copy(isBooting = false) }
        refreshLauncherStatus()
    }

    // ------------------------------------------------------------------ input

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun submit() {
        val state = _uiState.value
        val parsed = CommandParser.parse(state.input) ?: run {
            _uiState.update { it.copy(input = "") }
            return
        }
        if (!bootFinished) skipBoot()

        val prompt = state.settings.prompt
        _uiState.update { it.copy(input = "", isExecuting = true) }
        append(listOf(OutputLine("$prompt ${parsed.raw}", LineStyle.COMMAND)))

        viewModelScope.launch {
            try {
                val settings = settingsRepository.snapshot()
                runCatching { historyRepository.record(parsed.raw, settings.historyLimit) }
                val outcome = dispatcher.execute(parsed, settings, systemIntents.isDefaultLauncher())
                if (outcome.clearScreen) {
                    _uiState.update { it.copy(lines = emptyList()) }
                }
                append(outcome.lines)
                outcome.action?.let { _actions.send(it) }
            } catch (e: Exception) {
                append(
                    listOf(
                        OutputLine("COMMAND FAILED: ${e.javaClass.simpleName}", LineStyle.ERROR),
                        dim(e.message ?: "The command could not be completed."),
                        dim("Nothing was changed.")
                    )
                )
            } finally {
                _uiState.update { it.copy(isExecuting = false) }
                refreshLauncherStatus()
            }
        }
    }

    fun clearInput() {
        _uiState.update { it.copy(input = "") }
    }

    fun historyPrevious() {
        if (history.isEmpty()) return
        historyCursor = (historyCursor + 1).coerceAtMost(history.lastIndex)
        _uiState.update { it.copy(input = history[historyCursor]) }
    }

    fun historyNext() {
        if (history.isEmpty()) return
        historyCursor -= 1
        if (historyCursor < 0) {
            historyCursor = -1
            _uiState.update { it.copy(input = "") }
        } else {
            _uiState.update { it.copy(input = history[historyCursor]) }
        }
    }

    /** Tab completion for command names and application arguments. */
    fun completeInput() {
        val current = _uiState.value.input
        if (current.isBlank()) return

        viewModelScope.launch {
            val trimmed = current.trimStart()
            val hasSpace = trimmed.contains(' ')
            val firstWord = trimmed.substringBefore(' ').lowercase(Locale.US)

            if (!hasSpace) {
                val matches = CommandCatalog.names().filter { it.startsWith(firstWord) }
                when {
                    matches.size == 1 -> _uiState.update { it.copy(input = matches.first() + " ") }
                    matches.size in 2..8 -> append(listOf(dim("  " + matches.joinToString("  "))))
                }
                return@launch
            }

            if (firstWord in APP_ARGUMENT_COMMANDS) {
                val prefix = trimmed.substringAfter(' ')
                val apps = appRepository.installedApps()
                val aliases = aliasRepository.snapshot()
                val matches = AppResolver.match(prefix, apps, aliases)
                when {
                    matches.size == 1 -> _uiState.update {
                        it.copy(input = "$firstWord ${matches.first().label}")
                    }
                    matches.size in 2..8 -> append(listOf(dim("  " + matches.joinToString("  ") { it.label })))
                }
            }
        }
    }

    fun refreshLauncherStatus() {
        val isLauncher = systemIntents.isDefaultLauncher()
        _uiState.update { it.copy(isDefaultLauncher = isLauncher) }
    }

    // ------------------------------------------------------------------ output

    private fun append(lines: List<OutputLine>) {
        if (lines.isEmpty()) return
        _uiState.update { state ->
            val appended = state.lines + lines.map {
                TerminalLineUi(nextLineId++, it.text, it.style)
            }
            state.copy(
                lines = if (appended.size > MAX_LINES) appended.takeLast(MAX_LINES) else appended
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        bootJob?.cancel()
    }

    private companion object {
        const val MAX_LINES = 600
        const val MAX_HISTORY_FOR_NAVIGATION = 200
        const val STEP_DELAY_MS = 110L
        val APP_ARGUMENT_COMMANDS = setOf("open", "search", "install")
    }
}
