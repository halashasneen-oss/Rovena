package com.rovena.garage.domain.usecase

/**
 * Staged notification schedule for date-based reminders (spec: Registration/
 * Insurance reminder schedule - "a core feature"). Rather than a single
 * due/overdue notification, a date-based reminder is notified once per stage
 * as its deadline approaches: 30, 14, 7, 3, 1 days out, the due day itself,
 * and once more when it goes past due ("expired"). Each stage fires exactly
 * once - the caller compares the result against the reminder's last-notified
 * stage and only acts when it's genuinely new and more urgent.
 *
 * Deliberately calendar/date-only: mileage-based due tracking doesn't have
 * the same fixed real-world cadence, so it keeps its existing single
 * due/overdue notification instead of being staged.
 */
object ReminderStageCalculator {

    /** Day-before-due thresholds, ascending (most lenient first). */
    private val THRESHOLDS_DAYS = listOf(0, 1, 3, 7, 14, 30)

    /** Sentinel stage for "past due" - distinct from every real day-threshold above. */
    const val EXPIRED_STAGE = -1

    /**
     * The most urgent stage reached given [remainingDays] until the deadline
     * (negative means already past due). Returns null when the deadline is
     * still further out than the largest configured threshold - no
     * notification is due yet.
     */
    fun stageFor(remainingDays: Long): Int? {
        if (remainingDays < 0) return EXPIRED_STAGE
        return THRESHOLDS_DAYS.firstOrNull { remainingDays <= it }
    }
}
