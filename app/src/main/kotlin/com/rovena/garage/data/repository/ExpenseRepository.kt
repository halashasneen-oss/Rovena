package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.ExpenseCategoryTotal
import com.rovena.garage.data.local.dao.ExpenseDao
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.domain.usecase.ExpenseAggregator
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val timelineSyncer: TimelineSyncer
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<ExpenseEntity>> = expenseDao.observeByVehicle(vehicleId)

    fun observeByCategory(vehicleId: Long, category: ExpenseCategory): Flow<List<ExpenseEntity>> =
        expenseDao.observeByCategory(vehicleId, category)

    fun observeRecent(vehicleId: Long, limit: Int = 10): Flow<List<ExpenseEntity>> = expenseDao.observeRecent(vehicleId, limit)

    fun observeCategoryBreakdown(vehicleId: Long): Flow<List<ExpenseCategoryTotal>> = expenseDao.observeCategoryBreakdown(vehicleId)

    fun observeTotal(vehicleId: Long): Flow<Double?> = expenseDao.observeTotal(vehicleId)

    fun observeCount(vehicleId: Long): Flow<Int> = expenseDao.observeCount(vehicleId)

    fun search(vehicleId: Long, query: String): Flow<List<ExpenseEntity>> = expenseDao.search(vehicleId, query)

    fun searchAcrossGarage(query: String): Flow<List<ExpenseEntity>> = expenseDao.searchAcrossGarage(query)

    suspend fun getById(id: Long): ExpenseEntity? = expenseDao.getById(id)

    suspend fun addOrUpdate(expense: ExpenseEntity): Long {
        val id = if (expense.id == 0L) {
            expenseDao.insert(expense)
        } else {
            expenseDao.update(expense)
            expense.id
        }
        timelineSyncer.upsertForExpense(expense.copy(id = id))
        return id
    }

    suspend fun delete(expense: ExpenseEntity) {
        expenseDao.delete(expense)
        timelineSyncer.removeForSource(TimelineEventType.EXPENSE, expense.id)
    }

    suspend fun computeStats(vehicleId: Long, totalDistanceKm: Int?): ExpenseAggregator.ExpenseStats {
        val entries = expenseDao.getByVehicleOnce(vehicleId).map {
            ExpenseAggregator.ExpenseEntry(
                date = Instant.ofEpochMilli(it.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                amount = it.amount,
                category = it.category
            )
        }
        return ExpenseAggregator.compute(entries, totalDistanceKm)
    }
}
