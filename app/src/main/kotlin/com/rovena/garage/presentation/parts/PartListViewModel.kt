package com.rovena.garage.presentation.parts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.PartEntity
import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.usecase.DueStatusCalculator
import kotlinx.coroutines.flow.Flow
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

data class PartRowUi(val part: PartEntity, val warrantyStatus: DueStatus?)

data class PartListUiState(
    val vehicleId: Long? = null,
    val rows: List<PartRowUi> = emptyList(),
    val isLoading: Boolean = true
)

class PartListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<PartListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(PartListUiState(isLoading = false))
        combine(
            container.partRepository.observeByVehicle(vehicleId),
            container.vehicleRepository.observeById(vehicleId)
        ) { parts, vehicle ->
            val today = LocalDate.now()
            val rows = parts.map { part ->
                val status = if (vehicle != null && (part.warrantyExpiryDateMillis != null || part.warrantyExpiryMileageKm != null)) {
                    DueStatusCalculator.evaluate(
                        currentMileageKm = vehicle.currentMileageKm,
                        today = today,
                        dueMileageKm = part.warrantyExpiryMileageKm,
                        dueDate = part.warrantyExpiryDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    )?.status
                } else null
                PartRowUi(part, status)
            }
            PartListUiState(vehicleId, rows, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PartListUiState())

    fun save(vehicleId: Long, existing: PartEntity?, part: PartEntity) {
        viewModelScope.launch {
            container.partRepository.addOrUpdate(part.copy(id = existing?.id ?: 0, vehicleId = vehicleId, reminderId = existing?.reminderId))
        }
    }

    fun delete(part: PartEntity) {
        viewModelScope.launch { container.partRepository.delete(part) }
    }
}
