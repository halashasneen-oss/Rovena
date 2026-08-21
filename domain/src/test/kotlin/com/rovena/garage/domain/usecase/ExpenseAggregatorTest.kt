package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.ExpenseCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExpenseAggregatorTest {

    @Test
    fun `aggregates totals by category and month`() {
        val entries = listOf(
            ExpenseAggregator.ExpenseEntry(LocalDate.of(2026, 1, 5), 50.0, ExpenseCategory.CAR_WASH),
            ExpenseAggregator.ExpenseEntry(LocalDate.of(2026, 1, 20), 30.0, ExpenseCategory.PARKING),
            ExpenseAggregator.ExpenseEntry(LocalDate.of(2026, 2, 1), 200.0, ExpenseCategory.REPAIRS)
        )
        val stats = ExpenseAggregator.compute(entries)
        assertEquals(50.0, stats.totalByCategory[ExpenseCategory.CAR_WASH])
        assertEquals(280.0, stats.totalOwnershipCost)
        assertEquals(2, stats.monthlyTotal.size)
        assertEquals(140.0, stats.averageMonthlyCost!!, 0.0001)
    }

    @Test
    fun `cost per km is null without distance`() {
        val entries = listOf(ExpenseAggregator.ExpenseEntry(LocalDate.of(2026, 1, 5), 50.0, ExpenseCategory.CAR_WASH))
        val stats = ExpenseAggregator.compute(entries, totalDistanceKm = null)
        assertNull(stats.costPerKm)
    }

    @Test
    fun `cost per km computed when distance provided`() {
        val entries = listOf(ExpenseAggregator.ExpenseEntry(LocalDate.of(2026, 1, 5), 100.0, ExpenseCategory.REPAIRS))
        val stats = ExpenseAggregator.compute(entries, totalDistanceKm = 500)
        assertEquals(0.2, stats.costPerKm!!, 0.0001)
    }
}
