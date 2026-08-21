package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.InspectionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(inspection: InspectionEntity): Long

    @Update
    suspend fun update(inspection: InspectionEntity)

    @Delete
    suspend fun delete(inspection: InspectionEntity)

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun getById(id: Long): InspectionEntity?

    @Query("SELECT * FROM inspections WHERE id = :id")
    fun observeById(id: Long): Flow<InspectionEntity?>

    @Query("SELECT * FROM inspections WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    suspend fun getByVehicleOnce(vehicleId: Long): List<InspectionEntity>

    @Query("SELECT * FROM inspections WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC LIMIT 1")
    suspend fun getLatest(vehicleId: Long): InspectionEntity?

    @Query("SELECT * FROM inspections WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC LIMIT 1")
    fun observeLatest(vehicleId: Long): Flow<InspectionEntity?>
}

@Dao
interface InspectionItemDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: InspectionItemEntity): Long

    @Update
    suspend fun update(item: InspectionItemEntity)

    @Delete
    suspend fun delete(item: InspectionItemEntity)

    @Query("SELECT * FROM inspection_items WHERE inspectionId = :inspectionId")
    fun observeByInspection(inspectionId: Long): Flow<List<InspectionItemEntity>>

    @Query("SELECT * FROM inspection_items WHERE inspectionId = :inspectionId")
    suspend fun getByInspectionOnce(inspectionId: Long): List<InspectionItemEntity>

    @Query("DELETE FROM inspection_items WHERE inspectionId = :inspectionId")
    suspend fun deleteByInspection(inspectionId: Long)
}
