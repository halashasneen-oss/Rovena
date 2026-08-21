package com.rovena.garage.presentation.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.rovena.garage.RovenaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_DONE) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
        if (reminderId == 0L) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as RovenaApp).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.reminderRepository.getById(reminderId)?.let { container.reminderRepository.markCompleted(it) }
                NotificationManagerCompat.from(context).cancel(1000 + reminderId.toInt())
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.rovena.garage.action.MARK_REMINDER_DONE"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
