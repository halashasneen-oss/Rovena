package com.rovena.garage.presentation.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.HealthStatus
import com.rovena.garage.utils.HealthInputsBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VehicleCardUi(val vehicle: VehicleEntity, val healthScore: Int?, val healthStatus: HealthStatus)

data class GarageUiState(val vehicles: List<VehicleCardUi> = emptyList(), val isLoading: Boolean = true)

class GarageViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<GarageUiState> = container.vehicleRepository.observeAll()
        .flatMapLatest { vehicles ->
            if (vehicles.isEmpty()) return@flatMapLatest flowOf(GarageUiState(emptyList(), false))
            val perVehicleFlows = vehicles.map { vehicle ->
                combine(
                    container.maintenanceRepository.observeByVehicle(vehicle.id),
                    container.documentRepository.observeByVehicle(vehicle.id),
                    container.inspectionRepository.observeLatestConditionScores(vehicle.id)
                ) { maintenance, documents, conditionScores ->
                    val result = HealthInputsBuilder.calculate(vehicle, maintenance, documents, conditionScores)
                    VehicleCardUi(vehicle, result.score, result.status)
                }
            }
            combine(perVehicleFlows) { cards -> GarageUiState(cards.toList(), false) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GarageUiState())

    fun setPrimary(vehicleId: Long) {
        viewModelScope.launch { container.vehicleRepository.setPrimary(vehicleId) }
    }

    fun delete(vehicle: VehicleEntity) {
        viewModelScope.launch { container.vehicleRepository.deleteVehicle(vehicle) }
    }
}
