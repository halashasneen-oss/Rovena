package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun observeById(id: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId ORDER BY dueDateMillis ASC, dueMileageKm ASC")
    fun observeByVehicle(vehicleId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId AND isActive = 1 AND isCompleted = 0 ORDER BY dueDateMillis ASC")
    fun observeActive(vehicleId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId AND isActive = 1 AND isCompleted = 0")
    suspend fun getActiveOnce(vehicleId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND isCompleted = 0")
    suspend fun getAllActiveOnce(): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM reminders WHERE vehicleId = :vehicleId AND isActive = 1 AND isCompleted = 0")
    fun observeActiveCount(vehicleId: Long): Flow<Int>
}
