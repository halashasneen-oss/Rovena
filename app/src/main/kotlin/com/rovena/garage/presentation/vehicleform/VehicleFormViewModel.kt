package com.rovena.garage.presentation.vehicleform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.TransmissionType
import com.rovena.garage.domain.usecase.InputValidator
import com.rovena.garage.domain.usecase.MileageValidator
import com.rovena.garage.utils.EnumLabels
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
    val currencyCode: String? = null,
    val notes: String = "",
    val isPrimary: Boolean = false,
    val photoPath: String? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val savedVehicleId: Long = 0,
    val mileageWarning: MileageValidator.MileageCheck? = null,
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
                        currencyCode = v.currencyCode,
                        notes = v.notes.orEmpty(),
                        isPrimary = v.isPrimary,
                        photoPath = v.photoPath,
                        isLoading = false
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

    fun update(transform: (VehicleFormState) -> VehicleFormState) {
        val newState = transform(_state.value)
        _state.value = newState
        val mileage = newState.mileage.toIntOrNull()
        if (mileage != null && editingVehicleId != 0L) {
            viewModelScope.launch {
                val check = container.vehicleRepository.checkMileage(editingVehicleId, mileage)
                _state.value = _state.value.copy(mileageWarning = check.takeUnless { it is MileageValidator.MileageCheck.Ok })
            }
        } else {
            _state.value = _state.value.copy(mileageWarning = null)
        }
    }

    fun save() {
        val s = _state.value
        val errors = mutableMapOf<String, Int>()
        val year = s.year.toIntOrNull()
        val mileage = s.mileage.toIntOrNull()
        InputValidator.requiredText(s.make)?.let { errors["make"] = EnumLabels.of(it) }
        InputValidator.requiredText(s.model)?.let { errors["model"] = EnumLabels.of(it) }
        InputValidator.vehicleYear(year)?.let { errors["year"] = EnumLabels.of(it) }
        InputValidator.mileageKm(mileage)?.let { errors["mileage"] = EnumLabels.of(it) }

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
                currencyCode = s.currencyCode,
                notes = s.notes.trim().ifBlank { null },
                isPrimary = s.isPrimary,
                photoPath = s.photoPath
            )
            val id = if (s.id == 0L) {
                container.vehicleRepository.addVehicle(entity)
            } else {
                val previousPhotoPath = container.vehicleRepository.getById(s.id)?.photoPath
                container.vehicleRepository.updateVehicle(entity)
                if (s.isPrimary) container.vehicleRepository.setPrimary(s.id)
                // The user may have replaced or removed the vehicle photo - the old file
                // needs deleting too, or it leaks on disk forever.
                if (previousPhotoPath != null && previousPhotoPath != entity.photoPath) {
                    runCatching { java.io.File(previousPhotoPath).delete() }
                }
                s.id
            }
            _state.value = _state.value.copy(isSaved = true, savedVehicleId = id, errors = emptyMap())
        }
    }
}
