package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rovena.garage.data.local.entities.VehiclePhotoEntity
import com.rovena.garage.domain.model.PhotoLinkedType
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiclePhotoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(photo: VehiclePhotoEntity): Long

    @Delete
    suspend fun delete(photo: VehiclePhotoEntity)

    @Query("SELECT * FROM vehicle_photos WHERE vehicleId = :vehicleId ORDER BY createdAt DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<VehiclePhotoEntity>>

    @Query("SELECT * FROM vehicle_photos WHERE vehicleId = :vehicleId ORDER BY createdAt DESC")
    suspend fun getByVehicleOnce(vehicleId: Long): List<VehiclePhotoEntity>

    @Query("SELECT * FROM vehicle_photos WHERE linkedType = :linkedType AND linkedId = :linkedId ORDER BY createdAt ASC")
    fun observeByLink(linkedType: PhotoLinkedType, linkedId: Long): Flow<List<VehiclePhotoEntity>>

    @Query("SELECT * FROM vehicle_photos WHERE linkedType = :linkedType AND linkedId = :linkedId ORDER BY createdAt ASC")
    suspend fun getByLinkOnce(linkedType: PhotoLinkedType, linkedId: Long): List<VehiclePhotoEntity>

    @Query("DELETE FROM vehicle_photos WHERE linkedType = :linkedType AND linkedId = :linkedId")
    suspend fun deleteByLink(linkedType: PhotoLinkedType, linkedId: Long)
}
