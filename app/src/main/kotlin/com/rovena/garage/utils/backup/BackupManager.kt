package com.rovena.garage.utils.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.rovena.garage.AppContainer
import com.rovena.garage.BuildConfig
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.BackupMetadataEntity
import com.rovena.garage.domain.model.BackupRecordType
import com.rovena.garage.domain.model.PhotoLinkedType
import com.rovena.garage.domain.usecase.BackupPathValidator
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

/** User-facing progress stages for a backup/restore operation in flight (spec: backup security - always show progress, never let the UI look frozen). */
enum class BackupStage { PREPARING, CREATING, VALIDATING, RESTORING, VERIFYING }

data class BackupInspection(
    val manifest: BackupVersionValidator.Manifest?,
    val validation: BackupVersionValidator.ValidationResult,
    val extractedDir: File?
)

/**
 * Creates and restores `.rovena` backup files (spec #20): a zip containing a
 * manifest, a full snapshot of the Room database, and every locally-stored
 * photo/document/receipt file, all read/written through Storage Access
 * Framework `Uri`s so nothing needs broad storage permissions.
 *
 * Backward compatibility: restore never gates on file extension - the SAF
 * document picker accepts any file and validity is judged purely from the
 * zip's contents (manifest + database), so backup files created by earlier
 * app versions under the old `.motiva` extension still restore correctly.
 *
 * Security note: [inspect] is the *only* place a `.rovena` archive's bytes
 * are ever written to disk, so it is the single choke point where a
 * malicious archive must be defeated - Zip Slip path traversal
 * ([BackupPathValidator]) and zip-bomb entry/size limits are both enforced
 * there, before either restore path ever touches the extracted directory.
 */
object BackupManager {

    private const val ENTRY_MANIFEST = "manifest.json"
    private const val ENTRY_DATABASE = "database.db"
    private const val FILES_PREFIX = "files/"
    private const val COPY_BUFFER_SIZE = 8192

    /**
     * When [password] is non-null, the zip is built into a temp file first and
     * then encrypted (see [BackupEncryption]) into [destination] as a
     * `.rovena.secure` file, rather than writing the zip there directly.
     */
    suspend fun createBackup(
        context: Context,
        container: AppContainer,
        destination: Uri,
        password: String? = null,
        onProgress: (BackupStage) -> Unit = {}
    ): BackupResult = withContext(Dispatchers.IO) {
        val tempZipFile = if (password != null) File.createTempFile("rovena_backup_", ".tmp", context.cacheDir) else null
        try {
            onProgress(BackupStage.PREPARING)
            // Flush WAL into the main database file so the copy is a complete, consistent snapshot.
            container.database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath(RovenaDatabase.DATABASE_NAME)
            val vehicleCount = container.vehicleRepository.getAllOnce().size
            val checksum = sha256(dbFile)

            val filePaths = listOf("documents", "photos", "receipts")
                .map { File(context.filesDir, it) }
                .filter { it.exists() }
                .flatMap { it.listFiles()?.toList() ?: emptyList() }
            val totalSizeBytes = dbFile.length() + filePaths.sumOf { it.length() }

            val manifest = JSONObject().apply {
                put("backupFormatVersion", BackupVersionValidator.CURRENT_BACKUP_FORMAT_VERSION)
                put("databaseSchemaVersion", BackupVersionValidator.CURRENT_DATABASE_SCHEMA_VERSION)
                put("appVersionCode", BuildConfig.VERSION_CODE)
                put("appVersionName", BuildConfig.VERSION_NAME)
                put("createdAtMillis", System.currentTimeMillis())
                put("vehicleCount", vehicleCount)
                put("fileCount", filePaths.size)
                put("totalSizeBytes", totalSizeBytes)
                put("checksum", checksum)
            }

            fun writeZip(out: java.io.OutputStream) {
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
            }

            onProgress(BackupStage.CREATING)
            if (password != null && tempZipFile != null) {
                tempZipFile.outputStream().use { writeZip(it) }
                val encrypted = BackupEncryption.encrypt(tempZipFile.readBytes(), password)
                context.contentResolver.openOutputStream(destination)?.use { it.write(encrypted) }
                    ?: return@withContext BackupResult.Error("Could not open destination")
            } else {
                context.contentResolver.openOutputStream(destination)?.use { writeZip(it) }
                    ?: return@withContext BackupResult.Error("Could not open destination")
            }

            container.backupMetadataRepository.record(
                BackupMetadataEntity(
                    fileName = destination.lastPathSegment ?: "backup.rovena",
                    type = BackupRecordType.CREATED,
                    backupFormatVersion = BackupVersionValidator.CURRENT_BACKUP_FORMAT_VERSION,
                    vehicleCount = vehicleCount,
                    sizeBytes = dbFile.length()
                )
            )

            BackupResult.Success(vehicleCount)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Unknown error")
        } finally {
            tempZipFile?.delete()
        }
    }

    /**
     * Extracts the backup into a temp working directory and validates it without
     * touching live data. Every entry name is checked with [BackupPathValidator]
     * before a single byte is written (Zip Slip defense), and both the entry
     * count and total decompressed size are capped (zip-bomb defense) - either
     * violation aborts extraction and reports [BackupVersionValidator.ValidationResult.UnsafeArchive]
     * rather than throwing, so a hostile file can never crash the app.
     *
     * Transparently handles `.rovena.secure` files: a quick magic-header peek
     * decides whether [source] needs [password] at all, so a plain `.rovena`
     * file is still streamed straight from its content-resolver stream exactly
     * as before (never fully buffered in memory) - only an encrypted archive
     * is read fully into memory, which AES-GCM decryption requires anyway.
     */
    suspend fun inspect(
        context: Context,
        source: Uri,
        password: String? = null,
        onProgress: (BackupStage) -> Unit = {}
    ): BackupInspection = withContext(Dispatchers.IO) {
        val workDir = File(context.cacheDir, "restore_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            onProgress(BackupStage.VALIDATING)
            val magicPeek = context.contentResolver.openInputStream(source)?.use { input ->
                val buffer = ByteArray(BackupEncryption.MAGIC.size)
                if (input.read(buffer) == buffer.size) buffer else null
            }
            val isEncrypted = magicPeek != null && BackupEncryption.isEncrypted(magicPeek)

            val zipStream: ZipInputStream = if (isEncrypted) {
                if (password == null) {
                    return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.PasswordRequired, null)
                }
                val rawBytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
                    ?: return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.CorruptFile, null)
                val decrypted = BackupEncryption.decrypt(rawBytes, password)
                    ?: return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.WrongPassword, null)
                ZipInputStream(java.io.ByteArrayInputStream(decrypted))
            } else {
                val input = context.contentResolver.openInputStream(source)
                    ?: return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.CorruptFile, null)
                ZipInputStream(input)
            }

            var entryCount = 0
            var totalBytes = 0L

            zipStream.use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > BackupVersionValidator.MAX_ZIP_ENTRIES) {
                        return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.UnsafeArchive, workDir)
                    }

                    val outFile = BackupPathValidator.resolveSafeEntry(workDir, entry.name)
                        ?: return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.UnsafeArchive, workDir)

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out ->
                            val buffer = ByteArray(COPY_BUFFER_SIZE)
                            var read = zip.read(buffer)
                            while (read >= 0) {
                                totalBytes += read
                                if (totalBytes > BackupVersionValidator.MAX_TOTAL_UNCOMPRESSED_BYTES) {
                                    return@withContext BackupInspection(null, BackupVersionValidator.ValidationResult.UnsafeArchive, workDir)
                                }
                                out.write(buffer, 0, read)
                                read = zip.read(buffer)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

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
                checksumValid = expectedChecksum.isNotBlank() && expectedChecksum == actualChecksum,
                databaseSchemaVersion = json.optInt("databaseSchemaVersion", BackupVersionValidator.CURRENT_DATABASE_SCHEMA_VERSION)
            )
            BackupInspection(manifest, BackupVersionValidator.validate(manifest), workDir)
        } catch (e: Exception) {
            BackupInspection(null, BackupVersionValidator.ValidationResult.CorruptFile, workDir)
        }
    }

    /**
     * Replaces all current data with the backup's contents. Caller must have already
     * confirmed with the user.
     *
     * Transactional-style safety (spec #7): the backup's database is verified to
     * actually open through Room - with the real migration chain - *before* a
     * single byte of the live database or live files is touched. Only then is the
     * current live state snapshotted into a rollback copy, and only after that
     * snapshot succeeds does the actual swap happen. If anything from the swap
     * onward throws, the rollback copy is restored so the user's original garage
     * comes back exactly as it was - the original data is never deleted-then-hoped
     * to be replaced; it is preserved until the new data has proven itself.
     */
    suspend fun restoreReplacing(context: Context, extractedDir: File, onProgress: (BackupStage) -> Unit = {}): BackupResult = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(RovenaDatabase.DATABASE_NAME)
        val rollbackDir = File(context.cacheDir, "restore_rollback_${UUID.randomUUID()}")
        var swapped = false
        try {
            onProgress(BackupStage.VALIDATING)
            val backupDbFile = File(extractedDir, ENTRY_DATABASE)
            if (!backupDbFile.exists()) return@withContext BackupResult.Error("Backup database missing")
            verifyDatabaseOpens(context, backupDbFile)

            // Snapshot current live state now that the incoming data has proven it
            // opens cleanly - this is the last point at which nothing has been
            // touched yet, so a failure snapshotting itself is still perfectly safe.
            rollbackDir.mkdirs()
            RovenaDatabase.closeInstance()
            if (dbFile.exists()) dbFile.copyTo(File(rollbackDir, ENTRY_DATABASE), overwrite = true)
            listOf("documents", "photos", "receipts").forEach { subDir ->
                val liveDir = File(context.filesDir, subDir)
                if (liveDir.exists()) liveDir.copyRecursively(File(rollbackDir, subDir), overwrite = true)
            }

            onProgress(BackupStage.RESTORING)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            backupDbFile.copyTo(dbFile, overwrite = true)
            swapped = true

            // The live documents/photos/receipts directories are cleared so the result
            // faithfully matches the backup with no leftover files from records that no
            // longer exist after the swap - safe now because the rollback copy above
            // already has everything needed to restore them if a later step fails.
            listOf("documents", "photos", "receipts").forEach { subDir ->
                File(context.filesDir, subDir).deleteRecursively()
            }
            restoreFilesInto(context, extractedDir)

            onProgress(BackupStage.VERIFYING)
            verifyDatabaseOpens(context, dbFile)

            extractedDir.deleteRecursively()
            BackupResult.Success(0)
        } catch (e: Exception) {
            if (swapped) {
                runCatching {
                    RovenaDatabase.closeInstance()
                    val rollbackDbFile = File(rollbackDir, ENTRY_DATABASE)
                    if (rollbackDbFile.exists()) {
                        File(dbFile.path + "-wal").delete()
                        File(dbFile.path + "-shm").delete()
                        rollbackDbFile.copyTo(dbFile, overwrite = true)
                    }
                    listOf("documents", "photos", "receipts").forEach { subDir ->
                        File(context.filesDir, subDir).deleteRecursively()
                        val rollbackSubDir = File(rollbackDir, subDir)
                        if (rollbackSubDir.exists()) rollbackSubDir.copyRecursively(File(context.filesDir, subDir), overwrite = true)
                    }
                }
            }
            BackupResult.Error(e.message ?: "Unknown error")
        } finally {
            rollbackDir.deleteRecursively()
        }
    }

    /** Opens [dbFile] through the app's real Room builder (with its full migration chain) and forces it to actually read, then closes it - throws if the file is unreadable or fails to migrate. Never mutates [RovenaDatabase]'s cached singleton. */
    private suspend fun verifyDatabaseOpens(context: Context, dbFile: File) {
        val db = Room.databaseBuilder(context, RovenaDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .addMigrations(*com.rovena.garage.data.local.database.Migrations.ALL)
            .build()
        try {
            db.vehicleDao().getAllOnce()
        } finally {
            db.close()
        }
    }

    /**
     * Copies every vehicle (and every dependent record: maintenance, fuel, expenses,
     * reminders, documents, inspections, inspection items, and every linked photo) from
     * the backup into the current live garage, assigning fresh IDs throughout and
     * remapping every foreign key explicitly so nothing in the new garage ever points
     * back at an id from the old one. Files are copied into live storage under fresh
     * UUID names (never the backup's original filename) so they can never collide with
     * an existing file already used by the current garage.
     */
    suspend fun restoreAsNewGarage(context: Context, container: AppContainer, extractedDir: File, onProgress: (BackupStage) -> Unit = {}): BackupResult = withContext(Dispatchers.IO) {
        var sourceDb: RovenaDatabase? = null
        try {
            onProgress(BackupStage.RESTORING)
            val sourceDbFile = File(extractedDir, ENTRY_DATABASE)
            sourceDb = Room.databaseBuilder(context, RovenaDatabase::class.java, sourceDbFile.absolutePath)
                .allowMainThreadQueries()
                // A backup made by an older app version carries an older schema on disk -
                // without these, opening it here would throw rather than silently corrupt
                // data (no destructive fallback, by design - see RovenaDatabase.build()).
                .addMigrations(*com.rovena.garage.data.local.database.Migrations.ALL)
                .build()

            // "subDir/originalFileName" (e.g. "photos/car.jpg") -> freshly copied absolute
            // path. Keyed by the *backup's relative* name, not the old absolute path -
            // the DB rows store absolute paths from the original install, which mean
            // nothing on this device/session.
            val pathMap = copyBackupFilesAsNewFiles(context, extractedDir)

            val vehicles = sourceDb.vehicleDao().getAllOnce()
            var imported = 0
            for (oldVehicle in vehicles) {
                val newVehicleId = container.vehicleRepository.addVehicle(
                    oldVehicle.copy(id = 0, isPrimary = false, photoPath = remapPath(pathMap, oldVehicle.photoPath, "photos"))
                )
                imported++

                val maintenanceIdMap = mutableMapOf<Long, Long>()
                sourceDb.maintenanceDao().getByVehicleOnce(oldVehicle.id).forEach { old ->
                    val newId = container.maintenanceRepository.addOrUpdate(old.copy(id = 0, vehicleId = newVehicleId))
                    maintenanceIdMap[old.id] = newId
                }

                sourceDb.fuelDao().getByVehicleOrderedByMileage(oldVehicle.id).forEach {
                    container.fuelRepository.addOrUpdate(it.copy(id = 0, vehicleId = newVehicleId))
                }

                val expenseIdMap = mutableMapOf<Long, Long>()
                sourceDb.expenseDao().getByVehicleOnce(oldVehicle.id).forEach { old ->
                    val newId = container.expenseRepository.addOrUpdate(
                        old.copy(id = 0, vehicleId = newVehicleId, receiptPhotoPath = remapPath(pathMap, old.receiptPhotoPath, "receipts"))
                    )
                    expenseIdMap[old.id] = newId
                }

                sourceDb.reminderDao().getActiveOnce(oldVehicle.id).forEach {
                    container.reminderRepository.addOrUpdate(it.copy(id = 0, vehicleId = newVehicleId))
                }

                sourceDb.vehicleNoteDao().getByVehicleOnce(oldVehicle.id).forEach {
                    container.vehicleNoteRepository.addOrUpdate(it.copy(id = 0, vehicleId = newVehicleId))
                }

                sourceDb.partDao().getByVehicleOnce(oldVehicle.id).forEach {
                    container.partRepository.addOrUpdate(it.copy(id = 0, vehicleId = newVehicleId))
                }

                val documentIdMap = mutableMapOf<Long, Long>()
                sourceDb.documentDao().getByVehicleOnce(oldVehicle.id).forEach { old ->
                    val (newId, _) = container.documentRepository.addOrUpdate(
                        old.copy(
                            id = 0, vehicleId = newVehicleId, reminderId = null,
                            filePath = remapPath(pathMap, old.filePath, "documents") ?: old.filePath
                        )
                    )
                    documentIdMap[old.id] = newId
                }

                // Inspections + their items, each with an explicit old-id -> new-id map so
                // inspection-item photos below can be relinked correctly.
                val inspectionIdMap = mutableMapOf<Long, Long>()
                val inspectionItemIdMap = mutableMapOf<Long, Long>()
                sourceDb.inspectionDao().getByVehicleOnce(oldVehicle.id).forEach { oldInspection ->
                    val oldItems = sourceDb.inspectionItemDao().getByInspectionOnce(oldInspection.id)
                    val newInspectionId = container.inspectionRepository.saveInspection(
                        oldInspection.copy(id = 0, vehicleId = newVehicleId),
                        oldItems.map { it.copy(id = 0, inspectionId = 0) }
                    )
                    inspectionIdMap[oldInspection.id] = newInspectionId

                    // saveInspection upserts by itemKey, so match old items back to their
                    // freshly-created rows by that same key to build the item id map.
                    val newItemsByKey = container.inspectionRepository.getItemsOnce(newInspectionId).associateBy { it.itemKey }
                    oldItems.forEach { oldItem ->
                        newItemsByKey[oldItem.itemKey]?.let { newItem -> inspectionItemIdMap[oldItem.id] = newItem.id }
                    }
                }

                sourceDb.vehiclePhotoDao().getByVehicleOnce(oldVehicle.id).forEach { oldPhoto ->
                    val newLinkedId = when (oldPhoto.linkedType) {
                        PhotoLinkedType.VEHICLE -> newVehicleId
                        PhotoLinkedType.MAINTENANCE -> oldPhoto.linkedId?.let { maintenanceIdMap[it] }
                        PhotoLinkedType.EXPENSE -> oldPhoto.linkedId?.let { expenseIdMap[it] }
                        PhotoLinkedType.DOCUMENT -> oldPhoto.linkedId?.let { documentIdMap[it] }
                        PhotoLinkedType.INSPECTION -> oldPhoto.linkedId?.let { inspectionIdMap[it] }
                        PhotoLinkedType.INSPECTION_ITEM -> oldPhoto.linkedId?.let { inspectionItemIdMap[it] }
                    }
                    // A photo whose parent record couldn't be remapped (e.g. it referenced a
                    // row that failed to import) would dangle - skip it rather than restore a
                    // broken reference.
                    if (oldPhoto.linkedId != null && newLinkedId == null) return@forEach

                    val newFilePath = remapPath(pathMap, oldPhoto.filePath, "photos") ?: oldPhoto.filePath
                    container.photoRepository.add(
                        oldPhoto.copy(
                            id = 0,
                            vehicleId = newVehicleId,
                            linkedId = newLinkedId,
                            filePath = newFilePath,
                            thumbnailPath = remapPath(pathMap, oldPhoto.thumbnailPath, "photos")
                        )
                    )
                }
            }
            extractedDir.deleteRecursively()
            BackupResult.Success(imported)
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Unknown error")
        } finally {
            sourceDb?.close()
        }
    }

    /**
     * Copies every file under `files/` in an already-safely-extracted backup into live
     * storage under a fresh UUID filename (see [restoreAsNewGarage] doc), returning a map
     * keyed by "subDir/originalFileName" (e.g. "photos/car.jpg") to the new absolute path
     * - see [remapPath]. Never overwrites an existing live file, because it never reuses
     * an existing name in the first place.
     */
    private fun copyBackupFilesAsNewFiles(context: Context, extractedDir: File): Map<String, String> {
        val pathMap = mutableMapOf<String, String>()
        listOf("documents", "photos", "receipts").forEach { subDir ->
            val sourceDir = File(extractedDir, "$FILES_PREFIX$subDir")
            if (sourceDir.exists()) {
                val destDir = File(context.filesDir, subDir).apply { mkdirs() }
                sourceDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val extension = file.extension.let { if (it.isNotBlank()) ".$it" else "" }
                        val destFile = File(destDir, "${UUID.randomUUID()}$extension")
                        runCatching { file.copyTo(destFile, overwrite = false) }
                            .onSuccess { pathMap["$subDir/${file.name}"] = destFile.absolutePath }
                    }
                }
            }
        }
        return pathMap
    }

    /**
     * Resolves an old (original-install-absolute) file path stored on a backed-up entity
     * to its freshly-restored path, by looking up just the filename under [subDir] in
     * [pathMap] - the entity's stored absolute path is meaningless on this device/session,
     * only the filename it ends in matters.
     */
    private fun remapPath(pathMap: Map<String, String>, oldPath: String?, subDir: String): String? {
        oldPath ?: return null
        return pathMap["$subDir/${File(oldPath).name}"]
    }

    /** Used by [restoreReplacing]: the live directories are the target of a full swap, so reusing original filenames is correct there. */
    private fun restoreFilesInto(context: Context, extractedDir: File) {
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
