package com.tty.launcher.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tty.launcher.domain.models.AppSettings
import com.tty.launcher.domain.models.CursorStyle
import com.tty.launcher.domain.models.FontChoice
import com.tty.launcher.domain.models.HomeScreen
import com.tty.launcher.domain.models.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single owner of terminal preferences. Reads never throw: a corrupted file falls back to
 * defaults, and each field is coerced into a valid range.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
        .map { it.toSettings() }
        .distinctUntilChanged()

    suspend fun snapshot(): AppSettings = try {
        dataStore.data.catch { emit(emptyPreferences()) }.map { it.toSettings() }.first()
    } catch (e: Exception) {
        AppSettings()
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
    }

    suspend fun setUsername(value: String) = update { it.copy(username = sanitizeName(value, AppSettings.DEFAULT_USERNAME)) }
    suspend fun setHostname(value: String) = update { it.copy(hostname = sanitizeName(value, AppSettings.DEFAULT_HOSTNAME)) }
    suspend fun setTheme(value: ThemeId) = update { it.copy(theme = value) }
    suspend fun setFont(value: FontChoice) = update { it.copy(font = value) }
    suspend fun setFontSize(value: Int) = update { it.copy(fontSizeSp = value.coerceIn(AppSettings.MIN_FONT_SIZE, AppSettings.MAX_FONT_SIZE)) }
    suspend fun setCursor(value: CursorStyle) = update { it.copy(cursor = value) }
    suspend fun setOpacity(value: Float) = update { it.copy(opacity = value.coerceIn(AppSettings.MIN_OPACITY, 1f)) }
    suspend fun setHistoryLimit(value: Int) = update { it.copy(historyLimit = value.coerceIn(AppSettings.MIN_HISTORY_LIMIT, AppSettings.MAX_HISTORY_LIMIT)) }
    suspend fun setAnimations(enabled: Boolean) = update { it.copy(animations = enabled) }
    suspend fun setBootSequence(enabled: Boolean) = update { it.copy(bootSequence = enabled) }
    suspend fun setStartupMessage(value: String) = update { it.copy(startupMessage = value.take(48)) }
    suspend fun setDateFormat(value: String) = update { it.copy(dateFormat = value.ifBlank { "yyyy-MM-dd" }) }
    suspend fun setTimeFormat(value: String) = update { it.copy(timeFormat = value.ifBlank { "HH:mm" }) }
    suspend fun setHaptics(enabled: Boolean) = update { it.copy(haptics = enabled) }
    suspend fun setDefaultScreen(value: HomeScreen) = update { it.copy(defaultScreen = value) }
    suspend fun setTripleTap(enabled: Boolean) = update { it.copy(tripleTapEnabled = enabled) }
    suspend fun setSystemPanel(enabled: Boolean) = update { it.copy(showSystemPanel = enabled) }

    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }

    private fun sanitizeName(value: String, fallback: String): String {
        val cleaned = value.trim().replace(Regex("[^A-Za-z0-9._-]"), "")
        return cleaned.ifBlank { fallback }.take(16)
    }

    private fun Preferences.toSettings(): AppSettings = try {
        AppSettings(
            username = this[Keys.USERNAME] ?: AppSettings.DEFAULT_USERNAME,
            hostname = this[Keys.HOSTNAME] ?: AppSettings.DEFAULT_HOSTNAME,
            theme = ThemeId.fromKey(this[Keys.THEME]),
            font = FontChoice.fromKey(this[Keys.FONT]),
            fontSizeSp = (this[Keys.FONT_SIZE] ?: AppSettings.DEFAULT_FONT_SIZE)
                .coerceIn(AppSettings.MIN_FONT_SIZE, AppSettings.MAX_FONT_SIZE),
            cursor = CursorStyle.fromKey(this[Keys.CURSOR]),
            opacity = (this[Keys.OPACITY] ?: 1f).coerceIn(AppSettings.MIN_OPACITY, 1f),
            historyLimit = (this[Keys.HISTORY_LIMIT] ?: AppSettings.DEFAULT_HISTORY_LIMIT)
                .coerceIn(AppSettings.MIN_HISTORY_LIMIT, AppSettings.MAX_HISTORY_LIMIT),
            animations = this[Keys.ANIMATIONS] ?: true,
            startupMessage = this[Keys.STARTUP_MESSAGE] ?: "WELCOME",
            dateFormat = this[Keys.DATE_FORMAT] ?: "yyyy-MM-dd",
            timeFormat = this[Keys.TIME_FORMAT] ?: "HH:mm",
            haptics = this[Keys.HAPTICS] ?: true,
            defaultScreen = HomeScreen.fromKey(this[Keys.DEFAULT_SCREEN]),
            tripleTapEnabled = this[Keys.TRIPLE_TAP] ?: true,
            showSystemPanel = this[Keys.SYSTEM_PANEL] ?: true,
            bootSequence = this[Keys.BOOT_SEQUENCE] ?: true
        )
    } catch (e: Exception) {
        AppSettings()
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(settings: AppSettings) {
        this[Keys.USERNAME] = settings.username
        this[Keys.HOSTNAME] = settings.hostname
        this[Keys.THEME] = settings.theme.key
        this[Keys.FONT] = settings.font.key
        this[Keys.FONT_SIZE] = settings.fontSizeSp
        this[Keys.CURSOR] = settings.cursor.key
        this[Keys.OPACITY] = settings.opacity
        this[Keys.HISTORY_LIMIT] = settings.historyLimit
        this[Keys.ANIMATIONS] = settings.animations
        this[Keys.STARTUP_MESSAGE] = settings.startupMessage
        this[Keys.DATE_FORMAT] = settings.dateFormat
        this[Keys.TIME_FORMAT] = settings.timeFormat
        this[Keys.HAPTICS] = settings.haptics
        this[Keys.DEFAULT_SCREEN] = settings.defaultScreen.key
        this[Keys.TRIPLE_TAP] = settings.tripleTapEnabled
        this[Keys.SYSTEM_PANEL] = settings.showSystemPanel
        this[Keys.BOOT_SEQUENCE] = settings.bootSequence
    }

    private object Keys {
        val USERNAME = stringPreferencesKey("username")
        val HOSTNAME = stringPreferencesKey("hostname")
        val THEME = stringPreferencesKey("theme")
        val FONT = stringPreferencesKey("font")
        val FONT_SIZE = intPreferencesKey("font_size")
        val CURSOR = stringPreferencesKey("cursor")
        val OPACITY = floatPreferencesKey("opacity")
        val HISTORY_LIMIT = intPreferencesKey("history_limit")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val STARTUP_MESSAGE = stringPreferencesKey("startup_message")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val HAPTICS = booleanPreferencesKey("haptics")
        val DEFAULT_SCREEN = stringPreferencesKey("default_screen")
        val TRIPLE_TAP = booleanPreferencesKey("triple_tap")
        val SYSTEM_PANEL = booleanPreferencesKey("system_panel")
        val BOOT_SEQUENCE = booleanPreferencesKey("boot_sequence")
    }
}

