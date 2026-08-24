package com.rovena.garage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
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
     * True only before the very first successful authentication this process -
     * guarantees a cold process launch with App Lock enabled always locks,
     * regardless of the configured timeout. Once cleared by [markAuthenticated],
     * whether the app re-locks after backgrounding is decided entirely by
     * comparing [backgroundedAtMillis] against the user's configured
     * `appLockTimeoutSeconds` (see MainActivity.checkAppLock()), not this flag.
     */
    var requiresReauth: Boolean = true
        private set

    /**
     * Wall-clock time the whole app (not just one Activity, e.g. a document
     * picker) last left the foreground, or null while in the foreground / not
     * yet backgrounded since the last successful unlock. Immediate-timeout
     * (appLockTimeoutSeconds = 0, the default) still locks every time, since
     * any elapsed time - even a few milliseconds - already exceeds a 0s grace period.
     */
    var backgroundedAtMillis: Long? = null
        private set

    fun markAuthenticated() {
        requiresReauth = false
        backgroundedAtMillis = null
    }

    override fun onCreate() {
        super.onCreate()
        enableStrictModeForDebugBuilds()
        createNotificationChannels()
        applyPersistedTheme()
        ReminderCheckWorker.schedule(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                backgroundedAtMillis = System.currentTimeMillis()
            }
        })
    }

    /**
     * Debug-only (never affects a release build's behavior or performance): logs, rather than
     * crashes on, any accidental main-thread disk/network access or leaked SQLite/Closeable
     * object - this app is offline-first by design and every DB/file operation is already
     * routed through `Dispatchers.IO` or Room's own suspend dispatching, so this exists purely
     * to catch a *future* regression early instead of relying on that discipline holding by hand.
     */
    private fun enableStrictModeForDebugBuilds() {
        if (!BuildConfig.DEBUG) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }

    /**
     * One channel per [com.rovena.garage.domain.model.NotificationSeverity] tier, not one
     * per record category - on API 26+ (virtually every real device, and always true at this
     * app's targetSdk) a notification's actual importance (sound, heads-up, badge) is decided
     * solely by its channel's importance; `NotificationCompat.Builder.setPriority()` is a
     * silent no-op there. A single shared channel would make the tiered-severity feature
     * (spec: Critical/Important/Upcoming) cosmetically present but functionally inert on every
     * modern device. Channel importance can't be changed in place once created (only the user
     * can, in system settings), so these use their own ids rather than reusing the old
     * `rovena_reminders`/`rovena_documents` ones - those are simply abandoned, not migrated.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val critical = NotificationChannel(
                CHANNEL_CRITICAL,
                getString(R.string.notification_channel_critical),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.notification_channel_critical_desc) }

            val important = NotificationChannel(
                CHANNEL_IMPORTANT,
                getString(R.string.notification_channel_important),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(R.string.notification_channel_important_desc) }

            val upcoming = NotificationChannel(
                CHANNEL_UPCOMING,
                getString(R.string.notification_channel_upcoming),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.notification_channel_upcoming_desc) }

            manager.createNotificationChannel(critical)
            manager.createNotificationChannel(important)
            manager.createNotificationChannel(upcoming)
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
        const val CHANNEL_CRITICAL = "rovena_reminders_critical"
        const val CHANNEL_IMPORTANT = "rovena_reminders_important"
        const val CHANNEL_UPCOMING = "rovena_reminders_upcoming"
    }
}
