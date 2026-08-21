package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MileageValidatorTest {

    @Test
    fun `first record is always ok`() {
        val result = MileageValidator.check(newMileageKm = 50_000, previousMileageKm = null)
        assertEquals(MileageValidator.MileageCheck.Ok, result)
    }

    @Test
    fun `flags mileage lower than previous`() {
        val result = MileageValidator.check(newMileageKm = 40_000, previousMileageKm = 50_000)
        assertTrue(result is MileageValidator.MileageCheck.LowerThanPrevious)
    }

    @Test
    fun `flags unrealistic single day jump`() {
        val result = MileageValidator.check(newMileageKm = 60_000, previousMileageKm = 50_000, daysSincePrevious = 1)
        assertTrue(result is MileageValidator.MileageCheck.UnrealisticJump)
    }

    @Test
    fun `large jump over many days is plausible`() {
        val result = MileageValidator.check(newMileageKm = 60_000, previousMileageKm = 50_000, daysSincePrevious = 30)
        assertEquals(MileageValidator.MileageCheck.Ok, result)
    }

    @Test
    fun `equal mileage is ok`() {
        val result = MileageValidator.check(newMileageKm = 50_000, previousMileageKm = 50_000)
        assertEquals(MileageValidator.MileageCheck.Ok, result)
    }
}
