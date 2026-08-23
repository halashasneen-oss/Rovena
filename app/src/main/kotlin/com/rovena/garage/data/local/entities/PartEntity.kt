package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A part installed on a vehicle, with an optional warranty (spec: Parts
 * History + Warranty Tracking). Warranty can be date-based, mileage-based,
 * both, or neither - same "whichever triggers first" shape as
 * ReminderEntity/MaintenanceRecordEntity's next-due tracking, so it reuses
 * the same DueStatusCalculator evaluation rather than a bespoke rule.
 */
@Entity(
    tableName = "parts",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class PartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val name: String,
    val installedDateMillis: Long,
    val installedMileageKm: Int? = null,
    val warrantyExpiryDateMillis: Long? = null,
    val warrantyExpiryMileageKm: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
