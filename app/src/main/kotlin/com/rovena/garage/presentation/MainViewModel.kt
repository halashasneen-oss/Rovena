package com.rovena.garage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.VehicleEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Activity-scoped: tracks which vehicle is "current" across every bottom-nav
 * tab (spec #36 - "the current vehicle must remain easily accessible").
 * Persisted via [AppContainer.userPreferences] (DataStore) so it survives
 * process death without touching the Room settings table.
 */
class MainViewModel(private val container: AppContainer) : ViewModel() {

    val vehicles: StateFlow<List<VehicleEntity>> = container.vehicleRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Eagerly shared (not WhileSubscribed) because MainActivity reads `.value` directly for the
    // quick-add FAB without collecting - it must stay live even with zero active collectors.
    val currentVehicle: StateFlow<VehicleEntity?> = combine(
        container.userPreferences.currentVehicleId.distinctUntilChanged(),
        vehicles
    ) { savedId, allVehicles ->
        when {
            allVehicles.isEmpty() -> null
            savedId != null -> allVehicles.find { it.id == savedId } ?: allVehicles.find { it.isPrimary } ?: allVehicles.first()
            else -> allVehicles.find { it.isPrimary } ?: allVehicles.first()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectVehicle(vehicleId: Long) {
        viewModelScope.launch { container.userPreferences.setCurrentVehicleId(vehicleId) }
    }
}
