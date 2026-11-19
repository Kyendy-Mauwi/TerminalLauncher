package com.tty.launcher.domain.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEvaluatorTest {

    private fun value(expression: String): Double {
        val result = ExpressionEvaluator.evaluate(expression)
        require(result is ExpressionEvaluator.Result.Value) { "Expected a value for $expression" }
        return result.value
    }

    @Test
    fun evaluatesAdditionAndMultiplicationPrecedence() {
        assertEquals(14.0, value("2 + 3 * 4"), 0.0001)
    }

    @Test
    fun evaluatesParentheses() {
        assertEquals(20.0, value("(2 + 3) * 4"), 0.0001)
    }

    @Test
    fun evaluatesUnaryMinus() {
        assertEquals(-3.0, value("-5 + 2"), 0.0001)
    }

    @Test
    fun evaluatesPowerRightAssociative() {
        assertEquals(512.0, value("2 ^ 3 ^ 2"), 0.0001)
    }

    @Test
    fun evaluatesDecimalsAndModulo() {
        assertEquals(2.5, value("10 / 4"), 0.0001)
        assertEquals(1.0, value("10 % 3"), 0.0001)
    }

    @Test
    fun reportsDivisionByZero() {
        assertTrue(ExpressionEvaluator.evaluate("10 / 0") is ExpressionEvaluator.Result.Failure)
    }

    @Test
    fun reportsInvalidExpressions() {
        assertTrue(ExpressionEvaluator.evaluate("") is ExpressionEvaluator.Result.Failure)
        assertTrue(ExpressionEvaluator.evaluate("2 +") is ExpressionEvaluator.Result.Failure)
        assertTrue(ExpressionEvaluator.evaluate("abc") is ExpressionEvaluator.Result.Failure)
        assertTrue(ExpressionEvaluator.evaluate("(2 + 3") is ExpressionEvaluator.Result.Failure)
    }

    @Test
    fun formatsResultsWithoutTrailingZeros() {
        assertEquals("4", ExpressionEvaluator.format(4.0))
        assertEquals("2.5", ExpressionEvaluator.format(2.5))
    }
}
