package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.DueStatus

/**
 * ROVENA's "What Needs Attention?" engine (spec: unified priority engine).
 *
 * The dashboard's various sources of "something needs attention" - overdue/
 * due-soon maintenance, expiring/expired documents, due/overdue reminders -
 * are each evaluated independently by [DueStatusCalculator]. This object
 * only combines those already-evaluated results into one ranked list (most
 * urgent first) and derives a single overall [VehicleStatus] from them, so
 * the user sees one clear answer instead of several separate lists they'd
 * have to cross-reference themselves.
 *
 * Deliberately excludes anything with [DueStatus.UPCOMING] - "needs
 * attention" means actionable soon, not everything the app happens to be
 * tracking.
 */
object PriorityEngine {

    enum class AttentionSourceType { MAINTENANCE, DOCUMENT, REMINDER }

    data class AttentionItem(
        val type: AttentionSourceType,
        val sourceId: Long,
        val title: String,
        val status: DueStatus,
        val remainingKm: Int?,
        val remainingDays: Long?
    )

    enum class VehicleStatus { HEALTHY, ATTENTION, URGENT }

    data class Result(val items: List<AttentionItem>, val vehicleStatus: VehicleStatus)

    /**
     * Filters out anything not actually urgent (UPCOMING), ranks the rest
     * most-urgent-first, and derives the overall vehicle status: URGENT if
     * anything is overdue, ATTENTION if anything is due/due-soon, else
     * HEALTHY (nothing actionable right now - which also covers "no data
     * tracked yet", the same non-alarmist default the Health Score uses).
     */
    fun build(candidates: List<AttentionItem>): Result {
        val actionable = candidates.filter { it.status != DueStatus.UPCOMING }
        val ranked = actionable.sortedWith(
            compareBy(
                { it.status.urgencyRank() },
                { it.remainingKm ?: Int.MAX_VALUE },
                { it.remainingDays ?: Long.MAX_VALUE }
            )
        )
        val vehicleStatus = when {
            ranked.any { it.status == DueStatus.OVERDUE } -> VehicleStatus.URGENT
            ranked.isNotEmpty() -> VehicleStatus.ATTENTION
            else -> VehicleStatus.HEALTHY
        }
        return Result(ranked, vehicleStatus)
    }

    private fun DueStatus.urgencyRank(): Int = when (this) {
        DueStatus.OVERDUE -> 0
        DueStatus.DUE -> 1
        DueStatus.DUE_SOON -> 2
        DueStatus.UPCOMING -> 3
    }
}
