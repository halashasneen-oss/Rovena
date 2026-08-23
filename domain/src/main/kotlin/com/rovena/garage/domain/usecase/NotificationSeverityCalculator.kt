package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.model.NotificationSeverity

/**
 * Maps a reminder's staged-schedule threshold (see [ReminderStageCalculator])
 * or plain [DueStatus] onto a [NotificationSeverity], so the notification
 * worker and the per-severity Settings toggles agree on exactly the same
 * classification.
 */
object NotificationSeverityCalculator {

    /** For date-based reminders, driven by the day-threshold that just fired. */
    fun forStage(stageDays: Int): NotificationSeverity = when (stageDays) {
        ReminderStageCalculator.EXPIRED_STAGE, 0, 1 -> NotificationSeverity.CRITICAL
        3, 7 -> NotificationSeverity.IMPORTANT
        else -> NotificationSeverity.UPCOMING
    }

    /** For mileage-only reminders, which only ever notify while DUE or OVERDUE. */
    fun forDueStatus(status: DueStatus): NotificationSeverity = when (status) {
        DueStatus.OVERDUE -> NotificationSeverity.CRITICAL
        else -> NotificationSeverity.IMPORTANT
    }
}
