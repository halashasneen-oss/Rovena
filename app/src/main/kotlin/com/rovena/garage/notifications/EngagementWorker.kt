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
import java.util.concurrent.TimeUnit

class EngagementWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        if (!prefs.engagementEnabled.first()) return Result.success()

        val frequencyDays = prefs.engagementFrequencyDays.first().coerceIn(1, 30)
        val threshold = TimeUnit.DAYS.toMillis(frequencyDays.toLong())
        val now = System.currentTimeMillis()
        val lastOpened = prefs.lastOpenedAt.first()
        val lastNotification = prefs.lastEngagementNotificationAt.first()

        if (lastOpened == 0L || now - lastOpened < threshold || now - lastNotification < threshold) {
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

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(localized, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.engagement_title))
            .setContentText(messages[(now / threshold).toInt().mod(messages.size)])
            .setStyle(NotificationCompat.BigTextStyle().bigText(messages[(now / threshold).toInt().mod(messages.size)]))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
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
