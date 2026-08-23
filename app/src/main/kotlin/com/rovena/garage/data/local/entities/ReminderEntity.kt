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
    /**
     * The most urgent staged day-threshold (30/14/7/3/1/0, or
     * [com.rovena.garage.domain.usecase.ReminderStageCalculator.EXPIRED_STAGE])
     * already notified for a date-based reminder, so the staged schedule never
     * repeats a stage. Null means no stage has fired yet. Reset to null whenever
     * the reminder's dueDateMillis actually changes (renewal/recurrence), so a
     * fresh deadline gets its own fresh countdown.
     */
    val lastNotifiedStageDays: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
