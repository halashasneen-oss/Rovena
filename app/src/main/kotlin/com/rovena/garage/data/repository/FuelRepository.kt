package com.rovena.garage.data.repository

import androidx.room.withTransaction
import com.rovena.garage.data.local.dao.FuelDao
import com.rovena.garage.data.local.dao.VehicleDao
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.domain.usecase.FuelStatsCalculator
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

class FuelRepository(
    private val fuelDao: FuelDao,
    private val vehicleDao: VehicleDao,
    private val timelineSyncer: TimelineSyncer,
    private val database: RovenaDatabase
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<FuelRecordEntity>> = fuelDao.observeByVehicle(vehicleId)

    fun observeRecent(vehicleId: Long, limit: Int = 10): Flow<List<FuelRecordEntity>> = fuelDao.observeRecent(vehicleId, limit)

    fun observeTotalCost(vehicleId: Long): Flow<Double?> = fuelDao.observeTotalCost(vehicleId)

    fun observeCount(vehicleId: Long): Flow<Int> = fuelDao.observeCount(vehicleId)

    fun search(vehicleId: Long, query: String): Flow<List<FuelRecordEntity>> = fuelDao.search(vehicleId, query)

    fun searchAcrossGarage(query: String): Flow<List<FuelRecordEntity>> = fuelDao.searchAcrossGarage(query)

    suspend fun getById(id: Long): FuelRecordEntity? = fuelDao.getById(id)

    suspend fun addOrUpdate(record: FuelRecordEntity): Long = database.withTransaction {
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
        id
    }

    suspend fun delete(record: FuelRecordEntity) = database.withTransaction {
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
                isFullTank = it.isFullTank,
                currencyCode = it.currencyCode ?: "JOD"
            )
        }
        return FuelStatsCalculator.compute(entries)
    }
}
