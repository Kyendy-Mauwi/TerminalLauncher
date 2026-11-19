package com.tty.launcher.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    @Test
    fun formatsBytes() {
        assertEquals("512 B", Formatters.bytes(512))
        assertEquals("1 KB", Formatters.bytes(1024))
        assertEquals("1.5 MB", Formatters.bytes(1024L * 1024 + 512 * 1024))
        assertEquals("2.0 GB", Formatters.bytes(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun formatsUptime() {
        val millis = 4L * 3600 * 1000 + 32L * 60 * 1000 + 18L * 1000
        assertEquals("04:32:18", Formatters.uptime(millis))
    }

    @Test
    fun uptimeHandlesNegativeValues() {
        assertEquals("00:00:00", Formatters.uptime(-1000))
    }

    @Test
    fun formatsShortDurations() {
        assertEquals("45s", Formatters.shortDuration(45_000))
        assertEquals("2m 5s", Formatters.shortDuration(125_000))
        assertEquals("1h 0m", Formatters.shortDuration(3_600_000))
    }

    @Test
    fun percentIsClampedAndSafeForZeroTotal() {
        assertEquals(50, Formatters.percent(5, 10))
        assertEquals(0, Formatters.percent(5, 0))
        assertEquals(100, Formatters.percent(20, 10))
    }

    @Test
    fun rowPadsLabels() {
        assertTrue(Formatters.row("DEVICE", "ANDROID").startsWith("DEVICE     : "))
    }

    @Test
    fun invalidDatePatternFallsBack() {
        val formatted = Formatters.dateTime(System.currentTimeMillis(), "not-a-pattern")
        assertTrue(formatted.isNotBlank())
    }
}
