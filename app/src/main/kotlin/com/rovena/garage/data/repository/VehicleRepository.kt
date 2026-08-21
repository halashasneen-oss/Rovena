package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.VehicleDao
import com.rovena.garage.data.local.entities.VehicleEntity
import kotlinx.coroutines.flow.Flow

class VehicleRepository(
    private val vehicleDao: VehicleDao,
    private val timelineSyncer: TimelineSyncer
) {
    fun observeAll(): Flow<List<VehicleEntity>> = vehicleDao.observeAll()

    fun observeById(id: Long): Flow<VehicleEntity?> = vehicleDao.observeById(id)

    fun observePrimary(): Flow<VehicleEntity?> = vehicleDao.observePrimary()

    fun observeCount(): Flow<Int> = vehicleDao.observeCount()

    suspend fun getById(id: Long): VehicleEntity? = vehicleDao.getById(id)

    suspend fun getPrimaryOrFirst(): VehicleEntity? =
        vehicleDao.getPrimaryOnce() ?: vehicleDao.getAllOnce().firstOrNull()

    /** Inserts a vehicle; the first vehicle ever added is automatically made primary. */
    suspend fun addVehicle(vehicle: VehicleEntity): Long {
        val isFirst = vehicleDao.getAllOnce().isEmpty()
        val id = vehicleDao.insert(if (isFirst) vehicle.copy(isPrimary = true) else vehicle)
        return id
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) {
        val previous = vehicleDao.getById(vehicle.id)
        val updated = vehicle.copy(updatedAt = System.currentTimeMillis())
        vehicleDao.update(updated)
        if (previous != null && previous.currentMileageKm != updated.currentMileageKm) {
            timelineSyncer.upsertForVehicleUpdate(updated, note = "MILEAGE_UPDATE")
        }
    }

    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        // Room cascades all vehicle-scoped child rows (maintenance, fuel, expenses,
        // documents, inspections, reminders, timeline, photos) via ON DELETE CASCADE.
        vehicleDao.delete(vehicle)
        val remaining = vehicleDao.getAllOnce()
        if (vehicle.isPrimary && remaining.isNotEmpty()) {
            vehicleDao.setPrimary(remaining.first().id)
        }
    }

    suspend fun setPrimary(vehicleId: Long) {
        vehicleDao.setPrimary(vehicleId)
    }

    suspend fun updateMileage(vehicleId: Long, mileageKm: Int) {
        vehicleDao.updateMileage(vehicleId, mileageKm)
    }

    fun search(query: String): Flow<List<VehicleEntity>> = vehicleDao.search(query)
}
