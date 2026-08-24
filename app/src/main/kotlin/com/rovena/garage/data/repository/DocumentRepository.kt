package com.rovena.garage.data.repository

import androidx.room.withTransaction
import com.rovena.garage.data.local.dao.DocumentDao
import com.rovena.garage.data.local.dao.ReminderDao
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.domain.model.ReminderCategory
import com.rovena.garage.domain.model.TimelineEventType
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Note on reminders: this repository only writes the Room rows for a
 * document's auto-generated expiry reminder. Actually scheduling the Android
 * alarm/notification is done by the presentation layer calling
 * `ReminderScheduler` after `addOrUpdate` returns - repositories stay
 * Android-Context-free and easily testable.
 *
 * Reminder lifecycle: a document with an expiry date always has exactly one
 * linked reminder, kept in sync on every save - created the first time an
 * expiry is set, updated when the expiry date changes, and deleted (with the
 * document's own reminderId cleared) the moment the expiry date is removed.
 * Deleting the document deletes its reminder too.
 */
class DocumentRepository(
    private val documentDao: DocumentDao,
    private val reminderDao: ReminderDao,
    private val timelineSyncer: TimelineSyncer,
    private val database: RovenaDatabase
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<DocumentEntity>> = documentDao.observeByVehicle(vehicleId)

    fun observeExpiringSoon(vehicleId: Long, withinDays: Int = 30, nowMillis: Long = System.currentTimeMillis()): Flow<List<DocumentEntity>> =
        documentDao.observeExpiringSoon(vehicleId, nowMillis, nowMillis + withinDays * 24L * 60 * 60 * 1000)

    fun observeCount(vehicleId: Long): Flow<Int> = documentDao.observeCount(vehicleId)

    fun searchAcrossGarage(query: String): Flow<List<DocumentEntity>> = documentDao.searchAcrossGarage(query)

    suspend fun getById(id: Long): DocumentEntity? = documentDao.getById(id)

    suspend fun countExpired(vehicleId: Long, nowMillis: Long = System.currentTimeMillis()): Int =
        documentDao.countExpired(vehicleId, nowMillis)

    suspend fun hasAnyForVehicle(vehicleId: Long): Boolean = documentDao.hasAnyForVehicle(vehicleId)

    /** Returns the saved document's id and the id of its linked reminder, if it still has one. */
    suspend fun addOrUpdate(document: DocumentEntity): Pair<Long, Long?> = database.withTransaction {
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
            val existingReminder = reminderId?.let { reminderDao.getById(it) }
            // Base the reminder on the existing row (preserving its staged-notification
            // progress, createdAt, etc.) when there is one, only overriding the fields a
            // document edit can actually change - a freshly-constructed ReminderEntity
            // here would silently wipe lastNotifiedStageDays/lastTriggeredAtMillis on
            // every single document save, not just when the expiry date changes.
            val candidate = existingReminder?.copy(
                vehicleId = toSave.vehicleId,
                title = toSave.name,
                dueDateMillis = toSave.expiryDateMillis,
                category = ReminderCategory.DOCUMENT
            ) ?: ReminderEntity(
                vehicleId = toSave.vehicleId,
                title = toSave.name,
                basis = ReminderBasis.DATE,
                dueDateMillis = toSave.expiryDateMillis,
                isRecurring = false,
                isActive = true,
                category = ReminderCategory.DOCUMENT
            )
            val reminder = ReminderRepository.resetStageIfDateChanged(existingReminder, candidate)
            reminderId = if (reminderId == null) reminderDao.insert(reminder) else {
                reminderDao.update(reminder); reminderId
            }
        } else if (reminderId != null) {
            // The expiry date was removed - the auto-generated reminder no longer applies.
            reminderDao.getById(reminderId)?.let { reminderDao.delete(it) }
            reminderId = null
        }

        if (toSave.reminderId != reminderId) {
            toSave = toSave.copy(reminderId = reminderId)
            documentDao.update(toSave)
        }

        timelineSyncer.upsertForDocument(toSave)
        docId to reminderId
    }

    suspend fun delete(document: DocumentEntity) {
        database.withTransaction {
            documentDao.delete(document)
            timelineSyncer.removeForSource(TimelineEventType.DOCUMENT, document.id)
            document.reminderId?.let { reminderId ->
                reminderDao.getById(reminderId)?.let { reminderDao.delete(it) }
            }
        }
        runCatching { File(document.filePath).delete() }
    }
}
