package com.rovena.garage.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.usecase.InputValidator
import com.rovena.garage.utils.EnumLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseFormState(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val dateMillis: Long = System.currentTimeMillis(),
    val amount: String = "",
    /** Stamped from the app's default currency for a new record, preserved unchanged from the loaded record when editing - never rewritten by a later default-currency change (spec: currency architecture). */
    val currencyCode: String? = null,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val description: String = "",
    val mileage: String = "",
    val vendor: String = "",
    val notes: String = "",
    val receiptPhotoPath: String? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errors: Map<String, Int> = emptyMap()
)

class ExpenseFormViewModel(private val container: AppContainer, private val vehicleId: Long, private val recordId: Long) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseFormState(vehicleId = vehicleId, isLoading = recordId != 0L))
    val state: StateFlow<ExpenseFormState> = _state.asStateFlow()

    init {
        if (recordId != 0L) {
            viewModelScope.launch {
                container.expenseRepository.getById(recordId)?.let { e ->
                    _state.value = ExpenseFormState(
                        id = e.id, vehicleId = e.vehicleId, dateMillis = e.dateMillis, amount = e.amount.toString(),
                        currencyCode = e.currencyCode,
                        category = e.category, description = e.description.orEmpty(), mileage = e.mileageKm?.toString().orEmpty(),
                        vendor = e.vendor.orEmpty(), notes = e.notes.orEmpty(), receiptPhotoPath = e.receiptPhotoPath, isLoading = false
                    )
                } ?: run { _state.value = _state.value.copy(isLoading = false) }
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

    fun update(transform: (ExpenseFormState) -> ExpenseFormState) {
        _state.value = transform(_state.value)
    }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, Int>()
        val amount = s.amount.toDoubleOrNull()
        InputValidator.cost(amount)?.let { errors["amount"] = EnumLabels.of(it) }
        val mileage = s.mileage.toIntOrNull()
        InputValidator.optionalMileageKm(mileage)?.let { errors["mileage"] = EnumLabels.of(it) }
        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            return
        }
        viewModelScope.launch {
            val previousReceiptPath = if (s.id != 0L) container.expenseRepository.getById(s.id)?.receiptPhotoPath else null
            val entity = ExpenseEntity(
                id = s.id, vehicleId = s.vehicleId, dateMillis = s.dateMillis, amount = amount!!, currencyCode = s.currencyCode,
                category = s.category, description = s.description.trim().ifBlank { null },
                mileageKm = mileage, vendor = s.vendor.trim().ifBlank { null },
                notes = s.notes.trim().ifBlank { null }, receiptPhotoPath = s.receiptPhotoPath
            )
            container.expenseRepository.addOrUpdate(entity)
            // The user may have replaced or removed the receipt photo on an existing expense -
            // the old file needs deleting too, or it leaks on disk forever.
            if (previousReceiptPath != null && previousReceiptPath != entity.receiptPhotoPath) {
                runCatching { java.io.File(previousReceiptPath).delete() }
            }
            _state.value = _state.value.copy(isSaved = true, errors = emptyMap())
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) return
        viewModelScope.launch {
            container.expenseRepository.getById(s.id)?.let { container.expenseRepository.delete(it) }
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
