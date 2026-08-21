package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.PhotoLinkedType

@Entity(
    tableName = "vehicle_photos",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index(value = ["linkedType", "linkedId"])]
)
data class VehiclePhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val linkedType: PhotoLinkedType,
    val linkedId: Long? = null,
    val filePath: String,
    val thumbnailPath: String? = null,
    val caption: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
