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
import com.rovena.garage.domain.usecase.ReminderStageCalculator
import com.rovena.garage.utils.NotificationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Periodic check (spec #17 - local reminders, no internet; spec: Registration/
 * Insurance reminder schedule). Runs roughly every 12 hours, for every
 * active, non-completed reminder across every vehicle:
 *
 * - **Date-based reminders** (`dueDateMillis` set) use the staged schedule in
 *   [ReminderStageCalculator] - notified once at each of 30/14/7/3/1 days
 *   out, on the due day, and once more when it goes overdue.
 *   `lastNotifiedStageDays` tracks the most urgent stage already notified so
 *   none of them repeats.
 * - **Mileage-only reminders** (no date) keep the simpler single due/overdue
 *   notification, deduplicated via `lastNotifiedAtMillis` - km doesn't carry
 *   the same fixed real-world cadence a calendar staged schedule assumes.
 * - A reminder with **both** triggers is handled via the date-staged path
 *   only, to avoid two independent notification streams for one reminder.
 */
class ReminderCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as RovenaApp).container
        val reminders = container.reminderRepository.getAllActiveOnce()
        val today = LocalDate.now()

        for (reminder in reminders) {
            val vehicle = container.vehicleRepository.getById(reminder.vehicleId) ?: continue

            if (reminder.dueDateMillis != null) {
                notifyStagedIfDue(container, vehicle, reminder, today)
                continue
            }

            notifyMileageIfDue(container, vehicle, reminder, today)
        }

        return Result.success()
    }

    private suspend fun notifyStagedIfDue(
        container: com.rovena.garage.AppContainer,
        vehicle: com.rovena.garage.data.local.entities.VehicleEntity,
        reminder: com.rovena.garage.data.local.entities.ReminderEntity,
        today: LocalDate
    ) {
        val dueDate = Instant.ofEpochMilli(reminder.dueDateMillis!!).atZone(ZoneId.systemDefault()).toLocalDate()
        val remainingDays = ChronoUnit.DAYS.between(today, dueDate)
        val stage = ReminderStageCalculator.stageFor(remainingDays) ?: return
        if (stage == reminder.lastNotifiedStageDays) return

        val statusBody = if (stage == ReminderStageCalculator.EXPIRED_STAGE) {
            applicationContext.getString(R.string.reminder_notification_body_overdue)
        } else if (stage == 0) {
            applicationContext.getString(R.string.reminder_notification_body_due_today)
        } else {
            applicationContext.getString(R.string.reminder_notification_body_days, remainingDays.toInt())
        }
        postNotification(reminder.id, reminder.title, vehicle, statusBody)
        container.reminderRepository.markNotifiedStage(reminder.id, stage)
    }

    private suspend fun notifyMileageIfDue(
        container: com.rovena.garage.AppContainer,
        vehicle: com.rovena.garage.data.local.entities.VehicleEntity,
        reminder: com.rovena.garage.data.local.entities.ReminderEntity,
        today: LocalDate
    ) {
        val evaluation = DueStatusCalculator.evaluate(
            currentMileageKm = vehicle.currentMileageKm,
            today = today,
            dueMileageKm = reminder.dueMileageKm,
            dueDate = null
        ) ?: return

        if (evaluation.status != DueStatus.DUE && evaluation.status != DueStatus.OVERDUE) return

        // Already notified since the last time it became due? Skip (avoid spamming every 12h).
        val alreadyNotified = reminder.lastNotifiedAtMillis != null &&
            (reminder.lastTriggeredAtMillis == null || reminder.lastNotifiedAtMillis!! > reminder.lastTriggeredAtMillis!!)
        if (alreadyNotified) return

        val remainingKm = evaluation.remainingKm
        val statusBody = when {
            evaluation.status == DueStatus.OVERDUE -> applicationContext.getString(R.string.reminder_notification_body_overdue)
            remainingKm != null -> applicationContext.getString(R.string.reminder_notification_body_km, remainingKm)
            else -> ""
        }
        postNotification(reminder.id, reminder.title, vehicle, statusBody)
        container.reminderRepository.markNotified(reminder.id)
    }

    private fun postNotification(
        reminderId: Long,
        reminderTitle: String,
        vehicle: com.rovena.garage.data.local.entities.VehicleEntity,
        statusBody: String
    ) {
        // Multi-vehicle garages: without the vehicle name, a reminder notification is
        // ambiguous about which car it refers to.
        val vehicleLabel = "${vehicle.make} ${vehicle.model}"
        val body = applicationContext.getString(R.string.reminder_notification_body_with_vehicle, vehicleLabel, statusBody)
        NotificationHelper.showReminderNotification(
            applicationContext,
            reminderId,
            applicationContext.getString(R.string.reminder_notification_title, reminderTitle),
            body
        )
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
