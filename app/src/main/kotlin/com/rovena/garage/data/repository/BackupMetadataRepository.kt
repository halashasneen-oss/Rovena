package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.BackupMetadataDao
import com.rovena.garage.data.local.entities.BackupMetadataEntity
import kotlinx.coroutines.flow.Flow

class BackupMetadataRepository(private val dao: BackupMetadataDao) {
    fun observeAll(): Flow<List<BackupMetadataEntity>> = dao.observeAll()
    suspend fun getLatest(): BackupMetadataEntity? = dao.getLatest()
    suspend fun record(metadata: BackupMetadataEntity): Long = dao.insert(metadata)
}
