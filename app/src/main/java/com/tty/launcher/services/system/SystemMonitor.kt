package com.tty.launcher.services.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import androidx.core.content.ContextCompat
import com.tty.launcher.domain.models.SystemSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lifecycle-aware system state stream. Values are pushed on real system events (battery, network)
 * with a slow heartbeat for uptime; there is no busy polling loop. Collection stops as soon as the
 * consumer leaves composition or the app is backgrounded.
 */
@Singleton
class SystemMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceInfo: DeviceInfoProvider
) {

    fun stream(heartbeatMillis: Long = 30_000L): Flow<SystemSnapshot> = callbackFlow {
        fun push() {
            runCatching { trySend(deviceInfo.snapshot()) }
        }

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) = push()
        }
        runCatching {
            ContextCompat.registerReceiver(
                context,
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = push()
            override fun onLost(network: Network) = push()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: android.net.NetworkCapabilities) = push()
        }
        val registered = runCatching {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        }.isSuccess

        push()

        val heartbeat = launch {
            while (isActive) {
                delay(heartbeatMillis)
                push()
            }
        }

        awaitClose {
            heartbeat.cancel()
            runCatching { context.unregisterReceiver(batteryReceiver) }
            if (registered) runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        }
    }.conflate()
}
