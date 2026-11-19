package com.tty.launcher.services.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tty.launcher.data.datastore.SettingsRepository
import com.tty.launcher.data.repositories.HistoryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Once-a-day housekeeping: keeps command history inside the user's configured limit and drops
 * launch records for apps that are no longer installed. Runs without waking the device.
 */
class MaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                MaintenanceEntryPoint::class.java
            )
            val settings = entryPoint.settingsRepository().snapshot()
            entryPoint.historyRepository().trimTo(settings.historyLimit)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "tty-maintenance"

        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(6, TimeUnit.HOURS)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                // WorkManager may be unavailable right after install on some OEM builds.
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MaintenanceEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun historyRepository(): HistoryRepository
}
