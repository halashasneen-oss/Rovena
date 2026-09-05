package com.rovena.garage

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.data.RovenaBackupManager
import com.rovena.garage.ui.RovenaRoot
import com.rovena.garage.ui.theme.RovenaTheme
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { AppPreferences(this) }
    private val app by lazy { application as RovenaApp }
    private val vehicleRepository by lazy { app.vehicleRepository }
    private val recordRepository by lazy { app.recordRepository }
    private val backupManager by lazy { RovenaBackupManager(app.database) }
    private var pendingBackupText: String? = null
    private var startupPermissionCheckDone = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        lifecycleScope.launch { prefs.markNotificationPermissionAsked() }
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
                val result = runCatching {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Could not read backup")
                    backupManager.restoreJson(json)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        if (result.isSuccess) R.string.backup_import_success else R.string.backup_import_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val savedLanguage = prefs.languageTag.first()
            val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (savedLanguage.isNotBlank() && currentLanguage != savedLanguage) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage))
            }
            prefs.markOpened()
        }

        setContent {
            val themeMode = prefs.themeMode.collectAsStateWithLifecycle(initialValue = AppPreferences.THEME_SYSTEM).value
            RovenaTheme(themeMode = themeMode) {
                RovenaRoot(
                    preferences = prefs,
                    vehicleRepository = vehicleRepository,
                    recordRepository = recordRepository,
                    onLanguageSelected = ::changeLanguage,
                    onRequestNotifications = ::requestNotificationPermissionIfNeeded,
                    onExportBackup = ::exportBackup,
                    onImportBackup = ::importBackup
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { prefs.markOpened() }

        if (!startupPermissionCheckDone) {
            startupPermissionCheckDone = true
            lifecycleScope.launch {
                if (prefs.onboardingCompleted.first() && !prefs.notificationPermissionAsked.first()) {
                    requestNotificationPermissionIfNeeded()
                }
            }
        }
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
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            lifecycleScope.launch { prefs.markNotificationPermissionAsked() }
        }
    }

    private fun exportBackup() {
        lifecycleScope.launch {
            val json = runCatching { withContext(Dispatchers.IO) { backupManager.exportJson() } }.getOrElse {
                Toast.makeText(this@MainActivity, R.string.backup_export_failed, Toast.LENGTH_LONG).show()
                return@launch
            }
            pendingBackupText = json
            backupCreateLauncher.launch("Rovena-backup-${LocalDate.now()}.json")
        }
    }

    private fun importBackup() {
        backupOpenLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
    }
}
