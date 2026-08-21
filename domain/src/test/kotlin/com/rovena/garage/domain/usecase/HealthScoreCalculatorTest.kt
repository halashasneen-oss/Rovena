package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.HealthStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HealthScoreCalculatorTest {

    @Test
    fun `returns not enough data when almost nothing is known`() {
        val inputs = HealthScoreCalculator.VehicleHealthInputs(
            daysSinceLastMaintenance = null,
            overdueMaintenanceCount = null,
            totalActiveMaintenanceItems = null,
            brakesConditionScore = null,
            tiresConditionScore = null,
            batteryConditionScore = null,
            fluidsConditionScore = null,
            engineServiceUpToDate = null,
            transmissionServiceUpToDate = null,
            hasExpiredDocument = null,
            hasAnyTrackedDocument = null
        )
        val result = HealthScoreCalculator.fromVehicleInputs(inputs)
        assertNull(result.score)
        assertEquals(HealthStatus.NOT_ENOUGH_DATA, result.status)
    }

    @Test
    fun `full excellent inputs produce a high score`() {
        val inputs = HealthScoreCalculator.VehicleHealthInputs(
            daysSinceLastMaintenance = 30,
            overdueMaintenanceCount = 0,
            totalActiveMaintenanceItems = 5,
            brakesConditionScore = 100,
            tiresConditionScore = 95,
            batteryConditionScore = 100,
            fluidsConditionScore = 100,
            engineServiceUpToDate = true,
            transmissionServiceUpToDate = true,
            hasExpiredDocument = false,
            hasAnyTrackedDocument = true
        )
        val result = HealthScoreCalculator.fromVehicleInputs(inputs)
        assertTrue(result.score != null && result.score!! >= 90) { "expected excellent score, got ${result.score}" }
        assertEquals(HealthStatus.EXCELLENT, result.status)
    }

    @Test
    fun `overdue maintenance drags score down towards critical`() {
        val inputs = HealthScoreCalculator.VehicleHealthInputs(
            daysSinceLastMaintenance = 900,
            overdueMaintenanceCount = 4,
            totalActiveMaintenanceItems = 5,
            brakesConditionScore = 20,
            tiresConditionScore = 25,
            batteryConditionScore = 30,
            fluidsConditionScore = 20,
            engineServiceUpToDate = false,
            transmissionServiceUpToDate = false,
            hasExpiredDocument = true,
            hasAnyTrackedDocument = true
        )
        val result = HealthScoreCalculator.fromVehicleInputs(inputs)
        assertTrue(result.score != null && result.score!! < 40) { "expected critical score, got ${result.score}" }
        assertEquals(HealthStatus.CRITICAL, result.status)
    }

    @Test
    fun `status bands map correctly`() {
        assertEquals(HealthStatus.EXCELLENT, HealthScoreCalculator.statusFor(90))
        assertEquals(HealthStatus.GOOD, HealthScoreCalculator.statusFor(75))
        assertEquals(HealthStatus.FAIR, HealthScoreCalculator.statusFor(60))
        assertEquals(HealthStatus.ATTENTION_NEEDED, HealthScoreCalculator.statusFor(40))
        assertEquals(HealthStatus.CRITICAL, HealthScoreCalculator.statusFor(39))
    }

    @Test
    fun `score is clamped between 0 and 100`() {
        val inputs = listOf(
            HealthScoreCalculator.CategoryInput(com.rovena.garage.domain.model.HealthCategory.BRAKES, 0, 1.0)
        )
        val result = HealthScoreCalculator.calculate(inputs)
        assertEquals(0, result.score)
    }
}
