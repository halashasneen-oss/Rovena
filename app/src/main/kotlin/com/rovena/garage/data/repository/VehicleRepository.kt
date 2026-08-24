package com.rovena.garage.data.repository

import androidx.room.withTransaction
import com.rovena.garage.data.local.dao.VehicleDao
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.usecase.MileageValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class VehicleRepository(
    private val vehicleDao: VehicleDao,
    private val timelineSyncer: TimelineSyncer,
    private val database: RovenaDatabase,
    private val documentRepository: DocumentRepository,
    private val expenseRepository: ExpenseRepository,
    private val photoRepository: PhotoRepository
) {
    fun observeAll(): Flow<List<VehicleEntity>> = vehicleDao.observeAll()

    fun observeById(id: Long): Flow<VehicleEntity?> = vehicleDao.observeById(id)

    fun observePrimary(): Flow<VehicleEntity?> = vehicleDao.observePrimary()

    fun observeCount(): Flow<Int> = vehicleDao.observeCount()

    suspend fun getById(id: Long): VehicleEntity? = vehicleDao.getById(id)

    suspend fun getAllOnce(): List<VehicleEntity> = vehicleDao.getAllOnce()

    suspend fun getPrimaryOrFirst(): VehicleEntity? =
        vehicleDao.getPrimaryOnce() ?: vehicleDao.getAllOnce().firstOrNull()

    /** Inserts a vehicle; the first vehicle ever added is automatically made primary. */
    suspend fun addVehicle(vehicle: VehicleEntity): Long {
        val isFirst = vehicleDao.getAllOnce().isEmpty()
        val id = vehicleDao.insert(if (isFirst) vehicle.copy(isPrimary = true) else vehicle)
        return id
    }

    suspend fun updateVehicle(vehicle: VehicleEntity) = database.withTransaction {
        val previous = vehicleDao.getById(vehicle.id)
        val updated = vehicle.copy(updatedAt = System.currentTimeMillis())
        vehicleDao.update(updated)
        if (previous != null && previous.currentMileageKm != updated.currentMileageKm) {
            timelineSyncer.upsertForVehicleUpdate(updated, note = "MILEAGE_UPDATE")
        }
    }

    /**
     * Deletes a vehicle and everything scoped to it. Room cascades all vehicle-scoped
     * child rows (maintenance, fuel, expenses, documents, inspections, inspection items,
     * reminders, timeline, photos) via ON DELETE CASCADE inside one transaction, so the
     * database is never left in a half-deleted state. Cascade only removes the *rows*
     * though - SQLite has no idea those rows pointed at files on disk - so this also
     * collects every referenced file path beforehand and deletes them (best-effort)
     * after the transaction commits, to avoid leaking orphaned photos/documents/receipts.
     */
    suspend fun deleteVehicle(vehicle: VehicleEntity) {
        val documents = documentRepository.observeByVehicle(vehicle.id).first()
        val expenses = expenseRepository.observeByVehicle(vehicle.id).first()
        val photos = photoRepository.observeByVehicle(vehicle.id).first()

        database.withTransaction {
            vehicleDao.delete(vehicle)
            val remaining = vehicleDao.getAllOnce()
            if (vehicle.isPrimary && remaining.isNotEmpty()) {
                vehicleDao.setPrimary(remaining.first().id)
            }
        }

        vehicle.photoPath?.let { runCatching { File(it).delete() } }
        documents.forEach { doc -> runCatching { File(doc.filePath).delete() } }
        expenses.forEach { expense -> expense.receiptPhotoPath?.let { runCatching { File(it).delete() } } }
        photos.forEach { photo ->
            runCatching { File(photo.filePath).delete() }
            photo.thumbnailPath?.let { runCatching { File(it).delete() } }
        }
    }

    suspend fun setPrimary(vehicleId: Long) {
        vehicleDao.setPrimary(vehicleId)
    }

    suspend fun updateMileage(vehicleId: Long, mileageKm: Int) {
        vehicleDao.updateMileage(vehicleId, mileageKm)
    }

    /**
     * Single canonical odometer-plausibility check, shared by every form that
     * captures a mileage reading (Fuel, Maintenance, Inspection, Vehicle edit).
     * Uses [VehicleEntity.currentMileageKm] as the "previous" reading, since
     * every mileage-capturing repository already keeps that field bumped to
     * the highest mileage seen across all of a vehicle's records - see
     * FuelRepository.addOrUpdate / MaintenanceRepository.bumpVehicleMileageIfHigher.
     */
    suspend fun checkMileage(vehicleId: Long, newMileageKm: Int): MileageValidator.MileageCheck {
        val vehicle = vehicleDao.getById(vehicleId) ?: return MileageValidator.MileageCheck.Ok
        return MileageValidator.check(newMileageKm, vehicle.currentMileageKm)
    }

    fun search(query: String): Flow<List<VehicleEntity>> = vehicleDao.search(query)
}
