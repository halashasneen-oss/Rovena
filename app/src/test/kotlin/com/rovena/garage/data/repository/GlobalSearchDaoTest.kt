package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Global Search (spec: compact offline garage-wide search). The DAO
 * search() methods these queries are new siblings of already existed and
 * were unit-tested per-vehicle; these cover the garage-wide variant added
 * for Global Search - the same keyword found across two different
 * vehicles' records, in one query, with no vehicleId filter.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: see
 * InspectionRepositoryTest for why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class GlobalSearchDaoTest {

    private lateinit var db: RovenaDatabase
    private var vehicleAId: Long = 0
    private var vehicleBId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            vehicleAId = db.vehicleDao().insert(
                VehicleEntity(make = "Toyota", model = "Corolla", year = 2019, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 30_000)
            )
            vehicleBId = db.vehicleDao().insert(
                VehicleEntity(make = "Honda", model = "Civic", year = 2021, fuelType = FuelType.PETROL, transmission = TransmissionType.MANUAL, currentMileageKm = 10_000)
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `maintenance search across garage finds matches from every vehicle`() = runTest {
        db.maintenanceDao().insert(
            MaintenanceRecordEntity(vehicleId = vehicleAId, dateMillis = 1000, mileageKm = 30_000, category = MaintenanceCategory.BRAKE_PADS, description = "Front brake pads replaced at CarCenter")
        )
        db.maintenanceDao().insert(
            MaintenanceRecordEntity(vehicleId = vehicleBId, dateMillis = 2000, mileageKm = 10_000, category = MaintenanceCategory.ENGINE_OIL, description = "Oil change")
        )

        val results = db.maintenanceDao().searchAcrossGarage("brake").first()
        assertEquals(1, results.size)
        assertEquals(vehicleAId, results.single().vehicleId)
    }

    @Test
    fun `fuel search across garage matches station name regardless of vehicle`() = runTest {
        db.fuelDao().insert(
            FuelRecordEntity(vehicleId = vehicleAId, dateMillis = 1000, mileageKm = 30_000, liters = 40.0, pricePerLiter = 1.0, totalCost = 40.0, fuelType = FuelType.PETROL, isFullTank = true, station = "Total Amman")
        )
        db.fuelDao().insert(
            FuelRecordEntity(vehicleId = vehicleBId, dateMillis = 2000, mileageKm = 10_000, liters = 30.0, pricePerLiter = 1.0, totalCost = 30.0, fuelType = FuelType.PETROL, isFullTank = true, station = "Shell Zarqa")
        )

        val results = db.fuelDao().searchAcrossGarage("Amman").first()
        assertEquals(1, results.size)
        assertEquals(vehicleAId, results.single().vehicleId)
    }

    @Test
    fun `expense search across garage is not scoped to a single vehicle`() = runTest {
        db.expenseDao().insert(ExpenseEntity(vehicleId = vehicleAId, dateMillis = 1000, category = ExpenseCategory.PARKING, amount = 5.0, description = "Airport parking"))
        db.expenseDao().insert(ExpenseEntity(vehicleId = vehicleBId, dateMillis = 2000, category = ExpenseCategory.CAR_WASH, amount = 10.0, description = "Car wash downtown"))

        assertTrue(db.expenseDao().searchAcrossGarage("nonexistentkeyword").first().isEmpty())
        assertEquals(2, db.expenseDao().searchAcrossGarage("").first().size)
    }
}
