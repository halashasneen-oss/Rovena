package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.DueStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DueStatusCalculatorTest {

    private val today = LocalDate.of(2026, 6, 1)

    @Test
    fun `returns null when neither trigger is set`() {
        val result = DueStatusCalculator.evaluate(10_000, today, null, null)
        assertNull(result)
    }

    @Test
    fun `mileage overdue when remaining is zero or negative`() {
        val result = DueStatusCalculator.evaluate(10_500, today, dueMileageKm = 10_000, dueDate = null)
        assertEquals(DueStatus.OVERDUE, result!!.status)
        assertEquals(-500, result.remainingKm)
    }

    @Test
    fun `mileage due soon within threshold`() {
        val result = DueStatusCalculator.evaluate(9_200, today, dueMileageKm = 10_000, dueDate = null)
        assertEquals(DueStatus.DUE_SOON, result!!.status)
    }

    @Test
    fun `mileage upcoming when far away`() {
        val result = DueStatusCalculator.evaluate(1_000, today, dueMileageKm = 10_000, dueDate = null)
        assertEquals(DueStatus.UPCOMING, result!!.status)
    }

    @Test
    fun `most urgent of mileage and date trigger wins`() {
        val result = DueStatusCalculator.evaluate(
            currentMileageKm = 1_000,
            today = today,
            dueMileageKm = 50_000, // far away -> UPCOMING
            dueDate = today.plusDays(2) // very close -> OVERDUE-ish DUE
        )
        assertEquals(DueStatus.DUE, result!!.status)
    }

    @Test
    fun `estimated due date projected from average km per day when only mileage known`() {
        val result = DueStatusCalculator.evaluate(
            currentMileageKm = 9_000,
            today = today,
            dueMileageKm = 10_000,
            dueDate = null,
            averageKmPerDay = 50.0
        )
        assertEquals(today.plusDays(20), result!!.estimatedDueDate)
    }

    @Test
    fun `no estimated due date fabricated without driving history`() {
        val result = DueStatusCalculator.evaluate(
            currentMileageKm = 9_000,
            today = today,
            dueMileageKm = 10_000,
            dueDate = null,
            averageKmPerDay = null
        )
        assertNull(result!!.estimatedDueDate)
    }
}
