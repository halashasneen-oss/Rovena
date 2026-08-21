package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.TimelineDao
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.domain.model.TimelineEventType
import kotlinx.coroutines.flow.Flow

class TimelineRepository(private val timelineDao: TimelineDao) {
    fun observeByVehicle(vehicleId: Long): Flow<List<TimelineEventEntity>> = timelineDao.observeByVehicle(vehicleId)

    fun observeByVehicleAndType(vehicleId: Long, type: TimelineEventType): Flow<List<TimelineEventEntity>> =
        timelineDao.observeByVehicleAndType(vehicleId, type)

    fun observeRecent(vehicleId: Long, limit: Int = 10): Flow<List<TimelineEventEntity>> =
        timelineDao.observeRecent(vehicleId, limit)
}
