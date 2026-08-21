package com.rovena.garage.presentation.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.usecase.DueStatusCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MaintenanceRowUi(val record: MaintenanceRecordEntity, val dueStatus: DueStatus?)

data class MaintenanceListUiState(
    val vehicleId: Long? = null,
    val rows: List<MaintenanceRowUi> = emptyList(),
    val totalCost: Double = 0.0,
    val isLoading: Boolean = true
)

class MaintenanceListViewModel(private val container: AppContainer, vehicleIdFlow: kotlinx.coroutines.flow.Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<MaintenanceListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(MaintenanceListUiState(isLoading = false))
        combine(
            container.maintenanceRepository.observeByVehicle(vehicleId),
            container.vehicleRepository.observeById(vehicleId),
            container.maintenanceRepository.observeTotalCost(vehicleId)
        ) { records, vehicle, total ->
            val today = LocalDate.now()
            val rows = records.map { record ->
                val status = if (vehicle != null && (record.nextDueMileageKm != null || record.nextDueDateMillis != null)) {
                    DueStatusCalculator.evaluate(
                        currentMileageKm = vehicle.currentMileageKm,
                        today = today,
                        dueMileageKm = record.nextDueMileageKm,
                        dueDate = record.nextDueDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    )?.status
                } else null
                MaintenanceRowUi(record, status)
            }
            MaintenanceListUiState(vehicleId, rows, total ?: 0.0, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MaintenanceListUiState())

    fun delete(record: MaintenanceRecordEntity) {
        viewModelScope.launch { container.maintenanceRepository.delete(record) }
    }
}
