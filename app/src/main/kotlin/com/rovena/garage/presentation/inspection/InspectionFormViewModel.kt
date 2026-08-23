package com.rovena.garage.presentation.inspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.InspectionItemEntity
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.data.local.entities.VehiclePhotoEntity
import com.rovena.garage.domain.model.InspectionCategoryGroup
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.InspectionItemStatus
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.PhotoLinkedType
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.domain.usecase.InputValidator
import com.rovena.garage.domain.usecase.InspectionMaintenanceSuggester
import com.rovena.garage.domain.usecase.InspectionScoreCalculator
import com.rovena.garage.utils.EnumLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One PROBLEM inspection item with a suggested follow-up maintenance category - see InspectionMaintenanceSuggester. */
data class MaintenanceSuggestion(val itemKey: InspectionItemKey, val category: MaintenanceCategory)

data class InspectionItemDraft(
    val categoryGroup: InspectionCategoryGroup,
    val itemKey: InspectionItemKey,
    val status: InspectionItemStatus = InspectionItemStatus.UNKNOWN,
    val notes: String = "",
    val estimatedRepairCost: String = "",
    val currencyCode: String? = null,
    val photoPaths: List<String> = emptyList()
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
    /** PROBLEM items from the save that just happened, mapped to a suggested maintenance category - populated only on save(), never on load. */
    val maintenanceSuggestions: List<MaintenanceSuggestion> = emptyList(),
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
                                estimatedRepairCost = saved.estimatedRepairCost?.toString().orEmpty(),
                                currencyCode = saved.currencyCode,
                                photoPaths = container.photoRepository.getByLinkOnce(PhotoLinkedType.INSPECTION_ITEM, saved.id).map { it.filePath }
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
                val settings = container.settingsRepository.getOrDefault()
                val defaultCurrency = EnumLabels.effectiveCurrencyCode(settings.currency, settings.customCurrencyCode)
                _state.value = _state.value.copy(
                    mileage = vehicle?.currentMileageKm?.toString().orEmpty(),
                    items = _state.value.items.map { it.copy(currencyCode = defaultCurrency) },
                    isLoading = false
                )
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
        InputValidator.mileageKm(mileage)?.let {
            _state.value = s.copy(errors = mapOf("mileage" to EnumLabels.of(it)))
            return
        }
        viewModelScope.launch {
            val inspection = InspectionEntity(
                id = s.id, vehicleId = s.vehicleId, dateMillis = s.dateMillis, mileageKm = mileage!!,
                notes = s.notes.trim().ifBlank { null }
            )
            val itemEntities = s.items.map {
                InspectionItemEntity(
                    inspectionId = 0, categoryGroup = it.categoryGroup, itemKey = it.itemKey, status = it.status,
                    notes = it.notes.trim().ifBlank { null }, estimatedRepairCost = it.estimatedRepairCost.toDoubleOrNull(),
                    currencyCode = it.currencyCode
                )
            }
            val savedId = container.inspectionRepository.saveInspection(inspection, itemEntities)

            // Items are upserted by itemKey (stable ids across edits - see
            // InspectionRepository.saveInspection), so re-fetch them post-save to get the
            // real ids each item's staged photos need to be linked/relinked against.
            val savedItemsByKey = container.inspectionRepository.getItemsOnce(savedId).associateBy { it.itemKey }
            s.items.forEach { draft ->
                val savedItemId = savedItemsByKey[draft.itemKey]?.id ?: return@forEach
                container.photoRepository.deleteAllForLink(PhotoLinkedType.INSPECTION_ITEM, savedItemId)
                draft.photoPaths.forEach { path ->
                    container.photoRepository.add(
                        VehiclePhotoEntity(vehicleId = s.vehicleId, linkedType = PhotoLinkedType.INSPECTION_ITEM, linkedId = savedItemId, filePath = path)
                    )
                }
            }

            val suggestions = s.items
                .filter { it.status == InspectionItemStatus.PROBLEM }
                .mapNotNull { item -> InspectionMaintenanceSuggester.suggestedCategory(item.itemKey)?.let { MaintenanceSuggestion(item.itemKey, it) } }

            _state.value = _state.value.copy(isSaved = true, savedInspectionId = savedId, maintenanceSuggestions = suggestions, errors = emptyMap())
        }
    }

    /**
     * Creates one due-now reminder per confirmed suggestion (spec: Inspection ->
     * Maintenance task suggestion flow). Only ever called after the user
     * explicitly confirms in the Fragment's dialog - never automatically.
     * [titleFor] resolves each category's localized display name, which this
     * ViewModel has no Context to do itself.
     *
     * Deliberately `suspend` rather than `viewModelScope.launch`-ing internally:
     * the caller (the Fragment, from its own lifecycleScope) awaits this before
     * navigating back, so the writes are guaranteed to finish before this
     * ViewModel's scope can be cancelled by the resulting navigation.
     */
    suspend fun addSuggestedReminders(confirmed: List<MaintenanceSuggestion>, titleFor: (MaintenanceCategory) -> String) {
        val vehicleId = _state.value.vehicleId
        confirmed.forEach { suggestion ->
            container.reminderRepository.addOrUpdate(
                ReminderEntity(
                    vehicleId = vehicleId,
                    title = titleFor(suggestion.category),
                    basis = ReminderBasis.DATE,
                    dueDateMillis = System.currentTimeMillis(),
                    linkedMaintenanceCategory = suggestion.category
                )
            )
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
