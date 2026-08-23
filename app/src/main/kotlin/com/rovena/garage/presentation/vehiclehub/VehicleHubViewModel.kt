package com.rovena.garage.presentation.vehiclehub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.HealthCategory
import com.rovena.garage.domain.model.HealthStatus
import com.rovena.garage.domain.usecase.HealthScoreCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class VehicleHubUiState(
    val vehicle: VehicleEntity? = null,
    val healthScore: Int? = null,
    val healthStatus: HealthStatus = HealthStatus.NOT_ENOUGH_DATA,
    val healthCategoryBreakdown: Map<HealthCategory, Int?> = emptyMap(),
    val healthKnownWeightRatio: Double = 0.0,
    val maintenanceCount: Int = 0,
    val fuelCount: Int = 0,
    val expenseCount: Int = 0,
    val documentCount: Int = 0,
    val hasExpiredDocument: Boolean = false,
    val reminderCount: Int = 0,
    val noteCount: Int = 0,
    val partCount: Int = 0,
    val isLoading: Boolean = true
)

class VehicleHubViewModel(private val container: AppContainer, argVehicleId: Long?) : ViewModel() {

    private val vehicleIdFlow = argVehicleId?.let { flowOf<Long?>(it) } ?: container.userPreferences.currentVehicleId

    val uiState: StateFlow<VehicleHubUiState> = vehicleIdFlow.flatMapLatest { id ->
        if (id == null) return@flatMapLatest flowOf(VehicleHubUiState(isLoading = false))
        combine(
            container.vehicleRepository.observeById(id),
            container.maintenanceRepository.observeCount(id),
            container.fuelRepository.observeCount(id),
            container.expenseRepository.observeCount(id),
            container.documentRepository.observeCount(id)
        ) { vehicle, maintenanceCount, fuelCount, expenseCount, documentCount ->
            HubPartial(vehicle, maintenanceCount, fuelCount, expenseCount, documentCount)
        }.flatMapLatest { partial ->
            val innerPartial = combine(
                container.reminderRepository.observeActiveCount(id),
                container.maintenanceRepository.observeByVehicle(id),
                container.documentRepository.observeByVehicle(id),
                container.inspectionRepository.observeLatestConditionScores(id)
            ) { reminderCount, maintenance, documents, conditionScores ->
                InnerPartial(reminderCount, maintenance, documents, conditionScores)
            }
            combine(
                innerPartial,
                container.vehicleNoteRepository.observeCount(id),
                container.partRepository.observeCount(id)
            ) { inner, noteCount, partCount ->
                val (reminderCount, maintenance, documents, conditionScores) = inner
                val vehicle = partial.vehicle ?: return@combine VehicleHubUiState(isLoading = false)
                val overdue = maintenance.count {
                    (it.nextDueMileageKm != null && it.nextDueMileageKm <= vehicle.currentMileageKm) ||
                        (it.nextDueDateMillis != null && it.nextDueDateMillis <= System.currentTimeMillis())
                }
                val tracked = maintenance.count { it.nextDueMileageKm != null || it.nextDueDateMillis != null }
                val lastDate = maintenance.maxOfOrNull { it.dateMillis }
                val days = lastDate?.let { ((System.currentTimeMillis() - it) / 86_400_000L).toInt() }
                val hasExpired = documents.any { it.expiryDateMillis != null && it.expiryDateMillis < System.currentTimeMillis() }

                val health = HealthScoreCalculator.fromVehicleInputs(
                    HealthScoreCalculator.VehicleHealthInputs(
                        daysSinceLastMaintenance = days,
                        overdueMaintenanceCount = if (tracked > 0) overdue else null,
                        totalActiveMaintenanceItems = if (tracked > 0) tracked else null,
                        brakesConditionScore = conditionScores.brakes,
                        tiresConditionScore = conditionScores.tires,
                        batteryConditionScore = conditionScores.battery,
                        fluidsConditionScore = conditionScores.fluids,
                        engineServiceUpToDate = null,
                        transmissionServiceUpToDate = null,
                        hasExpiredDocument = if (documents.isNotEmpty()) hasExpired else null,
                        hasAnyTrackedDocument = documents.isNotEmpty()
                    )
                )

                VehicleHubUiState(
                    vehicle = vehicle,
                    healthScore = health.score,
                    healthStatus = health.status,
                    healthCategoryBreakdown = health.categoryBreakdown,
                    healthKnownWeightRatio = health.knownWeightRatio,
                    maintenanceCount = partial.maintenanceCount,
                    fuelCount = partial.fuelCount,
                    expenseCount = partial.expenseCount,
                    documentCount = partial.documentCount,
                    hasExpiredDocument = hasExpired,
                    reminderCount = reminderCount,
                    noteCount = noteCount,
                    partCount = partCount,
                    isLoading = false
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VehicleHubUiState())

    private data class HubPartial(
        val vehicle: VehicleEntity?,
        val maintenanceCount: Int,
        val fuelCount: Int,
        val expenseCount: Int,
        val documentCount: Int
    )

    private data class InnerPartial(
        val reminderCount: Int,
        val maintenance: List<com.rovena.garage.data.local.entities.MaintenanceRecordEntity>,
        val documents: List<com.rovena.garage.data.local.entities.DocumentEntity>,
        val conditionScores: com.rovena.garage.data.repository.InspectionConditionScores
    )
}
