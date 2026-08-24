package com.rovena.garage.presentation.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.domain.usecase.FuelStatsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FuelListUiState(
    val vehicleId: Long? = null,
    val records: List<FuelRecordEntity> = emptyList(),
    val stats: FuelStatsCalculator.FuelStats? = null,
    val isLoading: Boolean = true
)

class FuelListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<FuelListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(FuelListUiState(isLoading = false))
        container.fuelRepository.observeByVehicle(vehicleId).map { records ->
            FuelListUiState(vehicleId, records, computeStats(records), false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FuelListUiState())

    private fun computeStats(records: List<FuelRecordEntity>): FuelStatsCalculator.FuelStats {
        val entries = records.sortedBy { it.mileageKm }.map {
            FuelStatsCalculator.FuelEntry(
                date = java.time.Instant.ofEpochMilli(it.dateMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                odometerKm = it.mileageKm,
                liters = it.liters,
                totalCost = it.totalCost,
                isFullTank = it.isFullTank,
                currencyCode = it.currencyCode ?: "JOD"
            )
        }
        return FuelStatsCalculator.compute(entries)
    }

    fun delete(record: FuelRecordEntity) {
        viewModelScope.launch { container.fuelRepository.delete(record) }
    }
}
