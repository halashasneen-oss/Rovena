package com.rovena.garage.presentation.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.DocumentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DocumentListUiState(
    val vehicleId: Long? = null,
    val documents: List<DocumentEntity> = emptyList(),
    val isLoading: Boolean = true
)

class DocumentListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<DocumentListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(DocumentListUiState(isLoading = false))
        container.documentRepository.observeByVehicle(vehicleId).map { docs -> DocumentListUiState(vehicleId, docs, false) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DocumentListUiState())

    fun delete(document: DocumentEntity) {
        viewModelScope.launch { container.documentRepository.delete(document) }
    }
}
