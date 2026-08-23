package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Staged reminder notification schedule (spec: Registration/Insurance
 * reminder schedule). `lastNotifiedStageDays` must survive an unrelated edit
 * (e.g. a title fix) but reset the moment the due date itself actually
 * changes (renewal) - see ReminderRepository.resetStageIfDateChanged.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: see
 * InspectionRepositoryTest for why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ReminderRepositoryTest {

    private lateinit var db: RovenaDatabase
    private lateinit var repository: ReminderRepository
    private var vehicleId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ReminderRepository(db.reminderDao(), TimelineSyncer(db.timelineDao()))
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Nissan", model = "Altima", year = 2018, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 60_000)
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `markNotifiedStage persists the stage`() = runTest {
        val id = repository.addOrUpdate(
            ReminderEntity(vehicleId = vehicleId, title = "Registration", basis = ReminderBasis.DATE, dueDateMillis = 2_000_000_000_000L)
        )

        repository.markNotifiedStage(id, 30)

        assertEquals(30, db.reminderDao().getById(id)?.lastNotifiedStageDays)
    }

    @Test
    fun `editing an unrelated field preserves the notified stage`() = runTest {
        val id = repository.addOrUpdate(
            ReminderEntity(vehicleId = vehicleId, title = "Registration", basis = ReminderBasis.DATE, dueDateMillis = 2_000_000_000_000L)
        )
        repository.markNotifiedStage(id, 14)
        val saved = db.reminderDao().getById(id)!!

        repository.addOrUpdate(saved.copy(title = "Registration (renewed name)"))

        assertEquals(14, db.reminderDao().getById(id)?.lastNotifiedStageDays)
    }

    @Test
    fun `changing the due date resets the notified stage`() = runTest {
        val id = repository.addOrUpdate(
            ReminderEntity(vehicleId = vehicleId, title = "Registration", basis = ReminderBasis.DATE, dueDateMillis = 2_000_000_000_000L)
        )
        repository.markNotifiedStage(id, 0)
        val saved = db.reminderDao().getById(id)!!

        repository.addOrUpdate(saved.copy(dueDateMillis = 2_100_000_000_000L))

        assertNull(db.reminderDao().getById(id)?.lastNotifiedStageDays)
    }

    @Test
    fun `a rolled-over recurring reminder gets a fresh stage`() = runTest {
        val id = repository.addOrUpdate(
            ReminderEntity(
                vehicleId = vehicleId, title = "Oil change", basis = ReminderBasis.DATE,
                dueDateMillis = 2_000_000_000_000L, isRecurring = true, intervalMonths = 6
            )
        )
        repository.markNotifiedStage(id, 0)
        val reminder = db.reminderDao().getById(id)!!

        repository.markCompleted(reminder)

        assertNull(db.reminderDao().getById(id)?.lastNotifiedStageDays)
    }
}
