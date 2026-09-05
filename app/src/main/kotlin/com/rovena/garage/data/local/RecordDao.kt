package com.rovena.garage.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY performedAt DESC, id DESC")
    fun observeMaintenance(vehicleId: Long): Flow<List<MaintenanceEntity>>

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY filledAt DESC, id DESC")
    fun observeFuel(vehicleId: Long): Flow<List<FuelEntryEntity>>

    @Query("SELECT * FROM expense_entries WHERE vehicleId = :vehicleId ORDER BY spentAt DESC, id DESC")
    fun observeExpenses(vehicleId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM vehicle_documents WHERE vehicleId = :vehicleId ORDER BY createdAt DESC, id DESC")
    fun observeDocuments(vehicleId: Long): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY performedAt DESC, id DESC")
    suspend fun getMaintenanceOnce(vehicleId: Long): List<MaintenanceEntity>

    @Query("SELECT * FROM vehicle_documents WHERE vehicleId = :vehicleId ORDER BY createdAt DESC, id DESC")
    suspend fun getDocumentsOnce(vehicleId: Long): List<DocumentEntity>

    @Insert
    suspend fun insertMaintenance(record: MaintenanceEntity): Long

    @Insert
    suspend fun insertFuel(entry: FuelEntryEntity): Long

    @Insert
    suspend fun insertExpense(entry: ExpenseEntity): Long

    @Insert
    suspend fun insertDocument(document: DocumentEntity): Long

    @Query("DELETE FROM maintenance_records WHERE id = :id")
    suspend fun deleteMaintenance(id: Long)

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun deleteFuel(id: Long)

    @Query("DELETE FROM expense_entries WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("DELETE FROM vehicle_documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)
}
