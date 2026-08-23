package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.model.NotificationSeverity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotificationSeverityCalculatorTest {

    @Test
    fun `overdue and imminent stages are critical`() {
        assertEquals(NotificationSeverity.CRITICAL, NotificationSeverityCalculator.forStage(ReminderStageCalculator.EXPIRED_STAGE))
        assertEquals(NotificationSeverity.CRITICAL, NotificationSeverityCalculator.forStage(0))
        assertEquals(NotificationSeverity.CRITICAL, NotificationSeverityCalculator.forStage(1))
    }

    @Test
    fun `3 and 7 day stages are important`() {
        assertEquals(NotificationSeverity.IMPORTANT, NotificationSeverityCalculator.forStage(3))
        assertEquals(NotificationSeverity.IMPORTANT, NotificationSeverityCalculator.forStage(7))
    }

    @Test
    fun `14 and 30 day stages are upcoming`() {
        assertEquals(NotificationSeverity.UPCOMING, NotificationSeverityCalculator.forStage(14))
        assertEquals(NotificationSeverity.UPCOMING, NotificationSeverityCalculator.forStage(30))
    }

    @Test
    fun `overdue due-status is critical, due is important`() {
        assertEquals(NotificationSeverity.CRITICAL, NotificationSeverityCalculator.forDueStatus(DueStatus.OVERDUE))
        assertEquals(NotificationSeverity.IMPORTANT, NotificationSeverityCalculator.forDueStatus(DueStatus.DUE))
    }
}
