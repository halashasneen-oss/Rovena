package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.usecase.MileageIntelligenceCalculator.OdometerReading
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MileageIntelligenceCalculatorTest {

    @Test
    fun `fewer than two readings yields null average`() {
        assertNull(MileageIntelligenceCalculator.averageKmPerDay(emptyList()))
        assertNull(MileageIntelligenceCalculator.averageKmPerDay(listOf(OdometerReading(LocalDate.now(), 1000))))
    }

    @Test
    fun `readings spanning less than a week yield null average`() {
        val readings = listOf(
            OdometerReading(LocalDate.of(2026, 1, 1), 1000),
            OdometerReading(LocalDate.of(2026, 1, 3), 1200)
        )
        assertNull(MileageIntelligenceCalculator.averageKmPerDay(readings))
    }

    @Test
    fun `mileage that did not increase yields null average`() {
        val readings = listOf(
            OdometerReading(LocalDate.of(2026, 1, 1), 5000),
            OdometerReading(LocalDate.of(2026, 2, 1), 5000)
        )
        assertNull(MileageIntelligenceCalculator.averageKmPerDay(readings))
    }

    @Test
    fun `computes average km per day from earliest to latest reading, order independent`() {
        val readings = listOf(
            OdometerReading(LocalDate.of(2026, 2, 1), 11000),
            OdometerReading(LocalDate.of(2026, 1, 1), 10000)
        )
        // 1000 km over 31 days
        assertEquals(1000.0 / 31, MileageIntelligenceCalculator.averageKmPerDay(readings)!!, 0.0001)
    }

    @Test
    fun `ignores intermediate readings, only uses earliest and latest`() {
        val readings = listOf(
            OdometerReading(LocalDate.of(2026, 1, 1), 10000),
            OdometerReading(LocalDate.of(2026, 1, 15), 10001), // a mid-span dip in reported mileage is ignored
            OdometerReading(LocalDate.of(2026, 2, 1), 11000)
        )
        assertEquals(1000.0 / 31, MileageIntelligenceCalculator.averageKmPerDay(readings)!!, 0.0001)
    }
}
