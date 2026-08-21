package com.rovena.garage.presentation.inspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.InspectionItemEntity
import com.rovena.garage.domain.model.InspectionCategoryGroup
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.InspectionItemStatus
import com.rovena.garage.domain.usecase.InspectionScoreCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InspectionItemDraft(
    val categoryGroup: InspectionCategoryGroup,
    val itemKey: InspectionItemKey,
    val status: InspectionItemStatus = InspectionItemStatus.UNKNOWN,
    val notes: String = "",
    val estimatedRepairCost: String = ""
)

data class InspectionFormState(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val dateMillis: Long = System.currentTimeMillis(),
    val mileage: String = "",
    val notes: String = "",
    val items: List<InspectionItemDraft> = defaultItems(),
    val liveScoreResult: InspectionScoreCalculator.Result = InspectionScoreCalculator.calculate(emptyList()),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val savedInspectionId: Long = 0,
    val errors: Map<String, Int> = emptyMap()
) {
    companion object {
        fun defaultItems(): List<InspectionItemDraft> = buildList {
            InspectionCategoryGroup.EXTERIOR.let { g ->
                listOf(
                    InspectionItemKey.PAINT, InspectionItemKey.BODY, InspectionItemKey.DOORS, InspectionItemKey.EXT_WINDOWS,
                    InspectionItemKey.LIGHTS, InspectionItemKey.MIRRORS, InspectionItemKey.TIRES, InspectionItemKey.WHEELS,
                    InspectionItemKey.CHASSIS
                ).forEach { add(InspectionItemDraft(g, it)) }
            }
            InspectionCategoryGroup.INTERIOR.let { g ->
                listOf(
                    InspectionItemKey.SEATS, InspectionItemKey.DASHBOARD, InspectionItemKey.INT_AC, InspectionItemKey.INT_WINDOWS,
                    InspectionItemKey.AUDIO, InspectionItemKey.ELECTRONICS, InspectionItemKey.INTERIOR_CONDITION
                ).forEach { add(InspectionItemDraft(g, it)) }
            }
            InspectionCategoryGroup.MECHANICAL.let { g ->
                listOf(
                    InspectionItemKey.ENGINE, InspectionItemKey.TRANSMISSION, InspectionItemKey.BRAKES, InspectionItemKey.SUSPENSION,
                    InspectionItemKey.STEERING, InspectionItemKey.COOLING, InspectionItemKey.FLUIDS, InspectionItemKey.BATTERY
                ).forEach { add(InspectionItemDraft(g, it)) }
            }
        }
    }
}

class InspectionFormViewModel(private val container: AppContainer, private val vehicleId: Long, private val recordId: Long) : ViewModel() {

    private val _state = MutableStateFlow(InspectionFormState(vehicleId = vehicleId, isLoading = true))
    val state: StateFlow<InspectionFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (recordId != 0L) {
                val inspection = container.inspectionRepository.getById(recordId)
                val savedItems = container.inspectionRepository.getItemsOnce(recordId)
                if (inspection != null) {
                    val merged = InspectionFormState.defaultItems().map { default ->
                        savedItems.find { it.itemKey == default.itemKey }?.let { saved ->
                            default.copy(
                                status = saved.status,
                                notes = saved.notes.orEmpty(),
                                estimatedRepairCost = saved.estimatedRepairCost?.toString().orEmpty()
                            )
                        } ?: default
                    }
                    _state.value = InspectionFormState(
                        id = inspection.id, vehicleId = inspection.vehicleId, dateMillis = inspection.dateMillis,
                        mileage = inspection.mileageKm.toString(), notes = inspection.notes.orEmpty(),
                        items = merged, liveScoreResult = InspectionScoreCalculator.calculate(merged.map { it.status }),
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false)
                }
            } else {
                val vehicle = container.vehicleRepository.getById(vehicleId)
                _state.value = _state.value.copy(mileage = vehicle?.currentMileageKm?.toString().orEmpty(), isLoading = false)
            }
        }
    }

    fun update(transform: (InspectionFormState) -> InspectionFormState) {
        _state.value = transform(_state.value)
    }

    fun updateItem(itemKey: InspectionItemKey, transform: (InspectionItemDraft) -> InspectionItemDraft) {
        val current = _state.value
        val newItems = current.items.map { if (it.itemKey == itemKey) transform(it) else it }
        _state.value = current.copy(items = newItems, liveScoreResult = InspectionScoreCalculator.calculate(newItems.map { it.status }))
    }

    fun save() {
        val s = _state.value
        val mileage = s.mileage.toIntOrNull()
        if (mileage == null || mileage < 0) {
            _state.value = s.copy(errors = mapOf("mileage" to com.rovena.garage.R.string.error_invalid_mileage))
            return
        }
        viewModelScope.launch {
            val inspection = InspectionEntity(
                id = s.id, vehicleId = s.vehicleId, dateMillis = s.dateMillis, mileageKm = mileage,
                notes = s.notes.trim().ifBlank { null }
            )
            val itemEntities = s.items.map {
                InspectionItemEntity(
                    inspectionId = 0, categoryGroup = it.categoryGroup, itemKey = it.itemKey, status = it.status,
                    notes = it.notes.trim().ifBlank { null }, estimatedRepairCost = it.estimatedRepairCost.toDoubleOrNull()
                )
            }
            val savedId = container.inspectionRepository.saveInspection(inspection, itemEntities)
            _state.value = _state.value.copy(isSaved = true, savedInspectionId = savedId, errors = emptyMap())
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) return
        viewModelScope.launch {
            container.inspectionRepository.getById(s.id)?.let { container.inspectionRepository.delete(it) }
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
