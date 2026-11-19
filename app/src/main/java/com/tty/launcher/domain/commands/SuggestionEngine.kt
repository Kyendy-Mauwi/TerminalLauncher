package com.tty.launcher.domain.commands

/**
 * Ranked fuzzy suggestions for mistyped commands and application names. Pure Kotlin so it can be
 * unit tested without Android.
 */
object SuggestionEngine {

    fun suggest(input: String, candidates: List<String>, max: Int = 3): List<String> {
        val query = input.trim().lowercase()
        if (query.isEmpty()) return emptyList()

        return candidates
            .asSequence()
            .map { it to score(query, it.lowercase()) }
            .filter { (_, score) -> score < query.length.coerceAtLeast(3) }
            .sortedWith(compareBy({ it.second }, { it.first }))
            .map { it.first }
            .distinct()
            .take(max)
            .toList()
    }

    /**
     * Lower is better. Exact prefix matches score 0; otherwise the edit distance, discounted for
     * candidates that start with the same character.
     */
    fun score(query: String, candidate: String): Int {
        if (query == candidate) return 0
        var distance = levenshtein(query, candidate)
        if (candidate.startsWith(query.firstOrNull() ?: ' ')) distance -= 1
        if (candidate.startsWith(query)) distance -= 1
        return distance.coerceAtLeast(0)
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                val insertion = current[j - 1] + 1
                val deletion = previous[j] + 1
                current[j] = minOf(substitution, insertion, deletion)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    /** Suggests full command lines, preserving typed arguments where useful. */
    fun suggestCommandLines(parsed: CommandParser.ParsedCommand): List<String> {
        val names = CommandCatalog.names()
        val matches = suggest(parsed.name, names)
        return matches.map { name ->
            if (parsed.args.isEmpty()) name else "$name ${parsed.argsText}"
        }
    }
}
