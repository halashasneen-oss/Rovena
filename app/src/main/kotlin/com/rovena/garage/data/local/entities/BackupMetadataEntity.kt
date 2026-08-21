package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.BackupRecordType

/** History log of backup/restore operations performed on this device. */
@Entity(tableName = "backup_metadata")
data class BackupMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val type: BackupRecordType,
    val backupFormatVersion: Int,
    val vehicleCount: Int,
    val sizeBytes: Long,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
