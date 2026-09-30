package com.macrophone.gaming

import android.app.Application
import android.content.Context
import android.util.Log
import com.macrophone.gaming.util.NotificationHelper

class MacroApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        try {
            NotificationHelper.createNotificationChannel(this)
        } catch (e: Throwable) {
            Log.e("MacroApp", "Init error: ${e.message}", e)
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MacroApp", "Crash on thread [${thread.name}]: ${throwable.message}", throwable)
            try {
                val prefs = getSharedPreferences("macro_diagnostics", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("last_error", throwable.localizedMessage ?: "Unknown error")
                    .putString("last_stacktrace", Log.getStackTraceString(throwable))
                    .putLong("timestamp", System.currentTimeMillis())
                    .apply()
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
