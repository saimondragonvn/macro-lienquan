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

    const val ACTION_TOGGLE_VISIBILITY = "com.macrophone.gaming.ACTION_TOGGLE_VISIBILITY"
    const val ACTION_START_RECORD = "com.macrophone.gaming.ACTION_START_RECORD"
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
        isButtonsHidden: Boolean = false,
        isRecording: Boolean = false,
        activeComboName: String? = null
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

        // 1. Nút Ẩn/Hiện Nút Nổi
        val toggleIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TOGGLE_VISIBILITY
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Nút Bắt đầu ghi Combo
        val recordIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_START_RECORD
        }
        val recordPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            recordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Nút Tắt Game Turbo hoàn toàn
        val closeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CLOSE
        }
        val closePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isRecording -> "🔴 Game Turbo: ĐANG GHI COMBO..."
            activeComboName != null -> "⚡ Đang xả combo: [$activeComboName]"
            isButtonsHidden -> "⚡ Game Turbo: ĐÃ ẨN NÚT (Chạm để hiện)"
            else -> "⚡ Game Turbo Pro (120Hz Fast Combo)"
        }

        val content = when {
            isRecording -> "Vào game thao tác bình thường, bấm Xong trên đỉnh màn hình để lưu"
            isButtonsHidden -> "Bấm [👁️ HIỆN NÚT] bên dưới để mở lại nút combo trên màn hình"
            else -> "Bấm [👁️ ẨN NÚT] để giấu nút • [🔴 GHI COMBO] để tạo combo mới"
        }

        val toggleLabel = if (isButtonsHidden) "👁️ HIỆN NÚT" else "👁️ ẨN NÚT"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_macro_tile)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_play, toggleLabel, togglePendingIntent)
            .addAction(R.drawable.ic_speed, "🔴 Ghi Combo", recordPendingIntent)
            .addAction(R.drawable.ic_close, "✕ Tắt Turbo", closePendingIntent)
            .build()
    }
}
