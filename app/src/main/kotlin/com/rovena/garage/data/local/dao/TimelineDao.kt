package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.domain.model.TimelineEventType
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TimelineEventEntity): Long

    @Update
    suspend fun update(event: TimelineEventEntity)

    @Delete
    suspend fun delete(event: TimelineEventEntity)

    @Query("SELECT * FROM timeline_events WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE vehicleId = :vehicleId AND type = :type ORDER BY dateMillis DESC")
    fun observeByVehicleAndType(vehicleId: Long, type: TimelineEventType): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE type = :type AND sourceRecordId = :sourceRecordId LIMIT 1")
    suspend fun getBySource(type: TimelineEventType, sourceRecordId: Long): TimelineEventEntity?

    @Query("DELETE FROM timeline_events WHERE type = :type AND sourceRecordId = :sourceRecordId")
    suspend fun deleteBySource(type: TimelineEventType, sourceRecordId: Long)

    @Query("SELECT * FROM timeline_events WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC LIMIT :limit")
    fun observeRecent(vehicleId: Long, limit: Int): Flow<List<TimelineEventEntity>>
}
