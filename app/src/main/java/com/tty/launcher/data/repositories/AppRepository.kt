package com.tty.launcher.data.repositories

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.tty.launcher.domain.models.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class Failure(val reason: String) : LaunchResult
}

/**
 * Discovers and launches applications. Visibility is limited by the `<queries>` declaration in the
 * manifest, which is the Google Play compliant way for a launcher to see launchable apps.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recentAppsRepository: RecentAppsRepository
) {

    private val mutex = Mutex()

    @Volatile
    private var cachedApps: List<AppInfo> = emptyList()

    @Volatile
    private var loadedAt: Long = 0L

    suspend fun installedApps(forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val stale = System.currentTimeMillis() - loadedAt > CACHE_TTL_MS
            if (!forceRefresh && cachedApps.isNotEmpty() && !stale) {
                cachedApps
            } else {
                val loaded = load()
                cachedApps = loaded
                loadedAt = System.currentTimeMillis()
                loaded
            }
        }
    }

    fun invalidate() {
        loadedAt = 0L
    }

    suspend fun launch(app: AppInfo): LaunchResult = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent: Intent? = try {
            val direct = if (app.activityName != null) {
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(app.packageName, app.activityName)
            } else {
                null
            }
            direct ?: packageManager.getLaunchIntentForPackage(app.packageName)
        } catch (e: Exception) {
            null
        }

        if (intent == null) {
            return@withContext LaunchResult.Failure(
                "No launchable activity. The app may be disabled or uninstalled."
            )
        }

        return@withContext try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
            runCatching { recentAppsRepository.record(app) }
            LaunchResult.Success
        } catch (e: ActivityNotFoundException) {
            LaunchResult.Failure("Android could not find a launch activity for ${app.label}.")
        } catch (e: SecurityException) {
            Log.w(TAG, "Launch blocked", e)
            LaunchResult.Failure("Android blocked launching ${app.label} from this app.")
        } catch (e: Exception) {
            Log.w(TAG, "Launch failed", e)
            LaunchResult.Failure("Launch failed: ${e.javaClass.simpleName}.")
        }
    }

    private fun load(): List<AppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            packageManager.queryIntentActivities(launcherIntent, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Package query failed", e)
            emptyList()
        }

        return resolved.mapNotNull { info ->
            try {
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(packageManager)?.toString()?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: packageName.substringAfterLast('.')
                val flags = info.activityInfo?.applicationInfo?.flags ?: 0
                AppInfo(
                    packageName = packageName,
                    activityName = info.activityInfo?.name,
                    label = label,
                    isSystemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            } catch (e: Exception) {
                null
            }
        }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.US) }
    }

    fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val TAG = "AppRepository"
        const val CACHE_TTL_MS = 60_000L
    }
}
