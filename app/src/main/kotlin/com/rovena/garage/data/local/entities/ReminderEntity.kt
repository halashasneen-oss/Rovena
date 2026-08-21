package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.ReminderBasis

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("isActive"), Index("dueDateMillis")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val basis: ReminderBasis,
    /** Recurrence interval, e.g. "every 10,000 km" / "every 12 months". Null if one-off. */
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val dueMileageKm: Int? = null,
    val dueDateMillis: Long? = null,
    val isRecurring: Boolean = false,
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val linkedMaintenanceCategory: MaintenanceCategory? = null,
    val notes: String? = null,
    val lastTriggeredAtMillis: Long? = null,
    /** Last time a due/overdue notification was posted for this reminder, so the periodic check never double-notifies. */
    val lastNotifiedAtMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
