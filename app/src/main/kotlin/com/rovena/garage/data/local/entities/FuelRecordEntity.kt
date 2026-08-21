package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.FuelType

@Entity(
    tableName = "fuel_records",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("dateMillis"), Index("mileageKm")]
)
data class FuelRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateMillis: Long,
    val mileageKm: Int,
    val liters: Double,
    val pricePerLiter: Double,
    val totalCost: Double,
    val currencyCode: String? = null,
    val fuelType: FuelType,
    val station: String? = null,
    val isFullTank: Boolean = true,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
