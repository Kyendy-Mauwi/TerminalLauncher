package com.tty.launcher.domain.models

/** A launchable application discovered through the package manager. */
data class AppInfo(
    val packageName: String,
    val activityName: String?,
    val label: String,
    val isSystemApp: Boolean = false
) {
    /** Stable display key, e.g. "chrome". */
    val key: String get() = label.lowercase()
}

/** Battery snapshot read from the sticky battery broadcast. */
data class BatterySnapshot(
    val percent: Int?,
    val isCharging: Boolean,
    val status: String,
    val plugType: String,
    val temperatureC: Float?,
    val health: String
)

data class StorageSnapshot(
    val availableBytes: Long,
    val totalBytes: Long
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usedPercent: Int
        get() = if (totalBytes <= 0L) 0 else ((usedBytes * 100) / totalBytes).toInt()
}

data class MemorySnapshot(
    val availableBytes: Long,
    val totalBytes: Long,
    val lowMemory: Boolean,
    val thresholdBytes: Long
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usedPercent: Int
        get() = if (totalBytes <= 0L) 0 else ((usedBytes * 100) / totalBytes).toInt()
}

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val device: String,
    val product: String
)

data class NetworkSnapshot(
    val connected: Boolean,
    val transport: String,
    val isWifi: Boolean,
    val isMetered: Boolean,
    val isValidated: Boolean,
    val downstreamKbps: Int?,
    val upstreamKbps: Int?
)

/** Aggregated live device state used by the status panel and sysinfo command. */
data class SystemSnapshot(
    val battery: BatterySnapshot,
    val network: NetworkSnapshot,
    val storage: StorageSnapshot,
    val memory: MemorySnapshot,
    val uptimeMillis: Long,
    val device: DeviceSnapshot
)

/** A notification surfaced through NotificationListenerService (never persisted). */
data class NotificationRecord(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long
)

data class RecentApp(
    val packageName: String,
    val label: String,
    val lastUsedAt: Long,
    val launchCount: Int
)

data class FavoriteApp(
    val packageName: String,
    val label: String,
    val addedAt: Long
)

data class CommandHistoryEntry(
    val id: Long,
    val command: String,
    val executedAt: Long
)

data class Note(
    val id: Long,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
