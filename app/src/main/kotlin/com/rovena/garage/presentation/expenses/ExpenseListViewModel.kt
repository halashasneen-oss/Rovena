package com.rovena.garage.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseListUiState(
    val vehicleId: Long? = null,
    val records: List<ExpenseEntity> = emptyList(),
    val total: Double = 0.0,
    val isLoading: Boolean = true
)

class ExpenseListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<ExpenseListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(ExpenseListUiState(isLoading = false))
        combine(
            container.expenseRepository.observeByVehicle(vehicleId),
            container.expenseRepository.observeTotal(vehicleId)
        ) { records, total -> ExpenseListUiState(vehicleId, records, total ?: 0.0, false) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseListUiState())

    fun delete(record: ExpenseEntity) {
        viewModelScope.launch { container.expenseRepository.delete(record) }
    }
}
