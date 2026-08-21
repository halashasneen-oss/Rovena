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
        val id = if (reminder.id == 0L) {
            reminderDao.insert(reminder)
        } else {
            reminderDao.update(reminder.copy(updatedAt = System.currentTimeMillis()))
            reminder.id
        }
        timelineSyncer.upsertForReminder(reminder.copy(id = id))
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
                updatedAt = completedAt
            )
            reminderDao.update(next)
            timelineSyncer.upsertForReminder(next)
        } else {
            val done = reminder.copy(isCompleted = true, lastTriggeredAtMillis = completedAt, updatedAt = completedAt)
            reminderDao.update(done)
        }
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
}
