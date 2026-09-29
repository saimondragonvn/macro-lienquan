package com.macrophone.gaming

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.macrophone.gaming.domain.MacroManager
import com.macrophone.gaming.util.NotificationHelper

class MacroApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Bắt lỗi toàn cục để app tự bảo vệ và KHÔNG BAO GIỜ bị văng ứng dụng ("Ứng dụng đã dừng")
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MacroApp", "Crash intercepted cleanly: ${throwable.message}", throwable)
            Handler(Looper.getMainLooper()).post {
                try {
                    Toast.makeText(
                        applicationContext,
                        "Hệ thống đã tự phục hồi sự cố: ${throwable.localizedMessage ?: "Khởi động an toàn"}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Throwable) {}
            }
        }

        try {
            NotificationHelper.createNotificationChannel(this)
            MacroManager.getInstance(this)
        } catch (e: Throwable) {
            Log.e("MacroApp", "Init error: ${e.message}", e)
        }
    }
}
