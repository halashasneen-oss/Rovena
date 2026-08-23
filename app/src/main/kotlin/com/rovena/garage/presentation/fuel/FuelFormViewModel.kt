package com.rovena.garage.presentation.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.R
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.usecase.MileageValidator
import com.rovena.garage.utils.EnumLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FuelFormState(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val dateMillis: Long = System.currentTimeMillis(),
    val mileage: String = "",
    val liters: String = "",
    val pricePerLiter: String = "",
    val totalCost: String = "",
    val currencyCode: String? = null,
    val fuelType: FuelType = FuelType.PETROL,
    val station: String = "",
    val isFullTank: Boolean = true,
    val notes: String = "",
    val mileageWarning: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errors: Map<String, Int> = emptyMap()
)

class FuelFormViewModel(private val container: AppContainer, private val vehicleId: Long, private val recordId: Long) : ViewModel() {

    private val _state = MutableStateFlow(FuelFormState(vehicleId = vehicleId, isLoading = recordId != 0L))
    val state: StateFlow<FuelFormState> = _state.asStateFlow()

    init {
        if (recordId != 0L) {
            viewModelScope.launch {
                container.fuelRepository.getById(recordId)?.let { r ->
                    _state.value = FuelFormState(
                        id = r.id, vehicleId = r.vehicleId, dateMillis = r.dateMillis,
                        mileage = r.mileageKm.toString(), liters = r.liters.toString(),
                        pricePerLiter = r.pricePerLiter.toString(), totalCost = r.totalCost.toString(),
                        currencyCode = r.currencyCode,
                        fuelType = r.fuelType, station = r.station.orEmpty(), isFullTank = r.isFullTank,
                        notes = r.notes.orEmpty(), isLoading = false
                    )
                } ?: run { _state.value = _state.value.copy(isLoading = false) }
            }
        } else {
            viewModelScope.launch {
                val vehicle = container.vehicleRepository.getById(vehicleId)
                val settings = container.settingsRepository.getOrDefault()
                _state.value = _state.value.copy(
                    fuelType = vehicle?.fuelType ?: _state.value.fuelType,
                    currencyCode = EnumLabels.effectiveCurrencyCode(settings.currency, settings.customCurrencyCode),
                    isLoading = false
                )
            }
        }
    }

    fun update(transform: (FuelFormState) -> FuelFormState) {
        val newState = transform(_state.value)
        _state.value = newState
        val mileage = newState.mileage.toIntOrNull()
        if (mileage != null) {
            viewModelScope.launch {
                val warning = container.fuelRepository.checkMileage(vehicleId, mileage) is MileageValidator.MileageCheck.LowerThanPrevious
                _state.value = _state.value.copy(mileageWarning = warning)
            }
        }
    }

    /** Auto-fills total cost when liters + price-per-liter are both known and total wasn't manually touched last. */
    fun recomputeTotal() {
        val s = _state.value
        val liters = s.liters.toDoubleOrNull()
        val price = s.pricePerLiter.toDoubleOrNull()
        if (liters != null && price != null) {
            _state.value = s.copy(totalCost = String.format("%.2f", liters * price))
        }
    }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, Int>()
        val mileage = s.mileage.toIntOrNull()
        val liters = s.liters.toDoubleOrNull()
        val total = s.totalCost.toDoubleOrNull()
        if (mileage == null || mileage < 0) errors["mileage"] = R.string.error_invalid_mileage
        if (liters == null || liters <= 0) errors["liters"] = R.string.error_required
        if (total == null || total < 0) errors["totalCost"] = R.string.error_required
        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            return
        }
        viewModelScope.launch {
            val pricePerLiter = s.pricePerLiter.toDoubleOrNull() ?: (total!! / liters!!)
            val entity = FuelRecordEntity(
                id = s.id, vehicleId = s.vehicleId, dateMillis = s.dateMillis, mileageKm = mileage!!,
                liters = liters!!, pricePerLiter = pricePerLiter, totalCost = total!!, currencyCode = s.currencyCode,
                fuelType = s.fuelType, station = s.station.trim().ifBlank { null }, isFullTank = s.isFullTank,
                notes = s.notes.trim().ifBlank { null }
            )
            container.fuelRepository.addOrUpdate(entity)
            _state.value = _state.value.copy(isSaved = true, errors = emptyMap())
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == 0L) return
        viewModelScope.launch {
            container.fuelRepository.getById(s.id)?.let { container.fuelRepository.delete(it) }
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
