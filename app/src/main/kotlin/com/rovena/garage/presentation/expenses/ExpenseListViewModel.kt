package com.rovena.garage.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.usecase.CurrencyAggregator
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
    /** Never a blind sum across currencies (spec: multi-currency analytics). */
    val total: CurrencyAggregator.CurrencyTotal = CurrencyAggregator.CurrencyTotal.Empty,
    val filter: ExpenseCategory? = null,
    val isLoading: Boolean = true
)

class ExpenseListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    private val filterFlow = MutableStateFlow<ExpenseCategory?>(null)

    val uiState: StateFlow<ExpenseListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(ExpenseListUiState(isLoading = false))
        combine(container.expenseRepository.observeByVehicle(vehicleId), filterFlow) { allRecords, filter ->
            val records = if (filter == null) allRecords else allRecords.filter { it.category == filter }
            val total = CurrencyAggregator.aggregate(records.map { it.amount to (it.currencyCode ?: "JOD") })
            ExpenseListUiState(vehicleId, records, total, filter, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseListUiState())

    fun setFilter(category: ExpenseCategory?) {
        filterFlow.value = category
    }

    fun delete(record: ExpenseEntity) {
        viewModelScope.launch { container.expenseRepository.delete(record) }
    }
}
