package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.MaintenanceCategory

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("category"), Index("dateMillis")]
)
data class MaintenanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateMillis: Long,
    val mileageKm: Int,
    val category: MaintenanceCategory,
    val description: String,
    val cost: Double? = null,
    val currencyCode: String? = null,
    val parts: String? = null,
    val workshop: String? = null,
    val technician: String? = null,
    val notes: String? = null,
    val nextDueMileageKm: Int? = null,
    val nextDueDateMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
