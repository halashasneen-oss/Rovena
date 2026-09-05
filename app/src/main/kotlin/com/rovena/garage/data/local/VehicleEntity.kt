package com.rovena.garage.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vehicles",
    indices = [Index(value = ["isPrimary"])]
)
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val make: String,
    val model: String,
    val year: Int,
    val mileage: Long,
    val fuelType: String,
    val nickname: String = "",
    val plateNumber: String = "",
    val vin: String = "",
    val currencyCode: String = "JOD",
    val photoUri: String? = null,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
