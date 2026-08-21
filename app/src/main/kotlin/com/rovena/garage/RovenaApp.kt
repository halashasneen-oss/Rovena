package com.rovena.garage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.rovena.garage.domain.model.AppThemeMode
import com.rovena.garage.presentation.reminders.ReminderCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RovenaApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    /** App-wide coroutine scope for fire-and-forget work (theme application, notification channel setup). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * True whenever the app (any Activity) needs to pass App Lock again
     * before showing content - starts true so a cold process launch with
     * App Lock enabled always locks, and is re-armed whenever the whole app
     * (not just one Activity, e.g. a document picker) leaves the foreground.
     */
    var requiresReauth: Boolean = true
        private set

    fun markAuthenticated() {
        requiresReauth = false
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        applyPersistedTheme()
        ReminderCheckWorker.schedule(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                requiresReauth = true
            }
        })
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminders = NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(R.string.notification_channel_reminders_desc) }

            val documents = NotificationChannel(
                CHANNEL_DOCUMENTS,
                getString(R.string.notification_channel_documents),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(R.string.notification_channel_documents_desc) }

            manager.createNotificationChannel(reminders)
            manager.createNotificationChannel(documents)
        }
    }

    private fun applyPersistedTheme() {
        applicationScope.launch {
            val settings = container.settingsRepository.getOrDefault()
            val mode = when (settings.themeMode) {
                AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "rovena_reminders"
        const val CHANNEL_DOCUMENTS = "rovena_documents"
    }
}
