package com.rovena.garage.domain.usecase

/**
 * Pure validation rules for the `.motiva` backup format. The actual zip/file
 * I/O lives in the app module (needs Android SAF); this object only decides
 * whether a parsed manifest is safe to restore.
 */
object BackupVersionValidator {

    /** Current backup schema version this build of Rovena writes. */
    const val CURRENT_BACKUP_FORMAT_VERSION = 1

    /** Oldest backup schema version this build can still read. */
    const val MIN_SUPPORTED_BACKUP_FORMAT_VERSION = 1

    data class Manifest(
        val backupFormatVersion: Int,
        val appVersionCode: Int,
        val vehicleCount: Int,
        val checksumValid: Boolean
    )

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data object CorruptFile : ValidationResult()
        data class UnsupportedVersion(val foundVersion: Int) : ValidationResult()
        data object Empty : ValidationResult()
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
