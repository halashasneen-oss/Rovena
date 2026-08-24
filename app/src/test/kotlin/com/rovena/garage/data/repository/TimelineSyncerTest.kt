package com.rovena.garage.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.DocumentType
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.flow.first
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
 * Every vehicle-scoped record type keeps the unified Timeline in sync via one
 * `upsertForX`/`removeForSource` pair (see TimelineSyncer) - these are only ever
 * exercised incidentally as a side effect inside other repositories' tests, so this
 * asserts the actual TimelineEventEntity fields each mapping produces, directly,
 * for every record type.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: see
 * InspectionRepositoryTest for why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class TimelineSyncerTest {

    private lateinit var db: RovenaDatabase
    private lateinit var syncer: TimelineSyncer
    private var vehicleId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RovenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        syncer = TimelineSyncer(db.timelineDao())
        vehicleId = runBlocking {
            db.vehicleDao().insert(
                VehicleEntity(make = "Mazda", model = "3", year = 2022, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 5_000)
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsertForFuel maps type, amount, currency and mileage`() = runTest {
        syncer.upsertForFuel(
            FuelRecordEntity(id = 1, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 5_100, liters = 40.0, pricePerLiter = 1.0, totalCost = 40.0, currencyCode = "JOD", fuelType = FuelType.PETROL)
        )

        val event = db.timelineDao().getBySource(TimelineEventType.FUEL, 1)!!
        assertEquals(TimelineEventType.FUEL, event.type)
        assertEquals(40.0, event.amount)
        assertEquals("JOD", event.currencyCode)
        assertEquals(5_100, event.mileageKm)
    }

    @Test
    fun `upsertForMaintenance maps category name as title and cost as amount`() = runTest {
        syncer.upsertForMaintenance(
            MaintenanceRecordEntity(id = 2, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 5_200, category = MaintenanceCategory.BRAKE_PADS, description = "Front pads", cost = 60.0, currencyCode = "JOD")
        )

        val event = db.timelineDao().getBySource(TimelineEventType.MAINTENANCE, 2)!!
        assertEquals("BRAKE_PADS", event.title)
        assertEquals(60.0, event.amount)
    }

    @Test
    fun `upsertForExpense falls back to category name when description is blank`() = runTest {
        syncer.upsertForExpense(
            ExpenseEntity(id = 3, vehicleId = vehicleId, dateMillis = 1_000L, amount = 15.0, category = ExpenseCategory.PARKING, description = "  ")
        )

        val event = db.timelineDao().getBySource(TimelineEventType.EXPENSE, 3)!!
        assertEquals("PARKING", event.title)
    }

    @Test
    fun `upsertForExpense keeps a non-blank description as the title`() = runTest {
        syncer.upsertForExpense(
            ExpenseEntity(id = 4, vehicleId = vehicleId, dateMillis = 1_000L, amount = 15.0, category = ExpenseCategory.PARKING, description = "Airport lot")
        )

        val event = db.timelineDao().getBySource(TimelineEventType.EXPENSE, 4)!!
        assertEquals("Airport lot", event.title)
    }

    @Test
    fun `upsertForDocument has no amount or mileage`() = runTest {
        syncer.upsertForDocument(
            DocumentEntity(id = 5, vehicleId = vehicleId, name = "Registration", type = DocumentType.REGISTRATION, filePath = "/tmp/reg.pdf")
        )

        val event = db.timelineDao().getBySource(TimelineEventType.DOCUMENT, 5)!!
        assertEquals("Registration", event.title)
        assertNull(event.amount)
        assertNull(event.mileageKm)
    }

    @Test
    fun `upsertForInspection maps overall score as amount`() = runTest {
        syncer.upsertForInspection(
            InspectionEntity(id = 6, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 5_300, overallScore = 82)
        )

        val event = db.timelineDao().getBySource(TimelineEventType.INSPECTION, 6)!!
        assertEquals(82.0, event.amount)
        assertEquals(5_300, event.mileageKm)
    }

    @Test
    fun `upsertForReminder maps title and due mileage`() = runTest {
        syncer.upsertForReminder(
            ReminderEntity(id = 7, vehicleId = vehicleId, title = "Insurance renewal", basis = ReminderBasis.DATE, dueDateMillis = 2_000_000_000_000L, dueMileageKm = 5_400)
        )

        val event = db.timelineDao().getBySource(TimelineEventType.REMINDER, 7)!!
        assertEquals("Insurance renewal", event.title)
        assertEquals(5_400, event.mileageKm)
    }

    @Test
    fun `upsertForVehicleUpdate uses the vehicle's own id as the source id`() = runTest {
        val vehicle = db.vehicleDao().getById(vehicleId)!!.copy(currentMileageKm = 5_500)
        syncer.upsertForVehicleUpdate(vehicle, note = "MILEAGE_UPDATE")

        val event = db.timelineDao().getBySource(TimelineEventType.VEHICLE_UPDATE, vehicleId)!!
        assertEquals("MILEAGE_UPDATE", event.title)
        assertEquals(5_500, event.mileageKm)
    }

    @Test
    fun `re-upserting the same source record updates the existing row rather than duplicating it`() = runTest {
        syncer.upsertForMaintenance(
            MaintenanceRecordEntity(id = 8, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 5_000, category = MaintenanceCategory.ENGINE_OIL, description = "Oil change", cost = 30.0)
        )
        val firstId = db.timelineDao().getBySource(TimelineEventType.MAINTENANCE, 8)!!.id

        syncer.upsertForMaintenance(
            MaintenanceRecordEntity(id = 8, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 5_000, category = MaintenanceCategory.ENGINE_OIL, description = "Oil change", cost = 35.0)
        )
        val updated = db.timelineDao().getBySource(TimelineEventType.MAINTENANCE, 8)!!

        assertEquals(firstId, updated.id)
        assertEquals(35.0, updated.amount)
        assertEquals(1, db.timelineDao().observeByVehicle(vehicleId).first().size)
    }

    @Test
    fun `removeForSource deletes only the matching type and source id`() = runTest {
        syncer.upsertForFuel(FuelRecordEntity(id = 9, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 5_000, liters = 30.0, pricePerLiter = 1.0, totalCost = 30.0, fuelType = FuelType.PETROL))
        syncer.upsertForMaintenance(MaintenanceRecordEntity(id = 9, vehicleId = vehicleId, dateMillis = 1_000L, mileageKm = 5_000, category = MaintenanceCategory.ENGINE_OIL, description = "Oil change"))

        syncer.removeForSource(TimelineEventType.FUEL, 9)

        assertNull(db.timelineDao().getBySource(TimelineEventType.FUEL, 9))
        assertEquals("ENGINE_OIL", db.timelineDao().getBySource(TimelineEventType.MAINTENANCE, 9)?.title)
    }
}
