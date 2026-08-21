package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.TimelineEventType

/**
 * Unified chronological feed row. Auto-generated (and kept in sync) whenever a
 * Fuel / Maintenance / Expense / Document / Inspection / Reminder record is
 * created, edited, or deleted - see `TimelineSyncer` in the repository layer.
 */
@Entity(
    tableName = "timeline_events",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("type"), Index("dateMillis"), Index(value = ["type", "sourceRecordId"])]
)
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val type: TimelineEventType,
    val dateMillis: Long,
    val title: String,
    val subtitle: String? = null,
    val amount: Double? = null,
    val currencyCode: String? = null,
    val mileageKm: Int? = null,
    /** ID of the record (fuel/maintenance/expense/...) this event was generated from, so it can be updated/removed with its source. */
    val sourceRecordId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
