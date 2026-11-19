package com.tty.launcher.services.system

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SettingsPanel(val key: String) {
    WIFI("wifi"),
    BLUETOOTH("bluetooth"),
    DISPLAY("display"),
    SOUND("sound"),
    BATTERY("battery"),
    STORAGE("storage"),
    APPS("apps"),
    NOTIFICATIONS("notifications"),
    LOCATION("location"),
    HOME("home");

    companion object {
        fun fromKey(key: String?): SettingsPanel? =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) }
    }
}

/**
 * All Android intents the launcher needs. Results are real: each method reports whether a handler
 * existed and the activity actually started, never a fabricated success.
 */
@Singleton
class SystemIntents @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun isDefaultLauncher(): Boolean = try {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        resolved?.activityInfo?.packageName == context.packageName
    } catch (e: Exception) {
        false
    }

    fun openHomeRoleSettings(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                if (start(intent)) return true
            }
        }
        return start(Intent(Settings.ACTION_HOME_SETTINGS)) || start(Intent(Settings.ACTION_SETTINGS))
    }

    fun goToSystemHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(intent)
    }

    fun openPanel(panel: SettingsPanel): Boolean {
        val action = when (panel) {
            SettingsPanel.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SettingsPanel.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SettingsPanel.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SettingsPanel.SOUND -> Settings.ACTION_SOUND_SETTINGS
            SettingsPanel.BATTERY -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            SettingsPanel.STORAGE -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            SettingsPanel.APPS -> Settings.ACTION_APPLICATION_SETTINGS
            SettingsPanel.NOTIFICATIONS -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            SettingsPanel.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            SettingsPanel.HOME -> Settings.ACTION_HOME_SETTINGS
        }
        return start(Intent(action)) || start(Intent(Settings.ACTION_SETTINGS))
    }

    fun openAppNotificationSettings(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        return start(intent) || start(Intent(Settings.ACTION_SETTINGS))
    }

    fun openNotificationAccess(): Boolean =
        start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) || start(Intent(Settings.ACTION_SETTINGS))

    fun openWriteSettings(): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
        return start(intent) || start(Intent(Settings.ACTION_SETTINGS))
    }

    fun openAppDetails(packageName: String): Boolean =
        start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    fun openWallpaperChooser(): Boolean = start(Intent(Intent.ACTION_SET_WALLPAPER))

    fun openCalendar(): Boolean {
        return start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)) ||
            start(Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time")))
    }

    fun openClock(): Boolean = start(Intent(AlarmClock.ACTION_SHOW_ALARMS)) ||
        start(Intent(AlarmClock.ACTION_SET_ALARM))

    fun openCamera(): Boolean = start(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

    fun openFiles(): Boolean = start(
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
    )

    fun openContacts(): Boolean = start(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) ||
        start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS))

    fun openMessages(): Boolean = start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)) ||
        start(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))

    fun openDialer(): Boolean = start(Intent(Intent.ACTION_DIAL)) ||
        start(Intent(Intent.ACTION_VIEW, Uri.parse("tel:")))

    /** Opens a store page for an installed application. */
    fun openStoreDetails(packageName: String): Boolean =
        start(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))) ||
            start(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))

    /** Opens a store search. Silent installation is never attempted. */
    fun openStoreSearch(query: String): Boolean {
        val encoded = Uri.encode(query)
        return start(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded&c=apps"))) ||
            start(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$encoded&c=apps")))
    }

    fun openApp(packageName: String, activityName: String? = null): Boolean {
        val direct = if (activityName != null) {
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(packageName, activityName))
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName)
        } ?: return false
        return start(direct)
    }

    fun start(intent: Intent): Boolean {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            Log.i(TAG, "No activity for ${intent.action}")
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception launching ${intent.action}", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Failed launching ${intent.action}", e)
            false
        }
    }

    private companion object {
        const val TAG = "SystemIntents"
    }
}
