package com.rovena.garage.presentation.inspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.InspectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InspectionListUiState(
    val vehicleId: Long? = null,
    val inspections: List<InspectionEntity> = emptyList(),
    val isLoading: Boolean = true
)

class InspectionListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<InspectionListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(InspectionListUiState(isLoading = false))
        container.inspectionRepository.observeByVehicle(vehicleId).map { list -> InspectionListUiState(vehicleId, list, false) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InspectionListUiState())

    fun delete(inspection: InspectionEntity) {
        viewModelScope.launch { container.inspectionRepository.delete(inspection) }
    }
}
