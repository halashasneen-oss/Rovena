package com.rovena.garage.utils.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rovena.garage.AppContainer
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.data.local.entities.VehiclePhotoEntity
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.PhotoLinkedType
import com.rovena.garage.domain.model.TransmissionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * `restoreAsNewGarage` (spec: "import as new vehicles" restore mode) is the most
 * subtle, least-tested piece of the backup system - ~150 lines of manual foreign-key
 * remapping (vehicle -> maintenance/fuel/expenses/documents/inspections/photos, each
 * assigned a fresh id, with every child row's old-id references rewritten to match)
 * that nothing exercised end-to-end before this test. It works directly against an
 * already-extracted backup directory (bypassing the zip/SAF/Uri layer inspect() owns,
 * which - along with the Zip Slip and zip-bomb defenses inspect() delegates to -
 * BackupPathValidatorTest/BackupVersionValidatorTest already cover thoroughly at the
 * validator level), so it exercises exactly the untested part: the id-remapping
 * orchestration against real Room databases via Robolectric.
 *
 * Uses a bare android.app.Application rather than the real RovenaApp: see
 * InspectionRepositoryTest for why. Unlike every other repository test in this suite,
 * this one deliberately does NOT use an in-memory database for the "live" side - it
 * uses the real file-backed RovenaDatabase.getInstance()/AppContainer wiring, since
 * BackupManager itself reads/writes real files on disk (context.filesDir/cacheDir)
 * and a real db file, not an abstraction that would work identically in-memory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class BackupManagerTest {

    private lateinit var context: android.content.Context
    private lateinit var container: AppContainer
    private lateinit var sourceDbFile: File
    private lateinit var extractedDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RovenaDatabase.closeInstance()
        context.getDatabasePath(RovenaDatabase.DATABASE_NAME).delete()
        container = AppContainer(context)

        sourceDbFile = File(context.cacheDir, "backup_test_source.db")
        extractedDir = File(context.cacheDir, "backup_test_extracted")
        extractedDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        RovenaDatabase.closeInstance()
        context.getDatabasePath(RovenaDatabase.DATABASE_NAME).delete()
        sourceDbFile.delete()
        extractedDir.deleteRecursively()
    }

    /** Builds the "backup's" source database directly (mirroring what inspect() would have already extracted), independent of the live AppContainer's database. */
    private fun buildSourceDb(): RovenaDatabase =
        Room.databaseBuilder(context, RovenaDatabase::class.java, sourceDbFile.absolutePath)
            .allowMainThreadQueries()
            .addMigrations(*com.rovena.garage.data.local.database.Migrations.ALL)
            .build()

    /** Flushes WAL into the main file before close, exactly like BackupManager.createBackup() does, so copying [sourceDbFile] afterward doesn't silently drop the rows this test just wrote. */
    private fun RovenaDatabase.checkpointAndClose() {
        openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        close()
    }

    @Test
    fun `restoreAsNewGarage imports a vehicle, its maintenance record, and its photo under fresh ids`() = runTest {
        val sourcePhotoFile = File(extractedDir, "files/photos/car.jpg").apply {
            parentFile?.mkdirs()
            writeText("fake source photo bytes")
        }

        val sourceVehicleId: Long
        val sourceDb = buildSourceDb()
        try {
            sourceVehicleId = runBlocking {
                sourceDb.vehicleDao().insert(
                    VehicleEntity(make = "Nissan", model = "Altima", year = 2019, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 45_000)
                )
            }
            runBlocking {
                sourceDb.maintenanceDao().insert(
                    MaintenanceRecordEntity(vehicleId = sourceVehicleId, dateMillis = 1_000L, mileageKm = 45_000, category = MaintenanceCategory.ENGINE_OIL, description = "Oil change")
                )
            }
            runBlocking {
                sourceDb.vehiclePhotoDao().insert(
                    VehiclePhotoEntity(vehicleId = sourceVehicleId, linkedType = PhotoLinkedType.VEHICLE, linkedId = null, filePath = sourcePhotoFile.absolutePath)
                )
            }
        } finally {
            sourceDb.checkpointAndClose()
        }
        sourceDbFile.copyTo(File(extractedDir, "database.db"), overwrite = true)

        val result = BackupManager.restoreAsNewGarage(context, container, extractedDir)

        assertTrue("expected Success, got $result", result is BackupResult.Success)
        assertEquals(1, (result as BackupResult.Success).vehicleCount)

        val liveVehicles = container.vehicleRepository.getAllOnce()
        assertEquals(1, liveVehicles.size)
        val newVehicle = liveVehicles.single()
        assertEquals("Nissan", newVehicle.make)

        val liveMaintenance = container.maintenanceRepository.observeByVehicle(newVehicle.id).first()
        assertEquals(1, liveMaintenance.size)
        // The real "fresh id" invariant that matters is this: the imported record is
        // re-parented to the *new* vehicle, not left pointing at the old vehicle's id.
        // Asserting sourceId != newVehicle.id itself would be a coincidence-dependent
        // check, not a real one - the source and live databases are two entirely
        // separate, independently-autoincrementing SQLite files, both starting fresh
        // in this test, so their row ids can legitimately collide (both 1) without
        // that meaning anything went wrong.
        assertEquals(newVehicle.id, liveMaintenance.single().vehicleId)

        val livePhotos = container.photoRepository.observeByVehicle(newVehicle.id).first()
        assertEquals(1, livePhotos.size)
        val newPhoto = livePhotos.single()
        // Copied under a fresh UUID filename into live storage, never the backup's own path.
        assertNotEquals(sourcePhotoFile.absolutePath, newPhoto.filePath)
        assertTrue(File(newPhoto.filePath).exists())
        assertEquals("fake source photo bytes", File(newPhoto.filePath).readText())

        // extractedDir is consumed - restoreAsNewGarage deletes it once the import succeeds.
        assertFalse(extractedDir.exists())
    }

    @Test
    fun `restoreAsNewGarage adds alongside an existing vehicle rather than replacing it`() = runTest {
        val existingVehicleId = container.vehicleRepository.addVehicle(
            VehicleEntity(make = "Honda", model = "Civic", year = 2015, fuelType = FuelType.PETROL, transmission = TransmissionType.MANUAL, currentMileageKm = 100_000)
        )

        val sourceDb = buildSourceDb()
        try {
            runBlocking {
                sourceDb.vehicleDao().insert(
                    VehicleEntity(make = "Subaru", model = "Outback", year = 2020, fuelType = FuelType.PETROL, transmission = TransmissionType.AUTOMATIC, currentMileageKm = 20_000)
                )
            }
        } finally {
            sourceDb.checkpointAndClose()
        }
        extractedDir.mkdirs()
        sourceDbFile.copyTo(File(extractedDir, "database.db"), overwrite = true)

        BackupManager.restoreAsNewGarage(context, container, extractedDir)

        val liveVehicles = container.vehicleRepository.getAllOnce()
        assertEquals(2, liveVehicles.size)
        assertTrue(liveVehicles.any { it.id == existingVehicleId && it.make == "Honda" })
        assertTrue(liveVehicles.any { it.make == "Subaru" })
    }
}
