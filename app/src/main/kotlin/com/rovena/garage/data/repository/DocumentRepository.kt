package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.DocumentDao
import com.rovena.garage.data.local.dao.ReminderDao
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.domain.model.TimelineEventType
import kotlinx.coroutines.flow.Flow

/**
 * Note on reminders: this repository only writes the Room rows for a
 * document's auto-generated expiry reminder. Actually scheduling the Android
 * alarm/notification is done by the presentation layer calling
 * `ReminderScheduler` after `addOrUpdate` returns - repositories stay
 * Android-Context-free and easily testable.
 */
class DocumentRepository(
    private val documentDao: DocumentDao,
    private val reminderDao: ReminderDao,
    private val timelineSyncer: TimelineSyncer
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<DocumentEntity>> = documentDao.observeByVehicle(vehicleId)

    fun observeExpiringSoon(vehicleId: Long, withinDays: Int = 30, nowMillis: Long = System.currentTimeMillis()): Flow<List<DocumentEntity>> =
        documentDao.observeExpiringSoon(vehicleId, nowMillis, nowMillis + withinDays * 24L * 60 * 60 * 1000)

    fun observeCount(vehicleId: Long): Flow<Int> = documentDao.observeCount(vehicleId)

    suspend fun getById(id: Long): DocumentEntity? = documentDao.getById(id)

    suspend fun countExpired(vehicleId: Long, nowMillis: Long = System.currentTimeMillis()): Int =
        documentDao.countExpired(vehicleId, nowMillis)

    suspend fun hasAnyForVehicle(vehicleId: Long): Boolean = documentDao.hasAnyForVehicle(vehicleId)

    /** Returns the saved document's id and, if it has an expiry date, the id of its (created or updated) reminder. */
    suspend fun addOrUpdate(document: DocumentEntity): Pair<Long, Long?> {
        var toSave = document
        val docId = if (document.id == 0L) {
            val newId = documentDao.insert(document)
            toSave = document.copy(id = newId)
            newId
        } else {
            documentDao.update(document.copy(updatedAt = System.currentTimeMillis()))
            document.id
        }

        var reminderId: Long? = toSave.reminderId
        if (toSave.expiryDateMillis != null) {
            val reminder = ReminderEntity(
                id = reminderId ?: 0,
                vehicleId = toSave.vehicleId,
                title = toSave.name,
                basis = ReminderBasis.DATE,
                dueDateMillis = toSave.expiryDateMillis,
                isRecurring = false,
                isActive = true
            )
            reminderId = if (reminderId == null) reminderDao.insert(reminder) else {
                reminderDao.update(reminder); reminderId
            }
            if (toSave.reminderId != reminderId) {
                toSave = toSave.copy(reminderId = reminderId)
                documentDao.update(toSave)
            }
        }

        timelineSyncer.upsertForDocument(toSave)
        return docId to reminderId
    }

    suspend fun delete(document: DocumentEntity) {
        documentDao.delete(document)
        timelineSyncer.removeForSource(TimelineEventType.DOCUMENT, document.id)
        document.reminderId?.let { reminderId ->
            reminderDao.getById(reminderId)?.let { reminderDao.delete(it) }
        }
    }
}
