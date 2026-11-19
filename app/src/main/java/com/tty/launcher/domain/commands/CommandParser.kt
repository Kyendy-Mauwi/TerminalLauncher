package com.tty.launcher.domain.commands

/**
 * Splits raw terminal input into a command name and arguments. Supports quoting with single or
 * double quotes, backslash escapes and arbitrary whitespace. Never throws: malformed input is
 * surfaced through [ParsedCommand.warnings].
 */
object CommandParser {

    data class ParsedCommand(
        val raw: String,
        val name: String,
        val args: List<String>,
        /** Everything after the command name, exactly as typed (quotes preserved). */
        val rawArgs: String,
        val warnings: List<String> = emptyList()
    ) {
        val hasArgs: Boolean get() = args.isNotEmpty()
        val firstArg: String? get() = args.firstOrNull()
        val argsText: String get() = args.joinToString(" ")
    }

    fun parse(input: String): ParsedCommand? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val tokens = ArrayList<String>()
        val warnings = ArrayList<String>()
        val builder = StringBuilder()
        var inToken = false
        var quote: Char? = null
        var escaped = false
        var nameEndIndex = -1

        for ((index, char) in trimmed.withIndex()) {
            when {
                escaped -> {
                    builder.append(char)
                    escaped = false
                    inToken = true
                }

                char == '\\' && quote != '\'' -> {
                    escaped = true
                    inToken = true
                }

                quote != null -> {
                    if (char == quote) {
                        quote = null
                    } else {
                        builder.append(char)
                    }
                }

                char == '"' || char == '\'' -> {
                    quote = char
                    inToken = true
                }

                char.isWhitespace() -> {
                    if (inToken) {
                        tokens.add(builder.toString())
                        builder.setLength(0)
                        inToken = false
                        if (nameEndIndex < 0) nameEndIndex = index
                    }
                }

                else -> {
                    builder.append(char)
                    inToken = true
                }
            }
        }

        if (escaped) {
            // A dangling escape is treated as a literal backslash.
            builder.append('\\')
        }
        if (quote != null) {
            warnings.add("Unterminated ${if (quote == '"') "double" else "single"} quote.")
        }
        if (inToken) {
            tokens.add(builder.toString())
            if (nameEndIndex < 0) nameEndIndex = trimmed.length
        }

        if (tokens.isEmpty()) return null

        val name = tokens.first()
        val args = tokens.drop(1)
        val rawArgs = if (args.isEmpty()) "" else trimmed.substring(minOf(nameEndIndex, trimmed.length)).trim()

        return ParsedCommand(
            raw = trimmed,
            name = name.lowercase(),
            args = args,
            rawArgs = rawArgs,
            warnings = warnings
        )
    }

    /** Convenience for tests and suggestion lookups. */
    fun tokenize(input: String): List<String> = parse(input)?.let { listOf(it.name) + it.args } ?: emptyList()

    /** Collapses repeated whitespace; used before storing history. */
    fun normalize(input: String): String = input.trim().replace(Regex("\\s+"), " ")
}
