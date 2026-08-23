package com.rovena.garage.presentation.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.PhotoLinkedType
import com.rovena.garage.domain.usecase.InputValidator
import com.rovena.garage.utils.EnumLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MaintenanceFormState(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val dateMillis: Long = System.currentTimeMillis(),
    val mileage: String = "",
    val category: MaintenanceCategory = MaintenanceCategory.ENGINE_OIL,
    val description: String = "",
    val cost: String = "",
    val currencyCode: String? = null,
    val parts: String = "",
    val workshop: String = "",
    val technician: String = "",
    val notes: String = "",
    val nextDueMileage: String = "",
    val nextDueDateMillis: Long? = null,
    val photoPaths: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errors: Map<String, Int> = emptyMap()
)

class MaintenanceFormViewModel(
    private val container: AppContainer,
    private val vehicleId: Long,
    private val recordId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(MaintenanceFormState(vehicleId = vehicleId, isLoading = recordId != 0L))
    val state: StateFlow<MaintenanceFormState> = _state.asStateFlow()

    init {
        if (recordId != 0L) {
            viewModelScope.launch {
                val record = container.maintenanceRepository.getById(recordId)
                val photos = container.photoRepository.getByLinkOnce(PhotoLinkedType.MAINTENANCE, recordId).map { it.filePath }
                if (record != null) {
                    _state.value = MaintenanceFormState(
                        id = record.id,
                        vehicleId = record.vehicleId,
                        dateMillis = record.dateMillis,
                        mileage = record.mileageKm.toString(),
                        category = record.category,
                        description = record.description,
                        cost = record.cost?.toString().orEmpty(),
                        currencyCode = record.currencyCode,
                        parts = record.parts.orEmpty(),
                        workshop = record.workshop.orEmpty(),
                        technician = record.technician.orEmpty(),
                        notes = record.notes.orEmpty(),
                        nextDueMileage = record.nextDueMileageKm?.toString().orEmpty(),
                        nextDueDateMillis = record.nextDueDateMillis,
                        photoPaths = photos,
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false)
                }
            }
        } else {
            viewModelScope.launch {
                val settings = container.settingsRepository.getOrDefault()
                _state.value = _state.value.copy(
                    currencyCode = EnumLabels.effectiveCurrencyCode(settings.currency, settings.customCurrencyCode),
                    isLoading = false
                )
            }
        }
    }

    fun update(transform: (MaintenanceFormState) -> MaintenanceFormState) {
        _state.value = transform(_state.value)
    }

    fun save() {
        val s = _state.value
        val mileage = s.mileage.toIntOrNull()
        val errors = mutableMapOf<String, Int>()
        InputValidator.mileageKm(mileage)?.let { errors["mileage"] = EnumLabels.of(it) }
        InputValidator.requiredText(s.description)?.let { errors["description"] = EnumLabels.of(it) }
        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            return
        }

        viewModelScope.launch {
            val entity = MaintenanceRecordEntity(
                id = s.id,
                vehicleId = s.vehicleId,
                dateMillis = s.dateMillis,
                mileageKm = mileage!!,
                category = s.category,
                description = s.description.trim(),
                cost = s.cost.toDoubleOrNull(),
                currencyCode = s.currencyCode,
                parts = s.parts.trim().ifBlank { null },
                workshop = s.workshop.trim().ifBlank { null },
                technician = s.technician.trim().ifBlank { null },
                notes = s.notes.trim().ifBlank { null },
                nextDueMileageKm = s.nextDueMileage.toIntOrNull(),
                nextDueDateMillis = s.nextDueDateMillis
            )
            val savedId = container.maintenanceRepository.addOrUpdate(entity)
            container.photoRepository.deleteAllForLink(PhotoLinkedType.MAINTENANCE, savedId)
            s.photoPaths.forEach { path ->
                container.photoRepository.add(
                    com.rovena.garage.data.local.entities.VehiclePhotoEntity(
                        vehicleId = s.vehicleId,
                        linkedType = PhotoLinkedType.MAINTENANCE,
                        linkedId = savedId,
                        filePath = path
                    )
                )
            }
            _state.value = _state.value.copy(isSaved = true, errors = emptyMap())
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) return
        viewModelScope.launch {
            container.maintenanceRepository.getById(s.id)?.let { container.maintenanceRepository.delete(it) }
            container.photoRepository.deleteAllForLink(PhotoLinkedType.MAINTENANCE, s.id)
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
