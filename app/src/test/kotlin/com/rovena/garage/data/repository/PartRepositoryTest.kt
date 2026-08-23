package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.PartEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.FuelType
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
 * Parts History + Warranty Tracking (spec). Runs against a real in-memory
 * Room database so the parts table (added in MIGRATION_3_4) and its
 * foreign key are actually exercised.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: see
 * InspectionRepositoryTest for why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class PartRepositoryTest {

    private lateinit var db: RovenaDatabase
    private lateinit var repository: PartRepository
    private var vehicleId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PartRepository(db.partDao())
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Mazda", model = "CX-5", year = 2020, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 40_000)
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `adding a part with warranty makes it observable for its vehicle`() = runTest {
        repository.addOrUpdate(
            PartEntity(vehicleId = vehicleId, name = "Battery", installedDateMillis = 1000L, warrantyExpiryDateMillis = 2_000_000_000_000L)
        )

        val parts = repository.observeByVehicle(vehicleId).first()
        assertEquals(1, parts.size)
        assertEquals("Battery", parts.single().name)
    }

    @Test
    fun `observeWithWarranty only returns parts that track a warranty`() = runTest {
        repository.addOrUpdate(PartEntity(vehicleId = vehicleId, name = "Wiper blades", installedDateMillis = 1000L))
        repository.addOrUpdate(PartEntity(vehicleId = vehicleId, name = "Battery", installedDateMillis = 1000L, warrantyExpiryMileageKm = 60_000))

        val withWarranty = repository.observeWithWarranty(vehicleId).first()
        assertEquals(1, withWarranty.size)
        assertEquals("Battery", withWarranty.single().name)
    }

    @Test
    fun `updating a part keeps the same row id`() = runTest {
        val id = repository.addOrUpdate(PartEntity(vehicleId = vehicleId, name = "Battery", installedDateMillis = 1000L))
        repository.addOrUpdate(PartEntity(id = id, vehicleId = vehicleId, name = "Battery (replaced)", installedDateMillis = 1000L))

        val parts = repository.observeByVehicle(vehicleId).first()
        assertEquals(1, parts.size)
        assertEquals(id, parts.single().id)
        assertEquals("Battery (replaced)", parts.single().name)
    }

    @Test
    fun `deleting the vehicle cascades to its parts`() = runTest {
        repository.addOrUpdate(PartEntity(vehicleId = vehicleId, name = "Battery", installedDateMillis = 1000L))
        val vehicle = db.vehicleDao().getById(vehicleId)!!

        db.vehicleDao().delete(vehicle)

        assertTrue(repository.observeByVehicle(vehicleId).first().isEmpty())
    }
}
