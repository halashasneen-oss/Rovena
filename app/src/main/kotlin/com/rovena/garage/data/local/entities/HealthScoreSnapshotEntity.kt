package com.rovena.garage.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded Health Score reading for a vehicle, at most one per calendar
 * day (spec: Health Score history/trend). Written passively whenever the
 * score is computed for a vehicle whose data is sufficient to score at all
 * (see HealthInputsBuilder callers) - never fabricated or backfilled, so the
 * trend only ever reflects real, previously-observed scores.
 */
@Entity(
    tableName = "health_score_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId", "dayMillis"], unique = true)]
)
data class HealthScoreSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    /** Start-of-day millis (local midnight) this snapshot belongs to - the natural upsert key for "one per day". */
    val dayMillis: Long,
    val score: Int,
    val recordedAtMillis: Long = System.currentTimeMillis()
)
