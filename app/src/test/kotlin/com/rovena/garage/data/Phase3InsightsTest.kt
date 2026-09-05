package com.rovena.garage.data

import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import com.rovena.garage.notifications.SmartReminderEngine
import com.rovena.garage.notifications.SmartReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class Phase3InsightsTest {
    @Test
    fun fuelAnalyticsDetectsMeaningfulEfficiencyDrop() {
        val entries = listOf(
            fuel(id = 1, mileage = 1_000, liters = 40.0, cost = 40.0),
            fuel(id = 2, mileage = 1_400, liters = 40.0, cost = 40.0),
            fuel(id = 3, mileage = 1_800, liters = 40.0, cost = 40.0),
            fuel(id = 4, mileage = 2_200, liters = 50.0, cost = 50.0),
            fuel(id = 5, mileage = 2_600, liters = 50.0, cost = 50.0)
        )

        val insights = FuelAnalytics.analyze(entries)

        assertEquals(4, insights.intervalCount)
        assertEquals(FuelTrend.WORSENING, insights.trend)
        assertNotNull(insights.averageKmPerLiter)
        assertTrue((insights.trendPercent ?: 0.0) <= -15.0)
        assertTrue(VehicleHealthEngine.isMeaningfulFuelDrop(insights))
    }

    @Test
    fun healthScoreUsesOnlyRecordedCareEvidence() {
        val now = 1_000_000L
        val vehicle = vehicle(mileage = 20_000)
        val maintenance = listOf(
            MaintenanceEntity(
                id = 1,
                vehicleId = 1,
                serviceType = "Oil change",
                performedAt = now - 1_000,
                mileage = 15_000,
                cost = 25.0,
                nextDueMileage = 19_000
            )
        )
        val documents = listOf(
            DocumentEntity(
                id = 1,
                vehicleId = 1,
                title = "Insurance",
                category = "Policy",
                expiryAt = now + TimeUnit.DAYS.toMillis(5),
                createdAt = now - 5_000
            )
        )

        val summary = VehicleHealthEngine.evaluate(vehicle, maintenance, documents, emptyList(), now)

        assertNotNull(summary.score)
        assertEquals(1, summary.overdueMaintenance)
        assertEquals(1, summary.expiringDocuments)
        assertTrue((summary.score ?: 100) < 100)
    }

    @Test
    fun smartReminderPrioritizesOverdueMaintenance() {
        val now = 2_000_000L
        val vehicle = vehicle(mileage = 25_000)
        val maintenance = listOf(
            MaintenanceEntity(
                id = 8,
                vehicleId = 1,
                serviceType = "Brake inspection",
                performedAt = now - 10_000,
                mileage = 20_000,
                cost = 30.0,
                nextDueMileage = 24_000
            )
        )
        val documents = listOf(
            DocumentEntity(
                id = 9,
                vehicleId = 1,
                title = "Registration",
                category = "Registration",
                expiryAt = now + TimeUnit.DAYS.toMillis(3),
                createdAt = now - 10_000
            )
        )

        val reminders = SmartReminderEngine.evaluateVehicle(vehicle, maintenance, documents, emptyList(), now)

        assertTrue(reminders.isNotEmpty())
        assertEquals(SmartReminderKind.MAINTENANCE_OVERDUE, reminders.first().kind)
    }

    private fun vehicle(mileage: Long) = VehicleEntity(
        id = 1,
        make = "Kia",
        model = "Morning",
        year = 2012,
        mileage = mileage,
        fuelType = "GASOLINE",
        currencyCode = "JOD",
        isPrimary = true
    )

    private fun fuel(id: Long, mileage: Long, liters: Double, cost: Double) = FuelEntryEntity(
        id = id,
        vehicleId = 1,
        filledAt = id * 1_000,
        mileage = mileage,
        liters = liters,
        totalCost = cost
    )
}
