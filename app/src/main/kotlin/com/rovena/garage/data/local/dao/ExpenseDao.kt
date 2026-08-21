package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.domain.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun observeById(id: Long): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    suspend fun getByVehicleOnce(vehicleId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId AND category = :category ORDER BY dateMillis DESC")
    fun observeByCategory(vehicleId: Long, category: ExpenseCategory): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC LIMIT :limit")
    fun observeRecent(vehicleId: Long, limit: Int): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT category as category, SUM(amount) as total, COUNT(*) as count " +
            "FROM expenses WHERE vehicleId = :vehicleId GROUP BY category ORDER BY total DESC"
    )
    fun observeCategoryBreakdown(vehicleId: Long): Flow<List<ExpenseCategoryTotal>>

    @Query("SELECT SUM(amount) FROM expenses WHERE vehicleId = :vehicleId")
    fun observeTotal(vehicleId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE vehicleId = :vehicleId AND dateMillis BETWEEN :fromMillis AND :toMillis")
    suspend fun totalBetween(vehicleId: Long, fromMillis: Long, toMillis: Long): Double?

    @Query(
        "SELECT * FROM expenses WHERE vehicleId = :vehicleId " +
            "AND (description LIKE '%' || :query || '%' OR vendor LIKE '%' || :query || '%') ORDER BY dateMillis DESC"
    )
    fun search(vehicleId: Long, query: String): Flow<List<ExpenseEntity>>

    @Query("SELECT COUNT(*) FROM expenses WHERE vehicleId = :vehicleId")
    fun observeCount(vehicleId: Long): Flow<Int>
}
