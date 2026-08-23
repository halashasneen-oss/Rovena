package com.rovena.garage.domain.usecase

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Estimates a vehicle's driving pace (km/day) purely from its own logged
 * odometer readings (fuel fill-ups and maintenance records already carry a
 * date + mileage - no new data entry required from the user). Feed the
 * result into [DueStatusCalculator.evaluate]'s `averageKmPerDay` parameter to
 * get a labeled, projected due date for mileage-only maintenance/reminders -
 * that projection math already lives there; this only supplies the pace.
 */
object MileageIntelligenceCalculator {

    data class OdometerReading(val date: LocalDate, val mileageKm: Int)

    /** Need at least this many days between the earliest and latest reading for a trustworthy average. */
    private const val MIN_DAY_SPAN = 7

    /**
     * Average km driven per day, computed from the earliest to the latest
     * reading. Null when there isn't enough data: fewer than 2 readings, all
     * readings fall within less than [MIN_DAY_SPAN] days, or mileage didn't
     * actually increase over that span (bad data, not a real pace).
     */
    fun averageKmPerDay(readings: List<OdometerReading>): Double? {
        if (readings.size < 2) return null
        val sorted = readings.sortedBy { it.date }
        val first = sorted.first()
        val last = sorted.last()
        val daySpan = ChronoUnit.DAYS.between(first.date, last.date)
        if (daySpan < MIN_DAY_SPAN) return null
        val kmSpan = last.mileageKm - first.mileageKm
        if (kmSpan <= 0) return null
        return kmSpan.toDouble() / daySpan
    }
}
