package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.TransmissionType

/**
 * A single vehicle in the user's garage. This is the root aggregate that every
 * other vehicle-scoped table (maintenance, fuel, expenses, documents,
 * inspections, reminders, timeline, photos) hangs off of via `vehicleId`.
 */
@Entity(
    tableName = "vehicles",
    indices = [Index(value = ["isPrimary"])]
)
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val make: String,
    val model: String,
    val year: Int,
    val trim: String? = null,
    val vin: String? = null,
    val licensePlate: String? = null,
    val color: String? = null,
    val fuelType: FuelType,
    val transmission: TransmissionType,
    val engineSizeLiters: Double? = null,
    val currentMileageKm: Int,
    val purchaseDateMillis: Long? = null,
    val purchasePrice: Double? = null,
    val currentEstimatedValue: Double? = null,
    val notes: String? = null,
    val isPrimary: Boolean = false,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
