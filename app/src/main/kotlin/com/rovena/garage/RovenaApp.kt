package com.rovena.garage

import android.app.Application
import com.rovena.garage.data.VehicleRepository
import com.rovena.garage.data.local.RovenaDatabase
import com.rovena.garage.notifications.NotificationScheduler

class RovenaApp : Application() {
    val database: RovenaDatabase by lazy { RovenaDatabase.create(this) }
    val vehicleRepository: VehicleRepository by lazy { VehicleRepository(database) }

    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.ensureScheduled(this)
    }
}
