package com.rovena.garage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartDestination { ONBOARDING, LOCK, MAIN }

/** Decides, once, what MainActivity should show first: onboarding, the PIN/biometric lock, or the app itself. */
class AppStartViewModel(container: AppContainer) : ViewModel() {

    private val _destination = MutableStateFlow<StartDestination?>(null)
    val destination: StateFlow<StartDestination?> = _destination.asStateFlow()

    /** Set true once the lock screen has been passed this process lifetime, so backgrounding briefly doesn't re-lock every resume. */
    var unlockedThisSession: Boolean = false

    init {
        viewModelScope.launch {
            val settings = container.settingsRepository.getOrDefault()
            _destination.value = when {
                !settings.onboardingCompleted -> StartDestination.ONBOARDING
                settings.appLockEnabled && !unlockedThisSession -> StartDestination.LOCK
                else -> StartDestination.MAIN
            }
        }
    }
}
