package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.TimelineDao
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.domain.model.TimelineEventType
import kotlinx.coroutines.flow.Flow

class TimelineRepository(private val timelineDao: TimelineDao) {

    /** Freeform note (spec Quick Add - "Add Note"): a plain timeline entry with no linked record. */
    suspend fun addNote(vehicleId: Long, text: String) {
        timelineDao.insert(
            TimelineEventEntity(
                vehicleId = vehicleId,
                type = TimelineEventType.VEHICLE_UPDATE,
                dateMillis = System.currentTimeMillis(),
                title = text,
                sourceRecordId = System.nanoTime()
            )
        )
    }
    fun observeByVehicle(vehicleId: Long): Flow<List<TimelineEventEntity>> = timelineDao.observeByVehicle(vehicleId)

    fun observeByVehicleAndType(vehicleId: Long, type: TimelineEventType): Flow<List<TimelineEventEntity>> =
        timelineDao.observeByVehicleAndType(vehicleId, type)

    fun observeRecent(vehicleId: Long, limit: Int = 10): Flow<List<TimelineEventEntity>> =
        timelineDao.observeRecent(vehicleId, limit)
}
