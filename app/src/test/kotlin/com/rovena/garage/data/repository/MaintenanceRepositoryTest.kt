package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.data.local.entities.VehiclePhotoEntity
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.PhotoLinkedType
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Deleting a maintenance record must also clean up its linked photos - both the DB
 * rows (VehiclePhoto is a generic polymorphic link, not a real FK Room can cascade)
 * and the actual files on disk (see MaintenanceRepository.delete()). Runs against a
 * real in-memory Room database plus real temp files via Robolectric, so the actual
 * read-paths-before-transaction/delete-rows-in-transaction/delete-files-after
 * ordering is verified, not a mock.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: see
 * InspectionRepositoryTest for why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class MaintenanceRepositoryTest {

    private lateinit var db: RovenaDatabase
    private lateinit var repository: MaintenanceRepository
    private lateinit var photoRepository: PhotoRepository
    private var vehicleId: Long = 0
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        photoRepository = PhotoRepository(db.vehiclePhotoDao())
        repository = MaintenanceRepository(db.maintenanceDao(), db.vehicleDao(), TimelineSyncer(db.timelineDao()), db, photoRepository)
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Kia", model = "Sportage", year = 2021, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 30_000)
            )
        }
        tempDir = File.createTempFile("maintenance_repo_test", "").apply { delete(); mkdirs() }
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.deleteRecursively()
    }

    private fun newRecord(mileageKm: Int = 30_000) = MaintenanceRecordEntity(
        vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = mileageKm,
        category = MaintenanceCategory.ENGINE_OIL, description = "Oil change"
    )

    @Test
    fun `deleting a maintenance record with no photos succeeds`() = runTest {
        val id = repository.addOrUpdate(newRecord())
        val record = repository.getById(id)!!

        repository.delete(record)

        assertNull(repository.getById(id))
    }

    @Test
    fun `deleting a maintenance record deletes its linked photo rows and files`() = runTest {
        val id = repository.addOrUpdate(newRecord())
        val photoFile = File(tempDir, "photo.jpg").apply { writeText("fake image bytes") }
        photoRepository.add(VehiclePhotoEntity(vehicleId = vehicleId, linkedType = PhotoLinkedType.MAINTENANCE, linkedId = id, filePath = photoFile.absolutePath))
        assertTrue(photoFile.exists())

        val record = repository.getById(id)!!
        repository.delete(record)

        assertTrue(photoRepository.getByLinkOnce(PhotoLinkedType.MAINTENANCE, id).isEmpty())
        assertFalse(photoFile.exists())
        assertNull(repository.getById(id))
    }

    @Test
    fun `deleting a maintenance record deletes every linked photo's thumbnail too`() = runTest {
        val id = repository.addOrUpdate(newRecord())
        val photoFile = File(tempDir, "photo.jpg").apply { writeText("fake image bytes") }
        val thumbFile = File(tempDir, "photo_thumb.jpg").apply { writeText("fake thumb bytes") }
        photoRepository.add(
            VehiclePhotoEntity(
                vehicleId = vehicleId, linkedType = PhotoLinkedType.MAINTENANCE, linkedId = id,
                filePath = photoFile.absolutePath, thumbnailPath = thumbFile.absolutePath
            )
        )

        val record = repository.getById(id)!!
        repository.delete(record)

        assertFalse(photoFile.exists())
        assertFalse(thumbFile.exists())
    }

    @Test
    fun `addOrUpdate bumps the vehicle's mileage when the record's mileage is higher`() = runTest {
        repository.addOrUpdate(newRecord(mileageKm = 35_000))

        assertEquals(35_000, db.vehicleDao().getById(vehicleId)?.currentMileageKm)
    }

    @Test
    fun `addOrUpdate never lowers the vehicle's mileage`() = runTest {
        repository.addOrUpdate(newRecord(mileageKm = 30_000))

        assertEquals(30_000, db.vehicleDao().getById(vehicleId)?.currentMileageKm)
    }

    @Test
    fun `getWorkshopSuggestions returns distinct previously-used workshop names for the vehicle`() = runTest {
        repository.addOrUpdate(newRecord().copy(workshop = "Downtown Garage"))
        repository.addOrUpdate(newRecord().copy(workshop = "Downtown Garage"))
        repository.addOrUpdate(newRecord().copy(workshop = "Highway Service Center"))
        repository.addOrUpdate(newRecord().copy(workshop = null))

        val suggestions = repository.getWorkshopSuggestions(vehicleId)

        assertEquals(listOf("Downtown Garage", "Highway Service Center"), suggestions)
    }
}
