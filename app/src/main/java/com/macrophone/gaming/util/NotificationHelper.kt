package com.macrophone.gaming.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.macrophone.gaming.R
import com.macrophone.gaming.service.NotificationActionReceiver

/**
 * Tiện ích thanh thông báo thông minh (Circle to Search Style):
 * - Vuốt thanh thông báo của điện thoại xuống và chạm vào thông báo để MỞ / ĐÓNG ngay menu Game Turbo giữa màn hình.
 * - Hiển thị tên game / hồ sơ đang chọn và số lượng nút combo tương ứng.
 */
object NotificationHelper {

    const val NOTIFICATION_ID = 1001
    const val CHANNEL_ID = "game_turbo_channel"
    private const val CHANNEL_NAME = "Game Turbo Pro Controller"

    const val ACTION_OPEN_MENU = "com.macrophone.gaming.OPEN_MENU"
    const val ACTION_START_RECORD = "com.macrophone.gaming.START_RECORD"
    const val ACTION_TOGGLE_VISIBILITY = "com.macrophone.gaming.TOGGLE_VISIBILITY"
    const val ACTION_CLOSE = "com.macrophone.gaming.CLOSE"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            val descriptionText = "Điều khiển Game Turbo 120Hz, ghi combo và chuyển đổi cấu hình game"
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
        isMenuOpen: Boolean = false,
        activeGameName: String? = null
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

        val gamePrefix = if (!activeGameName.isNullOrEmpty()) "[$activeGameName]" else "Game Turbo"
        val title = when {
            isRecording -> "🔴 $gamePrefix: ĐANG GHI COMBO..."
            isMenuOpen -> "⚡ $gamePrefix: MENU ĐANG MỞ"
            activeComboName != null -> "⚡ Đang xả combo: [$activeComboName]"
            isButtonsHidden -> "⚡ $gamePrefix: ĐÃ ẨN NÚT (Chạm mở menu)"
            else -> "⚡ $gamePrefix: SẴN SÀNG"
        }

        val content = when {
            isRecording -> "Vào game thao tác bình thường, bấm Xong trên đỉnh màn hình để lưu"
            isMenuOpen -> "Chạm thông báo hoặc bấm [✕ ĐÓNG MENU] để quay lại game"
            isButtonsHidden -> "Bấm [⚡ MỞ MENU] hoặc [👁️ HIỆN NÚT] để thao tác"
            else -> "Game: ${activeGameName ?: "Liên Quân Mobile"} • Liquid Glass by GiaHoai"
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
