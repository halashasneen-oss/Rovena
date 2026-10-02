package com.rovena.garage

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.rovena.garage.ads.RovenaAdManager
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.data.RovenaBackupManager
import com.rovena.garage.notifications.NotificationScheduler
import com.rovena.garage.ui.RovenaRoot
import com.rovena.garage.ui.theme.RovenaTheme
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { AppPreferences(this) }
    private val adsManager by lazy { RovenaAdManager(this, prefs) }
    private val app by lazy { application as RovenaApp }
    private val vehicleRepository by lazy { app.vehicleRepository }
    private val recordRepository by lazy { app.recordRepository }
    private val backupManager by lazy { RovenaBackupManager(app.database) }
    private var pendingBackupText: String? = null
    private var pendingRestoreText: String? = null
    private val showRestoreConfirmation = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        lifecycleScope.launch {
            prefs.markNotificationPermissionAsked()
            if (granted) {
                ensureNotificationsActive()
            }
            adsManager.requestConsent()
        }
    }

    private val backupCreateLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val text = pendingBackupText
        pendingBackupText = null
        if (uri != null && text != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val success = runCatching {
                    contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { writer ->
                        writer.write(text)
                    } ?: error("Could not open backup destination")
                }.isSuccess
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        if (success) R.string.backup_export_success else R.string.backup_export_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private val backupOpenLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val json = runCatching {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Could not read backup")
                }
                withContext(Dispatchers.Main) {
                    json.onSuccess { content ->
                        pendingRestoreText = content
                        showRestoreConfirmation.value = true
                    }.onFailure {
                        Toast.makeText(this@MainActivity, R.string.backup_import_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        adsManager.attach()

        lifecycleScope.launch {
            val savedLanguage = prefs.languageTag.first()
            val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (savedLanguage.isNotBlank() && currentLanguage != savedLanguage) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage))
            }
            prefs.markOpened()
        }

        setContent {
            val themeMode = prefs.themeMode.collectAsStateWithLifecycle(
                initialValue = AppPreferences.THEME_SYSTEM
            ).value
            RovenaTheme(themeMode = themeMode) {
                RovenaRoot(
                    preferences = prefs,
                    vehicleRepository = vehicleRepository,
                    recordRepository = recordRepository,
                    onLanguageSelected = ::changeLanguage,
                    onRequestNotifications = ::requestNotificationPermissionIfNeeded,
                    onExportBackup = ::exportBackup,
                    onImportBackup = ::importBackup,
                    adsManager = adsManager
                )
                if (showRestoreConfirmation.value) {
                    AlertDialog(
                        onDismissRequest = ::cancelRestore,
                        title = { Text(getString(R.string.vb_restore_title)) },
                        text = { Text(getString(R.string.vb_restore_message)) },
                        confirmButton = {
                            TextButton(onClick = ::restorePendingBackup) {
                                Text(getString(R.string.vb_restore_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = ::cancelRestore) {
                                Text(getString(R.string.vb_restore_cancel))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        adsManager.onActivityStopped()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            prefs.markOpened()

            if (!prefs.onboardingCompleted.first()) return@launch

            adsManager.updateClock()
            if (notificationsAllowed()) {
                ensureNotificationsActive()
                adsManager.requestConsent()
            } else if (!prefs.notificationPermissionAsked.first()) {
                requestNotificationPermissionIfNeeded()
            } else {
                adsManager.requestConsent()
            }
            adsManager.onActivityResumed()
        }
    }

    private suspend fun ensureNotificationsActive() {
        runCatching { NotificationScheduler.ensureScheduled(applicationContext) }

        // On a clean install or an upgrade from the affected alpha build, send one
        // verification reminder shortly after permission is available. After that,
        // the normal cadence remains every 12 hours.
        if (prefs.lastEngagementNotificationAt.first() == 0L) {
            runCatching { NotificationScheduler.scheduleInitialVerification(applicationContext) }
        }
    }

    private fun notificationsAllowed(): Boolean {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun changeLanguage(tag: String) {
        lifecycleScope.launch {
            prefs.setLanguage(tag)
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (current != tag) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                lifecycleScope.launch {
                    ensureNotificationsActive()
                    adsManager.requestConsent()
                }
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            lifecycleScope.launch {
                prefs.markNotificationPermissionAsked()
                if (NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()) {
                    ensureNotificationsActive()
                }
                adsManager.requestConsent()
            }
        }
    }

    override fun onDestroy() {
        adsManager.release()
        super.onDestroy()
    }

    private fun cancelRestore() {
        pendingRestoreText = null
        showRestoreConfirmation.value = false
    }

    private fun restorePendingBackup() {
        val json = pendingRestoreText ?: return
        cancelRestore()
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching { backupManager.restoreJson(json) }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    if (result.isSuccess) R.string.backup_import_success else R.string.backup_import_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun exportBackup() {
        lifecycleScope.launch {
            val json = runCatching { withContext(Dispatchers.IO) { backupManager.exportJson() } }.getOrElse {
                Toast.makeText(this@MainActivity, R.string.backup_export_failed, Toast.LENGTH_LONG).show()
                return@launch
            }
            pendingBackupText = json
            adsManager.suppressNextReturn()
            backupCreateLauncher.launch("Rovena-backup-${LocalDate.now()}.json")
        }
    }

    private fun importBackup() {
        adsManager.suppressNextReturn()
        backupOpenLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
    }
}
