package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.data.local.entities.VehicleNoteEntity
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
 * Vehicle Notes (spec: freeform, non-diagnostic notes). Runs against a real
 * in-memory Room database so the vehicle_notes table (added in
 * MIGRATION_2_3) and its foreign key are actually exercised.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: see
 * InspectionRepositoryTest for why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class VehicleNoteRepositoryTest {

    private lateinit var db: RovenaDatabase
    private lateinit var repository: VehicleNoteRepository
    private var vehicleId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VehicleNoteRepository(db.vehicleNoteDao())
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Kia", model = "Sportage", year = 2021, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 20_000)
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `adding a note makes it observable for its vehicle`() = runTest {
        repository.addOrUpdate(VehicleNoteEntity(vehicleId = vehicleId, text = "Rear tire slow leak"))

        val notes = repository.observeByVehicle(vehicleId).first()
        assertEquals(1, notes.size)
        assertEquals("Rear tire slow leak", notes.single().text)
    }

    @Test
    fun `updating an existing note keeps the same row id`() = runTest {
        val id = repository.addOrUpdate(VehicleNoteEntity(vehicleId = vehicleId, text = "Original"))
        repository.addOrUpdate(VehicleNoteEntity(id = id, vehicleId = vehicleId, text = "Edited"))

        val notes = repository.observeByVehicle(vehicleId).first()
        assertEquals(1, notes.size)
        assertEquals(id, notes.single().id)
        assertEquals("Edited", notes.single().text)
    }

    @Test
    fun `deleting a note removes it`() = runTest {
        val id = repository.addOrUpdate(VehicleNoteEntity(vehicleId = vehicleId, text = "Temporary"))
        val note = db.vehicleNoteDao().getById(id)!!

        repository.delete(note)

        assertTrue(repository.observeByVehicle(vehicleId).first().isEmpty())
    }

    @Test
    fun `deleting the vehicle cascades to its notes`() = runTest {
        repository.addOrUpdate(VehicleNoteEntity(vehicleId = vehicleId, text = "Will be cascaded"))
        val vehicle = db.vehicleDao().getById(vehicleId)!!

        db.vehicleDao().delete(vehicle)

        assertTrue(repository.observeByVehicle(vehicleId).first().isEmpty())
    }
}
