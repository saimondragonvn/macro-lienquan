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

        // Bắt lỗi toàn cục để app tự bảo vệ và không bị crash
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MacroApp", "Crash intercepted: ${throwable.message}", throwable)
            Handler(Looper.getMainLooper()).post {
                try {
                    Toast.makeText(
                        applicationContext,
                        "Lỗi hệ thống đã được xử lý: ${throwable.localizedMessage ?: "Tự phục hồi"}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Throwable) {}
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            NotificationHelper.createNotificationChannel(this)
            MacroManager.getInstance(this)
        } catch (e: Throwable) {
            Log.e("MacroApp", "Init error: ${e.message}", e)
        }
    }
}
