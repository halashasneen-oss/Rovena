package com.rovena.garage.data

import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ValueBoostAnalyticsTest {
    private val utc = ZoneId.of("UTC")
    private val now = LocalDate.of(2026, 9, 15).atStartOfDay(utc).toInstant().toEpochMilli()
    private val vehicle = VehicleEntity(id = 1, make = "Kia", model = "Morning", year = 2012, mileage = 10000, fuelType = "GASOLINE")

    @Test
    fun emptyRecordsNeverClaimExcellentVehicleHealth() {
        val health = VehicleHealthEngine.evaluate(vehicle, emptyList(), emptyList(), emptyList(), now)
        assertNull(health.score)
        assertEquals(VehicleHealthStatus.UNKNOWN, health.status)
        assertEquals(HealthConfidence.LOW, health.confidence)
    }

    @Test
    fun urgentPrioritiesReduceHealthScoreWithoutClaimingDiagnosis() {
        val service = MaintenanceEntity(
            id = 1, vehicleId = 1, serviceType = "Oil change", performedAt = now - 50_000,
            mileage = 9000, cost = 40.0, nextDueMileage = 9500
        )
        val document = DocumentEntity(
            id = 1, vehicleId = 1, title = "Registration", category = "Registration",
            expiryAt = now - 60_000, createdAt = now - 90_000
        )
        val health = VehicleHealthEngine.evaluate(vehicle, listOf(service), listOf(document), emptyList(), now)
        assertEquals(60, health.score)
        assertEquals(1, health.overdueMaintenance)
        assertEquals(1, health.expiredDocuments)
        assertEquals(2, health.attentionCount)
    }

    @Test
    fun expenseRadarDoesNotInventLastMonthOrMileage() {
        val current = ExpenseEntity(
            id = 1, vehicleId = 1,
            spentAt = LocalDate.of(2026, 9, 2).atStartOfDay(utc).toInstant().toEpochMilli(),
            category = "Parking", amount = 25.0
        )
        val insights = DashboardAnalytics.monthlyCosts(emptyList(), emptyList(), listOf(current), now, utc)
        assertEquals(25.0, insights.current.total, 0.001)
        assertEquals(0.0, insights.previous.total, 0.001)
        assertNull(insights.changePercent)
        assertNull(insights.distanceKm)
        assertNull(insights.costPer100Km)
    }
}
