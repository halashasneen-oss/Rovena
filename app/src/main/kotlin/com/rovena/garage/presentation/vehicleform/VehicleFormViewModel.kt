package com.rovena.garage.presentation.vehicleform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VehicleFormState(
    val id: Long = 0,
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val trim: String = "",
    val vin: String = "",
    val licensePlate: String = "",
    val color: String = "",
    val fuelType: FuelType = FuelType.PETROL,
    val transmission: TransmissionType = TransmissionType.AUTOMATIC,
    val engineSizeLiters: String = "",
    val mileage: String = "",
    val purchaseDateMillis: Long? = null,
    val purchasePrice: String = "",
    val estimatedValue: String = "",
    val notes: String = "",
    val isPrimary: Boolean = false,
    val photoPath: String? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val savedVehicleId: Long = 0,
    val errors: Map<String, Int> = emptyMap()
)

class VehicleFormViewModel(private val container: AppContainer, private val editingVehicleId: Long) : ViewModel() {

    private val _state = MutableStateFlow(VehicleFormState(isLoading = editingVehicleId != 0L))
    val state: StateFlow<VehicleFormState> = _state.asStateFlow()

    init {
        if (editingVehicleId != 0L) {
            viewModelScope.launch {
                container.vehicleRepository.getById(editingVehicleId)?.let { v ->
                    _state.value = VehicleFormState(
                        id = v.id,
                        make = v.make,
                        model = v.model,
                        year = v.year.toString(),
                        trim = v.trim.orEmpty(),
                        vin = v.vin.orEmpty(),
                        licensePlate = v.licensePlate.orEmpty(),
                        color = v.color.orEmpty(),
                        fuelType = v.fuelType,
                        transmission = v.transmission,
                        engineSizeLiters = v.engineSizeLiters?.toString().orEmpty(),
                        mileage = v.currentMileageKm.toString(),
                        purchaseDateMillis = v.purchaseDateMillis,
                        purchasePrice = v.purchasePrice?.toString().orEmpty(),
                        estimatedValue = v.currentEstimatedValue?.toString().orEmpty(),
                        notes = v.notes.orEmpty(),
                        isPrimary = v.isPrimary,
                        photoPath = v.photoPath,
                        isLoading = false
                    )
                } ?: run { _state.value = _state.value.copy(isLoading = false) }
            }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun update(transform: (VehicleFormState) -> VehicleFormState) {
        _state.value = transform(_state.value)
    }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, Int>()
        val year = s.year.toIntOrNull()
        val mileage = s.mileage.toIntOrNull()
        if (s.make.isBlank()) errors["make"] = com.rovena.garage.R.string.error_required
        if (s.model.isBlank()) errors["model"] = com.rovena.garage.R.string.error_required
        if (year == null || year < 1900 || year > 2100) errors["year"] = com.rovena.garage.R.string.error_invalid_year
        if (mileage == null || mileage < 0) errors["mileage"] = com.rovena.garage.R.string.error_invalid_mileage

        if (errors.isNotEmpty()) {
            _state.value = s.copy(errors = errors)
            return
        }

        viewModelScope.launch {
            val entity = VehicleEntity(
                id = s.id,
                make = s.make.trim(),
                model = s.model.trim(),
                year = year!!,
                trim = s.trim.trim().ifBlank { null },
                vin = s.vin.trim().ifBlank { null },
                licensePlate = s.licensePlate.trim().ifBlank { null },
                color = s.color.trim().ifBlank { null },
                fuelType = s.fuelType,
                transmission = s.transmission,
                engineSizeLiters = s.engineSizeLiters.toDoubleOrNull(),
                currentMileageKm = mileage!!,
                purchaseDateMillis = s.purchaseDateMillis,
                purchasePrice = s.purchasePrice.toDoubleOrNull(),
                currentEstimatedValue = s.estimatedValue.toDoubleOrNull(),
                notes = s.notes.trim().ifBlank { null },
                isPrimary = s.isPrimary,
                photoPath = s.photoPath
            )
            val id = if (s.id == 0L) {
                container.vehicleRepository.addVehicle(entity)
            } else {
                container.vehicleRepository.updateVehicle(entity)
                if (s.isPrimary) container.vehicleRepository.setPrimary(s.id)
                s.id
            }
            _state.value = _state.value.copy(isSaved = true, savedVehicleId = id, errors = emptyMap())
        }
    }
}
