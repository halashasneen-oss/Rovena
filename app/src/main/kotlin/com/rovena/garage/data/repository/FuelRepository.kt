package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.FuelDao
import com.rovena.garage.data.local.dao.VehicleDao
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.domain.usecase.FuelStatsCalculator
import com.rovena.garage.domain.usecase.MileageValidator
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

class FuelRepository(
    private val fuelDao: FuelDao,
    private val vehicleDao: VehicleDao,
    private val timelineSyncer: TimelineSyncer
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<FuelRecordEntity>> = fuelDao.observeByVehicle(vehicleId)

    fun observeRecent(vehicleId: Long, limit: Int = 10): Flow<List<FuelRecordEntity>> = fuelDao.observeRecent(vehicleId, limit)

    fun observeTotalCost(vehicleId: Long): Flow<Double?> = fuelDao.observeTotalCost(vehicleId)

    fun observeCount(vehicleId: Long): Flow<Int> = fuelDao.observeCount(vehicleId)

    fun search(vehicleId: Long, query: String): Flow<List<FuelRecordEntity>> = fuelDao.search(vehicleId, query)

    suspend fun getById(id: Long): FuelRecordEntity? = fuelDao.getById(id)

    suspend fun checkMileage(vehicleId: Long, newMileageKm: Int): MileageValidator.MileageCheck {
        val latest = fuelDao.getLatest(vehicleId)
        return MileageValidator.check(newMileageKm, latest?.mileageKm)
    }

    suspend fun addOrUpdate(record: FuelRecordEntity): Long {
        val id = if (record.id == 0L) {
            fuelDao.insert(record)
        } else {
            fuelDao.update(record)
            record.id
        }
        val saved = record.copy(id = id)
        timelineSyncer.upsertForFuel(saved)
        val vehicle = vehicleDao.getById(record.vehicleId)
        if (vehicle != null && record.mileageKm > vehicle.currentMileageKm) {
            vehicleDao.updateMileage(record.vehicleId, record.mileageKm)
        }
        return id
    }

    suspend fun delete(record: FuelRecordEntity) {
        fuelDao.delete(record)
        timelineSyncer.removeForSource(TimelineEventType.FUEL, record.id)
    }

    suspend fun computeStats(vehicleId: Long): FuelStatsCalculator.FuelStats {
        val entries = fuelDao.getByVehicleOrderedByMileage(vehicleId).map {
            FuelStatsCalculator.FuelEntry(
                date = Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                odometerKm = it.mileageKm,
                liters = it.liters,
                totalCost = it.totalCost,
                isFullTank = it.isFullTank
            )
        }
        return FuelStatsCalculator.compute(entries)
    }
}
