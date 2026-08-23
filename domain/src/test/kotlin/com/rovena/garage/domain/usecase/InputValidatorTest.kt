package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class InputValidatorTest {

    @Test
    fun `mileageKm rejects null and negative, accepts zero and positive`() {
        assertEquals(InputValidator.Error.REQUIRED, InputValidator.mileageKm(null))
        assertEquals(InputValidator.Error.NEGATIVE_MILEAGE, InputValidator.mileageKm(-1))
        assertNull(InputValidator.mileageKm(0))
        assertNull(InputValidator.mileageKm(50_000))
    }

    @Test
    fun `optionalMileageKm allows null but rejects negative`() {
        assertNull(InputValidator.optionalMileageKm(null))
        assertNull(InputValidator.optionalMileageKm(0))
        assertEquals(InputValidator.Error.NEGATIVE_MILEAGE, InputValidator.optionalMileageKm(-5))
    }

    @Test
    fun `positiveQuantity requires strictly positive`() {
        assertEquals(InputValidator.Error.REQUIRED, InputValidator.positiveQuantity(null))
        assertEquals(InputValidator.Error.NOT_POSITIVE_QUANTITY, InputValidator.positiveQuantity(0.0))
        assertEquals(InputValidator.Error.NOT_POSITIVE_QUANTITY, InputValidator.positiveQuantity(-3.0))
        assertNull(InputValidator.positiveQuantity(40.5))
    }

    @Test
    fun `cost allows zero but rejects negative and null`() {
        assertEquals(InputValidator.Error.REQUIRED, InputValidator.cost(null))
        assertEquals(InputValidator.Error.NEGATIVE_COST, InputValidator.cost(-0.01))
        assertNull(InputValidator.cost(0.0))
        assertNull(InputValidator.cost(99.99))
    }

    @Test
    fun `optionalCost allows null and zero but rejects negative`() {
        assertNull(InputValidator.optionalCost(null))
        assertNull(InputValidator.optionalCost(0.0))
        assertEquals(InputValidator.Error.NEGATIVE_COST, InputValidator.optionalCost(-10.0))
    }

    @Test
    fun `optionalEngineSize allows null but requires strictly positive when present`() {
        assertNull(InputValidator.optionalEngineSize(null))
        assertNull(InputValidator.optionalEngineSize(1.6))
        assertEquals(InputValidator.Error.NOT_POSITIVE_ENGINE_SIZE, InputValidator.optionalEngineSize(0.0))
        assertEquals(InputValidator.Error.NOT_POSITIVE_ENGINE_SIZE, InputValidator.optionalEngineSize(-2.0))
    }

    @Test
    fun `requiredText rejects blank`() {
        assertEquals(InputValidator.Error.REQUIRED, InputValidator.requiredText(""))
        assertEquals(InputValidator.Error.REQUIRED, InputValidator.requiredText("   "))
        assertNull(InputValidator.requiredText("Oil change"))
    }

    @Test
    fun `plausibleDate accepts today and rejects far past or future`() {
        val today = LocalDate.of(2026, 6, 15)
        val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertNull(InputValidator.plausibleDate(todayMillis, today))

        val tooFarPast = today.minusYears(101).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(InputValidator.Error.IMPLAUSIBLE_DATE, InputValidator.plausibleDate(tooFarPast, today))

        val tooFarFuture = today.plusYears(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(InputValidator.Error.IMPLAUSIBLE_DATE, InputValidator.plausibleDate(tooFarFuture, today))

        val nearFuture = today.plusMonths(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertNull(InputValidator.plausibleDate(nearFuture, today))
    }

    @Test
    fun `vehicleYear rejects null and out-of-range, accepts plausible range`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(InputValidator.Error.REQUIRED, InputValidator.vehicleYear(null, today))
        assertEquals(InputValidator.Error.INVALID_YEAR, InputValidator.vehicleYear(1899, today))
        assertEquals(InputValidator.Error.INVALID_YEAR, InputValidator.vehicleYear(2028, today))
        assertNull(InputValidator.vehicleYear(2020, today))
        assertNull(InputValidator.vehicleYear(2027, today))
    }

    @Test
    fun `vin is optional but rejects malformed values when provided`() {
        assertNull(InputValidator.vin(""))
        assertNull(InputValidator.vin("   "))
        assertNull(InputValidator.vin("WBA8E9G50GNT12345"))
        assertEquals(InputValidator.Error.INVALID_VIN, InputValidator.vin("TOO-SHORT"))
        assertEquals(InputValidator.Error.INVALID_VIN, InputValidator.vin("HASIOQCHARS12345")) // contains I, O, Q
    }
}
