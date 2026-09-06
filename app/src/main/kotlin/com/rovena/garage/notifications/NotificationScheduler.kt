package com.rovena.garage.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val REMINDER_INTERVAL_HOURS = NotificationPolicy.INTERVAL_HOURS
    const val FORCE_NOTIFICATION_KEY = "force_notification"

    private const val ENGAGEMENT_WORK_NAME = "rovena_engagement_check"
    private const val VEHICLE_DUE_WORK_NAME = "rovena_vehicle_due_check"
    private const val INITIAL_VERIFICATION_WORK_NAME = "rovena_notification_verification"

    fun ensureScheduled(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val engagementRequest = PeriodicWorkRequestBuilder<EngagementWorker>(
            REMINDER_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setInitialDelay(REMINDER_INTERVAL_HOURS, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            ENGAGEMENT_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            engagementRequest
        )

        val vehicleDueRequest = PeriodicWorkRequestBuilder<DueReminderWorker>(
            REMINDER_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setInitialDelay(REMINDER_INTERVAL_HOURS, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            VEHICLE_DUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            vehicleDueRequest
        )
    }

    /**
     * Posts one confirmation reminder shortly after notification permission becomes available.
     * This makes it obvious to the user that Android is actually allowing Rovena notifications,
     * while the normal reminder cadence remains every 12 hours.
     */
    fun scheduleInitialVerification(context: Context) {
        val request = OneTimeWorkRequestBuilder<EngagementWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .setInputData(workDataOf(FORCE_NOTIFICATION_KEY to true))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            INITIAL_VERIFICATION_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
