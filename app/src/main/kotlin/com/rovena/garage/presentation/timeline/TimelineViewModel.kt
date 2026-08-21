package com.rovena.garage.presentation.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.domain.model.TimelineEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class TimelineUiState(
    val vehicleId: Long? = null,
    val events: List<TimelineEventEntity> = emptyList(),
    val filter: TimelineEventType? = null,
    val isLoading: Boolean = true
)

class TimelineViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    private val filterFlow = MutableStateFlow<TimelineEventType?>(null)

    val uiState: StateFlow<TimelineUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(TimelineUiState(isLoading = false))
        combine(container.timelineRepository.observeByVehicle(vehicleId), filterFlow) { events, filter ->
            val filtered = if (filter == null) events else events.filter { it.type == filter }
            TimelineUiState(vehicleId, filtered, filter, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState())

    fun setFilter(type: TimelineEventType?) {
        filterFlow.value = type
    }
}
