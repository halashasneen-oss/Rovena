package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.VehicleNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleNoteDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: VehicleNoteEntity): Long

    @Update
    suspend fun update(note: VehicleNoteEntity)

    @Delete
    suspend fun delete(note: VehicleNoteEntity)

    @Query("SELECT * FROM vehicle_notes WHERE id = :id")
    suspend fun getById(id: Long): VehicleNoteEntity?

    @Query("SELECT * FROM vehicle_notes WHERE vehicleId = :vehicleId ORDER BY updatedAtMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<VehicleNoteEntity>>

    @Query("SELECT * FROM vehicle_notes WHERE vehicleId = :vehicleId ORDER BY updatedAtMillis DESC")
    suspend fun getByVehicleOnce(vehicleId: Long): List<VehicleNoteEntity>

    @Query("SELECT COUNT(*) FROM vehicle_notes WHERE vehicleId = :vehicleId")
    fun observeCount(vehicleId: Long): Flow<Int>

    @Query("SELECT * FROM vehicle_notes WHERE text LIKE '%' || :query || '%' ORDER BY updatedAtMillis DESC LIMIT 20")
    fun searchAcrossGarage(query: String): Flow<List<VehicleNoteEntity>>
}
