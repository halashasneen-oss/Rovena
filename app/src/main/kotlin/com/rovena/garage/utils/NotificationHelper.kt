package com.rovena.garage.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rovena.garage.R
import com.rovena.garage.RovenaApp
import com.rovena.garage.domain.model.NotificationSeverity
import com.rovena.garage.presentation.MainActivity
import com.rovena.garage.presentation.reminders.NotificationActionReceiver

object NotificationHelper {

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun showReminderNotification(context: Context, reminderId: Long, title: String, body: String, severity: NotificationSeverity = NotificationSeverity.IMPORTANT) {
        if (!hasPermission(context)) return

        val contentIntent = PendingIntent.getActivity(
            context, reminderId.toInt(),
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = PendingIntent.getBroadcast(
            context, reminderId.toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_DONE
                putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // The channel (not setPriority(), a no-op on API 26+) is what actually controls
        // sound/heads-up/badge behavior on modern Android - see createNotificationChannels().
        val channelId = when (severity) {
            NotificationSeverity.CRITICAL -> RovenaApp.CHANNEL_CRITICAL
            NotificationSeverity.IMPORTANT -> RovenaApp.CHANNEL_IMPORTANT
            NotificationSeverity.UPCOMING -> RovenaApp.CHANNEL_UPCOMING
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.reminder_mark_done), markDoneIntent)
            .setPriority(
                when (severity) {
                    NotificationSeverity.CRITICAL -> NotificationCompat.PRIORITY_HIGH
                    NotificationSeverity.IMPORTANT -> NotificationCompat.PRIORITY_DEFAULT
                    NotificationSeverity.UPCOMING -> NotificationCompat.PRIORITY_LOW
                }
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + reminderId.toInt(), notification)
    }

    private const val NOTIFICATION_ID_BASE = 1000
}
