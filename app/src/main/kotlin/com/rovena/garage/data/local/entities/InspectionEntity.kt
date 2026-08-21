package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inspections",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("dateMillis")]
)
data class InspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateMillis: Long,
    val mileageKm: Int,
    /** Cached 0-100 score computed from [InspectionItemEntity] results at save time. */
    val overallScore: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
