package com.rovena.garage.utils.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.rovena.garage.AppContainer
import com.rovena.garage.BuildConfig
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.BackupMetadataEntity
import com.rovena.garage.domain.model.BackupRecordType
import com.rovena.garage.domain.usecase.BackupVersionValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed class BackupResult {
    data class Success(val vehicleCount: Int) : BackupResult()
    data class Invalid(val reason: BackupVersionValidator.ValidationResult) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

data class BackupInspection(
    val manifest: BackupVersionValidator.Manifest?,
    val validation: BackupVersionValidator.ValidationResult,
    val extractedDir: File?
)

/**
 * Creates and restores `.motiva` backup files (spec #20): a zip containing a
 * manifest, a full snapshot of the Room database, and every locally-stored
 * photo/document/receipt file, all read/written through Storage Access
 * Framework `Uri`s so nothing needs broad storage permissions.
 */
object BackupManager {

    private const val ENTRY_MANIFEST = "manifest.json"
    private const val ENTRY_DATABASE = "database.db"
    private const val FILES_PREFIX = "files/"

    suspend fun createBackup(context: Context, container: AppContainer, destination: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Flush WAL into the main database file so the copy is a complete, consistent snapshot.
            container.database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath(RovenaDatabase.DATABASE_NAME)
            val vehicleCount = container.vehicleRepository.getAllOnce().size
            val checksum = sha256(dbFile)

            val manifest = JSONObject().apply {
                put("backupFormatVersion", BackupVersionValidator.CURRENT_BACKUP_FORMAT_VERSION)
                put("appVersionCode", BuildConfig.VERSION_CODE)
                put("createdAtMillis", System.currentTimeMillis())
                put("vehicleCount", vehicleCount)
                put("checksum", checksum)
            }

            context.contentResolver.openOutputStream(destination)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
                    zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry(ENTRY_DATABASE))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()

                    listOf("documents", "photos", "receipts").forEach { subDir ->
                        val dir = File(context.filesDir, subDir)
                        if (dir.exists()) {
                            dir.listFiles()?.forEach { file ->
                                zip.putNextEntry(ZipEntry("$FILES_PREFIX$subDir/${file.name}"))
                                file.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            } ?: return@withContext BackupResult.Error("Could not open destination")

            container.backupMetadataRepository.record(
                BackupMetadataEntity(
                    fileName = destination.lastPathSegment ?: "backup.motiva",
                    type = BackupRecordType.CREATED,
                    backupFormatVersion = BackupVersionValidator.CURRENT_BACKUP_FORMAT_VERSION,
                    vehicleCount = vehicleCount,
                    sizeBytes = dbFile.length()
                )
            )

            BackupResult.Success(vehicleCount)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Unknown error")
        }
    }

    /** Extracts the backup into a temp working directory and validates it without touching live data. */
    suspend fun inspect(context: Context, source: Uri): BackupInspection = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "restore_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        val outFile = File(workDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        if (!entry.isDirectory) {
                            outFile.outputStream().use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.CorruptFile, null)

            val manifestFile = File(workDir, ENTRY_MANIFEST)
            val dbFile = File(workDir, ENTRY_DATABASE)
            if (!manifestFile.exists() || !dbFile.exists()) {
                return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.CorruptFile, workDir)
            }

            val json = JSONObject(manifestFile.readText())
            val expectedChecksum = json.optString("checksum", "")
            val actualChecksum = sha256(dbFile)
            val manifest = BackupVersionValidator.Manifest(
                backupFormatVersion = json.optInt("backupFormatVersion", -1),
                appVersionCode = json.optInt("appVersionCode", -1),
                vehicleCount = json.optInt("vehicleCount", -1),
                checksumValid = expectedChecksum.isNotBlank() && expectedChecksum == actualChecksum
            )
            BackupInspection(manifest, BackupVersionValidator.validate(manifest), workDir)
        } catch (e: Exception) {
            BackupInspection(null, BackupVersionValidator.ValidationResult.CorruptFile, workDir)
        }
    }

    /** Replaces all current data with the backup's contents. Caller must have already confirmed with the user. */
    suspend fun restoreReplacing(context: Context, extractedDir: File): BackupResult = withContext(Dispatchers.IO) {
        try {
            RovenaDatabase.closeInstance()
            val dbFile = context.getDatabasePath(RovenaDatabase.DATABASE_NAME)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            File(extractedDir, ENTRY_DATABASE).copyTo(dbFile, overwrite = true)

            restoreFiles(context, extractedDir)
            extractedDir.deleteRecursively()
            BackupResult.Success(0)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Unknown error")
        }
    }

    /** Copies every vehicle (and its dependent records) from the backup into the current live garage, assigning fresh IDs. */
    suspend fun restoreAsNewGarage(context: Context, container: AppContainer, extractedDir: File): BackupResult = withContext(Dispatchers.IO) {
        try {
            val sourceDbFile = File(extractedDir, ENTRY_DATABASE)
            val sourceDb = Room.databaseBuilder(context, RovenaDatabase::class.java, sourceDbFile.absolutePath)
                .allowMainThreadQueries()
                .build()

            restoreFiles(context, extractedDir)

            val vehicles = sourceDb.vehicleDao().getAllOnce()
            var imported = 0
            for (oldVehicle in vehicles) {
                val newVehicleId = container.vehicleRepository.addVehicle(
                    oldVehicle.copy(id = 0, isPrimary = false, photoPath = remapPath(context, oldVehicle.photoPath, "photos"))
                )
                imported++

                sourceDb.maintenanceDao().getByVehicleOnce(oldVehicle.id).forEach {
                    container.maintenanceRepository.addOrUpdate(it.copy(id = 0, vehicleId = newVehicleId))
                }
                sourceDb.fuelDao().getByVehicleOrderedByMileage(oldVehicle.id).forEach {
                    container.fuelRepository.addOrUpdate(it.copy(id = 0, vehicleId = newVehicleId))
                }
                sourceDb.expenseDao().getByVehicleOnce(oldVehicle.id).forEach {
                    container.expenseRepository.addOrUpdate(
                        it.copy(id = 0, vehicleId = newVehicleId, receiptPhotoPath = remapPath(context, it.receiptPhotoPath, "receipts"))
                    )
                }
                sourceDb.reminderDao().getActiveOnce(oldVehicle.id).forEach {
                    container.reminderRepository.addOrUpdate(it.copy(id = 0, vehicleId = newVehicleId))
                }
                sourceDb.documentDao().getByVehicleOnce(oldVehicle.id).forEach { doc ->
                    container.documentRepository.addOrUpdate(
                        doc.copy(id = 0, vehicleId = newVehicleId, reminderId = null, filePath = remapPath(context, doc.filePath, "documents") ?: doc.filePath)
                    )
                }
                sourceDb.vehiclePhotoDao().getByVehicleOnce(oldVehicle.id).forEach {
                    container.photoRepository.add(
                        it.copy(
                            id = 0, vehicleId = newVehicleId,
                            filePath = remapPath(context, it.filePath, "photos") ?: it.filePath,
                            thumbnailPath = remapPath(context, it.thumbnailPath, "photos")
                        )
                    )
                }
            }
            sourceDb.close()
            extractedDir.deleteRecursively()
            BackupResult.Success(imported)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Unknown error")
        }
    }

    /** Backup rows store absolute file paths from the original install; point them at the freshly-copied file in this app's own storage instead. */
    private fun remapPath(context: Context, oldPath: String?, subDir: String): String? {
        oldPath ?: return null
        val fileName = File(oldPath).name
        val candidate = File(File(context.filesDir, subDir), fileName)
        return if (candidate.exists()) candidate.absolutePath else null
    }

    private fun restoreFiles(context: Context, extractedDir: File) {
        listOf("documents", "photos", "receipts").forEach { subDir ->
            val sourceDir = File(extractedDir, "$FILES_PREFIX$subDir")
            if (sourceDir.exists()) {
                val destDir = File(context.filesDir, subDir).apply { mkdirs() }
                sourceDir.listFiles()?.forEach { file -> file.copyTo(File(destDir, file.name), overwrite = true) }
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
