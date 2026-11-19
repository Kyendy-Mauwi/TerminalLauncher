package com.tty.launcher.services.system

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.tty.launcher.domain.models.BatterySnapshot
import com.tty.launcher.domain.models.DeviceSnapshot
import com.tty.launcher.domain.models.MemorySnapshot
import com.tty.launcher.domain.models.NetworkSnapshot
import com.tty.launcher.domain.models.StorageSnapshot
import com.tty.launcher.domain.models.SystemSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads real device state through public Android APIs. Every call is defensive: a missing
 * service, a revoked permission or a restricted OEM build yields a clearly marked unknown value
 * instead of an exception.
 */
@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val activityManager: ActivityManager? by lazy {
        runCatching { context.getSystemService(ActivityManager::class.java) }.getOrNull()
    }
    private val connectivityManager: ConnectivityManager? by lazy {
        runCatching { context.getSystemService(ConnectivityManager::class.java) }.getOrNull()
    }

    fun snapshot(): SystemSnapshot = SystemSnapshot(
        battery = battery(),
        network = network(),
        storage = storage(),
        memory = memory(),
        uptimeMillis = uptimeMillis(),
        device = device()
    )

    fun battery(): BatterySnapshot {
        return try {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent == null) {
                BatterySnapshot(null, false, "UNKNOWN", "UNKNOWN", null, "UNKNOWN")
            } else {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percent = if (level >= 0 && scale > 0) (level * 100) / scale else null
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                BatterySnapshot(
                    percent = percent,
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0,
                    status = batteryStatusLabel(status),
                    plugType = plugLabel(plugged),
                    temperatureC = if (temperature == Int.MIN_VALUE) null else temperature / 10f,
                    health = healthLabel(health)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Battery read failed", e)
            BatterySnapshot(null, false, "UNKNOWN", "UNKNOWN", null, "UNKNOWN")
        }
    }

    fun storage(): StorageSnapshot {
        return try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            StorageSnapshot(availableBytes = stat.availableBytes, totalBytes = stat.totalBytes)
        } catch (e: Exception) {
            Log.w(TAG, "Storage read failed", e)
            StorageSnapshot(0L, 0L)
        }
    }

    fun memory(): MemorySnapshot {
        return try {
            val info = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(info)
            MemorySnapshot(
                availableBytes = info.availMem,
                totalBytes = info.totalMem,
                lowMemory = info.lowMemory,
                thresholdBytes = info.threshold
            )
        } catch (e: Exception) {
            Log.w(TAG, "Memory read failed", e)
            MemorySnapshot(0L, 0L, false, 0L)
        }
    }

    fun device(): DeviceSnapshot = DeviceSnapshot(
        manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "UNKNOWN" },
        model = Build.MODEL.orEmpty().ifBlank { "UNKNOWN" },
        androidRelease = Build.VERSION.RELEASE.orEmpty().ifBlank { "UNKNOWN" },
        sdkInt = Build.VERSION.SDK_INT,
        device = Build.DEVICE.orEmpty().ifBlank { "UNKNOWN" },
        product = Build.PRODUCT.orEmpty().ifBlank { "UNKNOWN" }
    )

    fun network(): NetworkSnapshot {
        return try {
            val cm = connectivityManager
                ?: return NetworkSnapshot(false, "UNKNOWN", false, false, false, null, null)
            val active = cm.activeNetwork ?: return NetworkSnapshot(false, "OFFLINE", false, false, false, null, null)
            val capabilities = cm.getNetworkCapabilities(active)
                ?: return NetworkSnapshot(false, "OFFLINE", false, false, false, null, null)
            val wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val ethernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val vpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            val transport = when {
                vpn -> "VPN"
                wifi -> "WI-FI"
                cellular -> "CELLULAR"
                ethernet -> "ETHERNET"
                else -> "CONNECTED"
            }
            NetworkSnapshot(
                connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                transport = transport,
                isWifi = wifi,
                isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                downstreamKbps = capabilities.linkDownstreamBandwidthKbps,
                upstreamKbps = capabilities.linkUpstreamBandwidthKbps
            )
        } catch (e: Exception) {
            Log.w(TAG, "Network read failed", e)
            NetworkSnapshot(false, "UNKNOWN", false, false, false, null, null)
        }
    }

    fun uptimeMillis(): Long = SystemClock.elapsedRealtime()

    fun wallClockMillis(): Long = System.currentTimeMillis()

    /** Bluetooth state is only readable with BLUETOOTH_CONNECT on Android 12+. */
    fun bluetoothState(): String {
        return try {
            val manager = context.getSystemService(BluetoothManager::class.java)
            val adapter: BluetoothAdapter = manager?.adapter ?: return "UNSUPPORTED"
            if (adapter.isEnabled) "ON" else "OFF"
        } catch (e: SecurityException) {
            "PERMISSION REQUIRED"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    @Suppress("DEPRECATION")
    fun wifiEnabled(): Boolean? {
        return try {
            context.getSystemService(Context.WIFI_SERVICE)?.let { service ->
                (service as? android.net.wifi.WifiManager)?.isWifiEnabled
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 0-100 screen brightness, or null when unreadable. */
    fun screenBrightnessPercent(): Int? {
        return try {
            val value = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            ((value * 100) / 255).coerceIn(0, 100)
        } catch (e: Settings.SettingNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun batteryStatusLabel(status: Int): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
        BatteryManager.BATTERY_STATUS_FULL -> "FULL"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT CHARGING"
        else -> "UNKNOWN"
    }

    private fun plugLabel(plugged: Int): String = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
        0 -> "BATTERY"
        else -> "OTHER"
    }

    private fun healthLabel(health: Int): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
        BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER VOLTAGE"
        BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
        else -> "UNKNOWN"
    }

    private companion object {
        const val TAG = "DeviceInfoProvider"
    }
}
