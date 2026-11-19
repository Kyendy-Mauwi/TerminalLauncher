package com.tty.launcher.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tty.launcher.data.datastore.SettingsRepository
import com.tty.launcher.domain.models.AppSettings
import com.tty.launcher.domain.models.CursorStyle
import com.tty.launcher.domain.models.FontChoice
import com.tty.launcher.domain.models.HomeScreen
import com.tty.launcher.domain.models.ThemeId
import com.tty.launcher.services.system.SettingsPanel
import com.tty.launcher.services.system.SystemIntents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val systemIntents: SystemIntents
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _isDefaultLauncher = MutableStateFlow(systemIntents.isDefaultLauncher())
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    fun setUsername(value: String) {
        viewModelScope.launch { settingsRepository.setUsername(value) }
    }

    fun setHostname(value: String) {
        viewModelScope.launch { settingsRepository.setHostname(value) }
    }

    fun setStartupMessage(value: String) {
        viewModelScope.launch { settingsRepository.setStartupMessage(value) }
    }

    fun setTheme(value: ThemeId) {
        viewModelScope.launch { settingsRepository.setTheme(value) }
    }

    fun setFont(value: FontChoice) {
        viewModelScope.launch { settingsRepository.setFont(value) }
    }

    fun setFontSize(value: Int) {
        viewModelScope.launch { settingsRepository.setFontSize(value) }
    }

    fun setCursor(value: CursorStyle) {
        viewModelScope.launch { settingsRepository.setCursor(value) }
    }

    fun setOpacity(value: Float) {
        viewModelScope.launch { settingsRepository.setOpacity(value) }
    }

    fun setHistoryLimit(value: Int) {
        viewModelScope.launch { settingsRepository.setHistoryLimit(value) }
    }

    fun setAnimations(value: Boolean) {
        viewModelScope.launch { settingsRepository.setAnimations(value) }
    }

    fun setBootSequence(value: Boolean) {
        viewModelScope.launch { settingsRepository.setBootSequence(value) }
    }

    fun setDateFormat(value: String) {
        viewModelScope.launch { settingsRepository.setDateFormat(value) }
    }

    fun setTimeFormat(value: String) {
        viewModelScope.launch { settingsRepository.setTimeFormat(value) }
    }

    fun setHaptics(value: Boolean) {
        viewModelScope.launch { settingsRepository.setHaptics(value) }
    }

    fun setDefaultScreen(value: HomeScreen) {
        viewModelScope.launch { settingsRepository.setDefaultScreen(value) }
    }

    fun setTripleTap(value: Boolean) {
        viewModelScope.launch { settingsRepository.setTripleTap(value) }
    }

    fun setSystemPanel(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSystemPanel(value) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { settingsRepository.resetToDefaults() }
    }

    fun refreshLauncherStatus() {
        _isDefaultLauncher.value = systemIntents.isDefaultLauncher()
    }

    fun requestHomeRole() {
        systemIntents.openHomeRoleSettings()
    }

    fun openNotificationAccess() {
        systemIntents.openNotificationAccess()
    }

    fun openWriteSettings() {
        systemIntents.openWriteSettings()
    }

    fun openPanel(panel: SettingsPanel) {
        systemIntents.openPanel(panel)
    }
}
