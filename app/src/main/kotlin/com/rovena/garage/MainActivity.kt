package com.rovena.garage

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.rovena.garage.data.AppPreferences
import com.rovena.garage.ui.RovenaRoot
import com.rovena.garage.ui.theme.RovenaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { AppPreferences(this) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        lifecycleScope.launch { prefs.markNotificationPermissionAsked() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val savedLanguage = prefs.languageTag.first()
            if (savedLanguage.isNotBlank()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage))
            }
            prefs.markOpened()
        }

        setContent {
            RovenaTheme {
                RovenaRoot(
                    preferences = prefs,
                    onLanguageSelected = ::changeLanguage,
                    onRequestNotifications = ::requestNotificationPermissionIfNeeded
                )
            }
        }

        lifecycleScope.launch {
            if (prefs.onboardingCompleted.first() && !prefs.notificationPermissionAsked.first()) {
                requestNotificationPermissionIfNeeded()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { prefs.markOpened() }
    }

    private fun changeLanguage(tag: String) {
        lifecycleScope.launch {
            prefs.setLanguage(tag)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            lifecycleScope.launch { prefs.markNotificationPermissionAsked() }
        }
    }
}
