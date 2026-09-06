package com.rovena.garage.notifications

import java.util.concurrent.TimeUnit

object NotificationPolicy {
    const val INTERVAL_HOURS = 12L
    val intervalMillis: Long = TimeUnit.HOURS.toMillis(INTERVAL_HOURS)

    fun shouldSendPeriodic(
        lastSentAt: Long,
        now: Long,
        force: Boolean = false
    ): Boolean {
        if (force) return true
        if (lastSentAt <= 0L) return true
        return now - lastSentAt >= intervalMillis
    }
}
