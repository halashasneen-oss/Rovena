package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.AppSettingsDao
import com.rovena.garage.data.local.entities.AppSettingsEntity
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.AppLanguage
import com.rovena.garage.domain.model.AppThemeMode
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.utils.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed class PinVerifyResult {
    data object Success : PinVerifyResult()
    data object NoPinSet : PinVerifyResult()
    data class WrongPin(val attemptsRemaining: Int) : PinVerifyResult()
    data class LockedOut(val untilMillis: Long) : PinVerifyResult()
}

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

    suspend fun setNotifyCriticalEnabled(enabled: Boolean) = update { it.copy(notifyCriticalEnabled = enabled) }

    suspend fun setNotifyImportantEnabled(enabled: Boolean) = update { it.copy(notifyImportantEnabled = enabled) }

    suspend fun setNotifyUpcomingEnabled(enabled: Boolean) = update { it.copy(notifyUpcomingEnabled = enabled) }

    suspend fun setNotifyDocumentCategoryEnabled(enabled: Boolean) = update { it.copy(notifyDocumentCategoryEnabled = enabled) }

    suspend fun setNotifyGeneralCategoryEnabled(enabled: Boolean) = update { it.copy(notifyGeneralCategoryEnabled = enabled) }

    suspend fun setOnboardingCompleted(completed: Boolean) = update { it.copy(onboardingCompleted = completed) }

    suspend fun setSampleDataSeeded(seeded: Boolean) = update { it.copy(sampleDataSeeded = seeded) }

    suspend fun setPin(pin: String) {
        val salt = PinHasher.generateSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
        update {
            it.copy(
                appLockEnabled = true, pinHash = hash, pinSalt = salt, pinLength = pin.length,
                pinFailedAttempts = 0, pinLockoutUntilMillis = null
            )
        }
    }

    suspend fun clearPin() = update {
        it.copy(
            appLockEnabled = false, pinHash = null, pinSalt = null, pinLength = null,
            biometricEnabled = false, pinFailedAttempts = 0, pinLockoutUntilMillis = null
        )
    }

    /**
     * Verifies [pin] against the stored hash, enforcing a temporary lockout after too many
     * consecutive wrong attempts (spec #10/#11: never a permanent lockout - it always
     * expires on its own). A currently-active lockout is checked *before* touching the PIN
     * hash at all, so a locked-out caller can't burn the (deliberately slow) PBKDF2 cost by
     * hammering the unlock screen.
     */
    suspend fun verifyPin(pin: String): PinVerifyResult {
        val settings = getOrDefault()
        val hash = settings.pinHash
        val salt = settings.pinSalt
        if (hash == null || salt == null) return PinVerifyResult.NoPinSet

        val now = System.currentTimeMillis()
        val lockoutUntil = settings.pinLockoutUntilMillis
        if (lockoutUntil != null && now < lockoutUntil) {
            return PinVerifyResult.LockedOut(lockoutUntil)
        }

        val isValid = withContext(Dispatchers.Default) { PinHasher.verify(pin, salt, hash) }
        if (isValid) {
            update { it.copy(pinFailedAttempts = 0, pinLockoutUntilMillis = null) }
            return PinVerifyResult.Success
        }

        val failedAttempts = settings.pinFailedAttempts + 1
        return if (failedAttempts >= MAX_FAILED_PIN_ATTEMPTS) {
            val until = now + LOCKOUT_DURATION_MILLIS
            update { it.copy(pinFailedAttempts = 0, pinLockoutUntilMillis = until) }
            PinVerifyResult.LockedOut(until)
        } else {
            update { it.copy(pinFailedAttempts = failedAttempts) }
            PinVerifyResult.WrongPin(MAX_FAILED_PIN_ATTEMPTS - failedAttempts)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = update { it.copy(biometricEnabled = enabled) }

    /** Backfills [AppSettingsEntity.pinLength] for a PIN set before that field existed, once its length is known from a successful unlock. */
    suspend fun recordPinLength(length: Int) = update { it.copy(pinLength = length) }

    companion object {
        const val MAX_FAILED_PIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MILLIS = 30_000L
    }
}
