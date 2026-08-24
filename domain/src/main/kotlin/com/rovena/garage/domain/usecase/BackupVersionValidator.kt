package com.rovena.garage.domain.usecase

/**
 * Pure validation rules for the `.rovena` backup format. The actual zip/file
 * I/O lives in the app module (needs Android SAF); this object only decides
 * whether a parsed manifest is safe to restore.
 */
object BackupVersionValidator {

    /** Current backup schema version this build of Rovena writes. */
    const val CURRENT_BACKUP_FORMAT_VERSION = 1

    /** Oldest backup schema version this build can still read. */
    const val MIN_SUPPORTED_BACKUP_FORMAT_VERSION = 1

    /** Current Room database schema version this build writes/expects. */
    const val CURRENT_DATABASE_SCHEMA_VERSION = 10

    /** Zip-bomb guard: an archive naming more entries than this is rejected outright. */
    const val MAX_ZIP_ENTRIES = 10_000

    /** Zip-bomb guard: total decompressed bytes across the whole archive is capped here. */
    const val MAX_TOTAL_UNCOMPRESSED_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB

    data class Manifest(
        val backupFormatVersion: Int,
        val appVersionCode: Int,
        val vehicleCount: Int,
        val checksumValid: Boolean,
        val databaseSchemaVersion: Int = CURRENT_DATABASE_SCHEMA_VERSION
    )

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data object CorruptFile : ValidationResult()
        data class UnsupportedVersion(val foundVersion: Int) : ValidationResult()
        data object Empty : ValidationResult()
        /** Archive failed the Zip Slip / zip-bomb / entry-limit safety checks. */
        data object UnsafeArchive : ValidationResult()
        /** A `.rovena.secure` archive whose password hasn't been supplied yet. */
        data object PasswordRequired : ValidationResult()
        /** A `.rovena.secure` archive whose supplied password failed to decrypt it. */
        data object WrongPassword : ValidationResult()
    }

    fun validate(manifest: Manifest?): ValidationResult {
        if (manifest == null) return ValidationResult.CorruptFile
        if (!manifest.checksumValid) return ValidationResult.CorruptFile
        if (manifest.backupFormatVersion < MIN_SUPPORTED_BACKUP_FORMAT_VERSION ||
            manifest.backupFormatVersion > CURRENT_BACKUP_FORMAT_VERSION
        ) {
            return ValidationResult.UnsupportedVersion(manifest.backupFormatVersion)
        }
        if (manifest.vehicleCount < 0) return ValidationResult.CorruptFile
        return ValidationResult.Valid
    }
}
