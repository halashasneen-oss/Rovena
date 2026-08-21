package com.rovena.garage.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.domain.usecase.DueStatusCalculator
import kotlinx.coroutines.flow.Flow
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

data class ReminderRowUi(val reminder: ReminderEntity, val evaluation: DueStatusCalculator.Evaluation?)

data class ReminderListUiState(
    val vehicleId: Long? = null,
    val rows: List<ReminderRowUi> = emptyList(),
    val isLoading: Boolean = true
)

class ReminderListViewModel(private val container: AppContainer, vehicleIdFlow: Flow<Long?>) : ViewModel() {

    val uiState: StateFlow<ReminderListUiState> = vehicleIdFlow.flatMapLatest { vehicleId ->
        if (vehicleId == null) return@flatMapLatest flowOf(ReminderListUiState(isLoading = false))
        combine(
            container.reminderRepository.observeByVehicle(vehicleId),
            container.vehicleRepository.observeById(vehicleId)
        ) { reminders, vehicle ->
            val today = LocalDate.now()
            val rows = reminders.map { r ->
                val eval = if (vehicle != null) {
                    DueStatusCalculator.evaluate(
                        currentMileageKm = vehicle.currentMileageKm,
                        today = today,
                        dueMileageKm = r.dueMileageKm,
                        dueDate = r.dueDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    )
                } else null
                ReminderRowUi(r, eval)
            }.sortedWith(compareBy({ !it.reminder.isActive || it.reminder.isCompleted }, { it.evaluation?.status?.ordinal ?: 99 }))
            ReminderListUiState(vehicleId, rows, false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderListUiState())

    fun markCompleted(reminder: ReminderEntity) {
        viewModelScope.launch { container.reminderRepository.markCompleted(reminder) }
    }

    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch { container.reminderRepository.delete(reminder) }
    }
}
