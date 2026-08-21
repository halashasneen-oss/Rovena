package com.rovena.garage.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.domain.model.ReminderBasis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReminderFormState(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val title: String = "",
    val basis: ReminderBasis = ReminderBasis.MILEAGE,
    val dueMileage: String = "",
    val dueDateMillis: Long? = null,
    val intervalKm: String = "",
    val intervalMonths: String = "",
    val isRecurring: Boolean = false,
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errors: Map<String, Int> = emptyMap()
)

class ReminderFormViewModel(private val container: AppContainer, private val vehicleId: Long, private val recordId: Long) : ViewModel() {

    private val _state = MutableStateFlow(ReminderFormState(vehicleId = vehicleId, isLoading = recordId != 0L))
    val state: StateFlow<ReminderFormState> = _state.asStateFlow()

    init {
        if (recordId != 0L) {
            viewModelScope.launch {
                container.reminderRepository.getById(recordId)?.let { r ->
                    _state.value = ReminderFormState(
                        id = r.id, vehicleId = r.vehicleId, title = r.title, basis = r.basis,
                        dueMileage = r.dueMileageKm?.toString().orEmpty(), dueDateMillis = r.dueDateMillis,
                        intervalKm = r.intervalKm?.toString().orEmpty(), intervalMonths = r.intervalMonths?.toString().orEmpty(),
                        isRecurring = r.isRecurring, notes = r.notes.orEmpty(), isLoading = false
                    )
                } ?: run { _state.value = _state.value.copy(isLoading = false) }
            }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun update(transform: (ReminderFormState) -> ReminderFormState) {
        _state.value = transform(_state.value)
    }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, Int>()
        if (s.title.isBlank()) errors["title"] = R.string.error_required
        val dueMileage = s.dueMileage.toIntOrNull()
        val hasMileageTrigger = s.basis != ReminderBasis.DATE && dueMileage != null
        val hasDateTrigger = s.basis != ReminderBasis.MILEAGE && s.dueDateMillis != null
        if (!hasMileageTrigger && !hasDateTrigger) errors["trigger"] = R.string.error_reminder_trigger_required
        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            return
        }
        viewModelScope.launch {
            val entity = ReminderEntity(
                id = s.id, vehicleId = s.vehicleId, title = s.title.trim(), basis = s.basis,
                intervalKm = s.intervalKm.toIntOrNull(), intervalMonths = s.intervalMonths.toIntOrNull(),
                dueMileageKm = if (s.basis != ReminderBasis.DATE) dueMileage else null,
                dueDateMillis = if (s.basis != ReminderBasis.MILEAGE) s.dueDateMillis else null,
                isRecurring = s.isRecurring, notes = s.notes.trim().ifBlank { null }
            )
            container.reminderRepository.addOrUpdate(entity)
            _state.value = _state.value.copy(isSaved = true, errors = emptyMap())
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) return
        viewModelScope.launch {
            container.reminderRepository.getById(s.id)?.let { container.reminderRepository.delete(it) }
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
