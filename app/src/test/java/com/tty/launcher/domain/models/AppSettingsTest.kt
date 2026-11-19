package com.tty.launcher.domain.models

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun defaultsAreSafe() {
        val settings = AppSettings()
        assertEquals("KYENDY", settings.username)
        assertEquals("ANDROID", settings.hostname)
        assertEquals(ThemeId.GREEN, settings.theme)
        assertEquals(FontChoice.JETBRAINS_MONO, settings.font)
        assertEquals(CursorStyle.BLOCK, settings.cursor)
    }

    @Test
    fun promptUsesUppercaseIdentity() {
        assertEquals("KYENDY@ANDROID:~$", AppSettings().prompt)
        assertEquals("ALEX@PIXEL:~$", AppSettings(username = "alex", hostname = "pixel").prompt)
    }

    @Test
    fun themeParsingIsForgiving() {
        assertEquals(ThemeId.AMBER, ThemeId.fromKey("AMBER"))
        assertEquals(ThemeId.CYAN, ThemeId.fromKey(" cyan "))
        assertEquals(ThemeId.GREEN, ThemeId.fromKey("does-not-exist"))
        assertEquals(ThemeId.GREEN, ThemeId.fromKey(null))
    }

    @Test
    fun fontParsingAcceptsCommonSpellings() {
        assertEquals(FontChoice.JETBRAINS_MONO, FontChoice.fromKey("jetbrains mono"))
        assertEquals(FontChoice.MONOSPACE, FontChoice.fromKey("system"))
        assertEquals(FontChoice.JETBRAINS_MONO, FontChoice.fromKey("unknown"))
    }

    @Test
    fun cursorParsingFallsBackToBlock() {
        assertEquals(CursorStyle.BAR, CursorStyle.fromKey("bar"))
        assertEquals(CursorStyle.BLOCK, CursorStyle.fromKey("nope"))
    }

    @Test
    fun homeScreenParsing() {
        assertEquals(HomeScreen.APPLICATIONS, HomeScreen.fromKey("applications"))
        assertEquals(HomeScreen.TERMINAL, HomeScreen.fromKey(null))
    }
}
