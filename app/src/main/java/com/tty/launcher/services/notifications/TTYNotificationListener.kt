package com.tty.launcher.services.notifications

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.tty.launcher.domain.models.NotificationRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads notification metadata only while the user has explicitly granted notification access.
 * Nothing is persisted, logged or transmitted: the cache is in-memory and cleared when the
 * listener disconnects.
 */
class TTYNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationStore.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        try {
            val active = activeNotifications ?: return
            val packageManager = packageManager
            val labelCache = HashMap<String, String>()

            val records = active
                .asSequence()
                .filter { sbn -> sbn.notification != null && !sbn.isOngoing }
                .sortedByDescending { it.postTime }
                .take(MAX_NOTIFICATIONS)
                .mapNotNull { sbn ->
                    runCatching {
                        val extras = sbn.notification.extras
                        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                        val text = (
                            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                            )?.toString()?.trim().orEmpty()
                        if (title.isBlank() && text.isBlank()) return@runCatching null

                        val appLabel = labelCache.getOrPut(sbn.packageName) {
                            runCatching {
                                val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                                packageManager.getApplicationLabel(appInfo).toString()
                            }.getOrDefault(sbn.packageName.substringAfterLast('.'))
                        }

                        NotificationRecord(
                            packageName = sbn.packageName,
                            appLabel = appLabel,
                            title = title,
                            text = text,
                            postedAt = sbn.postTime
                        )
                    }.getOrNull()
                }
                .toList()

            NotificationStore.update(records)
        } catch (e: Exception) {
            Log.w(TAG, "Notification refresh failed", e)
        }
    }

    private companion object {
        const val TAG = "TTYNotificationListener"
        const val MAX_NOTIFICATIONS = 25
    }
}

/** In-memory, process-local notification cache. */
object NotificationStore {
    private val _notifications = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val notifications: StateFlow<List<NotificationRecord>> = _notifications.asStateFlow()

    fun update(records: List<NotificationRecord>) {
        _notifications.value = records
    }

    fun clear() {
        _notifications.value = emptyList()
    }
}

object NotificationAccess {
    fun isGranted(context: Context): Boolean = try {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    } catch (e: Exception) {
        false
    }
}
