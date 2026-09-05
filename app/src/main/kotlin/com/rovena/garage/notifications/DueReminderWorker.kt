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
        val repeatThreshold = TimeUnit.DAYS.toMillis(3)
        if (now - prefs.lastVehicleReminderAt.first() < repeatThreshold) return Result.success()

        val database = RovenaDatabase.create(applicationContext)
        return try {
            val vehicles = database.vehicleDao().getAllOnce()
            val recordDao = database.recordDao()
            val attention = vehicles.firstNotNullOfOrNull { vehicle ->
                val maintenance = recordDao.getMaintenanceOnce(vehicle.id)
                val documents = recordDao.getDocumentsOnce(vehicle.id)
                val dueCount = maintenance.count { ReminderEvaluator.isMaintenanceDue(it, vehicle.mileage, now) } +
                    documents.count { ReminderEvaluator.isDocumentDueSoon(it, now) }
                if (dueCount > 0) vehicle to dueCount else null
            } ?: return Result.success()

            val language = prefs.languageTag.first().ifBlank { Locale.getDefault().language }
            val localized = localizedContext(applicationContext, language)
            createChannel(localized)

            val vehicle = attention.first
            val displayName = vehicle.nickname.ifBlank { "${vehicle.make} ${vehicle.model}" }
            val message = localized.getString(R.string.vehicle_due_message, attention.second, displayName)
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                1201,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(localized, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(localized.getString(R.string.vehicle_due_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
                prefs.markVehicleReminderSent(now)
            }
            Result.success()
        } finally {
            database.close()
        }
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
                    context.getString(R.string.notification_channel_vehicle_due),
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

object ReminderEvaluator {
    fun isMaintenanceDue(record: MaintenanceEntity, mileage: Long, now: Long): Boolean =
        (record.nextDueMileage != null && record.nextDueMileage <= mileage) ||
            (record.nextDueAt != null && record.nextDueAt <= now)

    fun isDocumentDueSoon(
        document: DocumentEntity,
        now: Long,
        horizonMillis: Long = TimeUnit.DAYS.toMillis(7)
    ): Boolean = document.expiryAt != null && document.expiryAt <= now + horizonMillis
}
