package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.ReminderDao
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.domain.model.TimelineEventType
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val timelineSyncer: TimelineSyncer
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<ReminderEntity>> = reminderDao.observeByVehicle(vehicleId)

    fun observeActive(vehicleId: Long): Flow<List<ReminderEntity>> = reminderDao.observeActive(vehicleId)

    fun observeActiveCount(vehicleId: Long): Flow<Int> = reminderDao.observeActiveCount(vehicleId)

    suspend fun getById(id: Long): ReminderEntity? = reminderDao.getById(id)

    suspend fun getActiveOnce(vehicleId: Long): List<ReminderEntity> = reminderDao.getActiveOnce(vehicleId)

    suspend fun getAllActiveOnce(): List<ReminderEntity> = reminderDao.getAllActiveOnce()

    suspend fun addOrUpdate(reminder: ReminderEntity): Long {
        val existing = if (reminder.id != 0L) reminderDao.getById(reminder.id) else null
        val toSave = resetStageIfDateChanged(existing, reminder)
        val id = if (toSave.id == 0L) {
            reminderDao.insert(toSave)
        } else {
            reminderDao.update(toSave.copy(updatedAt = System.currentTimeMillis()))
            toSave.id
        }
        timelineSyncer.upsertForReminder(toSave.copy(id = id))
        return id
    }

    suspend fun markCompleted(reminder: ReminderEntity, completedAt: Long = System.currentTimeMillis()) {
        if (reminder.isRecurring) {
            val next = reminder.copy(
                dueMileageKm = reminder.intervalKm?.let { (reminder.dueMileageKm ?: 0) + it },
                dueDateMillis = reminder.intervalMonths?.let { months ->
                    addMonthsToMillis(reminder.dueDateMillis ?: completedAt, months)
                },
                lastTriggeredAtMillis = completedAt,
                lastNotifiedStageDays = null, // a rolled-over cycle gets its own fresh staged countdown
                updatedAt = completedAt
            )
            reminderDao.update(next)
            timelineSyncer.upsertForReminder(next)
        } else {
            val done = reminder.copy(isCompleted = true, lastTriggeredAtMillis = completedAt, updatedAt = completedAt)
            reminderDao.update(done)
        }
    }

    suspend fun markNotified(reminderId: Long, whenMillis: Long = System.currentTimeMillis()) {
        reminderDao.markNotified(reminderId, whenMillis)
    }

    suspend fun markNotifiedStage(reminderId: Long, stage: Int, whenMillis: Long = System.currentTimeMillis()) {
        reminderDao.markNotifiedStage(reminderId, stage, whenMillis)
    }

    suspend fun delete(reminder: ReminderEntity) {
        reminderDao.delete(reminder)
        timelineSyncer.removeForSource(TimelineEventType.REMINDER, reminder.id)
    }

    private fun addMonthsToMillis(baseMillis: Long, months: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = baseMillis
        cal.add(java.util.Calendar.MONTH, months)
        return cal.timeInMillis
    }

    companion object {
        /**
         * Resets a date-based reminder's staged-notification progress when its
         * due date actually changed (e.g. a document was renewed, or a recurring
         * reminder rolled to its next cycle) - the countdown should restart for
         * the new deadline, not treat it as already partly notified.
         */
        fun resetStageIfDateChanged(existing: ReminderEntity?, incoming: ReminderEntity): ReminderEntity =
            if (existing != null && existing.dueDateMillis != incoming.dueDateMillis) {
                incoming.copy(lastNotifiedStageDays = null)
            } else {
                incoming
            }
    }
}
