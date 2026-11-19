package com.tty.launcher.domain.commands

import com.tty.launcher.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResolverTest {

    private val apps = listOf(
        AppInfo("com.android.chrome", "Chrome", "Chrome"),
        AppInfo("com.android.camera", "Camera", "Camera"),
        AppInfo("com.example.notes", "Notes", "Notes"),
        AppInfo("com.example.notepad", "Notepad", "Notepad")
    )

    @Test
    fun matchesExactLabelCaseInsensitively() {
        val matches = AppResolver.match("chrome", apps, emptyMap())
        assertEquals(1, matches.size)
        assertEquals("com.android.chrome", matches.first().packageName)
    }

    @Test
    fun aliasTakesPriority() {
        val aliases = mapOf("com.android.chrome" to "browser")
        val matches = AppResolver.match("browser", apps, aliases)
        assertEquals("com.android.chrome", matches.first().packageName)
    }

    @Test
    fun matchesByPrefix() {
        val matches = AppResolver.match("cam", apps, emptyMap())
        assertEquals("com.android.camera", matches.first().packageName)
    }

    @Test
    fun matchesByPackageName() {
        val matches = AppResolver.match("com.example.notes", apps, emptyMap())
        assertEquals("com.example.notes", matches.first().packageName)
    }

    @Test
    fun returnsEmptyForUnknownQuery() {
        assertTrue(AppResolver.match("photoshop", apps, emptyMap()).isEmpty())
    }

    @Test
    fun returnsEmptyForBlankQuery() {
        assertTrue(AppResolver.match("   ", apps, emptyMap()).isEmpty())
    }

    @Test
    fun suggestsInstalledApplicationsForTypos() {
        val suggestions = AppResolver.suggestions("crom", apps, emptyMap())
        assertTrue(suggestions.any { it.equals("Chrome", ignoreCase = true) })
    }

    @Test
    fun displayNamePrefersAlias() {
        val app = apps.first()
        assertEquals("browser", AppResolver.displayName(app, mapOf("com.android.chrome" to "browser")))
        assertEquals("Chrome", AppResolver.displayName(app, emptyMap()))
    }
}
