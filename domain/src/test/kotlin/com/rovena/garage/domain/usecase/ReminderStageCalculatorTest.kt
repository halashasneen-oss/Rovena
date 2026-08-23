package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReminderStageCalculatorTest {

    @Test
    fun `far in the future yields no stage yet`() {
        assertNull(ReminderStageCalculator.stageFor(45))
        assertNull(ReminderStageCalculator.stageFor(31))
    }

    @Test
    fun `entering the 30-day window yields the 30 stage`() {
        assertEquals(30, ReminderStageCalculator.stageFor(30))
        assertEquals(30, ReminderStageCalculator.stageFor(25))
        assertEquals(30, ReminderStageCalculator.stageFor(15))
    }

    @Test
    fun `each threshold yields its own stage, most urgent applicable one wins`() {
        assertEquals(14, ReminderStageCalculator.stageFor(14))
        assertEquals(14, ReminderStageCalculator.stageFor(10))
        assertEquals(7, ReminderStageCalculator.stageFor(7))
        assertEquals(7, ReminderStageCalculator.stageFor(5))
        assertEquals(3, ReminderStageCalculator.stageFor(3))
        assertEquals(3, ReminderStageCalculator.stageFor(2))
        assertEquals(1, ReminderStageCalculator.stageFor(1))
        assertEquals(0, ReminderStageCalculator.stageFor(0))
    }

    @Test
    fun `past due yields the expired sentinel regardless of how overdue`() {
        assertEquals(ReminderStageCalculator.EXPIRED_STAGE, ReminderStageCalculator.stageFor(-1))
        assertEquals(ReminderStageCalculator.EXPIRED_STAGE, ReminderStageCalculator.stageFor(-365))
    }
}
