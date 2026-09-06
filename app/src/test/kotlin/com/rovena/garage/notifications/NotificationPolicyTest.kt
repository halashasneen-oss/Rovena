package com.rovena.garage.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class NotificationPolicyTest {

    @Test
    fun periodicReminderWaitsForTwelveHours() {
        val lastSent = 1_000L
        assertFalse(
            NotificationPolicy.shouldSendPeriodic(
                lastSentAt = lastSent,
                now = lastSent + TimeUnit.HOURS.toMillis(11) + TimeUnit.MINUTES.toMillis(59)
            )
        )
        assertTrue(
            NotificationPolicy.shouldSendPeriodic(
                lastSentAt = lastSent,
                now = lastSent + TimeUnit.HOURS.toMillis(12)
            )
        )
    }

    @Test
    fun firstAndForcedReminderCanSendImmediately() {
        assertTrue(NotificationPolicy.shouldSendPeriodic(lastSentAt = 0L, now = 10_000L))
        assertTrue(
            NotificationPolicy.shouldSendPeriodic(
                lastSentAt = 9_999L,
                now = 10_000L,
                force = true
            )
        )
    }
}
