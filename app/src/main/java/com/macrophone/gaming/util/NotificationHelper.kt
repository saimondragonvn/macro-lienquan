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

    const val ACTION_OPEN_MENU = "com.macrophone.gaming.ACTION_OPEN_MENU"
    const val ACTION_START_RECORD = "com.macrophone.gaming.ACTION_START_RECORD"
    const val ACTION_TOGGLE_VISIBILITY = "com.macrophone.gaming.ACTION_TOGGLE_VISIBILITY"
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
        activeComboName: String? = null,
        isMenuOpen: Boolean = false
    ): Notification {
        // Chạm vào thông báo sẽ BẬT/TẮT ngay Menu Game Turbo nổi (Chuẩn Circle to Search)!
        val openMenuIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_OPEN_MENU
        }
        val openMenuPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            openMenuIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Phím tắt: [⚡ MỞ MENU] hoặc [✕ ĐÓNG MENU]
        val menuActionTitle = if (isMenuOpen) "✕ ĐÓNG MENU" else "⚡ MỞ MENU"
        val menuActionPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_OPEN_MENU },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Phím tắt: [🔴 Ghi Combo]
        val recordPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_START_RECORD },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Phím tắt: [👁️ Ẩn/Hiện Nút]
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_TOGGLE_VISIBILITY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Phím tắt: [✕ Tắt Turbo]
        val closePendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_CLOSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isRecording -> "🔴 Game Turbo: ĐANG GHI COMBO..."
            isMenuOpen -> "⚡ Game Turbo: MENU ĐANG MỞ"
            activeComboName != null -> "⚡ Đang xả combo: [$activeComboName]"
            isButtonsHidden -> "⚡ Game Turbo: ĐÃ ẨN NÚT (Chạm mở menu)"
            else -> "⚡ Game Turbo (Circle to Search Style)"
        }

        val content = when {
            isRecording -> "Vào game thao tác bình thường, bấm Xong trên đỉnh màn hình để lưu"
            isMenuOpen -> "Chạm thông báo hoặc bấm [✕ ĐÓNG MENU] để quay lại game"
            isButtonsHidden -> "Bấm [⚡ MỞ MENU] hoặc [👁️ HIỆN NÚT] để thao tác"
            else -> "Chạm thông báo hoặc bấm [⚡ MỞ MENU] để cài đặt & ghi combo"
        }

        val toggleLabel = if (isButtonsHidden) "👁️ Hiện Nút" else "🙈 Ẩn Nút"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_macro_tile)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openMenuPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_macro_tile, menuActionTitle, menuActionPendingIntent)
            .addAction(R.drawable.ic_speed, "🔴 Ghi Combo", recordPendingIntent)
            .addAction(R.drawable.ic_play, toggleLabel, togglePendingIntent)
            .addAction(R.drawable.ic_close, "✕ Tắt", closePendingIntent)
            .build()
    }
}
