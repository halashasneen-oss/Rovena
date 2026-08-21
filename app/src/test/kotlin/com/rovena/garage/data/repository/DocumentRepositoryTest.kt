package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.DocumentType
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Document expiry -> reminder lifecycle (spec #8): a document with an expiry date always
 * has exactly one linked reminder, created/updated/deleted in lockstep with the document.
 * Runs against a real in-memory Room database via Robolectric so the actual DAOs, foreign
 * keys, and the transaction wrapping in DocumentRepository are exercised, not a mock.
 */
@RunWith(RobolectricTestRunner::class)
class DocumentRepositoryTest {

    private lateinit var db: RovenaDatabase
    private lateinit var repository: DocumentRepository
    private var vehicleId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DocumentRepository(db.documentDao(), db.reminderDao(), TimelineSyncer(db.timelineDao()), db)
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Toyota", model = "Corolla", year = 2020, fuelType = FuelType.GASOLINE, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 10_000)
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newDocument(expiryDateMillis: Long?) = DocumentEntity(
        vehicleId = vehicleId,
        name = "Registration",
        type = DocumentType.REGISTRATION,
        filePath = "/tmp/reg.pdf",
        expiryDateMillis = expiryDateMillis
    )

    @Test
    fun `creating a document with an expiry date creates a reminder`() = runTest {
        val (docId, reminderId) = repository.addOrUpdate(newDocument(expiryDateMillis = 2_000_000_000_000L))
        assertNotNull(reminderId)
        val saved = repository.getById(docId)
        assertEquals(reminderId, saved?.reminderId)
        assertNotNull(db.reminderDao().getById(reminderId!!))
    }

    @Test
    fun `editing the expiry date updates the same reminder`() = runTest {
        val (docId, firstReminderId) = repository.addOrUpdate(newDocument(expiryDateMillis = 2_000_000_000_000L))
        val saved = repository.getById(docId)!!

        val (_, secondReminderId) = repository.addOrUpdate(saved.copy(expiryDateMillis = 2_100_000_000_000L))

        assertEquals(firstReminderId, secondReminderId)
        val reminder = db.reminderDao().getById(secondReminderId!!)
        assertEquals(2_100_000_000_000L, reminder?.dueDateMillis)
    }

    @Test
    fun `clearing the expiry date deletes the reminder`() = runTest {
        val (docId, reminderId) = repository.addOrUpdate(newDocument(expiryDateMillis = 2_000_000_000_000L))
        val saved = repository.getById(docId)!!

        val (_, newReminderId) = repository.addOrUpdate(saved.copy(expiryDateMillis = null))

        assertNull(newReminderId)
        assertNull(repository.getById(docId)?.reminderId)
        assertNull(db.reminderDao().getById(reminderId!!))
    }

    @Test
    fun `deleting the document deletes its reminder`() = runTest {
        val (docId, reminderId) = repository.addOrUpdate(newDocument(expiryDateMillis = 2_000_000_000_000L))
        val saved = repository.getById(docId)!!

        repository.delete(saved)

        assertNull(repository.getById(docId))
        assertNull(db.reminderDao().getById(reminderId!!))
    }

    @Test
    fun `a document without an expiry date never creates a reminder`() = runTest {
        val (docId, reminderId) = repository.addOrUpdate(newDocument(expiryDateMillis = null))
        assertNull(reminderId)
        assertNull(repository.getById(docId)?.reminderId)
    }
}
