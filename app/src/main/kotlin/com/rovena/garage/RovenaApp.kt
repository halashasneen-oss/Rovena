package com.rovena.garage

import android.app.Application
import com.rovena.garage.notifications.NotificationScheduler

class RovenaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.ensureScheduled(this)
    }
}
