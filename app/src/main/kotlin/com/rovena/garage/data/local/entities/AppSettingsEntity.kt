package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.AppLanguage
import com.rovena.garage.domain.model.AppThemeMode
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit

/**
 * Single-row settings table (id is always [SINGLETON_ID]). Durable, structured
 * app configuration lives here so it's part of the same Room database (and
 * therefore included automatically in backups); fast, purely-local UI state
 * (currently selected vehicle, onboarding step) lives in DataStore instead -
 * see `UserPreferences`.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val fuelEconomyUnit: FuelEconomyUnit = FuelEconomyUnit.L_100KM,
    val currency: AppCurrency = AppCurrency.JOD,
    val customCurrencyCode: String? = null,
    val notificationsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    /** Digit count of the set PIN (never the PIN itself) - lets the lock screen know exactly when a full entry has been typed, instead of guessing by re-checking the hash after every keystroke. */
    val pinLength: Int? = null,
    /** Consecutive wrong-PIN attempts since the last success; reset to 0 on a correct unlock. */
    val pinFailedAttempts: Int = 0,
    /** Set after too many consecutive wrong attempts; PIN checks are refused until this instant passes. Never permanent. */
    val pinLockoutUntilMillis: Long? = null,
    val onboardingCompleted: Boolean = false,
    val sampleDataSeeded: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
