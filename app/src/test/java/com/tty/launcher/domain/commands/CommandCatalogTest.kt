package com.tty.launcher.domain.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandCatalogTest {

    @Test
    fun commandNamesAreUnique() {
        val names = CommandCatalog.names()
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun aliasesDoNotCollideWithCommandNames() {
        val names = CommandCatalog.names().toSet()
        CommandCatalog.specs.forEach { spec ->
            spec.aliases.forEach { alias ->
                assertTrue("Alias '$alias' of ${spec.name} collides", alias !in names)
            }
        }
    }

    @Test
    fun everySpecHasUsageAndDescription() {
        CommandCatalog.specs.forEach { spec ->
            assertTrue(spec.name.isNotBlank())
            assertTrue(spec.description.length > 8)
            assertTrue(spec.usage.startsWith(spec.name))
        }
    }

    @Test
    fun aliasesResolveToCanonicalCommand() {
        assertEquals("open", CommandCatalog.find("launch")?.name)
        assertEquals("clear", CommandCatalog.find("cls")?.name)
        assertEquals("calculator", CommandCatalog.find("calc")?.name)
        assertEquals("help", CommandCatalog.find("?")?.name)
    }

    @Test
    fun lookupIsCaseInsensitive() {
        assertEquals("battery", CommandCatalog.find("BATTERY")?.name)
    }

    @Test
    fun unknownCommandIsNotKnown() {
        assertNull(CommandCatalog.find("nonsense"))
    }

    @Test
    fun allCategoriesArePopulated() {
        val grouped = CommandCatalog.byCategory()
        CommandCategory.entries.forEach { category ->
            assertNotNull(grouped[category])
        }
    }

    @Test
    fun requiredCommandsExist() {
        listOf(
            "help", "clear", "home", "exit", "back", "about", "settings",
            "apps", "open", "search", "recent", "favorites", "install", "alias",
            "sysinfo", "battery", "storage", "memory", "device", "uptime", "network",
            "wifi", "bluetooth", "brightness", "volume", "date", "time",
            "notes", "calculator", "calendar", "clock", "camera", "files", "contacts",
            "messages", "phone", "theme", "font", "fontsize", "wallpaper", "opacity",
            "animations", "whoami", "hostname", "history", "echo", "version", "ping", "reboot"
        ).forEach { name ->
            assertNotNull("Missing command: $name", CommandCatalog.find(name))
        }
    }
}
