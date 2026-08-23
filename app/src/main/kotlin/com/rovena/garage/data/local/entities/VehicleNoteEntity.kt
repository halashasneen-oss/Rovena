package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A freeform, non-diagnostic note the user attaches to a vehicle (spec:
 * Vehicle Notes) - reminders to self, quirks, dealer contacts, anything that
 * doesn't fit a structured record type. Deliberately just a title/body pair
 * with no status, category, or linkage to any scoring/reminder logic.
 */
@Entity(
    tableName = "vehicle_notes",
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
data class VehicleNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val text: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
