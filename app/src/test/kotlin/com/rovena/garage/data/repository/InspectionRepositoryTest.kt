package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.InspectionItemEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.data.local.entities.VehiclePhotoEntity
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.InspectionCategoryGroup
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.InspectionItemStatus
import com.rovena.garage.domain.model.PhotoLinkedType
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Inspection items must keep a stable row id across edits (see
 * InspectionRepository.saveInspection) - inspection-item photos are linked to that id,
 * and the old delete-all/insert-all persistence would silently orphan every photo the
 * moment the inspection was saved a second time. These tests exercise a real in-memory
 * Room database so the actual upsert-by-itemKey logic is verified, not a mock.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: RovenaApp.onCreate()
 * schedules ReminderCheckWorker via WorkManager, which isn't initialized under Robolectric's
 * default test setup and throws - this test only needs a Context to build an in-memory Room
 * database, not the app's real startup side effects.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class InspectionRepositoryTest {

    private lateinit var db: RovenaDatabase
    private lateinit var repository: InspectionRepository
    private lateinit var photoRepository: PhotoRepository
    private var vehicleId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        photoRepository = PhotoRepository(db.vehiclePhotoDao())
        repository = InspectionRepository(db.inspectionDao(), db.inspectionItemDao(), TimelineSyncer(db.timelineDao()), db, photoRepository)
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Honda", model = "Civic", year = 2019, fuelType = FuelType.PETROL, transmission = TransmissionType.MANUAL, currentMileageKm = 50_000)
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun draftItem(status: InspectionItemStatus = InspectionItemStatus.GOOD) = InspectionItemEntity(
        inspectionId = 0, categoryGroup = InspectionCategoryGroup.EXTERIOR, itemKey = InspectionItemKey.PAINT, status = status
    )

    @Test
    fun `saving the same inspection twice keeps the item's row id stable`() = runTest {
        val inspectionId = repository.saveInspection(
            InspectionEntity(vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 50_000),
            listOf(draftItem(InspectionItemStatus.GOOD))
        )
        val firstItemId = repository.getItemsOnce(inspectionId).single().id

        repository.saveInspection(
            InspectionEntity(id = inspectionId, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 50_500),
            listOf(draftItem(InspectionItemStatus.ATTENTION))
        )
        val secondItemId = repository.getItemsOnce(inspectionId).single().id

        assertEquals(firstItemId, secondItemId)
        assertEquals(InspectionItemStatus.ATTENTION, repository.getItemsOnce(inspectionId).single().status)
    }

    @Test
    fun `a photo linked to an item survives re-saving the inspection`() = runTest {
        val inspectionId = repository.saveInspection(
            InspectionEntity(vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 50_000),
            listOf(draftItem())
        )
        val itemId = repository.getItemsOnce(inspectionId).single().id
        photoRepository.add(VehiclePhotoEntity(vehicleId = vehicleId, linkedType = PhotoLinkedType.INSPECTION_ITEM, linkedId = itemId, filePath = "/tmp/photo.jpg"))

        // Re-save (e.g. the user edits notes and saves again) - the old delete-all/insert-all
        // persistence would have assigned the item a brand new id here, orphaning the photo.
        repository.saveInspection(
            InspectionEntity(id = inspectionId, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 50_000),
            listOf(draftItem())
        )

        val photosAfterResave = photoRepository.getByLinkOnce(PhotoLinkedType.INSPECTION_ITEM, itemId)
        assertEquals(1, photosAfterResave.size)
        assertEquals("/tmp/photo.jpg", photosAfterResave.single().filePath)
    }

    @Test
    fun `deleting an inspection deletes its item photos`() = runTest {
        val inspectionId = repository.saveInspection(
            InspectionEntity(vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 50_000),
            listOf(draftItem())
        )
        val itemId = repository.getItemsOnce(inspectionId).single().id
        photoRepository.add(VehiclePhotoEntity(vehicleId = vehicleId, linkedType = PhotoLinkedType.INSPECTION_ITEM, linkedId = itemId, filePath = "/tmp/photo.jpg"))

        val inspection = repository.getById(inspectionId)!!
        repository.delete(inspection)

        assertTrue(photoRepository.getByLinkOnce(PhotoLinkedType.INSPECTION_ITEM, itemId).isEmpty())
        assertNull(repository.getById(inspectionId))
    }
}
