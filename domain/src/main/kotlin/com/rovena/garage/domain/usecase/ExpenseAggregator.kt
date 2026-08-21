package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.ExpenseCategory
import java.time.LocalDate
import java.time.YearMonth

object ExpenseAggregator {

    data class ExpenseEntry(
        val date: LocalDate,
        val amount: Double,
        val category: ExpenseCategory
    )

    data class ExpenseStats(
        val totalByCategory: Map<ExpenseCategory, Double>,
        val monthlyTotal: Map<YearMonth, Double>,
        val yearlyTotal: Map<Int, Double>,
        val averageMonthlyCost: Double?,
        val costPerKm: Double?,
        val totalOwnershipCost: Double
    )

    fun compute(entries: List<ExpenseEntry>, totalDistanceKm: Int? = null): ExpenseStats {
        val totalByCategory = entries.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val monthlyTotal = entries.groupBy { YearMonth.from(it.date) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val yearlyTotal = entries.groupBy { it.date.year }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val total = entries.sumOf { it.amount }
        val avgMonthly = if (monthlyTotal.isNotEmpty()) total / monthlyTotal.size else null
        val costPerKm = if (totalDistanceKm != null && totalDistanceKm > 0) total / totalDistanceKm else null

        return ExpenseStats(
            totalByCategory = totalByCategory,
            monthlyTotal = monthlyTotal,
            yearlyTotal = yearlyTotal,
            averageMonthlyCost = avgMonthly,
            costPerKm = costPerKm,
            totalOwnershipCost = total
        )
    }
}
