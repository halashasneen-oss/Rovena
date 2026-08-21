package com.rovena.garage.data.local.dao

import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.MaintenanceCategory

/** Lightweight projection rows returned by GROUP BY aggregation queries. */
data class MaintenanceCategoryTotal(val category: MaintenanceCategory, val total: Double, val count: Int)

data class ExpenseCategoryTotal(val category: ExpenseCategory, val total: Double, val count: Int)

data class MonthlyTotal(val yearMonth: String, val total: Double)
