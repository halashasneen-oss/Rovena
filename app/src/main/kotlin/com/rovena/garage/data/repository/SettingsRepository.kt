package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.AppSettingsDao
import com.rovena.garage.data.local.entities.AppSettingsEntity
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.AppLanguage
import com.rovena.garage.domain.model.AppThemeMode
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.utils.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val settingsDao: AppSettingsDao) {

    /** Always emits a value - falls back to defaults before the singleton row exists. */
    fun observe(): Flow<AppSettingsEntity> = settingsDao.observe().map { it ?: AppSettingsEntity() }

    suspend fun getOrDefault(): AppSettingsEntity = settingsDao.getOnce() ?: AppSettingsEntity()

    private suspend fun update(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        val current = getOrDefault()
        settingsDao.upsert(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setThemeMode(mode: AppThemeMode) = update { it.copy(themeMode = mode) }

    suspend fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }

    suspend fun setDistanceUnit(unit: DistanceUnit) = update { it.copy(distanceUnit = unit) }

    suspend fun setFuelEconomyUnit(unit: FuelEconomyUnit) = update { it.copy(fuelEconomyUnit = unit) }

    suspend fun setCurrency(currency: AppCurrency, customCode: String? = null) =
        update { it.copy(currency = currency, customCurrencyCode = customCode) }

    suspend fun setNotificationsEnabled(enabled: Boolean) = update { it.copy(notificationsEnabled = enabled) }

    suspend fun setOnboardingCompleted(completed: Boolean) = update { it.copy(onboardingCompleted = completed) }

    suspend fun setSampleDataSeeded(seeded: Boolean) = update { it.copy(sampleDataSeeded = seeded) }

    suspend fun setPin(pin: String) {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin, salt)
        update { it.copy(appLockEnabled = true, pinHash = hash, pinSalt = salt) }
    }

    suspend fun clearPin() = update { it.copy(appLockEnabled = false, pinHash = null, pinSalt = null, biometricEnabled = false) }

    suspend fun verifyPin(pin: String): Boolean {
        val settings = getOrDefault()
        val hash = settings.pinHash ?: return false
        val salt = settings.pinSalt ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = update { it.copy(biometricEnabled = enabled) }
}
