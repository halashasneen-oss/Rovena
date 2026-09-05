package com.rovena.garage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY isPrimary DESC, updatedAt DESC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles ORDER BY isPrimary DESC, updatedAt DESC")
    suspend fun getAllOnce(): List<VehicleEntity>

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(vehicle: VehicleEntity): Long

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE vehicles SET isPrimary = 0")
    suspend fun clearPrimary()

    @Query("UPDATE vehicles SET isPrimary = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markPrimary(id: Long, updatedAt: Long)

    @Query("UPDATE vehicles SET mileage = :mileage, updatedAt = :updatedAt WHERE id = :id AND mileage < :mileage")
    suspend fun updateMileageIfHigher(id: Long, mileage: Long, updatedAt: Long)
}
