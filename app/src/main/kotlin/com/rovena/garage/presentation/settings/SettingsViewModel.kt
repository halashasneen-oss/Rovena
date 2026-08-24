package com.rovena.garage.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.AppSettingsEntity
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.AppLanguage
import com.rovena.garage.domain.model.AppThemeMode
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.FuelEconomyUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettingsEntity> = container.settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
        val nightMode = when (mode) {
            AppThemeMode.LIGHT -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            AppThemeMode.SYSTEM -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch { container.settingsRepository.setDistanceUnit(unit) }
    }

    fun setFuelEconomyUnit(unit: FuelEconomyUnit) {
        viewModelScope.launch { container.settingsRepository.setFuelEconomyUnit(unit) }
    }

    fun setCurrency(currency: AppCurrency, customCode: String?) {
        viewModelScope.launch { container.settingsRepository.setCurrency(currency, customCode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { container.settingsRepository.setLanguage(language) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun setNotifyCriticalEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotifyCriticalEnabled(enabled) }
    }

    fun setNotifyImportantEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotifyImportantEnabled(enabled) }
    }

    fun setNotifyUpcomingEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotifyUpcomingEnabled(enabled) }
    }

    fun setNotifyDocumentCategoryEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotifyDocumentCategoryEnabled(enabled) }
    }

    fun setNotifyGeneralCategoryEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotifyGeneralCategoryEnabled(enabled) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch { container.settingsRepository.setPin(pin) }
    }

    fun disableAppLock() {
        viewModelScope.launch { container.settingsRepository.clearPin() }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setBiometricEnabled(enabled) }
    }

    fun setAppLockTimeoutSeconds(seconds: Int) {
        viewModelScope.launch { container.settingsRepository.setAppLockTimeoutSeconds(seconds) }
    }

    fun generateSampleData() {
        viewModelScope.launch { com.rovena.garage.utils.SampleDataGenerator.generate(container) }
    }

    /**
     * Deletes every vehicle via [com.rovena.garage.data.repository.VehicleRepository.deleteVehicle],
     * reusing its already-transactional cascade (child records) + best-effort file cleanup
     * (photos/documents/receipts) rather than a raw Room clearAllTables() - which would also
     * wipe the app_settings table (theme/language/PIN/etc.), not just garage content.
     */
    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            container.vehicleRepository.getAllOnce().forEach { vehicle ->
                container.vehicleRepository.deleteVehicle(vehicle)
            }
            onComplete()
        }
    }
}
