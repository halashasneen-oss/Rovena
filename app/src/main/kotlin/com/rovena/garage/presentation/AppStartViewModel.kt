package com.rovena.garage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartDestination { ONBOARDING, MAIN }

/**
 * Decides, once, whether MainActivity should show onboarding or the app
 * itself. Whether the PIN/biometric lock screen is also required is a
 * separate, ongoing check (see `RovenaApp.requiresReauth`) since it must be
 * re-evaluated every time the app returns to the foreground, not just once.
 */
class AppStartViewModel(container: AppContainer) : ViewModel() {

    private val _destination = MutableStateFlow<StartDestination?>(null)
    val destination: StateFlow<StartDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = container.settingsRepository.getOrDefault()
            _destination.value = if (!settings.onboardingCompleted) StartDestination.ONBOARDING else StartDestination.MAIN
        }
    }
}
