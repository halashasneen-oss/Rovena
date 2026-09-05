package com.rovena.garage.data

import androidx.room.withTransaction
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.ExpenseEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.RovenaDatabase

class CarRecordRepository(
    private val database: RovenaDatabase
) {
    private val recordDao = database.recordDao()
    private val vehicleDao = database.vehicleDao()

    fun maintenance(vehicleId: Long) = recordDao.observeMaintenance(vehicleId)
    fun fuel(vehicleId: Long) = recordDao.observeFuel(vehicleId)
    fun expenses(vehicleId: Long) = recordDao.observeExpenses(vehicleId)
    fun documents(vehicleId: Long) = recordDao.observeDocuments(vehicleId)

    suspend fun addMaintenance(vehicleId: Long, draft: MaintenanceDraft): Long {
        require(RecordValidator.validMaintenance(draft)) { "Invalid maintenance draft" }
        return database.withTransaction {
            requireNotNull(vehicleDao.getById(vehicleId)) { "Vehicle does not exist" }
            val now = System.currentTimeMillis()
            val id = recordDao.insertMaintenance(
                MaintenanceEntity(
                    vehicleId = vehicleId,
                    serviceType = draft.serviceType.trim(),
                    performedAt = now,
                    mileage = draft.mileage,
                    cost = draft.cost,
                    notes = draft.notes.trim(),
                    nextDueMileage = draft.nextDueMileage,
                    nextDueAt = draft.nextDueAt,
                    attachmentUri = draft.attachmentUri
                )
            )
            vehicleDao.updateMileageIfHigher(vehicleId, draft.mileage, now)
            id
        }
    }

    suspend fun addFuel(vehicleId: Long, draft: FuelDraft): Long {
        require(RecordValidator.validFuel(draft)) { "Invalid fuel draft" }
        return database.withTransaction {
            requireNotNull(vehicleDao.getById(vehicleId)) { "Vehicle does not exist" }
            val now = System.currentTimeMillis()
            val id = recordDao.insertFuel(
                FuelEntryEntity(
                    vehicleId = vehicleId,
                    filledAt = now,
                    mileage = draft.mileage,
                    liters = draft.liters,
                    totalCost = draft.totalCost,
                    notes = draft.notes.trim(),
                    attachmentUri = draft.attachmentUri
                )
            )
            vehicleDao.updateMileageIfHigher(vehicleId, draft.mileage, now)
            id
        }
    }

    suspend fun addExpense(vehicleId: Long, draft: ExpenseDraft): Long {
        require(RecordValidator.validExpense(draft)) { "Invalid expense draft" }
        return database.withTransaction {
            requireNotNull(vehicleDao.getById(vehicleId)) { "Vehicle does not exist" }
            recordDao.insertExpense(
                ExpenseEntity(
                    vehicleId = vehicleId,
                    spentAt = System.currentTimeMillis(),
                    category = draft.category.trim(),
                    amount = draft.amount,
                    notes = draft.notes.trim(),
                    attachmentUri = draft.attachmentUri
                )
            )
        }
    }

    suspend fun addDocument(vehicleId: Long, draft: DocumentDraft): Long {
        require(RecordValidator.validDocument(draft)) { "Invalid document draft" }
        return database.withTransaction {
            requireNotNull(vehicleDao.getById(vehicleId)) { "Vehicle does not exist" }
            recordDao.insertDocument(
                DocumentEntity(
                    vehicleId = vehicleId,
                    title = draft.title.trim(),
                    category = draft.category.trim(),
                    expiryAt = draft.expiryAt,
                    notes = draft.notes.trim(),
                    fileUri = draft.fileUri,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteMaintenance(id: Long) = recordDao.deleteMaintenance(id)
    suspend fun deleteFuel(id: Long) = recordDao.deleteFuel(id)
    suspend fun deleteExpense(id: Long) = recordDao.deleteExpense(id)
    suspend fun deleteDocument(id: Long) = recordDao.deleteDocument(id)
}
