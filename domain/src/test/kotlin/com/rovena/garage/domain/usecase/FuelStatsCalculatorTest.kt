package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FuelStatsCalculatorTest {

    private fun entry(day: Int, odo: Int, liters: Double, cost: Double, full: Boolean, currencyCode: String = "JOD") =
        FuelStatsCalculator.FuelEntry(LocalDate.of(2026, 1, day), odo, liters, cost, full, currencyCode)

    @Test
    fun `not enough data with fewer than two full tanks`() {
        val entries = listOf(entry(1, 1000, 40.0, 28.0, true))
        val stats = FuelStatsCalculator.compute(entries)
        assertFalse(stats.hasEnoughDataForConsumption)
        assertNull(stats.averageLitersPer100Km)
    }

    @Test
    fun `computes consumption between two full tanks ignoring partial fills`() {
        val entries = listOf(
            entry(1, 1000, 40.0, 28.0, true),
            entry(10, 1300, 15.0, 10.5, false), // partial top-up mid interval
            entry(20, 1600, 33.0, 23.1, true)
        )
        val stats = FuelStatsCalculator.compute(entries)
        assertTrue(stats.hasEnoughDataForConsumption)
        // distance 600km, liters used = 15 + 33 = 48L -> 8.0 L/100km
        assertEquals(8.0, stats.averageLitersPer100Km!!, 0.0001)
        assertEquals(1, stats.intervals.size)
        assertEquals(600, stats.intervals[0].distanceKm)
    }

    @Test
    fun `best and worst pick the extremes across multiple intervals`() {
        val entries = listOf(
            entry(1, 0, 40.0, 28.0, true),
            entry(10, 500, 40.0, 28.0, true),   // 8.0 L/100km
            entry(20, 1100, 42.0, 29.0, true)   // 7.0 L/100km
        )
        val stats = FuelStatsCalculator.compute(entries)
        assertEquals(2, stats.intervals.size)
        assertEquals(7.0, stats.bestLitersPer100Km!!, 0.0001)
        assertEquals(8.0, stats.worstLitersPer100Km!!, 0.0001)
    }

    @Test
    fun `cost per km uses full odometer span regardless of full-tank status`() {
        val entries = listOf(
            entry(1, 0, 40.0, 40.0, true),
            entry(10, 500, 20.0, 20.0, false)
        )
        val stats = FuelStatsCalculator.compute(entries)
        assertEquals(500, stats.totalDistanceKm)
        assertEquals(60.0 / 500.0, stats.costPerKm!!, 0.0001)
    }

    @Test
    fun `cost fields are restricted to the majority currency, never summed across currencies`() {
        val entries = listOf(
            entry(1, 0, 40.0, 40.0, true, currencyCode = "JOD"),
            entry(10, 500, 20.0, 20.0, false, currencyCode = "JOD"),
            entry(20, 1000, 20.0, 100.0, true, currencyCode = "USD") // a single fill-up abroad, different currency
        )
        val stats = FuelStatsCalculator.compute(entries)
        assertTrue(stats.hasMixedCostCurrencies)
        assertEquals("JOD", stats.costCurrencyCode)
        // Only the two JOD fill-ups (0..500km, 40+20 JOD) feed cost fields - the USD one is excluded, not blindly added.
        assertEquals(60.0 / 500.0, stats.costPerKm!!, 0.0001)
        // totalDistanceKm (consumption-side, currency-agnostic) still spans the full history including the USD entry.
        assertEquals(1000, stats.totalDistanceKm)
    }
}
