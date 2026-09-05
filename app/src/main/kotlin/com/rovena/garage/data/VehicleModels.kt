package com.rovena.garage.data

import java.time.Year

enum class FuelType {
    GASOLINE,
    DIESEL,
    HYBRID,
    ELECTRIC,
    LPG,
    OTHER
}

data class VehicleDraft(
    val make: String,
    val model: String,
    val year: Int,
    val mileage: Long,
    val fuelType: FuelType = FuelType.GASOLINE,
    val nickname: String = "",
    val plateNumber: String = "",
    val vin: String = "",
    val currencyCode: String = "JOD"
)

enum class VehicleValidationError {
    MAKE_REQUIRED,
    MODEL_REQUIRED,
    YEAR_INVALID,
    MILEAGE_INVALID
}

object VehicleValidator {
    fun validate(
        draft: VehicleDraft,
        currentYear: Int = Year.now().value
    ): VehicleValidationError? {
        if (draft.make.isBlank()) return VehicleValidationError.MAKE_REQUIRED
        if (draft.model.isBlank()) return VehicleValidationError.MODEL_REQUIRED
        if (draft.year !in 1886..(currentYear + 1)) return VehicleValidationError.YEAR_INVALID
        if (draft.mileage < 0L) return VehicleValidationError.MILEAGE_INVALID
        return null
    }
}
