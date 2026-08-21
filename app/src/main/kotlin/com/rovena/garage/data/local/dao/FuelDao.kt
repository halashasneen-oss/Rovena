package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.FuelRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: FuelRecordEntity): Long

    @Update
    suspend fun update(record: FuelRecordEntity)

    @Delete
    suspend fun delete(record: FuelRecordEntity)

    @Query("SELECT * FROM fuel_records WHERE id = :id")
    suspend fun getById(id: Long): FuelRecordEntity?

    @Query("SELECT * FROM fuel_records WHERE id = :id")
    fun observeById(id: Long): Flow<FuelRecordEntity?>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<FuelRecordEntity>>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY mileageKm ASC")
    suspend fun getByVehicleOrderedByMileage(vehicleId: Long): List<FuelRecordEntity>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC LIMIT :limit")
    fun observeRecent(vehicleId: Long, limit: Int): Flow<List<FuelRecordEntity>>

    @Query("SELECT MAX(mileageKm) FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun getLatestMileage(vehicleId: Long): Int?

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY mileageKm DESC LIMIT 1")
    suspend fun getLatest(vehicleId: Long): FuelRecordEntity?

    @Query("SELECT SUM(totalCost) FROM fuel_records WHERE vehicleId = :vehicleId AND dateMillis BETWEEN :fromMillis AND :toMillis")
    suspend fun totalCostBetween(vehicleId: Long, fromMillis: Long, toMillis: Long): Double?

    @Query("SELECT SUM(totalCost) FROM fuel_records WHERE vehicleId = :vehicleId")
    fun observeTotalCost(vehicleId: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM fuel_records WHERE vehicleId = :vehicleId")
    fun observeCount(vehicleId: Long): Flow<Int>

    @Query(
        "SELECT * FROM fuel_records WHERE vehicleId = :vehicleId " +
            "AND (station LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY dateMillis DESC"
    )
    fun search(vehicleId: Long, query: String): Flow<List<FuelRecordEntity>>
}
