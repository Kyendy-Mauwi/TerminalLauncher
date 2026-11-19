package com.tty.launcher.domain.commands

enum class CommandCategory(val label: String) {
    NAVIGATION("NAVIGATION"),
    APPLICATIONS("APPLICATIONS"),
    SYSTEM("SYSTEM"),
    PRODUCTIVITY("PRODUCTIVITY"),
    APPEARANCE("APPEARANCE"),
    UTILITY("UTILITY")
}

data class CommandSpec(
    val name: String,
    val usage: String,
    val description: String,
    val category: CommandCategory,
    val aliases: List<String> = emptyList()
)

/**
 * Single source of truth for command metadata: used by `help`, completion, the settings screen
 * and the suggestion engine.
 */
object CommandCatalog {

    val specs: List<CommandSpec> = listOf(
        // Navigation
        CommandSpec("help", "help [command]", "List commands or show usage for one command.", CommandCategory.NAVIGATION, listOf("?")),
        CommandSpec("clear", "clear", "Clear the terminal output.", CommandCategory.NAVIGATION, listOf("cls")),
        CommandSpec("home", "home", "Return to the home screen / terminal start.", CommandCategory.NAVIGATION),
        CommandSpec("exit", "exit", "Leave the terminal (when not the default launcher).", CommandCategory.NAVIGATION, listOf("quit")),
        CommandSpec("back", "back", "Close the keyboard or go back one screen.", CommandCategory.NAVIGATION),
        CommandSpec("about", "about", "About TTY, permissions and privacy.", CommandCategory.NAVIGATION, listOf("info")),
        CommandSpec("settings", "settings [panel]", "Open TTY settings or an Android settings panel.", CommandCategory.NAVIGATION, listOf("config")),

        // Applications
        CommandSpec("apps", "apps [filter]", "List installed applications.", CommandCategory.APPLICATIONS, listOf("list")),
        CommandSpec("open", "open [application]", "Launch an installed application.", CommandCategory.APPLICATIONS, listOf("launch", "run")),
        CommandSpec("search", "search [query]", "Search installed applications.", CommandCategory.APPLICATIONS, listOf("find")),
        CommandSpec("recent", "recent [n]", "Applications launched from TTY, most recent first.", CommandCategory.APPLICATIONS, listOf("recents")),
        CommandSpec("favorites", "favorites [add|remove] [application]", "List or manage favourite applications.", CommandCategory.APPLICATIONS, listOf("faves", "fav")),
        CommandSpec("install", "install [application]", "Open the app store for an application.", CommandCategory.APPLICATIONS, listOf("get")),
        CommandSpec("alias", "alias [application] [alias] | alias remove [application]", "Manage custom application aliases.", CommandCategory.APPLICATIONS, listOf("aliases")),

        // System
        CommandSpec("sysinfo", "sysinfo", "Full system status report.", CommandCategory.SYSTEM, listOf("status")),
        CommandSpec("battery", "battery", "Battery level, charging state and temperature.", CommandCategory.SYSTEM, listOf("bat")),
        CommandSpec("storage", "storage", "Internal storage usage.", CommandCategory.SYSTEM, listOf("df")),
        CommandSpec("memory", "memory", "Runtime memory information.", CommandCategory.SYSTEM, listOf("mem", "free")),
        CommandSpec("device", "device", "Device model and Android version.", CommandCategory.SYSTEM, listOf("uname")),
        CommandSpec("uptime", "uptime", "Time since the device booted.", CommandCategory.SYSTEM),
        CommandSpec("network", "network", "Connectivity state and transport.", CommandCategory.SYSTEM, listOf("net")),
        CommandSpec("wifi", "wifi [settings]", "Wi-Fi state; `wifi settings` opens Wi-Fi settings.", CommandCategory.SYSTEM),
        CommandSpec("bluetooth", "bluetooth [settings]", "Bluetooth state; opens Bluetooth settings.", CommandCategory.SYSTEM, listOf("bt")),
        CommandSpec("brightness", "brightness [0-100]", "Read or set screen brightness.", CommandCategory.SYSTEM),
        CommandSpec("volume", "volume [0-100]", "Read or set media volume.", CommandCategory.SYSTEM, listOf("vol")),
        CommandSpec("notifications", "notifications [clear]", "Recent notifications (needs notification access).", CommandCategory.SYSTEM, listOf("notif")),
        CommandSpec("date", "date", "Current date in the configured format.", CommandCategory.SYSTEM),
        CommandSpec("time", "time", "Current time in the configured format.", CommandCategory.SYSTEM),

        // Productivity
        CommandSpec("notes", "notes [add|remove] [text]", "Local notes stored on device.", CommandCategory.PRODUCTIVITY, listOf("note")),
        CommandSpec("calculator", "calculator [expression]", "Evaluate an expression or open the calculator.", CommandCategory.PRODUCTIVITY, listOf("calc")),
        CommandSpec("calendar", "calendar", "Open the calendar application.", CommandCategory.PRODUCTIVITY),
        CommandSpec("clock", "clock", "Open alarms and clock.", CommandCategory.PRODUCTIVITY, listOf("alarm")),
        CommandSpec("camera", "camera", "Open the camera.", CommandCategory.PRODUCTIVITY),
        CommandSpec("files", "files", "Open the system document picker.", CommandCategory.PRODUCTIVITY),
        CommandSpec("contacts", "contacts", "Open contacts.", CommandCategory.PRODUCTIVITY, listOf("people")),
        CommandSpec("messages", "messages", "Open the messaging application.", CommandCategory.PRODUCTIVITY, listOf("sms")),
        CommandSpec("phone", "phone", "Open the dialer.", CommandCategory.PRODUCTIVITY, listOf("dial")),
        CommandSpec("wallpaper", "wallpaper", "Open the Android wallpaper chooser.", CommandCategory.PRODUCTIVITY),

        // Appearance
        CommandSpec("theme", "theme [${ThemeNames.KEYS}]", "Switch the terminal colour theme.", CommandCategory.APPEARANCE, listOf("color", "colour")),
        CommandSpec("font", "font [jetbrains|mono]", "Choose the terminal font.", CommandCategory.APPEARANCE),
        CommandSpec("fontsize", "fontsize [9-22]", "Set terminal text size in sp.", CommandCategory.APPEARANCE, listOf("fs")),
        CommandSpec("opacity", "opacity [40-100]", "Set terminal background opacity.", CommandCategory.APPEARANCE),
        CommandSpec("cursor", "cursor [block|bar|underline]", "Set the cursor style.", CommandCategory.APPEARANCE),
        CommandSpec("animations", "animations [on|off]", "Toggle terminal animations and effects.", CommandCategory.APPEARANCE, listOf("anim")),

        // Utility
        CommandSpec("whoami", "whoami", "Show the configured terminal user.", CommandCategory.UTILITY),
        CommandSpec("hostname", "hostname [new-name]", "Show or set the device alias.", CommandCategory.UTILITY),
        CommandSpec("history", "history [n|clear]", "Show or clear stored command history.", CommandCategory.UTILITY),
        CommandSpec("echo", "echo [text]", "Print text back to the terminal.", CommandCategory.UTILITY),
        CommandSpec("version", "version", "TTY build information.", CommandCategory.UTILITY, listOf("ver")),
        CommandSpec("ping", "ping [host]", "TCP reachability and latency test.", CommandCategory.UTILITY),
        CommandSpec("reboot", "reboot", "Explains why reboot is unavailable.", CommandCategory.UTILITY)
    )

    private val byName: Map<String, CommandSpec> = buildMap {
        specs.forEach { spec ->
            put(spec.name, spec)
            spec.aliases.forEach { alias -> put(alias, spec) }
        }
    }

    fun find(input: String): CommandSpec? = byName[input.trim().lowercase()]

    fun names(): List<String> = specs.map { it.name }

    fun allLookupKeys(): List<String> = byName.keys.toList()

    fun byCategory(): Map<CommandCategory, List<CommandSpec>> =
        specs.groupBy { it.category }

    fun isKnown(input: String): Boolean = byName.containsKey(input.trim().lowercase())

    /** Object holding theme keys to avoid importing the models package here. */
    private object ThemeNames {
        const val KEYS = "green|amber|cyan|matrix|mono"
    }
}
