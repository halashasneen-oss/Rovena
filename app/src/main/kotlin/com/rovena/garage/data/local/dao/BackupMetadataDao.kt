package com.rovena.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rovena.garage.data.local.entities.BackupMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupMetadataDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(metadata: BackupMetadataEntity): Long

    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackupMetadataEntity>>

    @Query("SELECT * FROM backup_metadata ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): BackupMetadataEntity?
}
