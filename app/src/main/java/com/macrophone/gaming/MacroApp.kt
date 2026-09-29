package com.macrophone.gaming

import android.app.Application
import com.macrophone.gaming.domain.MacroManager
import com.macrophone.gaming.util.NotificationHelper

class MacroApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        MacroManager.getInstance(this)
    }
}
