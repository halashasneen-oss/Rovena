package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rovena.garage.data.local.entities.HealthScoreSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthScoreSnapshotDao {

    /** REPLACE collapses same-day writes for a vehicle into one row via the (vehicleId, dayMillis) unique index. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: HealthScoreSnapshotEntity)

    @Query("SELECT * FROM health_score_snapshots WHERE vehicleId = :vehicleId ORDER BY dayMillis ASC")
    fun observeByVehicle(vehicleId: Long): Flow<List<HealthScoreSnapshotEntity>>

    @Query("SELECT * FROM health_score_snapshots WHERE vehicleId = :vehicleId AND dayMillis = :dayMillis LIMIT 1")
    suspend fun getForDay(vehicleId: Long, dayMillis: Long): HealthScoreSnapshotEntity?
}
