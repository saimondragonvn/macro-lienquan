package com.macrophone.gaming.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.macrophone.gaming.domain.MacroManager
import com.macrophone.gaming.util.NotificationHelper

/**
 * Lắng nghe và xử lý sự kiện khi người dùng nhấn nút trên thanh Notification
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val macroManager = MacroManager.getInstance(context)

        when (intent?.action) {
            NotificationHelper.ACTION_PLAY -> {
                macroManager.startPlayback()
            }
            NotificationHelper.ACTION_STOP -> {
                macroManager.stopPlayback()
            }
            NotificationHelper.ACTION_CLOSE -> {
                macroManager.stopPlayback()
                val stopServiceIntent = Intent(context, FloatingWidgetService::class.java).apply {
                    action = FloatingWidgetService.ACTION_STOP_SERVICE
                }
                context.startService(stopServiceIntent)
            }
        }
    }
}
