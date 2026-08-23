package com.rovena.garage.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.VehicleNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VehicleNoteListUiState(
    val vehicleId: Long? = null,
    val notes: List<VehicleNoteEntity> = emptyList(),
    val isLoading: Boolean = true
)

class VehicleNoteListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<VehicleNoteListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(VehicleNoteListUiState(isLoading = false))
        container.vehicleNoteRepository.observeByVehicle(vehicleId)
            .map { notes -> VehicleNoteListUiState(vehicleId, notes, false) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VehicleNoteListUiState())

    fun save(vehicleId: Long, existing: VehicleNoteEntity?, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            container.vehicleNoteRepository.addOrUpdate(
                existing?.copy(text = trimmed) ?: VehicleNoteEntity(vehicleId = vehicleId, text = trimmed)
            )
        }
    }

    fun delete(note: VehicleNoteEntity) {
        viewModelScope.launch { container.vehicleNoteRepository.delete(note) }
    }
}
