package com.macrophone.gaming.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.macrophone.gaming.R
import com.macrophone.gaming.data.model.MacroState
import com.macrophone.gaming.service.NotificationActionReceiver
import com.macrophone.gaming.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "macro_gaming_channel"
    const val NOTIFICATION_ID = 1001

    const val ACTION_PLAY = "com.macrophone.gaming.ACTION_PLAY"
    const val ACTION_STOP = "com.macrophone.gaming.ACTION_STOP"
    const val ACTION_CLOSE = "com.macrophone.gaming.ACTION_CLOSE"

    fun createNotificationChannel(context: Context) {
        val name = context.getString(R.string.notif_channel_name)
        val descriptionText = context.getString(R.string.notif_channel_desc)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(false)
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        context: Context,
        macroState: MacroState,
        macroName: String?
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_PLAY
        }
        val playPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CLOSE
        }
        val closePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (macroState) {
            MacroState.PLAYING -> context.getString(R.string.notif_title_playing, macroName ?: "Combo")
            MacroState.RECORDING -> context.getString(R.string.notif_title_recording)
            else -> context.getString(R.string.notif_title_ready)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_macro_tile)
            .setContentTitle(title)
            .setContentText("Kích hoạt combo nhanh cho game")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (macroState == MacroState.PLAYING) {
            builder.addAction(R.drawable.ic_stop, context.getString(R.string.notif_action_stop), stopPendingIntent)
        } else if (macroState != MacroState.RECORDING) {
            builder.addAction(R.drawable.ic_play, context.getString(R.string.notif_action_play), playPendingIntent)
        }

        builder.addAction(R.drawable.ic_close, context.getString(R.string.notif_action_close), closePendingIntent)

        return builder.build()
    }
}
