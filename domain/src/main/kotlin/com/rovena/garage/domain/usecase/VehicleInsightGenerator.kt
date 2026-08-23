package com.rovena.garage.domain.usecase

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Local, deterministic, rules-based "Vehicle Intelligence" text generator
 * (spec: Vehicle Intelligence Engine). Compares the recent half of a trend
 * against its older half and only emits an [Insight] when the change is
 * large enough to be worth telling the user about - never a fabricated
 * observation, and never anything resembling AI-generated prose. Callers
 * map each [Insight] to a localized string template; this object never
 * produces user-facing text itself, since the app is localized to 4
 * languages and domain has no string resources.
 */
object VehicleInsightGenerator {

    private const val SIGNIFICANCE_THRESHOLD_PERCENT = 8.0
    private const val MIN_FUEL_INTERVALS = 4
    private const val MIN_MAINTENANCE_MONTHS = 6

    sealed class Insight {
        data class FuelEconomyChanged(val percent: Int, val improved: Boolean) : Insight()
        data class MaintenanceCostChanged(val percent: Int, val increased: Boolean) : Insight()
    }

    /** [litersPer100KmChronological] must be oldest-first, e.g. FuelStats.intervals.map { it.litersPer100Km }. */
    fun fuelEconomyInsight(litersPer100KmChronological: List<Double>): Insight.FuelEconomyChanged? {
        if (litersPer100KmChronological.size < MIN_FUEL_INTERVALS) return null
        val mid = litersPer100KmChronological.size / 2
        val older = litersPer100KmChronological.take(mid).average()
        val recent = litersPer100KmChronological.takeLast(litersPer100KmChronological.size - mid).average()
        if (older <= 0.0) return null

        // Lower L/100km is better fuel economy, so a negative % change is an improvement.
        val percentChange = ((recent - older) / older) * 100.0
        if (abs(percentChange) < SIGNIFICANCE_THRESHOLD_PERCENT) return null
        return Insight.FuelEconomyChanged(percent = abs(percentChange).roundToInt(), improved = percentChange < 0)
    }

    /** [monthlyMaintenanceCostChronological] must be oldest-first, one entry per calendar month, e.g. the last 6 months. */
    fun maintenanceCostInsight(monthlyMaintenanceCostChronological: List<Double>): Insight.MaintenanceCostChanged? {
        if (monthlyMaintenanceCostChronological.size < MIN_MAINTENANCE_MONTHS) return null
        val mid = monthlyMaintenanceCostChronological.size / 2
        val older = monthlyMaintenanceCostChronological.take(mid).sum()
        val recent = monthlyMaintenanceCostChronological.takeLast(monthlyMaintenanceCostChronological.size - mid).sum()
        if (older <= 0.0) return null

        val percentChange = ((recent - older) / older) * 100.0
        if (abs(percentChange) < SIGNIFICANCE_THRESHOLD_PERCENT) return null
        return Insight.MaintenanceCostChanged(percent = abs(percentChange).roundToInt(), increased = percentChange > 0)
    }

    fun generate(litersPer100KmChronological: List<Double>, monthlyMaintenanceCostChronological: List<Double>): List<Insight> =
        listOfNotNull(
            fuelEconomyInsight(litersPer100KmChronological),
            maintenanceCostInsight(monthlyMaintenanceCostChronological)
        )
}
