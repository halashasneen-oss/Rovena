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

    fun setPin(pin: String) {
        viewModelScope.launch { container.settingsRepository.setPin(pin) }
    }

    fun disableAppLock() {
        viewModelScope.launch { container.settingsRepository.clearPin() }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setBiometricEnabled(enabled) }
    }

    fun generateSampleData() {
        viewModelScope.launch { com.rovena.garage.utils.SampleDataGenerator.generate(container) }
    }
}
