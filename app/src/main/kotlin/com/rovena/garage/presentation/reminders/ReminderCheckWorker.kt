package com.rovena.garage.presentation.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rovena.garage.R
import com.rovena.garage.RovenaApp
import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.usecase.DueStatusCalculator
import com.rovena.garage.utils.NotificationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Periodic check (spec #17 - local reminders, no internet). Runs roughly
 * every 12 hours: for every active, non-completed reminder across every
 * vehicle, evaluates due status against that vehicle's current mileage/today
 * and posts a local notification the first time it becomes DUE or OVERDUE.
 * `lastNotifiedAtMillis` prevents re-notifying on every run.
 */
class ReminderCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as RovenaApp).container
        val reminders = container.reminderRepository.getAllActiveOnce()
        val today = LocalDate.now()

        for (reminder in reminders) {
            val vehicle = container.vehicleRepository.getById(reminder.vehicleId) ?: continue
            val evaluation = DueStatusCalculator.evaluate(
                currentMileageKm = vehicle.currentMileageKm,
                today = today,
                dueMileageKm = reminder.dueMileageKm,
                dueDate = reminder.dueDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            ) ?: continue

            if (evaluation.status != DueStatus.DUE && evaluation.status != DueStatus.OVERDUE) continue

            // Already notified since the last time it became due? Skip (avoid spamming every 12h).
            val alreadyNotified = reminder.lastNotifiedAtMillis != null &&
                (reminder.lastTriggeredAtMillis == null || reminder.lastNotifiedAtMillis!! > reminder.lastTriggeredAtMillis!!)
            if (alreadyNotified) continue

            val body = when {
                evaluation.status == DueStatus.OVERDUE -> applicationContext.getString(R.string.reminder_notification_body_overdue)
                evaluation.remainingKm != null -> applicationContext.getString(R.string.reminder_notification_body_km, evaluation.remainingKm)
                evaluation.remainingDays != null -> applicationContext.getString(R.string.reminder_notification_body_days, evaluation.remainingDays.toInt())
                else -> ""
            }
            NotificationHelper.showReminderNotification(
                applicationContext,
                reminder.id,
                applicationContext.getString(R.string.reminder_notification_title, reminder.title),
                body
            )
            container.reminderRepository.markNotified(reminder.id)
        }

        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "reminder_check_periodic"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
