package com.rovena.garage.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val REMINDER_INTERVAL_HOURS = 12L
    private const val ENGAGEMENT_WORK_NAME = "rovena_engagement_check"
    private const val VEHICLE_DUE_WORK_NAME = "rovena_vehicle_due_check"

    fun ensureScheduled(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val engagementRequest = PeriodicWorkRequestBuilder<EngagementWorker>(REMINDER_INTERVAL_HOURS, TimeUnit.HOURS)
            .setInitialDelay(REMINDER_INTERVAL_HOURS, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            ENGAGEMENT_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            engagementRequest
        )

        val vehicleDueRequest = PeriodicWorkRequestBuilder<DueReminderWorker>(REMINDER_INTERVAL_HOURS, TimeUnit.HOURS)
            .setInitialDelay(REMINDER_INTERVAL_HOURS, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            VEHICLE_DUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            vehicleDueRequest
        )
    }
}
