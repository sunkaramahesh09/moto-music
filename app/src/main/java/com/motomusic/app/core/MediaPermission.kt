package com.motomusic.app.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * The single place that knows which storage permission a given Android version needs.
 *
 * Android 13 replaced the broad `READ_EXTERNAL_STORAGE` with the narrow `READ_MEDIA_AUDIO`,
 * which is all Moto Music ever asks for. No other runtime permission is requested except the
 * Android 13+ notification permission, which the platform requires before a media notification
 * can be shown at all.
 */
object MediaPermission {

    val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            @Suppress("DEPRECATION")
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val notificationPermission: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    fun hasAudioAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationAccess(context: Context): Boolean {
        val permission = notificationPermission ?: return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
