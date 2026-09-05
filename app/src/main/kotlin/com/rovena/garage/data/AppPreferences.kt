package com.rovena.garage.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rovena_preferences")

class AppPreferences(private val context: Context) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val languageTag = stringPreferencesKey("language_tag")
        val themeMode = stringPreferencesKey("theme_mode")
        val lastOpenedAt = longPreferencesKey("last_opened_at")
        val notificationPermissionAsked = booleanPreferencesKey("notification_permission_asked")
        val engagementEnabled = booleanPreferencesKey("engagement_enabled")
        val engagementFrequencyDays = intPreferencesKey("engagement_frequency_days")
        val lastEngagementNotificationAt = longPreferencesKey("last_engagement_notification_at")
        val vehicleRemindersEnabled = booleanPreferencesKey("vehicle_reminders_enabled")
        val lastVehicleReminderAt = longPreferencesKey("last_vehicle_reminder_at")
        val lastSmartReminderKey = stringPreferencesKey("last_smart_reminder_key")
        val lastSmartReminderAt = longPreferencesKey("last_smart_reminder_at")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboardingCompleted] ?: false }
    val languageTag: Flow<String> = context.dataStore.data.map { it[Keys.languageTag] ?: "" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.themeMode] ?: THEME_SYSTEM }
    val lastOpenedAt: Flow<Long> = context.dataStore.data.map { it[Keys.lastOpenedAt] ?: 0L }
    val notificationPermissionAsked: Flow<Boolean> = context.dataStore.data.map { it[Keys.notificationPermissionAsked] ?: false }
    val engagementEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.engagementEnabled] ?: true }
    val engagementFrequencyDays: Flow<Int> = context.dataStore.data.map { it[Keys.engagementFrequencyDays] ?: 7 }
    val lastEngagementNotificationAt: Flow<Long> = context.dataStore.data.map { it[Keys.lastEngagementNotificationAt] ?: 0L }
    val vehicleRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.vehicleRemindersEnabled] ?: true }
    val lastVehicleReminderAt: Flow<Long> = context.dataStore.data.map { it[Keys.lastVehicleReminderAt] ?: 0L }
    val lastSmartReminderKey: Flow<String> = context.dataStore.data.map { it[Keys.lastSmartReminderKey] ?: "" }
    val lastSmartReminderAt: Flow<Long> = context.dataStore.data.map { it[Keys.lastSmartReminderAt] ?: 0L }

    suspend fun completeOnboarding() = context.dataStore.edit { it[Keys.onboardingCompleted] = true }
    suspend fun setLanguage(tag: String) = context.dataStore.edit { it[Keys.languageTag] = tag }
    suspend fun setThemeMode(mode: String) = context.dataStore.edit {
        it[Keys.themeMode] = if (mode in SUPPORTED_THEME_MODES) mode else THEME_SYSTEM
    }
    suspend fun markOpened() = context.dataStore.edit { it[Keys.lastOpenedAt] = System.currentTimeMillis() }
    suspend fun markNotificationPermissionAsked() = context.dataStore.edit { it[Keys.notificationPermissionAsked] = true }
    suspend fun setEngagementEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.engagementEnabled] = enabled }
    suspend fun setEngagementFrequencyDays(days: Int) = context.dataStore.edit { it[Keys.engagementFrequencyDays] = days.coerceIn(1, 30) }
    suspend fun markEngagementNotificationSent(time: Long) = context.dataStore.edit { it[Keys.lastEngagementNotificationAt] = time }
    suspend fun setVehicleRemindersEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.vehicleRemindersEnabled] = enabled }
    suspend fun markVehicleReminderSent(time: Long) = context.dataStore.edit { it[Keys.lastVehicleReminderAt] = time }
    suspend fun markSmartReminderSent(key: String, time: Long) = context.dataStore.edit {
        it[Keys.lastSmartReminderKey] = key
        it[Keys.lastSmartReminderAt] = time
        it[Keys.lastVehicleReminderAt] = time
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        val SUPPORTED_THEME_MODES = setOf(THEME_SYSTEM, THEME_LIGHT, THEME_DARK)
    }
}
