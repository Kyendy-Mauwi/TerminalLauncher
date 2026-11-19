package com.tty.launcher.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tty.launcher.R
import com.tty.launcher.domain.models.AppSettings
import com.tty.launcher.domain.models.FontChoice
import com.tty.launcher.domain.models.LineStyle
import com.tty.launcher.domain.models.ThemeId

/** The colours of one terminal theme. */
data class TerminalPalette(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val warning: Color,
    val error: Color,
    val border: Color,
    val isMatrix: Boolean = false
)

fun paletteFor(theme: ThemeId): TerminalPalette = when (theme) {
    ThemeId.GREEN -> TerminalPalette(
        background = Color(0xFF080B0F),
        surface = Color(0xFF0E141A),
        primary = Color(0xFF39FF88),
        secondary = Color(0xFF8AAE9A),
        accent = Color(0xFF00D9FF),
        warning = Color(0xFFFFB547),
        error = Color(0xFFFF5555),
        border = Color(0xFF1B2A24)
    )

    ThemeId.AMBER -> TerminalPalette(
        background = Color(0xFF0B0905),
        surface = Color(0xFF171208),
        primary = Color(0xFFFFB547),
        secondary = Color(0xFFC9A167),
        accent = Color(0xFFFFE0A3),
        warning = Color(0xFFFF8A3D),
        error = Color(0xFFFF5555),
        border = Color(0xFF3A2C15)
    )

    ThemeId.CYAN -> TerminalPalette(
        background = Color(0xFF05090C),
        surface = Color(0xFF0A1419),
        primary = Color(0xFF00D9FF),
        secondary = Color(0xFF7FB6C4),
        accent = Color(0xFF39FF88),
        warning = Color(0xFFFFB547),
        error = Color(0xFFFF5555),
        border = Color(0xFF153039)
    )

    ThemeId.MATRIX -> TerminalPalette(
        background = Color(0xFF000000),
        surface = Color(0xFF04120A),
        primary = Color(0xFF00FF41),
        secondary = Color(0xFF2E8B57),
        accent = Color(0xFFB6FFC9),
        warning = Color(0xFFFFD166),
        error = Color(0xFFFF5555),
        border = Color(0xFF0E3018),
        isMatrix = true
    )

    ThemeId.MONO -> TerminalPalette(
        background = Color(0xFF050608),
        surface = Color(0xFF101317),
        primary = Color(0xFFE6EDF3),
        secondary = Color(0xFF9AA7B2),
        accent = Color(0xFFFFFFFF),
        warning = Color(0xFFFFC66D),
        error = Color(0xFFFF7B72),
        border = Color(0xFF232A31)
    )
}

val LocalTerminalPalette = staticCompositionLocalOf { paletteFor(ThemeId.GREEN) }

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

fun fontFamilyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.JETBRAINS_MONO -> JetBrainsMono
    FontChoice.MONOSPACE -> FontFamily.Monospace
}

fun LineStyle.colorFor(palette: TerminalPalette): Color = when (this) {
    LineStyle.NORMAL -> palette.primary
    LineStyle.DIM -> palette.secondary
    LineStyle.ACCENT -> palette.accent
    LineStyle.SUCCESS -> palette.primary
    LineStyle.WARNING -> palette.warning
    LineStyle.ERROR -> palette.error
    LineStyle.BANNER -> palette.accent
    LineStyle.COMMAND -> palette.secondary
}

@Composable
fun TTYTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(settings.theme)
    val family = fontFamilyFor(settings.font)
    val base = settings.fontSizeSp.sp

    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = family, fontSize = base, lineHeight = base * 1.4f, color = palette.primary),
        bodyMedium = TextStyle(fontFamily = family, fontSize = base, lineHeight = base * 1.4f, color = palette.primary),
        bodySmall = TextStyle(fontFamily = family, fontSize = (base.value * 0.85f).sp, lineHeight = base, color = palette.secondary),
        titleMedium = TextStyle(fontFamily = family, fontSize = base * 1.1f, fontWeight = FontWeight.Bold, color = palette.primary),
        labelLarge = TextStyle(fontFamily = family, fontSize = base, color = palette.primary),
        labelSmall = TextStyle(fontFamily = family, fontSize = (base.value * 0.8f).sp, letterSpacing = 0.5.sp, color = palette.secondary)
    )

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.background,
        secondary = palette.accent,
        onSecondary = palette.background,
        background = palette.background,
        onBackground = palette.primary,
        surface = palette.surface,
        onSurface = palette.primary,
        surfaceVariant = palette.surface,
        onSurfaceVariant = palette.secondary,
        error = palette.error,
        onError = palette.background,
        outline = palette.border
    )

    CompositionLocalProvider(LocalTerminalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
