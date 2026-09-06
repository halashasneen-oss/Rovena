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
import kotlinx.coroutines.flow.first
import java.util.Locale

class EngagementWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        val threshold = NotificationPolicy.intervalMillis
        val now = System.currentTimeMillis()
        val lastNotification = prefs.lastEngagementNotificationAt.first()
        val force = inputData.getBoolean(NotificationScheduler.FORCE_NOTIFICATION_KEY, false)

        // The affected alpha build also required 12 hours of app inactivity.
        // Opening Rovena reset that clock, so a user who checked the app regularly
        // could never see an engagement reminder. The release policy is a real
        // 12-hour cadence that is independent of app-open time.
        if (!NotificationPolicy.shouldSendPeriodic(lastNotification, now, force)) {
            return Result.success()
        }

        val language = prefs.languageTag.first().ifBlank { Locale.getDefault().language }
        val localized = localizedContext(applicationContext, language)
        createChannel(localized)

        val messages = listOf(
            localized.getString(R.string.engagement_message_mileage),
            localized.getString(R.string.engagement_message_costs),
            localized.getString(R.string.engagement_message_health)
        )
        val messageIndex = ((now / threshold) % messages.size).toInt()
        val title = if (force) {
            localized.getString(R.string.notification_fixed_title)
        } else {
            localized.getString(R.string.engagement_title)
        }
        val message = if (force) {
            localized.getString(R.string.notification_fixed_every_12h)
        } else {
            messages[messageIndex]
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1001,
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = NotificationManagerCompat.from(applicationContext)
        if (manager.areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, notification)
            prefs.markEngagementNotificationSent(now)
        }
        return Result.success()
    }

    private fun localizedContext(context: Context, languageTag: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(languageTag))
        return context.createConfigurationContext(config)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_engagement),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "rovena_engagement"
        private const val NOTIFICATION_ID = 4101
    }
}
