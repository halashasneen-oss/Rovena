package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.InspectionCategoryGroup
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.InspectionItemStatus

@Entity(
    tableName = "inspection_items",
    foreignKeys = [
        ForeignKey(
            entity = InspectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("inspectionId"), Index("categoryGroup")]
)
data class InspectionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inspectionId: Long,
    val categoryGroup: InspectionCategoryGroup,
    val itemKey: InspectionItemKey,
    val status: InspectionItemStatus = InspectionItemStatus.UNKNOWN,
    val notes: String? = null,
    val estimatedRepairCost: Double? = null
)
