package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackupVersionValidatorTest {

    @Test
    fun `null manifest is corrupt`() {
        assertEquals(BackupVersionValidator.ValidationResult.CorruptFile, BackupVersionValidator.validate(null))
    }

    @Test
    fun `bad checksum is corrupt`() {
        val manifest = BackupVersionValidator.Manifest(1, 1, 2, checksumValid = false)
        assertEquals(BackupVersionValidator.ValidationResult.CorruptFile, BackupVersionValidator.validate(manifest))
    }

    @Test
    fun `future version is unsupported`() {
        val manifest = BackupVersionValidator.Manifest(99, 1, 2, checksumValid = true)
        assertEquals(
            BackupVersionValidator.ValidationResult.UnsupportedVersion(99),
            BackupVersionValidator.validate(manifest)
        )
    }

    @Test
    fun `current version with valid checksum is valid`() {
        val manifest = BackupVersionValidator.Manifest(
            BackupVersionValidator.CURRENT_BACKUP_FORMAT_VERSION, 1, 2, checksumValid = true
        )
        assertEquals(BackupVersionValidator.ValidationResult.Valid, BackupVersionValidator.validate(manifest))
    }
}
