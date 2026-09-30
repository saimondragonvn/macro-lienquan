package com.macrophone.gaming.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.macrophone.gaming.util.NotificationHelper

/**
 * Lắng nghe và xử lý sự kiện khi người dùng nhấn nút trên thanh Notification
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            NotificationHelper.ACTION_CLOSE -> {
                TurboOverlayService.stop(context)
            }
            NotificationHelper.ACTION_OPEN_MENU -> {
                TurboOverlayService.openMenu(context)
            }
            NotificationHelper.ACTION_TOGGLE_VISIBILITY -> {
                TurboOverlayService.toggleVisibility(context)
            }
            NotificationHelper.ACTION_START_RECORD -> {
                TurboOverlayService.startRecord(context)
            }
        }
    }
}
