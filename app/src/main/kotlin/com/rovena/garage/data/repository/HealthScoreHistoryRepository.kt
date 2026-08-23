package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.HealthScoreSnapshotDao
import com.rovena.garage.data.local.entities.HealthScoreSnapshotEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class HealthScoreHistoryRepository(private val dao: HealthScoreSnapshotDao) {

    fun observeByVehicle(vehicleId: Long): Flow<List<HealthScoreSnapshotEntity>> = dao.observeByVehicle(vehicleId)

    /**
     * Records today's Health Score for a vehicle, collapsing to at most one row per
     * calendar day (see [HealthScoreSnapshotEntity]). Safe to call on every score
     * recomputation: skips the write entirely when today's stored score already
     * matches, both to avoid a growing log of duplicate same-day rows and - more
     * importantly - because a caller observing this vehicle's history (as
     * VehicleHubViewModel does, to feed the trend chart) would otherwise see Room's
     * invalidation tracker fire on every identical write and recompute forever.
     */
    suspend fun recordToday(vehicleId: Long, score: Int) {
        val dayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (dao.getForDay(vehicleId, dayMillis)?.score == score) return
        dao.upsert(HealthScoreSnapshotEntity(vehicleId = vehicleId, dayMillis = dayMillis, score = score))
    }
}
