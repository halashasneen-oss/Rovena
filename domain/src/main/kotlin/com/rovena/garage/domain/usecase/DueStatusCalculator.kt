package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.DueStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Shared "is this due yet" engine used by both maintenance next-due tracking
 * (spec #18 Smart Maintenance Logic) and reminders (spec #17). Supports
 * mileage-based, date-based, or combined triggers and never silently rewrites
 * user data - it only reports a status.
 */
object DueStatusCalculator {

    /** Distance/time windows that define "due soon" vs "due" vs "overdue". */
    data class Thresholds(
        val dueKm: Int = 200,
        val dueSoonKm: Int = 1000,
        val dueDays: Long = 7,
        val dueSoonDays: Long = 30
    )

    data class Evaluation(
        val status: DueStatus,
        val remainingKm: Int?,
        val remainingDays: Long?,
        /** Only populated when we have enough driving history to project it; never fabricated. */
        val estimatedDueDate: LocalDate?
    )

    fun evaluate(
        currentMileageKm: Int,
        today: LocalDate,
        dueMileageKm: Int?,
        dueDate: LocalDate?,
        averageKmPerDay: Double? = null,
        thresholds: Thresholds = Thresholds()
    ): Evaluation? {
        if (dueMileageKm == null && dueDate == null) return null

        val remainingKm = dueMileageKm?.let { it - currentMileageKm }
        val remainingDays = dueDate?.let { ChronoUnit.DAYS.between(today, it) }

        val mileageStatus = remainingKm?.let { statusFromKm(it, thresholds) }
        val dateStatus = remainingDays?.let { statusFromDays(it, thresholds) }

        // Most urgent status wins when both triggers are present.
        val status = listOfNotNull(mileageStatus, dateStatus).minByOrNull { it.urgencyRank() }
            ?: DueStatus.UPCOMING

        val estimatedDueDate = when {
            dueDate != null -> dueDate
            dueMileageKm != null && averageKmPerDay != null && averageKmPerDay > 0 -> {
                val daysAway = (remainingKm!! / averageKmPerDay)
                today.plusDays(daysAway.toLong().coerceAtLeast(0))
            }
            else -> null
        }

        return Evaluation(status, remainingKm, remainingDays, estimatedDueDate)
    }

    private fun statusFromKm(remainingKm: Int, t: Thresholds): DueStatus = when {
        remainingKm <= 0 -> DueStatus.OVERDUE
        remainingKm <= t.dueKm -> DueStatus.DUE
        remainingKm <= t.dueSoonKm -> DueStatus.DUE_SOON
        else -> DueStatus.UPCOMING
    }

    private fun statusFromDays(remainingDays: Long, t: Thresholds): DueStatus = when {
        remainingDays <= 0 -> DueStatus.OVERDUE
        remainingDays <= t.dueDays -> DueStatus.DUE
        remainingDays <= t.dueSoonDays -> DueStatus.DUE_SOON
        else -> DueStatus.UPCOMING
    }

    private fun DueStatus.urgencyRank(): Int = when (this) {
        DueStatus.OVERDUE -> 0
        DueStatus.DUE -> 1
        DueStatus.DUE_SOON -> 2
        DueStatus.UPCOMING -> 3
    }
}
