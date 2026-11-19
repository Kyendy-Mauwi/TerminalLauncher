package com.tty.launcher.domain.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {

    @Test
    fun levenshteinComputesEditDistance() {
        assertEquals(1, SuggestionEngine.levenshtein("opn", "open"))
        assertEquals(3, SuggestionEngine.levenshtein("kitten", "sitting"))
        assertEquals(0, SuggestionEngine.levenshtein("same", "same"))
        assertEquals(4, SuggestionEngine.levenshtein("", "text"))
    }

    @Test
    fun exactMatchScoresZero() {
        assertEquals(0, SuggestionEngine.score("help", "help"))
    }

    @Test
    fun suggestsCloseCommand() {
        val suggestions = SuggestionEngine.suggest("opn", listOf("help", "open", "recent"))
        assertTrue(suggestions.contains("open"))
    }

    @Test
    fun suggestsFullCommandLineWithArguments() {
        val parsed = requireNotNull(CommandParser.parse("opn chrome"))
        val lines = SuggestionEngine.suggestCommandLines(parsed)
        assertTrue(lines.contains("open chrome"))
    }

    @Test
    fun returnsNothingForUnrelatedInput() {
        val suggestions = SuggestionEngine.suggest("zzzzzz", listOf("help", "open"))
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun capsSuggestionCount() {
        val candidates = listOf("open", "opens", "opened", "opening", "opener")
        assertTrue(SuggestionEngine.suggest("opne", candidates, max = 2).size <= 2)
    }
}
