package com.rovena.garage.utils

import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.TransmissionType
import java.util.concurrent.TimeUnit

/**
 * Debug-only sample data generator (spec #34). Only reachable from the
 * Settings screen when `BuildConfig.SAMPLE_DATA_ENABLED` is true (the `dev`
 * product flavor) - never seeded automatically into a real user's garage.
 */
object SampleDataGenerator {

    suspend fun generate(container: AppContainer) {
        val now = System.currentTimeMillis()
        fun daysAgo(days: Int) = now - TimeUnit.DAYS.toMillis(days.toLong())

        val vehicle = VehicleEntity(
            make = "BMW", model = "320i", year = 2020, trim = "Sport Line",
            vin = "WBA8E9G50GNT12345", licensePlate = "12-AB-345", color = "Alpine White",
            fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC,
            engineSizeLiters = 2.0, currentMileageKm = 142_850,
            purchaseDateMillis = daysAgo(900), purchasePrice = 28_500.0, currentEstimatedValue = 19_000.0,
            notes = "Sample vehicle for testing.", isPrimary = false
        )
        val vehicleId = container.vehicleRepository.addVehicle(vehicle)

        val fuelFillUps = listOf(
            Triple(140_200, 42.0, daysAgo(60)),
            Triple(140_650, 40.5, daysAgo(46)),
            Triple(141_100, 41.0, daysAgo(32)),
            Triple(141_600, 43.2, daysAgo(18)),
            Triple(142_100, 39.8, daysAgo(9)),
            Triple(142_850, 41.5, daysAgo(1))
        )
        fuelFillUps.forEach { (mileage, liters, date) ->
            val pricePerLiter = 0.70
            container.fuelRepository.addOrUpdate(
                FuelRecordEntity(
                    vehicleId = vehicleId, dateMillis = date, mileageKm = mileage, liters = liters,
                    pricePerLiter = pricePerLiter, totalCost = liters * pricePerLiter, currencyCode = "JOD",
                    fuelType = FuelType.PETROL, station = "Total Station", isFullTank = true
                )
            )
        }

        container.maintenanceRepository.addOrUpdate(
            MaintenanceRecordEntity(
                vehicleId = vehicleId, dateMillis = daysAgo(75), mileageKm = 139_500,
                category = MaintenanceCategory.ENGINE_OIL, description = "Oil & filter change",
                cost = 35.0, currencyCode = "JOD", workshop = "AutoCare Amman",
                nextDueMileageKm = 149_500, nextDueDateMillis = now + TimeUnit.DAYS.toMillis(180)
            )
        )
        container.maintenanceRepository.addOrUpdate(
            MaintenanceRecordEntity(
                vehicleId = vehicleId, dateMillis = daysAgo(200), mileageKm = 132_000,
                category = MaintenanceCategory.BRAKE_PADS, description = "Front brake pads replaced",
                cost = 90.0, currencyCode = "JOD", workshop = "AutoCare Amman"
            )
        )
        container.maintenanceRepository.addOrUpdate(
            MaintenanceRecordEntity(
                vehicleId = vehicleId, dateMillis = daysAgo(20), mileageKm = 141_800,
                category = MaintenanceCategory.TIRES, description = "Tire rotation",
                cost = 15.0, currencyCode = "JOD", workshop = "QuickFix Garage"
            )
        )

        container.expenseRepository.addOrUpdate(
            ExpenseEntity(vehicleId = vehicleId, dateMillis = daysAgo(5), amount = 5.0, currencyCode = "JOD", category = ExpenseCategory.CAR_WASH, description = "Car wash")
        )
        container.expenseRepository.addOrUpdate(
            ExpenseEntity(vehicleId = vehicleId, dateMillis = daysAgo(40), amount = 320.0, currencyCode = "JOD", category = ExpenseCategory.INSURANCE, description = "Annual insurance renewal")
        )
        container.expenseRepository.addOrUpdate(
            ExpenseEntity(vehicleId = vehicleId, dateMillis = daysAgo(12), amount = 3.0, currencyCode = "JOD", category = ExpenseCategory.PARKING, description = "Mall parking")
        )

        container.reminderRepository.addOrUpdate(
            com.rovena.garage.data.local.entities.ReminderEntity(
                vehicleId = vehicleId, title = "Insurance renewal", basis = com.rovena.garage.domain.model.ReminderBasis.DATE,
                dueDateMillis = now + TimeUnit.DAYS.toMillis(300), isRecurring = true, intervalMonths = 12
            )
        )

        container.settingsRepository.setSampleDataSeeded(true)
    }
}
