package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.TimelineDao
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.domain.model.TimelineEventType

/**
 * Keeps the unified Timeline (spec #19) in sync with every vehicle-scoped
 * record type. Each repository calls the matching `upsertFor*` after a
 * successful insert/update, and `removeFor*` after a delete, so the timeline
 * never drifts from the underlying data.
 */
class TimelineSyncer(private val timelineDao: TimelineDao) {

    suspend fun upsertForFuel(record: FuelRecordEntity) {
        upsert(
            TimelineEventType.FUEL,
            record.id,
            record.vehicleId,
            record.dateMillis,
            title = record.fuelType.name,
            amount = record.totalCost,
            currencyCode = record.currencyCode,
            mileageKm = record.mileageKm
        )
    }

    suspend fun upsertForMaintenance(record: MaintenanceRecordEntity) {
        upsert(
            TimelineEventType.MAINTENANCE,
            record.id,
            record.vehicleId,
            record.dateMillis,
            title = record.category.name,
            amount = record.cost,
            currencyCode = record.currencyCode,
            mileageKm = record.mileageKm
        )
    }

    suspend fun upsertForExpense(record: ExpenseEntity) {
        upsert(
            TimelineEventType.EXPENSE,
            record.id,
            record.vehicleId,
            record.dateMillis,
            title = record.description?.takeIf { it.isNotBlank() } ?: record.category.name,
            amount = record.amount,
            currencyCode = record.currencyCode,
            mileageKm = record.mileageKm
        )
    }

    suspend fun upsertForDocument(record: DocumentEntity) {
        upsert(
            TimelineEventType.DOCUMENT,
            record.id,
            record.vehicleId,
            record.issueDateMillis ?: record.createdAt,
            title = record.name,
            amount = null,
            currencyCode = null,
            mileageKm = null
        )
    }

    suspend fun upsertForInspection(record: InspectionEntity) {
        upsert(
            TimelineEventType.INSPECTION,
            record.id,
            record.vehicleId,
            record.dateMillis,
            title = "INSPECTION",
            amount = record.overallScore?.toDouble(),
            currencyCode = null,
            mileageKm = record.mileageKm
        )
    }

    suspend fun upsertForReminder(record: ReminderEntity) {
        upsert(
            TimelineEventType.REMINDER,
            record.id,
            record.vehicleId,
            record.dueDateMillis ?: record.createdAt,
            title = record.title,
            amount = null,
            currencyCode = null,
            mileageKm = record.dueMileageKm
        )
    }

    suspend fun upsertForVehicleUpdate(vehicle: VehicleEntity, note: String) {
        upsert(
            TimelineEventType.VEHICLE_UPDATE,
            vehicle.id,
            vehicle.id,
            vehicle.updatedAt,
            title = note,
            amount = null,
            currencyCode = null,
            mileageKm = vehicle.currentMileageKm
        )
    }

    suspend fun removeForSource(type: TimelineEventType, sourceRecordId: Long) {
        timelineDao.deleteBySource(type, sourceRecordId)
    }

    private suspend fun upsert(
        type: TimelineEventType,
        sourceRecordId: Long,
        vehicleId: Long,
        dateMillis: Long,
        title: String,
        amount: Double?,
        currencyCode: String?,
        mileageKm: Int?
    ) {
        val existing = timelineDao.getBySource(type, sourceRecordId)
        val event = TimelineEventEntity(
            id = existing?.id ?: 0,
            vehicleId = vehicleId,
            type = type,
            dateMillis = dateMillis,
            title = title,
            subtitle = null,
            amount = amount,
            currencyCode = currencyCode,
            mileageKm = mileageKm,
            sourceRecordId = sourceRecordId,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        timelineDao.insert(event)
    }
}
