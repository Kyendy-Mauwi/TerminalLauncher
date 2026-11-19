package com.tty.launcher.services.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tty.launcher.MainActivity
import com.tty.launcher.R

/** Quick Settings entry point for users who have not made TTY their default launcher. */
class TTYTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        try {
            qsTile?.apply {
                label = "TTY"
                state = Tile.STATE_INACTIVE
                icon = Icon.createWithResource(this@TTYTileService, R.drawable.ic_terminal)
                updateTile()
            }
        } catch (e: Exception) {
            // Tile may be null while the system is binding the service.
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        if (isLocked) {
            unlockAndRun { collapseAndLaunch(intent) }
        } else {
            collapseAndLaunch(intent)
        }
    }

    /**
     * Android 14 removed the Intent overload, so a PendingIntent is used there. The Intent call
     * remains the only collapse-and-launch API on Android 10-13, which is why it is kept behind
     * an explicit SDK check.
     */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun collapseAndLaunch(intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            startActivity(intent)
        }
    }
}
