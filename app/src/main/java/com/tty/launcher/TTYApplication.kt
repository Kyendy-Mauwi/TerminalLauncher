package com.tty.launcher

import android.app.Application
import com.tty.launcher.services.system.MaintenanceWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TTYApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Keeps the local database tidy without any background service or polling loop.
        MaintenanceWorker.schedule(this)
    }
}
