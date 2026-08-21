package com.rovena.garage.presentation.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.HealthStatus
import com.rovena.garage.domain.usecase.HealthScoreCalculator
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
                    container.documentRepository.observeByVehicle(vehicle.id)
                ) { maintenance, documents ->
                    val overdue = maintenance.count {
                        (it.nextDueMileageKm != null && it.nextDueMileageKm <= vehicle.currentMileageKm) ||
                            (it.nextDueDateMillis != null && it.nextDueDateMillis <= System.currentTimeMillis())
                    }
                    val tracked = maintenance.count { it.nextDueMileageKm != null || it.nextDueDateMillis != null }
                    val lastMaintenance = maintenance.maxOfOrNull { it.dateMillis }
                    val days = lastMaintenance?.let { ((System.currentTimeMillis() - it) / 86_400_000L).toInt() }
                    val hasExpired = documents.any { it.expiryDateMillis != null && it.expiryDateMillis < System.currentTimeMillis() }

                    val result = HealthScoreCalculator.fromVehicleInputs(
                        HealthScoreCalculator.VehicleHealthInputs(
                            daysSinceLastMaintenance = days,
                            overdueMaintenanceCount = if (tracked > 0) overdue else null,
                            totalActiveMaintenanceItems = if (tracked > 0) tracked else null,
                            brakesConditionScore = null,
                            tiresConditionScore = null,
                            batteryConditionScore = null,
                            fluidsConditionScore = null,
                            engineServiceUpToDate = null,
                            transmissionServiceUpToDate = null,
                            hasExpiredDocument = if (documents.isNotEmpty()) hasExpired else null,
                            hasAnyTrackedDocument = documents.isNotEmpty()
                        )
                    )
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
