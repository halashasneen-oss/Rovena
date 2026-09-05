package com.rovena.garage.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleValidatorTest {
    @Test
    fun validDraftPasses() {
        val draft = VehicleDraft(
            make = "Toyota",
            model = "Corolla",
            year = 2022,
            mileage = 50_000
        )
        assertNull(VehicleValidator.validate(draft, currentYear = 2026))
    }

    @Test
    fun blankMakeFails() {
        val draft = VehicleDraft(
            make = " ",
            model = "Corolla",
            year = 2022,
            mileage = 50_000
        )
        assertEquals(
            VehicleValidationError.MAKE_REQUIRED,
            VehicleValidator.validate(draft, currentYear = 2026)
        )
    }

    @Test
    fun unrealisticFutureYearFails() {
        val draft = VehicleDraft(
            make = "Toyota",
            model = "Corolla",
            year = 2030,
            mileage = 10
        )
        assertEquals(
            VehicleValidationError.YEAR_INVALID,
            VehicleValidator.validate(draft, currentYear = 2026)
        )
    }

    @Test
    fun negativeMileageFails() {
        val draft = VehicleDraft(
            make = "Toyota",
            model = "Corolla",
            year = 2022,
            mileage = -1
        )
        assertEquals(
            VehicleValidationError.MILEAGE_INVALID,
            VehicleValidator.validate(draft, currentYear = 2026)
        )
    }
}
