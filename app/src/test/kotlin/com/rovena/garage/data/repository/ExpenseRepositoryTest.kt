package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
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
 * Deleting an expense with a receipt photo must delete that file from disk too, not
 * just its DB row (see ExpenseRepository.delete()). Runs against a real in-memory
 * Room database plus real temp files via Robolectric.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: see
 * InspectionRepositoryTest for why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ExpenseRepositoryTest {

    private lateinit var db: RovenaDatabase
    private lateinit var repository: ExpenseRepository
    private var vehicleId: Long = 0
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExpenseRepository(db.expenseDao(), TimelineSyncer(db.timelineDao()), db)
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Ford", model = "Focus", year = 2017, fuelType = FuelType.PETROL, transmission = TransmissionType.MANUAL, currentMileageKm = 80_000)
            )
        }
        tempDir = File.createTempFile("expense_repo_test", "").apply { delete(); mkdirs() }
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.deleteRecursively()
    }

    private fun newExpense(receiptPhotoPath: String? = null) = ExpenseEntity(
        vehicleId = vehicleId, dateMillis = 1_000L, amount = 25.0, category = ExpenseCategory.PARKING, receiptPhotoPath = receiptPhotoPath
    )

    @Test
    fun `deleting an expense with no receipt photo succeeds`() = runTest {
        val id = repository.addOrUpdate(newExpense())
        val expense = db.expenseDao().getById(id)!!

        repository.delete(expense)

        assertNull(db.expenseDao().getById(id))
    }

    @Test
    fun `deleting an expense deletes its receipt photo file`() = runTest {
        val receiptFile = File(tempDir, "receipt.jpg").apply { writeText("fake receipt bytes") }
        val id = repository.addOrUpdate(newExpense(receiptPhotoPath = receiptFile.absolutePath))
        assertTrue(receiptFile.exists())

        val expense = db.expenseDao().getById(id)!!
        repository.delete(expense)

        assertFalse(receiptFile.exists())
        assertNull(db.expenseDao().getById(id))
    }
}
