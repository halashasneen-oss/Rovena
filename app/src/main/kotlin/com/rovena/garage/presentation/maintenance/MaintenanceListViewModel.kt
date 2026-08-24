package com.rovena.garage.presentation.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.usecase.CurrencyAggregator
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
    /** Never a blind sum across currencies (spec: multi-currency analytics). */
    val totalCost: CurrencyAggregator.CurrencyTotal = CurrencyAggregator.CurrencyTotal.Empty,
    val filter: MaintenanceCategory? = null,
    val isLoading: Boolean = true
)

class MaintenanceListViewModel(private val container: AppContainer, vehicleIdFlow: kotlinx.coroutines.flow.Flow<Long?>) : ViewModel() {

    private val filterFlow = MutableStateFlow<MaintenanceCategory?>(null)

    val uiState: StateFlow<MaintenanceListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(MaintenanceListUiState(isLoading = false))
        combine(
            container.maintenanceRepository.observeByVehicle(vehicleId),
            container.vehicleRepository.observeById(vehicleId),
            filterFlow
        ) { allRecords, vehicle, filter ->
            val records = if (filter == null) allRecords else allRecords.filter { it.category == filter }
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
            val total = CurrencyAggregator.aggregate(records.mapNotNull { r -> r.cost?.let { it to (r.currencyCode ?: "JOD") } })
            MaintenanceListUiState(vehicleId, rows, total, filter, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MaintenanceListUiState())

    fun setFilter(category: MaintenanceCategory?) {
        filterFlow.value = category
    }

    fun delete(record: MaintenanceRecordEntity) {
        viewModelScope.launch { container.maintenanceRepository.delete(record) }
    }
}
