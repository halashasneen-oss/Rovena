package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.domain.model.MaintenanceCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: MaintenanceRecordEntity): Long

    @Update
    suspend fun update(record: MaintenanceRecordEntity)

    @Delete
    suspend fun delete(record: MaintenanceRecordEntity)

    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    suspend fun getById(id: Long): MaintenanceRecordEntity?

    @Query("SELECT * FROM maintenance_records WHERE id = :id")
    fun observeById(id: Long): Flow<MaintenanceRecordEntity?>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<MaintenanceRecordEntity>>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    suspend fun getByVehicleOnce(vehicleId: Long): List<MaintenanceRecordEntity>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId AND category = :category ORDER BY dateMillis DESC")
    fun observeByVehicleAndCategory(vehicleId: Long, category: MaintenanceCategory): Flow<List<MaintenanceRecordEntity>>

    @Query(
        "SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId " +
            "AND (nextDueMileageKm IS NOT NULL OR nextDueDateMillis IS NOT NULL) ORDER BY dateMillis DESC"
    )
    fun observeUpcoming(vehicleId: Long): Flow<List<MaintenanceRecordEntity>>

    @Query(
        "SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId " +
            "AND (description LIKE '%' || :query || '%' OR workshop LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') " +
            "ORDER BY dateMillis DESC"
    )
    fun search(vehicleId: Long, query: String): Flow<List<MaintenanceRecordEntity>>

    @Query(
        "SELECT * FROM maintenance_records " +
            "WHERE description LIKE '%' || :query || '%' OR workshop LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' " +
            "ORDER BY dateMillis DESC LIMIT 20"
    )
    fun searchAcrossGarage(query: String): Flow<List<MaintenanceRecordEntity>>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC LIMIT :limit")
    fun observeRecent(vehicleId: Long, limit: Int): Flow<List<MaintenanceRecordEntity>>

    @Query("SELECT SUM(cost) FROM maintenance_records WHERE vehicleId = :vehicleId")
    fun observeTotalCost(vehicleId: Long): Flow<Double?>

    @Query("SELECT SUM(cost) FROM maintenance_records WHERE vehicleId = :vehicleId AND dateMillis BETWEEN :fromMillis AND :toMillis")
    suspend fun totalCostBetween(vehicleId: Long, fromMillis: Long, toMillis: Long): Double?

    @Query(
        "SELECT category as category, SUM(COALESCE(cost, 0)) as total, COUNT(*) as count " +
            "FROM maintenance_records WHERE vehicleId = :vehicleId GROUP BY category ORDER BY total DESC"
    )
    fun observeCostByCategory(vehicleId: Long): Flow<List<MaintenanceCategoryTotal>>

    @Query("SELECT COUNT(*) FROM maintenance_records WHERE vehicleId = :vehicleId")
    fun observeCount(vehicleId: Long): Flow<Int>

    @Query("SELECT MAX(dateMillis) FROM maintenance_records WHERE vehicleId = :vehicleId")
    suspend fun getLastMaintenanceDateMillis(vehicleId: Long): Long?

    @Query(
        "SELECT COUNT(*) FROM maintenance_records WHERE vehicleId = :vehicleId AND " +
            "((nextDueMileageKm IS NOT NULL AND nextDueMileageKm <= :currentMileageKm) OR " +
            "(nextDueDateMillis IS NOT NULL AND nextDueDateMillis <= :nowMillis))"
    )
    suspend fun countOverdue(vehicleId: Long, currentMileageKm: Int, nowMillis: Long): Int

    @Query(
        "SELECT COUNT(*) FROM maintenance_records WHERE vehicleId = :vehicleId " +
            "AND (nextDueMileageKm IS NOT NULL OR nextDueDateMillis IS NOT NULL)"
    )
    suspend fun countActiveTracked(vehicleId: Long): Int
}
