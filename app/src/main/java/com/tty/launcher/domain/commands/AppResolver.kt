package com.tty.launcher.domain.commands

import com.tty.launcher.domain.models.AppInfo

/**
 * Pure application matching logic. Aliases take priority, then exact labels, then prefix and
 * substring matches, then package name matches. Case insensitive.
 */
object AppResolver {

    fun match(query: String, apps: List<AppInfo>, aliases: Map<String, String>): List<AppInfo> {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return emptyList()

        // alias -> package
        val aliasPackages = aliases.entries
            .filter { it.value.lowercase() == needle }
            .map { it.key }
            .toSet()
        val aliasMatches = apps.filter { it.packageName in aliasPackages }
        if (aliasMatches.isNotEmpty()) return aliasMatches

        val exact = apps.filter { it.label.lowercase() == needle }
        if (exact.isNotEmpty()) return exact

        val packageExact = apps.filter { it.packageName.lowercase() == needle }
        if (packageExact.isNotEmpty()) return packageExact

        val prefix = apps.filter { it.label.lowercase().startsWith(needle) }
        if (prefix.isNotEmpty()) return prefix

        val wordStart = apps.filter { app ->
            app.label.lowercase().split(' ', '-', '_', ':').any { it.startsWith(needle) }
        }
        if (wordStart.isNotEmpty()) return wordStart

        val contains = apps.filter {
            it.label.lowercase().contains(needle) || it.packageName.lowercase().contains(needle)
        }
        if (contains.isNotEmpty()) return contains

        return emptyList()
    }

    /** Suggestions shown when an application is not installed. */
    fun suggestions(query: String, apps: List<AppInfo>, aliases: Map<String, String>, max: Int = 4): List<String> {
        val candidates = buildList {
            addAll(apps.map { it.label })
            addAll(aliases.values)
        }.distinct()

        return SuggestionEngine.suggest(query, candidates, max)
    }

    fun aliasFor(packageName: String, aliases: Map<String, String>): String? = aliases[packageName]

    fun displayName(app: AppInfo, aliases: Map<String, String>): String =
        aliases[app.packageName]?.takeIf { it.isNotBlank() } ?: app.label
}
