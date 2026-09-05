package com.rovena.garage.data

import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class Phase5DashboardAnalyticsTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun upcomingUsesLatestMaintenanceScheduleAndPrioritizesOverdue() {
        val now = date(2026, 9, 15)
        val vehicle = vehicle(20_000)
        val maintenance = listOf(
            MaintenanceEntity(
                id = 1,
                vehicleId = 1,
                serviceType = "Oil change",
                performedAt = date(2026, 1, 1),
                mileage = 10_000,
                cost = 20.0,
                nextDueMileage = 15_000
            ),
            MaintenanceEntity(
                id = 2,
                vehicleId = 1,
                serviceType = "Oil change",
                performedAt = date(2026, 8, 1),
                mileage = 19_000,
                cost = 20.0,
                nextDueMileage = 24_000
            ),
            MaintenanceEntity(
                id = 3,
                vehicleId = 1,
                serviceType = "Brake inspection",
                performedAt = date(2026, 8, 2),
                mileage = 18_000,
                cost = 10.0,
                nextDueMileage = 19_500
            )
        )

        val items = DashboardAnalytics.upcoming(vehicle, maintenance, emptyList(), now)

        assertEquals(2, items.size)
        assertEquals("Brake inspection", items.first().title)
        assertEquals(UpcomingUrgency.OVERDUE, items.first().urgency)
        val oil = items.first { it.title == "Oil change" }
        assertEquals(4_000L, oil.remainingKm)
        assertEquals(UpcomingUrgency.LATER, oil.urgency)
    }

    @Test
    fun expiringDocumentIsDueSoonAndExpiredDocumentComesFirst() {
        val now = date(2026, 9, 15)
        val documents = listOf(
            DocumentEntity(
                id = 1,
                vehicleId = 1,
                title = "Insurance",
                category = "Insurance",
                expiryAt = date(2026, 9, 20),
                createdAt = date(2026, 1, 1)
            ),
            DocumentEntity(
                id = 2,
                vehicleId = 1,
                title = "Registration",
                category = "Registration",
                expiryAt = date(2026, 9, 10),
                createdAt = date(2026, 1, 1)
            )
        )

        val items = DashboardAnalytics.upcoming(vehicle(20_000), emptyList(), documents, now)

        assertEquals(UpcomingUrgency.OVERDUE, items[0].urgency)
        assertEquals("Registration", items[0].title)
        assertEquals(UpcomingUrgency.DUE_SOON, items[1].urgency)
    }

    @Test
    fun monthlyCostsCompareMonthsAndCalculateCostPer100Km() {
        val now = date(2026, 9, 15)
        val fuel = listOf(
            FuelEntryEntity(id = 1, vehicleId = 1, filledAt = date(2026, 8, 5), mileage = 9_500, liters = 30.0, totalCost = 30.0),
            FuelEntryEntity(id = 2, vehicleId = 1, filledAt = date(2026, 9, 2), mileage = 10_000, liters = 30.0, totalCost = 30.0),
            FuelEntryEntity(id = 3, vehicleId = 1, filledAt = date(2026, 9, 12), mileage = 10_500, liters = 30.0, totalCost = 30.0)
        )
        val maintenance = listOf(
            MaintenanceEntity(id = 1, vehicleId = 1, serviceType = "Oil", performedAt = date(2026, 9, 10), mileage = 10_400, cost = 20.0)
        )
        val expenses = listOf(
            ExpenseEntity(id = 1, vehicleId = 1, spentAt = date(2026, 8, 8), category = "Wash", amount = 10.0),
            ExpenseEntity(id = 2, vehicleId = 1, spentAt = date(2026, 9, 8), category = "Wash", amount = 10.0),
            ExpenseEntity(id = 3, vehicleId = 1, spentAt = date(2026, 9, 9), category = "Parking", amount = 5.0)
        )

        val result = DashboardAnalytics.monthlyCosts(maintenance, fuel, expenses, now, zone)

        assertEquals(95.0, result.current.total, 0.001)
        assertEquals(40.0, result.previous.total, 0.001)
        assertNotNull(result.changePercent)
        assertEquals(500L, result.distanceKm)
        assertEquals(19.0, result.costPer100Km ?: -1.0, 0.001)
        assertTrue(result.topOtherCategories.first().first == "Wash")
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

    private fun date(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()
}
