package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.PartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(part: PartEntity): Long

    @Update
    suspend fun update(part: PartEntity)

    @Delete
    suspend fun delete(part: PartEntity)

    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getById(id: Long): PartEntity?

    @Query("SELECT * FROM parts WHERE vehicleId = :vehicleId ORDER BY installedDateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts WHERE vehicleId = :vehicleId ORDER BY installedDateMillis DESC")
    suspend fun getByVehicleOnce(vehicleId: Long): List<PartEntity>

    @Query("SELECT COUNT(*) FROM parts WHERE vehicleId = :vehicleId")
    fun observeCount(vehicleId: Long): Flow<Int>

    @Query("SELECT * FROM parts WHERE vehicleId = :vehicleId AND (warrantyExpiryDateMillis IS NOT NULL OR warrantyExpiryMileageKm IS NOT NULL)")
    fun observeWithWarranty(vehicleId: Long): Flow<List<PartEntity>>
}
