package com.rovena.garage.data.repository

import androidx.room.withTransaction
import com.rovena.garage.data.local.dao.PartDao
import com.rovena.garage.data.local.dao.ReminderDao
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.PartEntity
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.domain.model.ReminderCategory
import kotlinx.coroutines.flow.Flow

/**
 * Note on reminders: mirrors DocumentRepository's auto-generated-reminder
 * lifecycle (spec: Warranty tracking + reminders) - a part with a warranty
 * trigger set always has exactly one linked reminder, kept in sync on every
 * save (created the first time a trigger is set, updated when it changes,
 * deleted the moment both triggers are cleared), so the existing
 * ReminderCheckWorker picks up warranty expiry the same way it already does
 * for documents, with no separate scheduling path needed. Unlike a document
 * (date-only), a part's warranty can be date-based, mileage-based, or both,
 * so the reminder's basis is derived from whichever trigger(s) are set.
 */
class PartRepository(
    private val partDao: PartDao,
    private val reminderDao: ReminderDao,
    private val database: RovenaDatabase
) {

    fun observeByVehicle(vehicleId: Long): Flow<List<PartEntity>> = partDao.observeByVehicle(vehicleId)

    fun observeCount(vehicleId: Long): Flow<Int> = partDao.observeCount(vehicleId)

    fun observeWithWarranty(vehicleId: Long): Flow<List<PartEntity>> = partDao.observeWithWarranty(vehicleId)

    suspend fun getById(id: Long): PartEntity? = partDao.getById(id)

    suspend fun addOrUpdate(part: PartEntity): Long = database.withTransaction {
        var toSave = part
        val partId = if (part.id == 0L) {
            val newId = partDao.insert(part)
            toSave = part.copy(id = newId)
            newId
        } else {
            partDao.update(part)
            part.id
        }

        var reminderId = toSave.reminderId
        val hasWarranty = toSave.warrantyExpiryDateMillis != null || toSave.warrantyExpiryMileageKm != null
        if (hasWarranty) {
            val basis = when {
                toSave.warrantyExpiryDateMillis != null && toSave.warrantyExpiryMileageKm != null -> ReminderBasis.BOTH
                toSave.warrantyExpiryDateMillis != null -> ReminderBasis.DATE
                else -> ReminderBasis.MILEAGE
            }
            val existingReminder = reminderId?.let { reminderDao.getById(it) }
            // Base the reminder on the existing row when there is one, preserving its
            // staged-notification progress - see DocumentRepository.addOrUpdate for why.
            val candidate = existingReminder?.copy(
                vehicleId = toSave.vehicleId,
                title = toSave.name,
                basis = basis,
                dueMileageKm = toSave.warrantyExpiryMileageKm,
                dueDateMillis = toSave.warrantyExpiryDateMillis,
                category = ReminderCategory.GENERAL
            ) ?: ReminderEntity(
                vehicleId = toSave.vehicleId,
                title = toSave.name,
                basis = basis,
                dueMileageKm = toSave.warrantyExpiryMileageKm,
                dueDateMillis = toSave.warrantyExpiryDateMillis,
                isRecurring = false,
                isActive = true,
                category = ReminderCategory.GENERAL
            )
            val reminder = ReminderRepository.resetStageIfDateChanged(existingReminder, candidate)
            reminderId = if (reminderId == null) reminderDao.insert(reminder) else {
                reminderDao.update(reminder); reminderId
            }
        } else if (reminderId != null) {
            // Both warranty triggers were cleared - the auto-generated reminder no longer applies.
            reminderDao.getById(reminderId)?.let { reminderDao.delete(it) }
            reminderId = null
        }

        if (toSave.reminderId != reminderId) {
            partDao.update(toSave.copy(reminderId = reminderId))
        }

        partId
    }

    suspend fun delete(part: PartEntity) = database.withTransaction {
        partDao.delete(part)
        part.reminderId?.let { id -> reminderDao.getById(id)?.let { reminderDao.delete(it) } }
    }
}
