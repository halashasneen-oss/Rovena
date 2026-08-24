package com.rovena.garage.presentation.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.domain.model.DocumentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DocumentListUiState(
    val vehicleId: Long? = null,
    val documents: List<DocumentEntity> = emptyList(),
    val filter: DocumentType? = null,
    val isLoading: Boolean = true
)

class DocumentListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    private val filterFlow = MutableStateFlow<DocumentType?>(null)

    val uiState: StateFlow<DocumentListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(DocumentListUiState(isLoading = false))
        combine(container.documentRepository.observeByVehicle(vehicleId), filterFlow) { allDocs, filter ->
            val docs = if (filter == null) allDocs else allDocs.filter { it.type == filter }
            DocumentListUiState(vehicleId, docs, filter, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DocumentListUiState())

    fun setFilter(type: DocumentType?) {
        filterFlow.value = type
    }

    fun delete(document: DocumentEntity) {
        viewModelScope.launch { container.documentRepository.delete(document) }
    }
}
