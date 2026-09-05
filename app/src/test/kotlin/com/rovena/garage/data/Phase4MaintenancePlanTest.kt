package com.rovena.garage.data

import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase4MaintenancePlanTest {
    private fun vehicle(mileage: Long, fuel: String = "GASOLINE") = VehicleEntity(
        id = 1,
        make = "Test",
        model = "Car",
        year = 2022,
        mileage = mileage,
        fuelType = fuel,
        currencyCode = "JOD",
        createdAt = 1,
        updatedAt = 1
    )

    @Test
    fun exactGenericMilestoneIsDueNow() {
        val oil = MaintenancePlanEngine.suggestions(vehicle(20_000), emptyList())
            .first { it.task == MaintenanceTask.ENGINE_OIL }

        assertEquals(20_000, oil.nextDueMileage)
        assertEquals(0, oil.remainingKm)
        assertEquals(MaintenancePlanStatus.DUE_SOON, oil.status)
        assertFalse(oil.basedOnRecordedService)
    }

    @Test
    fun latestRecordedServiceMovesNextDueMileage() {
        val records = listOf(
            MaintenanceEntity(
                vehicleId = 1,
                serviceType = "Engine oil change",
                performedAt = 100,
                mileage = 18_000,
                cost = 20.0
            )
        )
        val oil = MaintenancePlanEngine.suggestions(vehicle(21_000), records)
            .first { it.task == MaintenanceTask.ENGINE_OIL }

        assertEquals(28_000, oil.nextDueMileage)
        assertEquals(7_000, oil.remainingKm)
        assertTrue(oil.basedOnRecordedService)
        assertEquals(MaintenancePlanStatus.ON_TRACK, oil.status)
    }

    @Test
    fun electricVehicleSkipsCombustionOnlyTasks() {
        val tasks = MaintenancePlanEngine.suggestions(vehicle(12_000, "ELECTRIC"), emptyList())
            .map { it.task }
            .toSet()

        assertFalse(tasks.contains(MaintenanceTask.ENGINE_OIL))
        assertFalse(tasks.contains(MaintenanceTask.SPARK_PLUGS))
        assertFalse(tasks.contains(MaintenanceTask.TRANSMISSION_FLUID))
        assertTrue(tasks.contains(MaintenanceTask.BRAKE_INSPECTION))
        assertTrue(tasks.contains(MaintenanceTask.TIRE_ROTATION))
    }
}
