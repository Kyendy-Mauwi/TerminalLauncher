package com.tty.launcher.domain.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

    @Test
    fun blankInputReturnsNull() {
        assertNull(CommandParser.parse(""))
        assertNull(CommandParser.parse("    \t  "))
    }

    @Test
    fun lowercasesOnlyTheCommandName() {
        val parsed = CommandParser.parse("OPEN Chrome")
        requireNotNull(parsed)
        assertEquals("open", parsed.name)
        assertEquals(listOf("Chrome"), parsed.args)
    }

    @Test
    fun collapsesRepeatedAndLeadingWhitespace() {
        val parsed = CommandParser.parse("   echo    hello     world  ")
        requireNotNull(parsed)
        assertEquals("echo", parsed.name)
        assertEquals(listOf("hello", "world"), parsed.args)
        assertEquals("hello     world", parsed.rawArgs)
    }

    @Test
    fun supportsDoubleQuotedArguments() {
        val parsed = CommandParser.parse("echo \"hello world\"")
        requireNotNull(parsed)
        assertEquals(listOf("hello world"), parsed.args)
    }

    @Test
    fun supportsSingleQuotedArguments() {
        val parsed = CommandParser.parse("notes add 'buy milk and eggs'")
        requireNotNull(parsed)
        assertEquals(listOf("add", "buy milk and eggs"), parsed.args)
    }

    @Test
    fun supportsEscapedQuotesInsideQuotes() {
        val parsed = CommandParser.parse("echo \"a \\\"b\\\" c\"")
        requireNotNull(parsed)
        assertEquals(listOf("a \"b\" c"), parsed.args)
    }

    @Test
    fun flagsUnterminatedQuotesButStillParses() {
        val parsed = CommandParser.parse("echo \"oops")
        requireNotNull(parsed)
        assertEquals(listOf("oops"), parsed.args)
        assertEquals(1, parsed.warnings.size)
    }

    @Test
    fun allowsMissingArguments() {
        val parsed = CommandParser.parse("open")
        requireNotNull(parsed)
        assertEquals("open", parsed.name)
        assertTrue(parsed.args.isEmpty())
        assertEquals("", parsed.rawArgs)
    }

    @Test
    fun preservesSpecialCharacters() {
        val parsed = CommandParser.parse("echo a&b | c>d '\$HOME'")
        requireNotNull(parsed)
        assertEquals(listOf("a&b", "|", "c>d", "\$HOME"), parsed.args)
    }

    @Test
    fun treatsDanglingEscapeAsLiteralBackslash() {
        val parsed = CommandParser.parse("echo back\\")
        requireNotNull(parsed)
        assertEquals(listOf("back\\"), parsed.args)
    }

    @Test
    fun normalizeCompressesWhitespace() {
        assertEquals("open chrome", CommandParser.normalize("  open   chrome \n"))
    }

    @Test
    fun tokenizeReturnsNameAndArguments() {
        assertEquals(listOf("open", "chrome"), CommandParser.tokenize("Open   \"chrome\""))
    }
}
