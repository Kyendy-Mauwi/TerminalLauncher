package com.tty.launcher.domain.commands

/**
 * Tiny, dependency free arithmetic evaluator used by the `calculator` command. Supports
 * + - * / % ^, parentheses, unary signs and decimals. No code execution, no reflection.
 */
object ExpressionEvaluator {

    sealed interface Result {
        data class Value(val value: Double) : Result
        data class Failure(val reason: String) : Result
    }

    fun evaluate(expression: String): Result {
        val cleaned = expression.trim()
        if (cleaned.isEmpty()) return Result.Failure("Empty expression.")
        return try {
            val parser = Parser(cleaned)
            val value = parser.parseExpression()
            parser.skipWhitespace()
            if (!parser.atEnd()) return Result.Failure("Unexpected token near '${parser.remaining()}'.")
            if (value.isNaN() || value.isInfinite()) return Result.Failure("Result is not a finite number.")
            Result.Value(value)
        } catch (e: IllegalArgumentException) {
            Result.Failure(e.message ?: "Invalid expression.")
        } catch (e: ArithmeticException) {
            Result.Failure("Division by zero.")
        }
    }

    fun format(value: Double): String {
        if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) return value.toLong().toString()
        return String.format(java.util.Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun atEnd(): Boolean = index >= source.length

        fun remaining(): String = source.substring(index).trim()

        fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index++
        }

        private fun peek(): Char? {
            skipWhitespace()
            return if (index < source.length) source[index] else null
        }

        private fun consume(): Char {
            skipWhitespace()
            if (index >= source.length) throw IllegalArgumentException("Unexpected end of expression.")
            return source[index++]
        }

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { consume(); value += parseTerm() }
                    '-' -> { consume(); value -= parseTerm() }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                when (peek()) {
                    '*' -> {
                        consume()
                        // Support implicit percentage-free multiplication only.
                        value *= parsePower()
                    }
                    '/' -> {
                        consume()
                        val divisor = parsePower()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero.")
                        value /= divisor
                    }
                    '%' -> {
                        consume()
                        val divisor = parsePower()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero.")
                        value %= divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            if (peek() == '^') {
                consume()
                return Math.pow(base, parsePower())
            }
            return base
        }

        private fun parseUnary(): Double {
            return when (peek()) {
                '-' -> { consume(); -parseUnary() }
                '+' -> { consume(); parseUnary() }
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            if (peek() == '(') {
                consume()
                val value = parseExpression()
                if (peek() != ')') throw IllegalArgumentException("Missing closing parenthesis.")
                consume()
                return value
            }

            skipWhitespace()
            val start = index
            while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
            if (start == index) {
                val bad = if (index < source.length) source[index].toString() else "end of expression"
                throw IllegalArgumentException("Unexpected character '$bad'.")
            }
            val literal = source.substring(start, index)
            return literal.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number '$literal'.")
        }
    }
}
