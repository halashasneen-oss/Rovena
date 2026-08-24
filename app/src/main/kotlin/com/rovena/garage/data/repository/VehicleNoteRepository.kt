package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.VehicleNoteDao
import com.rovena.garage.data.local.entities.VehicleNoteEntity
import kotlinx.coroutines.flow.Flow

class VehicleNoteRepository(private val vehicleNoteDao: VehicleNoteDao) {

    fun observeByVehicle(vehicleId: Long): Flow<List<VehicleNoteEntity>> = vehicleNoteDao.observeByVehicle(vehicleId)

    fun observeCount(vehicleId: Long): Flow<Int> = vehicleNoteDao.observeCount(vehicleId)

    fun searchAcrossGarage(query: String): Flow<List<VehicleNoteEntity>> = vehicleNoteDao.searchAcrossGarage(query)

    suspend fun addOrUpdate(note: VehicleNoteEntity): Long =
        if (note.id == 0L) {
            vehicleNoteDao.insert(note)
        } else {
            vehicleNoteDao.update(note.copy(updatedAtMillis = System.currentTimeMillis()))
            note.id
        }

    suspend fun delete(note: VehicleNoteEntity) = vehicleNoteDao.delete(note)
}
