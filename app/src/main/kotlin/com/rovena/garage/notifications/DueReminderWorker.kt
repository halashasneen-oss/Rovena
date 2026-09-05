package com.rovena.garage.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rovena.garage.MainActivity
import com.rovena.garage.R
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.data.VehicleHealthEngine
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.RovenaDatabase
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.concurrent.TimeUnit

class DueReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        if (!prefs.vehicleRemindersEnabled.first()) return Result.success()

        val now = System.currentTimeMillis()
        val lastAt = prefs.lastSmartReminderAt.first()
        if (lastAt > 0L && now - lastAt < TimeUnit.HOURS.toMillis(24)) return Result.success()

        val database = RovenaDatabase.create(applicationContext)
        return try {
            val recordDao = database.recordDao()
            val reminders = database.vehicleDao().getAllOnce().flatMap { vehicle ->
                SmartReminderEngine.evaluateVehicle(
                    vehicle = vehicle,
                    maintenance = recordDao.getMaintenanceOnce(vehicle.id),
                    documents = recordDao.getDocumentsOnce(vehicle.id),
                    fuel = recordDao.getFuelOnce(vehicle.id),
                    now = now
                )
            }

            val candidate = reminders.firstOrNull() ?: return Result.success()
            val lastKey = prefs.lastSmartReminderKey.first()
            if (candidate.key == lastKey && lastAt > 0L && now - lastAt < TimeUnit.DAYS.toMillis(3)) {
                return Result.success()
            }

            val language = prefs.languageTag.first().ifBlank { Locale.getDefault().language }
            val localized = localizedContext(applicationContext, language)
            createChannel(localized)

            val message = localizedMessage(localized, candidate)
            val title = localized.getString(
                if (candidate.priority >= 90) R.string.smart_reminder_title_urgent
                else R.string.smart_reminder_title
            )

            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                1201,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(localized, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(if (candidate.priority >= 90) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .build()

            if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
                prefs.markSmartReminderSent(candidate.key, now)
            }
            Result.success()
        } finally {
            database.close()
        }
    }

    private fun localizedMessage(context: Context, reminder: SmartReminder): String = when (reminder.kind) {
        SmartReminderKind.MAINTENANCE_OVERDUE -> context.getString(
            R.string.smart_reminder_maintenance_overdue,
            reminder.vehicleName,
            reminder.itemLabel
        )
        SmartReminderKind.MAINTENANCE_SOON -> context.getString(
            R.string.smart_reminder_maintenance_soon,
            reminder.vehicleName,
            reminder.itemLabel
        )
        SmartReminderKind.DOCUMENT_EXPIRED -> context.getString(
            R.string.smart_reminder_document_expired,
            reminder.vehicleName,
            reminder.itemLabel
        )
        SmartReminderKind.DOCUMENT_SOON -> context.getString(
            R.string.smart_reminder_document_soon,
            reminder.vehicleName,
            reminder.itemLabel
        )
        SmartReminderKind.FUEL_EFFICIENCY_DROP -> context.getString(
            R.string.smart_reminder_fuel_drop,
            reminder.vehicleName,
            reminder.percent
        )
    }

    private fun localizedContext(context: Context, languageTag: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(languageTag))
        return context.createConfigurationContext(config)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_smart_care),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "rovena_vehicle_due"
        private const val NOTIFICATION_ID = 4201
    }
}

// Kept for existing tests and backwards-compatible reminder behavior.
object ReminderEvaluator {
    fun isMaintenanceDue(record: MaintenanceEntity, mileage: Long, now: Long): Boolean =
        VehicleHealthEngine.isMaintenanceOverdue(record, mileage, now)

    fun isDocumentDueSoon(
        document: DocumentEntity,
        now: Long,
        horizonMillis: Long = TimeUnit.DAYS.toMillis(7)
    ): Boolean = document.expiryAt != null && document.expiryAt <= now + horizonMillis
}
