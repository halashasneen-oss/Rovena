package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.ExpenseCategory

@Entity(
    tableName = "expenses",
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
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateMillis: Long,
    val amount: Double,
    val currencyCode: String? = null,
    val category: ExpenseCategory,
    val description: String? = null,
    val mileageKm: Int? = null,
    val vendor: String? = null,
    val notes: String? = null,
    val receiptPhotoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
