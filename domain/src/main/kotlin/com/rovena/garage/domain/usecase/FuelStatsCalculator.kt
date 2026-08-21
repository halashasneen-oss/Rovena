package com.rovena.garage.domain.usecase

import java.time.LocalDate
import java.time.YearMonth

/**
 * Computes fuel-economy statistics from a vehicle's fuel log.
 *
 * Consumption (L/100km, km/L) is only meaningful between two consecutive
 * FULL-TANK fill-ups, because partial fills don't tell us how much fuel was
 * actually burned since the tank wasn't filled from empty-to-full. So we only
 * derive consumption from "full-to-full" intervals. If fewer than two
 * full-tank records exist, we never fabricate a number - callers get `null`
 * and should show "Not enough data".
 */
object FuelStatsCalculator {

    data class FuelEntry(
        val date: LocalDate,
        val odometerKm: Int,
        val liters: Double,
        val totalCost: Double,
        val isFullTank: Boolean
    )

    data class IntervalConsumption(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val distanceKm: Int,
        val litersUsed: Double,
        val litersPer100Km: Double
    )

    data class FuelStats(
        val averageLitersPer100Km: Double?,
        val bestLitersPer100Km: Double?,
        val worstLitersPer100Km: Double?,
        val averageKmPerLiter: Double?,
        val costPerKm: Double?,
        val monthlyCost: Map<YearMonth, Double>,
        val yearlyCost: Map<Int, Double>,
        val totalDistanceKm: Int?,
        val intervals: List<IntervalConsumption>,
        val hasEnoughDataForConsumption: Boolean
    )

    fun compute(entries: List<FuelEntry>): FuelStats {
        val sorted = entries.sortedBy { it.odometerKm }

        val monthlyCost = sorted.groupBy { YearMonth.from(it.date) }
            .mapValues { (_, list) -> list.sumOf { it.totalCost } }
        val yearlyCost = sorted.groupBy { it.date.year }
            .mapValues { (_, list) -> list.sumOf { it.totalCost } }

        val fullTankIndices = sorted.withIndex().filter { it.value.isFullTank }.map { it.index }

        val intervals = mutableListOf<IntervalConsumption>()
        for (i in 0 until fullTankIndices.size - 1) {
            val startIdx = fullTankIndices[i]
            val endIdx = fullTankIndices[i + 1]
            val start = sorted[startIdx]
            val end = sorted[endIdx]
            val distance = end.odometerKm - start.odometerKm
            if (distance <= 0) continue

            // Liters burned in this interval = all fills strictly after the start
            // full-up, up to and including the end full-up (the start fill itself
            // "paid for" the fuel already in the tank at the start reading).
            val litersUsed = ((startIdx + 1)..endIdx).sumOf { sorted[it].liters }
            if (litersUsed <= 0) continue

            val per100 = (litersUsed / distance) * 100.0
            intervals.add(IntervalConsumption(start.date, end.date, distance, litersUsed, per100))
        }

        val hasEnough = intervals.isNotEmpty()
        val avg = if (hasEnough) intervals.sumOf { it.litersPer100Km } / intervals.size else null
        val best = intervals.minByOrNull { it.litersPer100Km }?.litersPer100Km
        val worst = intervals.maxByOrNull { it.litersPer100Km }?.litersPer100Km
        val avgKmPerL = avg?.let { if (it > 0) 100.0 / it else null }

        val totalDistance = if (sorted.size >= 2) sorted.last().odometerKm - sorted.first().odometerKm else null
        val totalCost = sorted.sumOf { it.totalCost }
        val costPerKm = if (totalDistance != null && totalDistance > 0) totalCost / totalDistance else null

        return FuelStats(
            averageLitersPer100Km = avg,
            bestLitersPer100Km = best,
            worstLitersPer100Km = worst,
            averageKmPerLiter = avgKmPerL,
            costPerKm = costPerKm,
            monthlyCost = monthlyCost,
            yearlyCost = yearlyCost,
            totalDistanceKm = totalDistance,
            intervals = intervals,
            hasEnoughDataForConsumption = hasEnough
        )
    }
}
