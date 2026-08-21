package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.MaintenanceCategoryTotal
import com.rovena.garage.data.local.dao.MaintenanceDao
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.data.local.dao.VehicleDao
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.TimelineEventType
import kotlinx.coroutines.flow.Flow

class MaintenanceRepository(
    private val maintenanceDao: MaintenanceDao,
    private val vehicleDao: VehicleDao,
    private val timelineSyncer: TimelineSyncer
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<MaintenanceRecordEntity>> = maintenanceDao.observeByVehicle(vehicleId)

    fun observeByVehicleAndCategory(vehicleId: Long, category: MaintenanceCategory): Flow<List<MaintenanceRecordEntity>> =
        maintenanceDao.observeByVehicleAndCategory(vehicleId, category)

    fun observeUpcoming(vehicleId: Long): Flow<List<MaintenanceRecordEntity>> = maintenanceDao.observeUpcoming(vehicleId)

    fun observeRecent(vehicleId: Long, limit: Int = 10): Flow<List<MaintenanceRecordEntity>> =
        maintenanceDao.observeRecent(vehicleId, limit)

    fun observeTotalCost(vehicleId: Long): Flow<Double?> = maintenanceDao.observeTotalCost(vehicleId)

    fun observeCostByCategory(vehicleId: Long): Flow<List<MaintenanceCategoryTotal>> = maintenanceDao.observeCostByCategory(vehicleId)

    fun observeCount(vehicleId: Long): Flow<Int> = maintenanceDao.observeCount(vehicleId)

    fun search(vehicleId: Long, query: String): Flow<List<MaintenanceRecordEntity>> = maintenanceDao.search(vehicleId, query)

    suspend fun getById(id: Long): MaintenanceRecordEntity? = maintenanceDao.getById(id)

    suspend fun addOrUpdate(record: MaintenanceRecordEntity): Long {
        val id = if (record.id == 0L) {
            maintenanceDao.insert(record)
        } else {
            maintenanceDao.update(record.copy(updatedAt = System.currentTimeMillis()))
            record.id
        }
        val saved = record.copy(id = id)
        timelineSyncer.upsertForMaintenance(saved)
        bumpVehicleMileageIfHigher(record.vehicleId, record.mileageKm)
        return id
    }

    suspend fun delete(record: MaintenanceRecordEntity) {
        maintenanceDao.delete(record)
        timelineSyncer.removeForSource(TimelineEventType.MAINTENANCE, record.id)
    }

    suspend fun overdueCount(vehicleId: Long, currentMileageKm: Int, nowMillis: Long = System.currentTimeMillis()): Int =
        maintenanceDao.countOverdue(vehicleId, currentMileageKm, nowMillis)

    suspend fun activeTrackedCount(vehicleId: Long): Int = maintenanceDao.countActiveTracked(vehicleId)

    suspend fun daysSinceLastMaintenance(vehicleId: Long, nowMillis: Long = System.currentTimeMillis()): Int? {
        val last = maintenanceDao.getLastMaintenanceDateMillis(vehicleId) ?: return null
        return ((nowMillis - last) / (1000L * 60 * 60 * 24)).toInt()
    }

    private suspend fun bumpVehicleMileageIfHigher(vehicleId: Long, mileageKm: Int) {
        val vehicle: VehicleEntity = vehicleDao.getById(vehicleId) ?: return
        if (mileageKm > vehicle.currentMileageKm) {
            vehicleDao.updateMileage(vehicleId, mileageKm)
        }
    }
}
