package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.InspectionDao
import com.rovena.garage.data.local.dao.InspectionItemDao
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.InspectionItemEntity
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.domain.usecase.InspectionScoreCalculator
import kotlinx.coroutines.flow.Flow

class InspectionRepository(
    private val inspectionDao: InspectionDao,
    private val itemDao: InspectionItemDao,
    private val timelineSyncer: TimelineSyncer
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<InspectionEntity>> = inspectionDao.observeByVehicle(vehicleId)

    fun observeLatest(vehicleId: Long): Flow<InspectionEntity?> = inspectionDao.observeLatest(vehicleId)

    fun observeItems(inspectionId: Long): Flow<List<InspectionItemEntity>> = itemDao.observeByInspection(inspectionId)

    suspend fun getById(id: Long): InspectionEntity? = inspectionDao.getById(id)

    suspend fun getLatest(vehicleId: Long): InspectionEntity? = inspectionDao.getLatest(vehicleId)

    suspend fun getItemsOnce(inspectionId: Long): List<InspectionItemEntity> = itemDao.getByInspectionOnce(inspectionId)

    /** Saves an inspection and its items together, computing the overall score from item statuses. */
    suspend fun saveInspection(inspection: InspectionEntity, items: List<InspectionItemEntity>): Long {
        val scoreResult = InspectionScoreCalculator.calculate(items.map { it.status })
        val toSave = inspection.copy(overallScore = scoreResult.score)

        val id = if (inspection.id == 0L) {
            inspectionDao.insert(toSave)
        } else {
            inspectionDao.update(toSave)
            inspection.id
        }

        itemDao.replaceAll(id, items.map { it.copy(inspectionId = id) })
        timelineSyncer.upsertForInspection(toSave.copy(id = id))
        return id
    }

    suspend fun delete(inspection: InspectionEntity) {
        inspectionDao.delete(inspection) // items cascade via FK
        timelineSyncer.removeForSource(TimelineEventType.INSPECTION, inspection.id)
    }
}
