package com.tty.launcher.services.system

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class VolumeInfo(val label: String, val percent: Int, val muted: Boolean)

/**
 * Media volume can be changed by ordinary applications. Ringer and notification volume are only
 * readable here; changing them needs a policy-owner app, which is intentionally out of scope.
 */
@Singleton
class VolumeController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager? by lazy {
        runCatching { context.getSystemService(AudioManager::class.java) }.getOrNull()
    }

    fun mediaPercent(): Int? = percentFor(AudioManager.STREAM_MUSIC)

    fun levels(): List<VolumeInfo> {
        val streams = listOf(
            AudioManager.STREAM_MUSIC to "MEDIA",
            AudioManager.STREAM_RING to "RINGER",
            AudioManager.STREAM_ALARM to "ALARM",
            AudioManager.STREAM_NOTIFICATION to "NOTIFICATION"
        )
        return streams.mapNotNull { (stream, label) ->
            val percent = percentFor(stream) ?: return@mapNotNull null
            val muted = runCatching { audioManager?.isStreamMute(stream) == true }.getOrDefault(false)
            VolumeInfo(label, percent, muted)
        }
    }

    fun setMediaPercent(percent: Int): Result<Int> {
        val manager = audioManager ?: return Result.failure(IllegalStateException("Audio service unavailable."))
        return try {
            val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = ((percent.coerceIn(0, 100) / 100f) * max).toInt().coerceIn(0, max)
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            Result.success((target * 100) / max.coerceAtLeast(1))
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun percentFor(stream: Int): Int? {
        val manager = audioManager ?: return null
        return try {
            val max = manager.getStreamMaxVolume(stream)
            if (max <= 0) return null
            (manager.getStreamVolume(stream) * 100) / max
        } catch (e: Exception) {
            null
        }
    }
}

/** Screen brightness: writing to Settings.System requires the user-granted WRITE_SETTINGS appop. */
@Singleton
class BrightnessController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun canWrite(): Boolean = try {
        Settings.System.canWrite(context)
    } catch (e: Exception) {
        false
    }

    fun currentPercent(): Int? = try {
        val value = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        ((value * 100) / 255).coerceIn(0, 100)
    } catch (e: Exception) {
        null
    }

    fun setPercent(percent: Int): Boolean {
        if (!canWrite()) return false
        return try {
            val target = ((percent.coerceIn(1, 100) / 100f) * 255).toInt().coerceIn(1, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, target)
        } catch (e: Exception) {
            false
        }
    }
}
