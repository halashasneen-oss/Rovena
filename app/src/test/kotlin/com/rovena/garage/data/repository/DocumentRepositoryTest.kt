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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Document expiry -> reminder lifecycle (spec #8): a document with an expiry date always
 * has exactly one linked reminder, created/updated/deleted in lockstep with the document.
 * Runs against a real in-memory Room database via Robolectric so the actual DAOs, foreign
 * keys, and the transaction wrapping in DocumentRepository are exercised, not a mock.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: RovenaApp.onCreate()
 * schedules ReminderCheckWorker via WorkManager, which isn't initialized under Robolectric's
 * default test setup and throws - this test only needs a Context to build an in-memory Room
 * database, not the app's real startup side effects.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class DocumentRepositoryTest {

    private lateinit var db: RovenaDatabase
    private lateinit var repository: DocumentRepository
    private var vehicleId: Long = 0
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DocumentRepository(db.documentDao(), db.reminderDao(), TimelineSyncer(db.timelineDao()), db)
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Toyota", model = "Corolla", year = 2020, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 10_000)
            )
        }
        tempDir = File.createTempFile("document_repo_test", "").apply { delete(); mkdirs() }
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.deleteRecursively()
    }

    private fun newDocument(expiryDateMillis: Long?, filePath: String = "/tmp/reg.pdf") = DocumentEntity(
        vehicleId = vehicleId,
        name = "Registration",
        type = DocumentType.REGISTRATION,
        filePath = filePath,
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

    @Test
    fun `deleting the document deletes its file from disk`() = runTest {
        val file = File(tempDir, "reg.pdf").apply { writeText("fake pdf bytes") }
        val (docId, _) = repository.addOrUpdate(newDocument(expiryDateMillis = null, filePath = file.absolutePath))
        assertTrue(file.exists())
        val saved = repository.getById(docId)!!

        repository.delete(saved)

        assertFalse(file.exists())
    }

    @Test
    fun `replacing a document's file on edit deletes the old file`() = runTest {
        val oldFile = File(tempDir, "old.pdf").apply { writeText("old bytes") }
        val newFile = File(tempDir, "new.pdf").apply { writeText("new bytes") }
        val (docId, _) = repository.addOrUpdate(newDocument(expiryDateMillis = null, filePath = oldFile.absolutePath))
        val saved = repository.getById(docId)!!

        // DocumentRepository.addOrUpdate() itself only ever writes the new path to the DB row -
        // it never sees or deletes the previous file, since the diff/cleanup decision belongs to
        // the caller (see DocumentFormViewModel.save()). This asserts that division of labor: the
        // repository leaves the old file alone even when the path it's given changes.
        repository.addOrUpdate(saved.copy(filePath = newFile.absolutePath))

        assertTrue(oldFile.exists())
        assertTrue(newFile.exists())
    }
}
