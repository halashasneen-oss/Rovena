package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rovena.garage.data.local.entities.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vehicle: VehicleEntity): Long

    @Update
    suspend fun update(vehicle: VehicleEntity)

    @Delete
    suspend fun delete(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles ORDER BY isPrimary DESC, createdAt ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY isPrimary DESC, createdAt ASC")
    suspend fun getAllOnce(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE isPrimary = 1 LIMIT 1")
    fun observePrimary(): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryOnce(): VehicleEntity?

    @Query("SELECT COUNT(*) FROM vehicles")
    fun observeCount(): Flow<Int>

    @Query("UPDATE vehicles SET isPrimary = 0")
    suspend fun clearPrimaryFlag()

    @Query("UPDATE vehicles SET isPrimary = 1, updatedAt = :now WHERE id = :vehicleId")
    suspend fun markPrimary(vehicleId: Long, now: Long)

    @Transaction
    suspend fun setPrimary(vehicleId: Long, now: Long = System.currentTimeMillis()) {
        clearPrimaryFlag()
        markPrimary(vehicleId, now)
    }

    @Query("UPDATE vehicles SET currentMileageKm = :mileageKm, updatedAt = :now WHERE id = :vehicleId")
    suspend fun updateMileage(vehicleId: Long, mileageKm: Int, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM vehicles WHERE make LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<VehicleEntity>>
}
