package com.tty.launcher.domain.commands

import com.tty.launcher.BuildConfig
import com.tty.launcher.data.datastore.SettingsRepository
import com.tty.launcher.data.repositories.AliasRepository
import com.tty.launcher.data.repositories.AppRepository
import com.tty.launcher.data.repositories.FavoritesRepository
import com.tty.launcher.data.repositories.HistoryRepository
import com.tty.launcher.data.repositories.LaunchResult
import com.tty.launcher.data.repositories.NotesRepository
import com.tty.launcher.data.repositories.RecentAppsRepository
import com.tty.launcher.domain.models.AppInfo
import com.tty.launcher.domain.models.AppSettings
import com.tty.launcher.domain.models.CommandOutcome
import com.tty.launcher.domain.models.CursorStyle
import com.tty.launcher.domain.models.FontChoice
import com.tty.launcher.domain.models.LineStyle
import com.tty.launcher.domain.models.OutputLine
import com.tty.launcher.domain.models.TerminalAction
import com.tty.launcher.domain.models.ThemeId
import com.tty.launcher.domain.models.accent
import com.tty.launcher.domain.models.banner
import com.tty.launcher.domain.models.dim
import com.tty.launcher.domain.models.error
import com.tty.launcher.domain.models.normal
import com.tty.launcher.domain.models.success
import com.tty.launcher.domain.models.warning
import com.tty.launcher.services.notifications.NotificationAccess
import com.tty.launcher.services.notifications.NotificationStore
import com.tty.launcher.services.system.BrightnessController
import com.tty.launcher.services.system.DeviceInfoProvider
import com.tty.launcher.services.system.SettingsPanel
import com.tty.launcher.services.system.SystemIntents
import com.tty.launcher.services.system.VolumeController
import com.tty.launcher.utils.Formatters
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Clock
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central command interpreter. Parsing lives in [CommandParser], metadata in [CommandCatalog] and
 * this class only performs validated side effects through real Android APIs. Every branch returns
 * a [CommandOutcome]; exceptions are converted into readable terminal errors by [execute].
 */
@Singleton
class CommandDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val aliasRepository: AliasRepository,
    private val notesRepository: NotesRepository,
    private val recentAppsRepository: RecentAppsRepository,
    private val appRepository: AppRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val volumeController: VolumeController,
    private val brightnessController: BrightnessController,
    private val systemIntents: SystemIntents,
    private val clock: Clock
) {

    suspend fun execute(
        parsed: CommandParser.ParsedCommand,
        settings: AppSettings,
        isDefaultLauncher: Boolean
    ): CommandOutcome {
        val preamble = parsed.warnings.map { warning(it) }
        val canonical = CommandCatalog.find(parsed.name)?.name ?: parsed.name
        val outcome = try {
            when (canonical) {
                "help" -> help(parsed.args)
                "clear" -> CommandOutcome(
                    lines = listOf(success("OUTPUT BUFFER CLEARED.")),
                    clearScreen = true
                )
                "home" -> home(isDefaultLauncher)
                "exit" -> exit(isDefaultLauncher)
                "back" -> CommandOutcome(
                    lines = listOf(dim("CLOSING CURRENT VIEW.")),
                    action = TerminalAction.NavigateBack
                )
                "about" -> about()
                "settings" -> settings(parsed.args)

                "apps" -> listApplications(parsed.args)
                "open" -> openApplication(parsed.args)
                "search" -> searchApplications(parsed.args)
                "recent" -> recentApplications(parsed.args)
                "favorites" -> favorites(parsed.args)
                "install" -> install(parsed.args)
                "alias" -> aliases(parsed.args)

                "sysinfo" -> systemInfo(settings)
                "battery" -> batteryInfo()
                "storage" -> storageInfo()
                "memory" -> memoryInfo()
                "device" -> deviceInfo()
                "uptime" -> uptimeInfo()
                "network" -> networkInfo()
                "wifi" -> wifi(parsed.args)
                "bluetooth" -> bluetooth(parsed.args)
                "brightness" -> brightness(parsed.args)
                "volume" -> volume(parsed.args)
                "notifications" -> notifications(parsed.args)
                "date" -> date(settings)
                "time" -> time(settings)

                "notes" -> notes(parsed.args)
                "calculator" -> calculator(parsed.rawArgs)
                "calendar" -> openEverydayApp("CALENDAR", systemIntents.openCalendar())
                "clock" -> openEverydayApp("CLOCK", systemIntents.openClock())
                "camera" -> openEverydayApp("CAMERA", systemIntents.openCamera())
                "files" -> openEverydayApp("FILES", systemIntents.openFiles())
                "contacts" -> openEverydayApp("CONTACTS", systemIntents.openContacts())
                "messages" -> openEverydayApp("MESSAGES", systemIntents.openMessages())
                "phone" -> openEverydayApp("DIALER", systemIntents.openDialer())
                "wallpaper" -> openEverydayApp("WALLPAPER PICKER", systemIntents.openWallpaperChooser())

                "theme" -> theme(parsed.args, settings)
                "font" -> font(parsed.args, settings)
                "fontsize" -> fontSize(parsed.args, settings)
                "opacity" -> opacity(parsed.args, settings)
                "cursor" -> cursor(parsed.args, settings)
                "animations" -> animations(parsed.args, settings)

                "whoami" -> whoAmI(settings)
                "hostname" -> hostname(parsed.args, settings)
                "history" -> history(parsed.args, settings)
                "echo" -> echo(parsed)
                "version" -> version()
                "ping" -> ping(parsed.args)
                "reboot" -> reboot()

                else -> unknownCommand(parsed)
            }
        } catch (e: Exception) {
            CommandOutcome(
                listOf(
                    error("UNEXPECTED ERROR: ${e.javaClass.simpleName}"),
                    dim(e.message ?: "The command could not be completed."),
                    dim("Nothing was changed. Type 'help' to continue.")
                )
            )
        }

        return if (preamble.isEmpty()) outcome else outcome.copy(lines = preamble + outcome.lines)
    }

    // ---------------------------------------------------------------- navigation

    private fun help(args: List<String>): CommandOutcome {
        val query = args.firstOrNull()
        if (query != null) {
            val spec = CommandCatalog.find(query)
            if (spec == null) {
                val lines = mutableListOf(error("NO HELP AVAILABLE FOR: ${query.lowercase(Locale.US)}"))
                lines += dim("")
                SuggestionEngine.suggest(query, CommandCatalog.names()).forEach {
                    lines += accent("  Did you mean: $it")
                }
                return CommandOutcome(lines)
            }
            val lines = mutableListOf(
                banner("MANUAL :: ${spec.name.uppercase(Locale.US)}"),
                dim(""),
                normal("USAGE       : ${spec.usage}"),
                normal("DESCRIPTION : ${spec.description}"),
                normal("CATEGORY    : ${spec.category.label}")
            )
            if (spec.aliases.isNotEmpty()) {
                lines += normal("ALIASES     : ${spec.aliases.joinToString(", ")}")
            }
            return CommandOutcome(lines)
        }

        val lines = mutableListOf(banner("AVAILABLE COMMANDS"), dim(""))
        CommandCatalog.byCategory().forEach { (category, specs) ->
            lines += accent(category.label)
            specs.forEach { spec ->
                lines += normal("  " + spec.name.padEnd(13) + spec.description)
            }
            lines += dim("")
        }
        lines += dim("Type 'help <command>' for usage details.")
        lines += dim("'help', 'clear', 'open' and 'settings' are always available.")
        return CommandOutcome(lines)
    }

    private fun home(isDefaultLauncher: Boolean): CommandOutcome {
        val lines = if (isDefaultLauncher) {
            listOf(dim("HOME :: TTY IS THE DEFAULT HOME. RESETTING TO PROMPT."))
        } else {
            listOf(accent("RETURNING TO SYSTEM HOME..."))
        }
        return CommandOutcome(lines, action = TerminalAction.GoHome)
    }

    private fun exit(isDefaultLauncher: Boolean): CommandOutcome {
        if (isDefaultLauncher) {
            return CommandOutcome(
                listOf(
                    warning("EXIT UNAVAILABLE WHILE TTY IS THE DEFAULT HOME."),
                    dim("Android requires a default home application at all times."),
                    dim("To leave, select another launcher in Settings > Apps > Default apps.")
                )
            )
        }
        return CommandOutcome(
            listOf(accent("CLOSING TERMINAL...")),
            action = TerminalAction.FinishActivity
        )
    }

    private fun about(): CommandOutcome = CommandOutcome(
        listOf(
            banner("TTY :: TERMINAL ANDROID LAUNCHER"),
            dim(""),
            normal("VERSION     : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
            normal("PACKAGE     : ${BuildConfig.APPLICATION_ID}"),
            normal("PLATFORM    : ANDROID ${android.os.Build.VERSION.RELEASE} / SDK ${android.os.Build.VERSION.SDK_INT}"),
            dim(""),
            normal("A terminal operating environment for Android. TTY reads real system"),
            normal("state, launches installed applications and stores everything locally."),
            dim(""),
            accent("PRIVACY"),
            normal("  - No account, no cloud sync, no analytics, no telemetry."),
            normal("  - Command history, notes and favourites never leave the device."),
            normal("  - Notification contents are held in memory only, never stored."),
            normal("  - Internet is used only by the 'ping' diagnostic and store links."),
            dim(""),
            accent("LIMITS"),
            normal("  - No root, no shell execution, no privileged system changes."),
            normal("  - Android does not let apps toggle Wi-Fi, Bluetooth or reboot."),
            dim(""),
            dim("Source: local build. Report issues to your fork maintainer.")
        )
    )

    private suspend fun settings(args: List<String>): CommandOutcome {
        val panelKey = args.firstOrNull()
        if (panelKey == null) {
            return CommandOutcome(
                listOf(accent("OPENING TTY SETTINGS...")),
                action = TerminalAction.OpenSettings
            )
        }

        if (panelKey.equals("terminal", true) || panelKey.equals("tty", true)) {
            return CommandOutcome(listOf(accent("OPENING TTY SETTINGS...")), action = TerminalAction.OpenSettings)
        }

        if (panelKey.equals("notifications", true) && args.getOrNull(1).equals("access", true)) {
            return CommandOutcome(
                listOf(accent("OPENING NOTIFICATION ACCESS SETTINGS...")),
                action = TerminalAction.RequestNotificationAccess
            )
        }

        val panel = SettingsPanel.fromKey(panelKey)
        if (panel == null) {
            val panels = SettingsPanel.entries.joinToString(", ") { it.key }
            return CommandOutcome(
                listOf(
                    error("UNKNOWN SETTINGS PANEL: $panelKey"),
                    normal("Usage: settings [panel]"),
                    dim("Panels: $panels, terminal")
                )
            )
        }

        val opened = systemIntents.openPanel(panel)
        return if (opened) {
            CommandOutcome(listOf(accent("OPENING ${panel.key.uppercase(Locale.US)} SETTINGS...")))
        } else {
            CommandOutcome(
                listOf(
                    error("ANDROID SETTINGS UNAVAILABLE ON THIS DEVICE."),
                    dim("Your OEM may not expose the ${panel.key} panel.")
                )
            )
        }
    }

    // ---------------------------------------------------------------- applications

    private suspend fun listApplications(args: List<String>): CommandOutcome {
        val filter = args.joinToString(" ").trim()
        val apps = appRepository.installedApps(forceRefresh = true)
        if (apps.isEmpty()) {
            return CommandOutcome(
                listOf(
                    warning("NO LAUNCHABLE APPLICATIONS FOUND."),
                    dim("Android package visibility may be restricted on this device.")
                )
            )
        }

        val aliasMap = aliasRepository.snapshot()
        val filtered = if (filter.isEmpty()) apps else apps.filter {
            it.label.contains(filter, true) || it.packageName.contains(filter, true)
        }

        if (filtered.isEmpty()) {
            val lines = mutableListOf(warning("NO APPLICATIONS MATCH \"$filter\"."), dim(""))
            SuggestionEngine.suggest(filter, apps.map { it.label }).forEach {
                lines += accent("  Did you mean: $it")
            }
            return CommandOutcome(lines)
        }

        val lines = mutableListOf(
            banner("INSTALLED APPLICATIONS (${filtered.size})"),
            dim("")
        )
        filtered.take(MAX_LISTED_APPS).forEach { app ->
            val name = AppResolver.displayName(app, aliasMap)
            lines += normal("  " + name.padEnd(24) + AppResolver.aliasFor(app.packageName, aliasMap)?.let { "($it)" } ?: "")
        }
        if (filtered.size > MAX_LISTED_APPS) {
            lines += dim("  ... ${filtered.size - MAX_LISTED_APPS} more. Refine with: apps <filter>")
        }
        lines += dim("")
        lines += dim("Launch with: open <application>")
        return CommandOutcome(lines)
    }

    private suspend fun openApplication(args: List<String>): CommandOutcome {
        if (args.isEmpty()) {
            return usage("open", "Missing application name.")
        }

        val query = args.joinToString(" ").trim()
        val apps = appRepository.installedApps()
        val aliasMap = aliasRepository.snapshot()
        val matches = AppResolver.match(query, apps, aliasMap)

        if (matches.isEmpty()) {
            val lines = mutableListOf(
                error("APPLICATION NOT FOUND."),
                dim(""),
                normal("Check the installed applications list with 'apps'.")
            )
            val suggestions = AppResolver.suggestions(query, apps, aliasMap)
            if (suggestions.isNotEmpty()) {
                lines += dim("")
                lines += dim("Did you mean:")
                suggestions.forEach { lines += accent("  open $it") }
            }
            return CommandOutcome(lines)
        }

        if (matches.size > 1) {
            val lines = mutableListOf(
                warning("MULTIPLE MATCHES FOR \"$query\"."),
                dim(""),
                dim("Refine your command:")
            )
            matches.take(8).forEach { lines += accent("  open ${it.label}") }
            return CommandOutcome(lines)
        }

        val app = matches.first()
        return when (val result = appRepository.launch(app)) {
            is LaunchResult.Success -> CommandOutcome(
                listOf(
                    accent("LAUNCHING ${app.label.uppercase(Locale.US)}..."),
                    success("APPLICATION STARTED SUCCESSFULLY.")
                )
            )
            is LaunchResult.Failure -> CommandOutcome(
                listOf(
                    error("LAUNCH FAILED."),
                    dim(result.reason),
                    dim(""),
                    dim("Installed applications: 'apps'. Store page: 'install ${app.label}'.")
                )
            )
        }
    }

    private suspend fun searchApplications(args: List<String>): CommandOutcome {
        if (args.isEmpty()) return usage("search", "Missing search query.")
        val query = args.joinToString(" ")
        val apps = appRepository.installedApps(forceRefresh = true)
        val aliasMap = aliasRepository.snapshot()
        val matches = AppResolver.match(query, apps, aliasMap)

        if (matches.isEmpty()) {
            val lines = mutableListOf(warning("NO RESULTS FOR \"$query\"."), dim(""))
            AppResolver.suggestions(query, apps, aliasMap).forEach { lines += accent("  Did you mean: $it") }
            return CommandOutcome(lines)
        }

        val lines = mutableListOf(banner("SEARCH RESULTS (${matches.size})"), dim(""))
        matches.take(MAX_LISTED_APPS).forEach { app ->
            lines += normal("  " + AppResolver.displayName(app, aliasMap).padEnd(24) + app.packageName)
        }
        lines += dim("")
        lines += dim("Launch with: open <name>")
        return CommandOutcome(lines)
    }

    private suspend fun recentApplications(args: List<String>): CommandOutcome {
        val limit = args.firstOrNull()?.toIntOrNull()?.coerceIn(1, 20) ?: 8
        val recent = recentAppsRepository.recent(limit)
        if (recent.isEmpty()) {
            return CommandOutcome(
                listOf(
                    warning("NO RECENT APPLICATIONS YET."),
                    dim("Android does not expose the system recents list to launchers."),
                    dim("TTY records applications you launch from this terminal.")
                )
            )
        }

        val now = System.currentTimeMillis()
        val lines = mutableListOf(banner("RECENT APPLICATIONS"), dim(""))
        recent.forEachIndexed { index, app ->
            val age = Formatters.shortDuration(now - app.lastUsedAt)
            lines += normal("  ${(index + 1).toString().padEnd(3)}${app.label.padEnd(24)}$age ago")
        }
        lines += dim("")
        lines += dim("Launch with: open <application>")
        return CommandOutcome(lines)
    }

    private suspend fun favorites(args: List<String>): CommandOutcome {
        val action = args.firstOrNull()?.lowercase(Locale.US)
        return when (action) {
            null -> {
                val favorites = favoritesRepository.list()
                if (favorites.isEmpty()) {
                    return CommandOutcome(
                        listOf(
                            warning("NO FAVOURITES YET."),
                            dim("Add one with: favorites add <application>")
                        )
                    )
                }
                val lines = mutableListOf(banner("FAVOURITES"), dim(""))
                favorites.forEach { lines += normal("  " + it.label.padEnd(24) + it.packageName) }
                lines += dim("")
                lines += dim("favorites add|remove <application>")
                CommandOutcome(lines)
            }

            "add" -> {
                val query = args.drop(1).joinToString(" ")
                if (query.isBlank()) return usage("favorites add", "Missing application name.")
                val app = resolveSingle(query) ?: return applicationNotFound(query)
                favoritesRepository.add(app)
                CommandOutcome(listOf(success("ADDED TO FAVOURITES: ${app.label}")))
            }

            "remove", "rm", "delete" -> {
                val query = args.drop(1).joinToString(" ")
                if (query.isBlank()) return usage("favorites remove", "Missing application name.")
                val app = resolveSingle(query) ?: return applicationNotFound(query)
                favoritesRepository.remove(app.packageName)
                CommandOutcome(listOf(success("REMOVED FROM FAVOURITES: ${app.label}")))
            }

            else -> usage("favorites", "Unknown action '$action'. Use add or remove.")
        }
    }

    private suspend fun install(args: List<String>): CommandOutcome {
        if (args.isEmpty()) {
            return CommandOutcome(
                listOf(
                    error("ERROR: Missing application name."),
                    normal("Usage: install [application]"),
                    dim(""),
                    dim("TTY cannot install applications silently.")
                )
            )
        }

        val query = args.joinToString(" ")
        val apps = appRepository.installedApps()
        val installed = AppResolver.match(query, apps, aliasRepository.snapshot()).firstOrNull()
        val opened = if (installed != null) {
            systemIntents.openStoreDetails(installed.packageName)
        } else {
            systemIntents.openStoreSearch(query)
        }

        return if (opened) {
            CommandOutcome(
                listOf(
                    accent(if (installed != null) "OPENING STORE PAGE FOR ${installed.label}..." else "SEARCHING THE APP STORE FOR \"$query\"..."),
                    dim(""),
                    dim("Android requires installation through the store's own installer."),
                    dim("TTY never installs apps silently.")
                )
            )
        } else {
            CommandOutcome(
                listOf(
                    error("NO APP STORE AVAILABLE."),
                    dim("Install an app store, then install \"$query\" from it."),
                    dim("Android only allows installation through an authorized installer.")
                )
            )
        }
    }

    private suspend fun aliases(args: List<String>): CommandOutcome {
        val action = args.firstOrNull()?.lowercase(Locale.US)
        when {
            action == null -> {
                val aliases = aliasRepository.snapshot()
                if (aliases.isEmpty()) {
                    return CommandOutcome(
                        listOf(
                            warning("NO ALIASES DEFINED."),
                            dim("Create one with: alias set <alias> <application>")
                        )
                    )
                }
                val apps = appRepository.installedApps()
                val lines = mutableListOf(banner("APPLICATION ALIASES"), dim(""))
                aliases.entries.sortedBy { it.value }.forEach { (pkg, alias) ->
                    val label = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                    lines += normal("  " + alias.padEnd(16) + label)
                }
                lines += dim("")
                lines += dim("Use directly: open <alias>")
                return CommandOutcome(lines)
            }

            action == "set" -> {
                if (args.size < 3) return usage("alias set", "Missing alias or application.")
                val alias = args[1].trim().lowercase(Locale.US)
                if (!Regex(ALIAS_PATTERN).matches(alias)) {
                    return CommandOutcome(
                        listOf(
                            error("INVALID ALIAS: $alias"),
                            dim("Use 1-16 characters: letters, digits, - or _.")
                        )
                    )
                }
                val query = args.drop(2).joinToString(" ")
                val app = resolveSingle(query) ?: return applicationNotFound(query)
                aliasRepository.set(app.packageName, alias)
                return CommandOutcome(
                    listOf(
                        success("ALIAS CREATED: $alias -> ${app.label}"),
                        dim("Try: open $alias")
                    )
                )
            }

            action == "remove" || action == "rm" || action == "delete" -> {
                if (args.size < 2) return usage("alias remove", "Missing alias or application.")
                val target = args.drop(1).joinToString(" ").trim()
                val aliases = aliasRepository.snapshot()
                val packageName = aliases.entries.firstOrNull { it.value.equals(target, true) }?.key
                    ?: resolveSingle(target)?.packageName
                if (packageName == null) {
                    return CommandOutcome(listOf(error("NO ALIAS OR APPLICATION MATCHES: $target")))
                }
                aliasRepository.remove(packageName)
                return CommandOutcome(listOf(success("ALIAS REMOVED.")))
            }

            else -> return usage("alias", "Unknown action '$action'. Use set or remove.")
        }
    }

    // ---------------------------------------------------------------- system

    private fun systemInfo(settings: AppSettings): CommandOutcome {
        val battery = deviceInfoProvider.battery()
        val storage = deviceInfoProvider.storage()
        val memory = deviceInfoProvider.memory()
        val network = deviceInfoProvider.network()
        val device = deviceInfoProvider.device()
        val now = clock.millis()

        return CommandOutcome(
            listOf(
                banner("SYSTEM MONITOR"),
                dim(""),
                normal(Formatters.row("DEVICE", "${device.manufacturer.uppercase(Locale.US)} ${device.model}")),
                normal(Formatters.row("ANDROID", "${device.androidRelease} (SDK ${device.sdkInt})")),
                normal(
                    Formatters.row(
                        "BATTERY",
                        battery.percent?.let { "$it%" } ?: "UNKNOWN",
                        11
                    ) + "  " + battery.status
                ),
                normal(Formatters.row("POWER", formatPlug(battery.plugType, battery.isCharging))),
                normal(
                    Formatters.row(
                        "NETWORK",
                        if (network.connected) "${network.transport} CONNECTED" else "OFFLINE"
                    )
                ),
                normal(Formatters.row("STORAGE", "${Formatters.bytes(storage.availableBytes)} FREE")),
                normal(Formatters.row("MEMORY", "${Formatters.bytes(memory.availableBytes)} FREE")),
                normal(Formatters.row("UPTIME", Formatters.uptime(deviceInfoProvider.uptimeMillis()))),
                normal(Formatters.row("TIME", Formatters.dateTime(now, settings.timeFormat))),
                dim(""),
                success("[ SYSTEM READY ]")
            )
        )
    }

    private fun batteryInfo(): CommandOutcome {
        val battery = deviceInfoProvider.battery()
        val lines = mutableListOf(banner("BATTERY STATUS"), dim(""))
        lines += normal(Formatters.row("LEVEL", battery.percent?.let { "$it%" } ?: "UNKNOWN"))
        lines += normal(Formatters.row("STATUS", battery.status))
        lines += normal(Formatters.row("POWER", formatPlug(battery.plugType, battery.isCharging)))
        lines += normal(Formatters.row("HEALTH", battery.health))
        lines += normal(
            Formatters.row("TEMP", battery.temperatureC?.let { String.format(Locale.US, "%.1f C", it) } ?: "UNKNOWN")
        )
        return CommandOutcome(lines)
    }

    private fun storageInfo(): CommandOutcome {
        val storage = deviceInfoProvider.storage()
        if (storage.totalBytes <= 0L) {
            return CommandOutcome(
                listOf(
                    warning("STORAGE INFORMATION UNAVAILABLE."),
                    dim("This device or profile restricts storage statistics.")
                )
            )
        }
        return CommandOutcome(
            listOf(
                banner("INTERNAL STORAGE"),
                dim(""),
                normal(Formatters.row("TOTAL", Formatters.bytes(storage.totalBytes))),
                normal(Formatters.row("USED", "${Formatters.bytes(storage.usedBytes)} (${storage.usedPercent}%)")),
                normal(Formatters.row("FREE", Formatters.bytes(storage.availableBytes))),
                dim(""),
                dim("Android reports the app-accessible internal data partition.")
            )
        )
    }

    private fun memoryInfo(): CommandOutcome {
        val memory = deviceInfoProvider.memory()
        if (memory.totalBytes <= 0L) {
            return CommandOutcome(listOf(warning("MEMORY INFORMATION UNAVAILABLE.")))
        }
        val lines = mutableListOf(banner("MEMORY"), dim(""))
        lines += normal(Formatters.row("TOTAL", Formatters.bytes(memory.totalBytes)))
        lines += normal(Formatters.row("USED", "${Formatters.bytes(memory.usedBytes)} (${memory.usedPercent}%)"))
        lines += normal(Formatters.row("FREE", Formatters.bytes(memory.availableBytes)))
        lines += normal(Formatters.row("LOW MEM", if (memory.lowMemory) "YES" else "NO"))
        lines += normal(Formatters.row("THRESHOLD", Formatters.bytes(memory.thresholdBytes)))
        return CommandOutcome(lines)
    }

    private suspend fun deviceInfo(): CommandOutcome {
        val device = deviceInfoProvider.device()
        return CommandOutcome(
            listOf(
                banner("DEVICE"),
                dim(""),
                normal(Formatters.row("MAKER", device.manufacturer)),
                normal(Formatters.row("MODEL", device.model)),
                normal(Formatters.row("CODENAME", device.device)),
                normal(Formatters.row("PRODUCT", device.product)),
                normal(Formatters.row("ANDROID", device.androidRelease)),
                normal(Formatters.row("SDK", device.sdkInt.toString())),
                normal(Formatters.row("HOSTNAME", currentHostname())),
                normal(Formatters.row("USER", currentUsername()))
            )
        )
    }

    private fun uptimeInfo(): CommandOutcome = CommandOutcome(
        listOf(
            banner("UPTIME"),
            dim(""),
            normal("UP " + Formatters.uptime(deviceInfoProvider.uptimeMillis())),
            dim("Time since boot, excluding deep sleep on some devices.")
        )
    )

    private fun networkInfo(): CommandOutcome {
        val network = deviceInfoProvider.network()
        val lines = mutableListOf(banner("NETWORK"), dim(""))
        lines += normal(Formatters.row("STATUS", if (network.connected) "CONNECTED" else "OFFLINE"))
        lines += normal(Formatters.row("TRANSPORT", network.transport))
        lines += normal(Formatters.row("WIFI", if (network.isWifi) "YES" else "NO"))
        lines += normal(Formatters.row("METERED", if (network.isMetered) "YES" else "NO"))
        lines += normal(Formatters.row("VALIDATED", if (network.isValidated) "YES" else "NO"))
        network.downstreamKbps?.let { lines += normal(Formatters.row("DOWN", "$it kbps")) }
        network.upstreamKbps?.let { lines += normal(Formatters.row("UP", "$it kbps")) }
        lines += dim("")
        lines += dim("Use 'wifi' or 'settings wifi' for Wi-Fi controls.")
        return CommandOutcome(lines)
    }

    private fun wifi(args: List<String>): CommandOutcome {
        val enabled = deviceInfoProvider.wifiEnabled()
        val network = deviceInfoProvider.network()
        if (args.firstOrNull()?.equals("settings", true) == true) {
            return if (systemIntents.openPanel(SettingsPanel.WIFI)) {
                CommandOutcome(listOf(accent("OPENING WI-FI SETTINGS...")))
            } else {
                CommandOutcome(listOf(error("WI-FI SETTINGS UNAVAILABLE.")))
            }
        }
        return CommandOutcome(
            listOf(
                banner("WI-FI"),
                dim(""),
                normal(Formatters.row("RADIO", when (enabled) { true -> "ON"; false -> "OFF"; null -> "UNKNOWN" })),
                normal(Formatters.row("CONNECTED", if (network.isWifi && network.connected) "YES" else "NO")),
                normal(Formatters.row("TRANSPORT", network.transport)),
                dim(""),
                dim("Android does not allow apps to toggle Wi-Fi. Use: wifi settings")
            )
        )
    }

    private fun bluetooth(args: List<String>): CommandOutcome {
        val state = deviceInfoProvider.bluetoothState()
        if (args.firstOrNull()?.equals("settings", true) == true) {
            return if (systemIntents.openPanel(SettingsPanel.BLUETOOTH)) {
                CommandOutcome(listOf(accent("OPENING BLUETOOTH SETTINGS...")))
            } else {
                CommandOutcome(listOf(error("BLUETOOTH SETTINGS UNAVAILABLE.")))
            }
        }
        val lines = mutableListOf(
            banner("BLUETOOTH"),
            dim(""),
            normal(Formatters.row("STATE", state))
        )
        if (state == "PERMISSION REQUIRED") {
            lines += dim("")
            lines += warning("Bluetooth state needs the Nearby devices permission.")
            lines += dim("Grant it from TTY settings, or use: bluetooth settings")
            return CommandOutcome(lines, action = TerminalAction.GrantBluetoothPermission)
        }
        lines += dim("")
        lines += dim("Android does not allow apps to toggle Bluetooth. Use: bluetooth settings")
        return CommandOutcome(lines)
    }

    private suspend fun brightness(args: List<String>): CommandOutcome {
        val current = brightnessController.currentPercent()
        val requested = args.firstOrNull()?.removeSuffix("%")?.toIntOrNull()
        if (requested == null) {
            val lines = mutableListOf(
                banner("BRIGHTNESS"),
                dim(""),
                normal(Formatters.row("LEVEL", current?.let { "$it%" } ?: "UNKNOWN")),
                dim("Set with: brightness <0-100>")
            )
            if (!brightnessController.canWrite()) {
                lines += dim("")
                lines += warning("WRITE_SETTINGS permission is required to change brightness.")
            }
            return CommandOutcome(lines)
        }

        if (requested !in 0..100) return usage("brightness", "Brightness must be between 0 and 100.")

        if (!brightnessController.canWrite()) {
            return CommandOutcome(
                listOf(
                    error("PERMISSION REQUIRED: WRITE_SETTINGS"),
                    dim("Android requires explicit approval to modify system settings."),
                    dim("Approve it in the screen that opens, then run the command again.")
                ),
                action = TerminalAction.RequestWriteSettings
            )
        }

        return if (brightnessController.setPercent(requested)) {
            CommandOutcome(listOf(success("BRIGHTNESS SET TO ${brightnessController.currentPercent() ?: requested}%.")))
        } else {
            CommandOutcome(
                listOf(
                    error("BRIGHTNESS CHANGE REJECTED."),
                    dim("The system refused the write. Some OEM builds block it.")
                )
            )
        }
    }

    private fun volume(args: List<String>): CommandOutcome {
        val requested = args.firstOrNull()?.removeSuffix("%")?.toIntOrNull()
        if (requested == null) {
            val lines = mutableListOf(banner("VOLUME"), dim(""))
            volumeController.levels().forEach { info ->
                val suffix = if (info.muted) "  (MUTED)" else ""
                lines += normal(Formatters.row(info.label, "${info.percent}%$suffix"))
            }
            lines += dim("")
            lines += dim("Set media volume with: volume <0-100>")
            return CommandOutcome(lines)
        }

        if (requested !in 0..100) return usage("volume", "Volume must be between 0 and 100.")
        val result = volumeController.setMediaPercent(requested)
        return if (result.isSuccess) {
            CommandOutcome(listOf(success("MEDIA VOLUME SET TO ${result.getOrNull()}%.")))
        } else {
            CommandOutcome(
                listOf(
                    error("VOLUME CHANGE REJECTED."),
                    dim(result.exceptionOrNull()?.javaClass?.simpleName ?: "The system refused the change.")
                )
            )
        }
    }

    private fun notifications(args: List<String>): CommandOutcome {
        if (args.firstOrNull()?.equals("clear", true) == true) {
            NotificationStore.clear()
            return CommandOutcome(listOf(success("NOTIFICATION CACHE CLEARED.")))
        }

        if (!NotificationAccess.isGranted(context)) {
            return CommandOutcome(
                listOf(
                    warning("NOTIFICATION ACCESS NOT GRANTED."),
                    dim(""),
                    normal("TTY can list notification titles only after you grant access."),
                    dim("Nothing is stored, uploaded or transmitted."),
                    dim(""),
                    dim("Grant access in Settings > Notifications > Device & app notifications.")
                ),
                action = TerminalAction.RequestNotificationAccess
            )
        }

        val records = NotificationStore.notifications.value
        if (records.isEmpty()) {
            return CommandOutcome(
                listOf(
                    dim("NO ACTIVE NOTIFICATIONS."),
                    dim("New notifications appear here while access is granted.")
                )
            )
        }

        val now = clock.millis()
        val lines = mutableListOf(banner("RECENT NOTIFICATIONS (${records.size})"), dim(""))
        records.take(12).forEach { record ->
            lines += accent("  ${record.appLabel.uppercase(Locale.US)}  " + Formatters.shortDuration(now - record.postedAt) + " ago")
            if (record.title.isNotBlank()) lines += normal("    ${record.title}")
            if (record.text.isNotBlank()) lines += dim("    ${record.text.take(90)}")
        }
        lines += dim("")
        lines += dim("Contents stay in memory only.")
        return CommandOutcome(lines)
    }

    private fun date(settings: AppSettings): CommandOutcome = CommandOutcome(
        listOf(
            normal(LocalDateTime.now(clock).formatSafe(settings.dateFormat)),
            dim("Format: ${settings.dateFormat}  Change in settings.")
        )
    )

    private fun time(settings: AppSettings): CommandOutcome = CommandOutcome(
        listOf(
            normal(LocalDateTime.now(clock).formatSafe(settings.timeFormat)),
            dim("Format: ${settings.timeFormat}  Change in settings.")
        )
    )

    // ---------------------------------------------------------------- productivity

    private suspend fun notes(args: List<String>): CommandOutcome {
        val action = args.firstOrNull()?.lowercase(Locale.US)
        return when (action) {
            null -> {
                val notes = notesRepository.list()
                if (notes.isEmpty()) {
                    return CommandOutcome(
                        listOf(
                            warning("NO NOTES SAVED."),
                            dim("Create one with: notes add <text>")
                        )
                    )
                }
                val lines = mutableListOf(banner("NOTES (${notes.size})"), dim(""))
                notes.forEach { note ->
                    lines += accent("  #${note.id}  " + Formatters.dateTime(note.updatedAt, "yyyy-MM-dd HH:mm"))
                    lines += normal("    ${note.content.take(160)}")
                }
                lines += dim("")
                lines += dim("notes add <text> | notes remove <id> | notes clear")
                CommandOutcome(lines)
            }

            "add" -> {
                val text = args.drop(1).joinToString(" ").trim()
                if (text.isBlank()) return usage("notes add", "Missing note text.")
                val id = notesRepository.add(text)
                CommandOutcome(listOf(success("NOTE #$id SAVED LOCALLY.")))
            }

            "remove", "rm", "delete" -> {
                val id = args.getOrNull(1)?.toLongOrNull()
                    ?: return usage("notes remove", "Missing numeric note id.")
                notesRepository.delete(id)
                CommandOutcome(listOf(success("NOTE #$id REMOVED.")))
            }

            "clear" -> {
                notesRepository.clear()
                CommandOutcome(listOf(success("ALL NOTES REMOVED.")))
            }

            else -> {
                // Treat unknown action as note text for convenience.
                val id = notesRepository.add(args.joinToString(" "))
                CommandOutcome(listOf(success("NOTE #$id SAVED LOCALLY.")))
            }
        }
    }

    private fun calculator(rawExpression: String): CommandOutcome {
        if (rawExpression.isBlank()) {
            return CommandOutcome(
                listOf(
                    accent("OPENING CALCULATOR..."),
                    dim("Or evaluate inline: calculator 12 * (3 + 4)")
                ),
                action = TerminalAction.OpenCalculator
            )
        }

        return when (val result = ExpressionEvaluator.evaluate(rawExpression)) {
            is ExpressionEvaluator.Result.Value -> CommandOutcome(
                listOf(
                    normal("  ${rawExpression.trim()}"),
                    success("= ${ExpressionEvaluator.format(result.value)}")
                )
            )
            is ExpressionEvaluator.Result.Failure -> CommandOutcome(
                listOf(
                    error("CALCULATION ERROR."),
                    dim(result.reason),
                    dim("Usage: calculator [expression]")
                )
            )
        }
    }

    private fun openEverydayApp(label: String, opened: Boolean): CommandOutcome =
        if (opened) {
            CommandOutcome(listOf(accent("OPENING $label...")))
        } else {
            CommandOutcome(
                listOf(
                    error("$label UNAVAILABLE."),
                    dim("No installed application responds to this intent on your device.")
                )
            )
        }

    // ---------------------------------------------------------------- appearance

    private suspend fun theme(args: List<String>, settings: AppSettings): CommandOutcome {
        val requested = args.firstOrNull()
        if (requested == null) {
            val lines = mutableListOf(banner("THEMES"), dim(""))
            ThemeId.entries.forEach { theme ->
                val marker = if (theme == settings.theme) "*" else " "
                lines += normal("  $marker ${theme.key.padEnd(10)}")
            }
            lines += dim("")
            lines += dim("Usage: theme <${ThemeId.keys().joinToString("|")}>")
            return CommandOutcome(lines)
        }

        val theme = ThemeId.entries.firstOrNull { it.key.equals(requested, true) }
            ?: return CommandOutcome(
                listOf(
                    error("UNKNOWN THEME: $requested"),
                    dim("Available: ${ThemeId.keys().joinToString(", ")}")
                )
            )

        settingsRepository.setTheme(theme)
        return CommandOutcome(listOf(success("THEME SET TO ${theme.key.uppercase(Locale.US)}.")))
    }

    private suspend fun font(args: List<String>, settings: AppSettings): CommandOutcome {
        val requested = args.firstOrNull()
        if (requested == null) {
            return CommandOutcome(
                listOf(
                    banner("FONT"),
                    dim(""),
                    normal("  CURRENT : ${settings.font.key}"),
                    dim("  Usage   : font jetbrains | font mono")
                )
            )
        }
        val choice = FontChoice.fromKey(requested)
        if (!requested.equals("jetbrains", true) && !requested.equals("mono", true) &&
            !requested.equals("monospace", true)
        ) {
            return usage("font", "Unknown font '$requested'. Use jetbrains or mono.")
        }
        settingsRepository.setFont(choice)
        return CommandOutcome(listOf(success("FONT SET TO ${choice.key.uppercase(Locale.US)}.")))
    }

    private suspend fun fontSize(args: List<String>, settings: AppSettings): CommandOutcome {
        val requested = args.firstOrNull()?.toIntOrNull()
            ?: return usage("fontsize", "Current size is ${settings.fontSizeSp}sp. Provide a value ${AppSettings.MIN_FONT_SIZE}-${AppSettings.MAX_FONT_SIZE}.")
        if (requested !in AppSettings.MIN_FONT_SIZE..AppSettings.MAX_FONT_SIZE) {
            return usage("fontsize", "Size must be between ${AppSettings.MIN_FONT_SIZE} and ${AppSettings.MAX_FONT_SIZE}.")
        }
        settingsRepository.setFontSize(requested)
        return CommandOutcome(listOf(success("FONT SIZE SET TO ${requested}SP.")))
    }

    private suspend fun opacity(args: List<String>, settings: AppSettings): CommandOutcome {
        val requested = args.firstOrNull()?.removeSuffix("%")?.toIntOrNull()
            ?: return usage("opacity", "Current background opacity is ${(settings.opacity * 100).toInt()}%.")
        if (requested !in 40..100) return usage("opacity", "Opacity must be between 40 and 100.")
        settingsRepository.setOpacity(requested / 100f)
        return CommandOutcome(listOf(success("BACKGROUND OPACITY SET TO $requested%.")))
    }

    private suspend fun cursor(args: List<String>, settings: AppSettings): CommandOutcome {
        val requested = args.firstOrNull()
        if (requested == null) {
            return CommandOutcome(
                listOf(
                    banner("CURSOR"),
                    dim(""),
                    normal("  CURRENT : ${settings.cursor.key}"),
                    dim("  Usage   : cursor block | bar | underline")
                )
            )
        }
        val style = CursorStyle.entries.firstOrNull { it.key.equals(requested, true) }
            ?: return usage("cursor", "Unknown style '$requested'.")
        settingsRepository.setCursor(style)
        return CommandOutcome(listOf(success("CURSOR SET TO ${style.key.uppercase(Locale.US)}.")))
    }

    private suspend fun animations(args: List<String>, settings: AppSettings): CommandOutcome {
        val toggled = when (args.firstOrNull()?.lowercase(Locale.US)) {
            null -> !settings.animations
            "on", "enable", "true", "1" -> true
            "off", "disable", "false", "0" -> false
            else -> return usage("animations", "Use on or off.")
        }
        settingsRepository.setAnimations(toggled)
        return CommandOutcome(
            listOf(success("ANIMATIONS ${if (toggled) "ENABLED" else "DISABLED"}."))
        )
    }

    // ---------------------------------------------------------------- utility

    private fun whoAmI(settings: AppSettings): CommandOutcome = CommandOutcome(
        listOf(
            normal(settings.username),
            dim("uid=${android.os.Process.myUid()}  host=${settings.hostname}"),
            dim("prompt: ${settings.prompt}")
        )
    )

    private suspend fun hostname(args: List<String>, settings: AppSettings): CommandOutcome {
        val requested = args.firstOrNull()
            ?: return CommandOutcome(listOf(normal(settings.hostname)))
        val sanitized = requested.trim().replace(Regex("[^A-Za-z0-9._-]"), "").take(16)
        if (sanitized.isBlank()) return usage("hostname", "Use letters, digits, dots, dashes or underscores.")
        settingsRepository.setHostname(sanitized)
        return CommandOutcome(listOf(success("HOSTNAME SET TO ${sanitized.uppercase(Locale.US)}.")))
    }

    private suspend fun history(args: List<String>, settings: AppSettings): CommandOutcome {
        val arg = args.firstOrNull()
        if (arg.equals("clear", true)) {
            historyRepository.clear()
            return CommandOutcome(listOf(success("COMMAND HISTORY CLEARED.")))
        }

        if (arg != null && arg.toIntOrNull() == null) {
            return usage("history", "Use a number or 'clear'.")
        }

        val limit = arg?.toIntOrNull()?.coerceIn(1, 100) ?: 30
        val entries = historyRepository.recent(limit)
        if (entries.isEmpty()) {
            return CommandOutcome(listOf(dim("HISTORY IS EMPTY.")))
        }

        val lines = mutableListOf(banner("COMMAND HISTORY (LAST ${entries.size})"), dim(""))
        entries.reversed().forEachIndexed { index, command ->
            lines += normal("  ${(index + 1).toString().padStart(3)}  $command")
        }
        lines += dim("")
        lines += dim("history clear  |  history <n>")
        return CommandOutcome(lines)
    }

    private fun echo(parsed: CommandParser.ParsedCommand): CommandOutcome {
        val text = parsed.rawArgs
        return CommandOutcome(listOf(normal(text)))
    }

    private fun version(): CommandOutcome = CommandOutcome(
        listOf(
            banner("TTY ${BuildConfig.VERSION_NAME}"),
            dim(""),
            normal(Formatters.row("BUILD", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")),
            normal(Formatters.row("TYPE", BuildConfig.BUILD_TYPE.uppercase(Locale.US))),
            normal(Formatters.row("PACKAGE", BuildConfig.APPLICATION_ID)),
            normal(Formatters.row("KOTLIN", KotlinVersion.CURRENT.toString())),
            normal(Formatters.row("ANDROID", "${android.os.Build.VERSION.RELEASE} / SDK ${android.os.Build.VERSION.SDK_INT}")),
            dim(""),
            dim("Offline by design. No telemetry, no analytics, no account.")
        )
    )

    private suspend fun ping(args: List<String>): CommandOutcome = withContext(Dispatchers.IO) {
        val target = args.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: "1.1.1.1"
        if (!Regex(HOST_PATTERN).matches(target)) {
            return@withContext usage("ping", "Invalid host name.")
        }

        val host = target.substringBefore(':')
        val port = target.substringAfter(':', "443").toIntOrNull()?.takeIf { it in 1..65535 } ?: 443

        val lines = mutableListOf(
            banner("PING $target"),
            dim("TCP connect probe to port $port. ICMP needs raw sockets."),
            dim("")
        )

        val address = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            lines += error("UNKNOWN HOST: $host")
            lines += dim("DNS resolution failed.")
            return@withContext CommandOutcome(lines)
        }

        lines += normal("RESOLVED  : ${address.hostAddress ?: host}")

        val latencies = mutableListOf<Long>()
        var failure: String? = null
        repeat(3) { attempt ->
            try {
                val socket = Socket()
                try {
                    val started = System.nanoTime()
                    socket.connect(InetSocketAddress(address, port), TIMEOUT_MS)
                    val elapsed = (System.nanoTime() - started) / 1_000_000
                    latencies += elapsed
                    lines += normal("  seq=$attempt  time=${elapsed}ms")
                } finally {
                    runCatching { socket.close() }
                }
            } catch (e: Exception) {
                failure = e.javaClass.simpleName
            }
        }

        if (latencies.isEmpty()) {
            lines += dim("")
            lines += error("HOST UNREACHABLE.")
            lines += dim("Reason: ${failure ?: "timeout"}.")
            lines += dim("The network may block this port, or you are offline.")
        } else {
            lines += dim("")
            lines += success(
                "3 probes, ${latencies.size} received, avg ${latencies.average().toInt()}ms, " +
                    "min ${latencies.min()}ms, max ${latencies.max()}ms"
            )
        }
        CommandOutcome(lines)
    }

    private fun reboot(): CommandOutcome = CommandOutcome(
        listOf(
            warning("REBOOT UNAVAILABLE."),
            dim(""),
            normal("Rebooting an Android device requires privileged system access."),
            normal("Ordinary applications cannot restart the phone, and TTY will not"),
            normal("pretend to do so."),
            dim(""),
            dim("If you need a restart, hold the power button and choose Restart.")
        )
    )

    // ---------------------------------------------------------------- helpers

    private suspend fun resolveSingle(query: String): AppInfo? {
        val apps = appRepository.installedApps()
        val aliases = aliasRepository.snapshot()
        return AppResolver.match(query, apps, aliases).firstOrNull()
    }

    private fun applicationNotFound(query: String): CommandOutcome = CommandOutcome(
        listOf(
            error("APPLICATION NOT FOUND."),
            dim("No installed application matches \"$query\"."),
            dim("Check the list with 'apps' or search with 'search <query>'.")
        )
    )

    private fun usage(command: String, message: String): CommandOutcome {
        val spec = CommandCatalog.find(command)
        return CommandOutcome(
            listOf(
                error("ERROR: $message"),
                dim(""),
                normal("Usage: ${spec?.usage ?: command}")
            )
        )
    }

    private fun unknownCommand(parsed: CommandParser.ParsedCommand): CommandOutcome {
        val lines = mutableListOf(error("COMMAND NOT FOUND: ${parsed.name}"), dim(""))
        val suggestions = SuggestionEngine.suggestCommandLines(parsed)
        if (suggestions.isNotEmpty()) {
            lines += dim("Did you mean:")
            suggestions.forEach { lines += accent("  $it") }
        } else {
            lines += dim("Type 'help' to see available commands.")
        }
        return CommandOutcome(lines)
    }

    private fun formatPlug(plugType: String, charging: Boolean): String =
        if (charging && plugType != "BATTERY") "$plugType (CHARGING)" else plugType

    private suspend fun currentHostname(): String = settingsRepository.snapshot().hostname

    private suspend fun currentUsername(): String = settingsRepository.snapshot().username

    private fun LocalDateTime.formatSafe(pattern: String): String = Formatters.dateTime(
        this.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        pattern
    )

    private companion object {
        const val MAX_LISTED_APPS = 40
        const val TIMEOUT_MS = 3000
        const val ALIAS_PATTERN = "^[a-z0-9_-]{1,16}$"
        const val HOST_PATTERN = "^[A-Za-z0-9._:\\-]{1,253}$"
    }
}
