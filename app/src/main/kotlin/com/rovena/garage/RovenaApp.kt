package com.rovena.garage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.rovena.garage.domain.model.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RovenaApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    /** App-wide coroutine scope for fire-and-forget work (theme application, notification channel setup). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        applyPersistedTheme()
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
