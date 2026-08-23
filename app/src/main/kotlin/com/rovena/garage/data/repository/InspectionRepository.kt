package com.rovena.garage.data.repository

import androidx.room.withTransaction
import com.rovena.garage.data.local.dao.InspectionDao
import com.rovena.garage.data.local.dao.InspectionItemDao
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.InspectionItemEntity
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.PhotoLinkedType
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.domain.usecase.InspectionScoreCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * A vehicle's latest-inspection condition per Health Score category (spec: Health
 * Score <-> Inspection integration). Each field is null when the vehicle has never
 * been inspected, or the latest inspection never evaluated that item - both mean
 * "no data", matching how [com.rovena.garage.domain.usecase.HealthScoreCalculator]
 * treats a null category input (excluded from the score, not penalized).
 */
data class InspectionConditionScores(
    val brakes: Int?,
    val tires: Int?,
    val battery: Int?,
    val fluids: Int?
) {
    companion object {
        val EMPTY = InspectionConditionScores(null, null, null, null)
    }
}

class InspectionRepository(
    private val inspectionDao: InspectionDao,
    private val itemDao: InspectionItemDao,
    private val timelineSyncer: TimelineSyncer,
    private val database: RovenaDatabase,
    private val photoRepository: PhotoRepository
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<InspectionEntity>> = inspectionDao.observeByVehicle(vehicleId)

    fun observeLatest(vehicleId: Long): Flow<InspectionEntity?> = inspectionDao.observeLatest(vehicleId)

    fun observeItems(inspectionId: Long): Flow<List<InspectionItemEntity>> = itemDao.observeByInspection(inspectionId)

    fun observeLatestConditionScores(vehicleId: Long): Flow<InspectionConditionScores> =
        inspectionDao.observeLatest(vehicleId).flatMapLatest { latest ->
            if (latest == null) {
                flowOf(InspectionConditionScores.EMPTY)
            } else {
                itemDao.observeByInspection(latest.id).map { items ->
                    val byKey = items.associateBy { it.itemKey }
                    InspectionConditionScores(
                        brakes = byKey[InspectionItemKey.BRAKES]?.status?.let(InspectionScoreCalculator::conditionScoreFor),
                        tires = byKey[InspectionItemKey.TIRES]?.status?.let(InspectionScoreCalculator::conditionScoreFor),
                        battery = byKey[InspectionItemKey.BATTERY]?.status?.let(InspectionScoreCalculator::conditionScoreFor),
                        fluids = byKey[InspectionItemKey.FLUIDS]?.status?.let(InspectionScoreCalculator::conditionScoreFor)
                    )
                }
            }
        }

    suspend fun getById(id: Long): InspectionEntity? = inspectionDao.getById(id)

    suspend fun getLatest(vehicleId: Long): InspectionEntity? = inspectionDao.getLatest(vehicleId)

    suspend fun getItemsOnce(inspectionId: Long): List<InspectionItemEntity> = itemDao.getByInspectionOnce(inspectionId)

    /**
     * Saves an inspection and its items together, computing the overall score from item
     * statuses. Items are upserted by (inspectionId, itemKey) rather than deleted and
     * reinserted, so each item keeps a stable row id across edits - inspection-item
     * photos are linked to that id (see [PhotoRepository]), and reassigning ids on every
     * save would silently orphan them.
     */
    suspend fun saveInspection(inspection: InspectionEntity, items: List<InspectionItemEntity>): Long =
        database.withTransaction {
            val scoreResult = InspectionScoreCalculator.calculate(items.map { it.status })
            val toSave = inspection.copy(overallScore = scoreResult.score)

            val id = if (inspection.id == 0L) {
                inspectionDao.insert(toSave)
            } else {
                inspectionDao.update(toSave)
                inspection.id
            }

            val existingByKey = itemDao.getByInspectionOnce(id).associateBy { it.itemKey }
            items.forEach { item ->
                val existing = existingByKey[item.itemKey]
                if (existing != null) {
                    itemDao.update(item.copy(id = existing.id, inspectionId = id))
                } else {
                    itemDao.insert(item.copy(inspectionId = id))
                }
            }
            val newKeys = items.map { it.itemKey }.toSet()
            existingByKey.values.filter { it.itemKey !in newKeys }.forEach { itemDao.delete(it) }

            timelineSyncer.upsertForInspection(toSave.copy(id = id))
            id
        }

    /**
     * Deletes an inspection and its items (cascade via FK). VehiclePhoto rows are a
     * generic polymorphic link (linkedType/linkedId), not a real foreign key Room can
     * cascade for us, so each item's photos - both the DB rows and their files on disk -
     * are cleaned up explicitly before the delete commits.
     */
    suspend fun delete(inspection: InspectionEntity) {
        val items = itemDao.getByInspectionOnce(inspection.id)
        val photoFiles = mutableListOf<String>()
        items.forEach { item ->
            photoRepository.getByLinkOnce(PhotoLinkedType.INSPECTION_ITEM, item.id).forEach { photo ->
                photoFiles += photo.filePath
                photo.thumbnailPath?.let { photoFiles += it }
            }
        }

        database.withTransaction {
            items.forEach { item -> photoRepository.deleteAllForLink(PhotoLinkedType.INSPECTION_ITEM, item.id) }
            inspectionDao.delete(inspection) // items cascade via FK
            timelineSyncer.removeForSource(TimelineEventType.INSPECTION, inspection.id)
        }

        photoFiles.forEach { runCatching { File(it).delete() } }
    }
}
