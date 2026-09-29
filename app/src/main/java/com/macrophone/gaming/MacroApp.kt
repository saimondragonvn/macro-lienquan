package com.macrophone.gaming

import android.app.Application
import android.content.Context
import android.util.Log
import com.macrophone.gaming.domain.MacroManager
import com.macrophone.gaming.util.NotificationHelper

class MacroApp : Application() {

    override fun onCreate() {
        super.onCreate()

        setupCrashHandler()

        try {
            NotificationHelper.createNotificationChannel(this)
            MacroManager.getInstance(this)
        } catch (e: Throwable) {
            Log.e("MacroApp", "Init error: ${e.message}", e)
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MacroApp", "Crash on thread [${thread.name}]: ${throwable.message}", throwable)

            // Lưu log sự cố vào SharedPreferences để chẩn đoán
            try {
                val prefs = getSharedPreferences("macro_diagnostics", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("last_error", throwable.localizedMessage ?: "Unknown error")
                    .putString("last_stacktrace", Log.getStackTraceString(throwable))
                    .putLong("timestamp", System.currentTimeMillis())
                    .apply()
            } catch (_: Throwable) {}

            // Bàn giao cho handler mặc định của hệ điều hành xử lý sạch sẽ, không để app bị đơ/treo ANR
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
