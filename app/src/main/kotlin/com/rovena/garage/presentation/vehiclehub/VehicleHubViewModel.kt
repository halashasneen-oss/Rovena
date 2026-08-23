package com.rovena.garage.presentation.vehiclehub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.HealthScoreSnapshotEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.HealthCategory
import com.rovena.garage.domain.model.HealthStatus
import com.rovena.garage.utils.HealthInputsBuilder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    /** Recorded Health Scores, oldest first, at most one per calendar day - see HealthScoreHistoryRepository. */
    val healthHistory: List<Int> = emptyList(),
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
                container.inspectionRepository.observeLatestConditionScores(id),
                container.healthScoreHistoryRepository.observeByVehicle(id)
            ) { reminderCount, maintenance, documents, conditionScores, history ->
                InnerPartial(reminderCount, maintenance, documents, conditionScores, history)
            }
            combine(
                innerPartial,
                container.vehicleNoteRepository.observeCount(id),
                container.partRepository.observeCount(id)
            ) { inner, noteCount, partCount ->
                val (reminderCount, maintenance, documents, conditionScores, history) = inner
                val vehicle = partial.vehicle ?: return@combine VehicleHubUiState(isLoading = false)
                val hasExpired = documents.any { it.expiryDateMillis != null && it.expiryDateMillis < System.currentTimeMillis() }
                val health = HealthInputsBuilder.calculate(vehicle, maintenance, documents, conditionScores)
                health.score?.let { score ->
                    viewModelScope.launch { container.healthScoreHistoryRepository.recordToday(vehicle.id, score) }
                }

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
                    healthHistory = history.map { it.score },
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
        val conditionScores: com.rovena.garage.data.repository.InspectionConditionScores,
        val history: List<HealthScoreSnapshotEntity>
    )
}
