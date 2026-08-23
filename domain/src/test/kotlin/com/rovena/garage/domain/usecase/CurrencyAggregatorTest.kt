package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CurrencyAggregatorTest {

    @Test
    fun `empty list returns Empty`() {
        assertEquals(CurrencyAggregator.CurrencyTotal.Empty, CurrencyAggregator.aggregate(emptyList()))
    }

    @Test
    fun `single currency sums correctly`() {
        val result = CurrencyAggregator.aggregate(listOf(100.0 to "JOD", 50.5 to "JOD", 10.0 to "JOD"))
        assertEquals(CurrencyAggregator.CurrencyTotal.Single(160.5, "JOD"), result)
    }

    @Test
    fun `mixed currencies never combine into one number`() {
        val result = CurrencyAggregator.aggregate(listOf(100.0 to "JOD", 100.0 to "USD"))
        assertTrue(result is CurrencyAggregator.CurrencyTotal.Mixed)
        val mixed = result as CurrencyAggregator.CurrencyTotal.Mixed
        assertEquals(100.0, mixed.byCurrency["JOD"])
        assertEquals(100.0, mixed.byCurrency["USD"])
        assertEquals(2, mixed.byCurrency.size)
    }

    @Test
    fun `mixed currencies group and sum each independently`() {
        val result = CurrencyAggregator.aggregate(
            listOf(100.0 to "JOD", 25.0 to "JOD", 50.0 to "USD", 20.0 to "EUR", 30.0 to "USD")
        )
        val mixed = result as CurrencyAggregator.CurrencyTotal.Mixed
        assertEquals(125.0, mixed.byCurrency["JOD"])
        assertEquals(80.0, mixed.byCurrency["USD"])
        assertEquals(20.0, mixed.byCurrency["EUR"])
    }

    @Test
    fun `isSingleCurrency reflects currency uniformity`() {
        assertTrue(CurrencyAggregator.isSingleCurrency(listOf(1.0 to "JOD", 2.0 to "JOD")))
        assertFalse(CurrencyAggregator.isSingleCurrency(listOf(1.0 to "JOD", 2.0 to "USD")))
        assertFalse(CurrencyAggregator.isSingleCurrency(emptyList()))
    }
}
