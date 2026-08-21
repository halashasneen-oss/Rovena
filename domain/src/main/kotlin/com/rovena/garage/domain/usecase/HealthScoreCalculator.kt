package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.HealthCategory
import com.rovena.garage.domain.model.HealthStatus
import kotlin.math.roundToInt

/**
 * ROVENA Vehicle Health Score algorithm.
 *
 * IMPORTANT: this is NOT a mechanical diagnosis. It is a transparent,
 * maintenance-condition score derived only from data the user has entered
 * (service history, inspection results, tracked documents). Categories the
 * user has never provided data for are treated as "unknown" and excluded
 * from the score rather than guessed.
 *
 * Algorithm:
 * 1. Each [HealthCategory] contributes a sub-score (0-100) and a relative
 *    weight (see [defaultWeights]). A category whose sub-score is `null`
 *    means "no data" and is dropped entirely from the calculation.
 * 2. If the total weight of categories WITH data falls below
 *    [MIN_KNOWN_WEIGHT_RATIO] of all possible weight, the vehicle does not
 *    have enough information for a trustworthy score and the result is
 *    [HealthStatus.NOT_ENOUGH_DATA] (score = null). This prevents a brand
 *    new vehicle profile from showing a fabricated number.
 * 3. Otherwise the final score is the weighted average of the known
 *    sub-scores, renormalized against the known weight (so missing
 *    categories don't unfairly drag the score down), rounded to the
 *    nearest integer and clamped to [0, 100].
 * 4. The numeric score maps to a status band:
 *    90-100 Excellent, 75-89 Good, 60-74 Fair, 40-59 Attention Needed, 0-39 Critical.
 */
object HealthScoreCalculator {

    /** Minimum fraction of total possible weight that must have real data before we score at all. */
    const val MIN_KNOWN_WEIGHT_RATIO = 0.34

    fun defaultWeights(): Map<HealthCategory, Double> = mapOf(
        HealthCategory.MAINTENANCE_RECENCY to 0.18,
        HealthCategory.OVERDUE_MAINTENANCE to 0.20,
        HealthCategory.BRAKES to 0.14,
        HealthCategory.TIRES to 0.12,
        HealthCategory.BATTERY to 0.08,
        HealthCategory.FLUIDS to 0.08,
        HealthCategory.ENGINE_SERVICE to 0.10,
        HealthCategory.TRANSMISSION_SERVICE to 0.05,
        HealthCategory.DOCUMENTATION to 0.05
    )

    data class CategoryInput(
        val category: HealthCategory,
        /** 0-100, or null when there is no data for this category yet. */
        val score: Int?,
        val weight: Double
    )

    data class Result(
        val score: Int?,
        val status: HealthStatus,
        val categoryBreakdown: Map<HealthCategory, Int?>,
        val knownWeightRatio: Double
    )

    fun calculate(inputs: List<CategoryInput>): Result {
        val totalWeight = inputs.sumOf { it.weight }
        require(totalWeight > 0.0) { "Total weight must be > 0" }

        val known = inputs.filter { it.score != null }
        val knownWeight = known.sumOf { it.weight }
        val knownRatio = knownWeight / totalWeight
        val breakdown = inputs.associate { it.category to it.score }

        if (known.isEmpty() || knownRatio < MIN_KNOWN_WEIGHT_RATIO) {
            return Result(
                score = null,
                status = HealthStatus.NOT_ENOUGH_DATA,
                categoryBreakdown = breakdown,
                knownWeightRatio = knownRatio
            )
        }

        val weightedSum = known.sumOf { (it.score ?: 0) * it.weight }
        val rawScore = weightedSum / knownWeight
        val finalScore = rawScore.roundToInt().coerceIn(0, 100)

        return Result(
            score = finalScore,
            status = statusFor(finalScore),
            categoryBreakdown = breakdown,
            knownWeightRatio = knownRatio
        )
    }

    fun statusFor(score: Int): HealthStatus = when {
        score >= 90 -> HealthStatus.EXCELLENT
        score >= 75 -> HealthStatus.GOOD
        score >= 60 -> HealthStatus.FAIR
        score >= 40 -> HealthStatus.ATTENTION_NEEDED
        else -> HealthStatus.CRITICAL
    }

    /**
     * Convenience overload that derives sub-scores from real vehicle signals using
     * documented, deterministic rules (no randomness, no fabrication). Any input left
     * `null` means "the user hasn't provided this yet" and that category is skipped.
     */
    data class VehicleHealthInputs(
        /** Days since the most recent maintenance record of any kind, null if none logged. */
        val daysSinceLastMaintenance: Int?,
        /** Count of maintenance items whose next-due mileage/date has passed. */
        val overdueMaintenanceCount: Int?,
        /** Total number of active (non-completed) maintenance reminders being tracked. */
        val totalActiveMaintenanceItems: Int?,
        /** Latest inspection sub-score (0-100) for brakes, or null if never inspected. */
        val brakesConditionScore: Int?,
        val tiresConditionScore: Int?,
        val batteryConditionScore: Int?,
        val fluidsConditionScore: Int?,
        /** True if engine service (oil/filters) is currently up to date per next-due tracking. */
        val engineServiceUpToDate: Boolean?,
        val transmissionServiceUpToDate: Boolean?,
        /** True if any tracked document (registration/insurance/inspection) is expired. */
        val hasExpiredDocument: Boolean?,
        /** True if there is at least one tracked document at all. */
        val hasAnyTrackedDocument: Boolean?
    )

    fun fromVehicleInputs(inputs: VehicleHealthInputs, weights: Map<HealthCategory, Double> = defaultWeights()): Result {
        val categoryInputs = listOf(
            CategoryInput(
                HealthCategory.MAINTENANCE_RECENCY,
                score = inputs.daysSinceLastMaintenance?.let { days ->
                    when {
                        days <= 90 -> 100
                        days <= 180 -> 85
                        days <= 365 -> 65
                        days <= 730 -> 40
                        else -> 20
                    }
                },
                weight = weights.getValue(HealthCategory.MAINTENANCE_RECENCY)
            ),
            CategoryInput(
                HealthCategory.OVERDUE_MAINTENANCE,
                score = if (inputs.overdueMaintenanceCount == null || inputs.totalActiveMaintenanceItems == null) {
                    null
                } else if (inputs.totalActiveMaintenanceItems == 0) {
                    null
                } else {
                    val overdueRatio = inputs.overdueMaintenanceCount.toDouble() / inputs.totalActiveMaintenanceItems
                    (100 - (overdueRatio * 100)).roundToInt().coerceIn(0, 100)
                },
                weight = weights.getValue(HealthCategory.OVERDUE_MAINTENANCE)
            ),
            CategoryInput(HealthCategory.BRAKES, inputs.brakesConditionScore, weights.getValue(HealthCategory.BRAKES)),
            CategoryInput(HealthCategory.TIRES, inputs.tiresConditionScore, weights.getValue(HealthCategory.TIRES)),
            CategoryInput(HealthCategory.BATTERY, inputs.batteryConditionScore, weights.getValue(HealthCategory.BATTERY)),
            CategoryInput(HealthCategory.FLUIDS, inputs.fluidsConditionScore, weights.getValue(HealthCategory.FLUIDS)),
            CategoryInput(
                HealthCategory.ENGINE_SERVICE,
                score = inputs.engineServiceUpToDate?.let { if (it) 100 else 45 },
                weight = weights.getValue(HealthCategory.ENGINE_SERVICE)
            ),
            CategoryInput(
                HealthCategory.TRANSMISSION_SERVICE,
                score = inputs.transmissionServiceUpToDate?.let { if (it) 100 else 45 },
                weight = weights.getValue(HealthCategory.TRANSMISSION_SERVICE)
            ),
            CategoryInput(
                HealthCategory.DOCUMENTATION,
                score = when {
                    inputs.hasAnyTrackedDocument != true -> null
                    inputs.hasExpiredDocument == true -> 30
                    else -> 100
                },
                weight = weights.getValue(HealthCategory.DOCUMENTATION)
            )
        )
        return calculate(categoryInputs)
    }
}
