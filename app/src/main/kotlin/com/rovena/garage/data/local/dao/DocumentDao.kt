package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rovena.garage.data.local.entities.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: DocumentEntity): Long

    @Update
    suspend fun update(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: Long): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId ORDER BY createdAt DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId ORDER BY createdAt DESC")
    suspend fun getByVehicleOnce(vehicleId: Long): List<DocumentEntity>

    @Query(
        "SELECT * FROM documents WHERE vehicleId = :vehicleId AND expiryDateMillis IS NOT NULL " +
            "AND expiryDateMillis BETWEEN :nowMillis AND :beforeMillis ORDER BY expiryDateMillis ASC"
    )
    fun observeExpiringSoon(vehicleId: Long, nowMillis: Long, beforeMillis: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId AND expiryDateMillis IS NOT NULL")
    suspend fun getAllWithExpiry(vehicleId: Long): List<DocumentEntity>

    @Query("SELECT COUNT(*) FROM documents WHERE vehicleId = :vehicleId AND expiryDateMillis IS NOT NULL AND expiryDateMillis < :nowMillis")
    suspend fun countExpired(vehicleId: Long, nowMillis: Long): Int

    @Query("SELECT COUNT(*) > 0 FROM documents WHERE vehicleId = :vehicleId")
    suspend fun hasAnyForVehicle(vehicleId: Long): Boolean

    @Query("SELECT COUNT(*) FROM documents WHERE vehicleId = :vehicleId")
    fun observeCount(vehicleId: Long): Flow<Int>
}
