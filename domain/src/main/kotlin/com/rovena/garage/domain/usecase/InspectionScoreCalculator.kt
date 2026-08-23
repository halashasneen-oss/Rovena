package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.InspectionItemStatus
import kotlin.math.roundToInt

/**
 * Scores a single vehicle inspection (spec #13), separate from the
 * longer-lived [HealthScoreCalculator]. GOOD items count fully, ATTENTION
 * items count partially, PROBLEM items count as failing, and UNKNOWN items
 * (not evaluated) are excluded entirely rather than penalized.
 */
object InspectionScoreCalculator {

    data class Result(
        val score: Int?,
        val goodCount: Int,
        val attentionCount: Int,
        val problemCount: Int,
        val unknownCount: Int
    )

    private const val GOOD_POINTS = 100
    private const val ATTENTION_POINTS = 55
    private const val PROBLEM_POINTS = 10

    fun calculate(statuses: List<InspectionItemStatus>): Result {
        val good = statuses.count { it == InspectionItemStatus.GOOD }
        val attention = statuses.count { it == InspectionItemStatus.ATTENTION }
        val problem = statuses.count { it == InspectionItemStatus.PROBLEM }
        val unknown = statuses.count { it == InspectionItemStatus.UNKNOWN }

        val evaluated = good + attention + problem
        val score = if (evaluated == 0) {
            null
        } else {
            ((good * GOOD_POINTS + attention * ATTENTION_POINTS + problem * PROBLEM_POINTS).toDouble() / evaluated)
                .roundToInt()
                .coerceIn(0, 100)
        }

        return Result(score, good, attention, problem, unknown)
    }

    /**
     * Maps a single inspection item's status to a 0-100 condition score using the same
     * point scale as [calculate] - used to feed real inspection results into
     * HealthScoreCalculator's per-category sub-scores (brakes/tires/battery/fluids).
     * UNKNOWN means "not evaluated", so it is excluded (null) rather than penalized.
     */
    fun conditionScoreFor(status: InspectionItemStatus): Int? = when (status) {
        InspectionItemStatus.GOOD -> GOOD_POINTS
        InspectionItemStatus.ATTENTION -> ATTENTION_POINTS
        InspectionItemStatus.PROBLEM -> PROBLEM_POINTS
        InspectionItemStatus.UNKNOWN -> null
    }
}
