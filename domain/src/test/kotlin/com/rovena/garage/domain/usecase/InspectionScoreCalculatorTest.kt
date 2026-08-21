package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.InspectionItemStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InspectionScoreCalculatorTest {

    @Test
    fun `all unknown yields null score`() {
        val result = InspectionScoreCalculator.calculate(List(5) { InspectionItemStatus.UNKNOWN })
        assertNull(result.score)
        assertEquals(5, result.unknownCount)
    }

    @Test
    fun `all good yields 100`() {
        val result = InspectionScoreCalculator.calculate(List(5) { InspectionItemStatus.GOOD })
        assertEquals(100, result.score)
    }

    @Test
    fun `mixed statuses average correctly and exclude unknown`() {
        val statuses = listOf(
            InspectionItemStatus.GOOD, InspectionItemStatus.GOOD,
            InspectionItemStatus.ATTENTION, InspectionItemStatus.PROBLEM,
            InspectionItemStatus.UNKNOWN
        )
        val result = InspectionScoreCalculator.calculate(statuses)
        // (100+100+55+10)/4 = 66.25 -> 66
        assertEquals(66, result.score)
        assertEquals(2, result.goodCount)
        assertEquals(1, result.attentionCount)
        assertEquals(1, result.problemCount)
        assertEquals(1, result.unknownCount)
    }
}
