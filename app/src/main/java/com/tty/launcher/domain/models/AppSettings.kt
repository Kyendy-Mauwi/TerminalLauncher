package com.tty.launcher.domain.models

/** Colour themes available to the terminal. */
enum class ThemeId {
    GREEN,
    AMBER,
    CYAN,
    MATRIX,
    MONO;

    val key: String get() = name.lowercase()

    companion object {
        fun fromKey(key: String?): ThemeId =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: GREEN

        fun keys(): List<String> = entries.map { it.key }
    }
}

/** Monospace font choices. Both are bundled or platform provided. */
enum class FontChoice {
    JETBRAINS_MONO,
    MONOSPACE;

    val key: String get() = if (this == JETBRAINS_MONO) "jetbrains" else "mono"

    companion object {
        fun fromKey(key: String?): FontChoice {
            val value = key?.trim()?.lowercase() ?: return JETBRAINS_MONO
            return when (value) {
                "jetbrains", "jetbrains mono", "jetbrains-mono", "default" -> JETBRAINS_MONO
                "mono", "monospace", "system" -> MONOSPACE
                else -> JETBRAINS_MONO
            }
        }
    }
}

enum class CursorStyle {
    BLOCK,
    BAR,
    UNDERLINE;

    val key: String get() = name.lowercase()

    companion object {
        fun fromKey(key: String?): CursorStyle =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: BLOCK
    }
}

enum class HomeScreen {
    TERMINAL,
    APPLICATIONS;

    val key: String get() = name.lowercase()

    companion object {
        fun fromKey(key: String?): HomeScreen =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: TERMINAL
    }
}

/**
 * All user-tunable terminal state. Persisted in DataStore; every field is optional so
 * corrupted or missing preferences fall back to safe defaults.
 */
data class AppSettings(
    val username: String = DEFAULT_USERNAME,
    val hostname: String = DEFAULT_HOSTNAME,
    val theme: ThemeId = ThemeId.GREEN,
    val font: FontChoice = FontChoice.JETBRAINS_MONO,
    val fontSizeSp: Int = DEFAULT_FONT_SIZE,
    val cursor: CursorStyle = CursorStyle.BLOCK,
    val opacity: Float = 1f,
    val historyLimit: Int = DEFAULT_HISTORY_LIMIT,
    val animations: Boolean = true,
    val startupMessage: String = "WELCOME",
    val dateFormat: String = "yyyy-MM-dd",
    val timeFormat: String = "HH:mm",
    val haptics: Boolean = true,
    val defaultScreen: HomeScreen = HomeScreen.TERMINAL,
    val tripleTapEnabled: Boolean = true,
    val showSystemPanel: Boolean = true,
    val bootSequence: Boolean = true
) {
    val prompt: String
        get() = "${username.uppercase()}@${hostname.uppercase()}:~\$"

    companion object {
        const val DEFAULT_USERNAME = "KYENDY"
        const val DEFAULT_HOSTNAME = "ANDROID"
        const val DEFAULT_FONT_SIZE = 13
        const val DEFAULT_HISTORY_LIMIT = 200
        const val MIN_FONT_SIZE = 9
        const val MAX_FONT_SIZE = 22
        const val MIN_HISTORY_LIMIT = 25
        const val MAX_HISTORY_LIMIT = 1000
        const val MIN_OPACITY = 0.4f
    }
}
