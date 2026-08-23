package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VehicleInsightGeneratorTest {

    @Test
    fun `fuel economy insight is null with fewer than 4 intervals`() {
        assertNull(VehicleInsightGenerator.fuelEconomyInsight(listOf(8.0, 8.0, 8.0)))
    }

    @Test
    fun `fuel economy insight is null when the change is below the significance threshold`() {
        // older avg 8.0, recent avg 8.2 -> 2.5% change, below the 8% threshold
        assertNull(VehicleInsightGenerator.fuelEconomyInsight(listOf(8.0, 8.0, 8.2, 8.2)))
    }

    @Test
    fun `fuel economy insight reports improved when recent consumption is lower`() {
        // older avg 10.0, recent avg 8.0 -> -20% (uses less fuel = improved)
        val insight = VehicleInsightGenerator.fuelEconomyInsight(listOf(10.0, 10.0, 8.0, 8.0))
        assertEquals(VehicleInsightGenerator.Insight.FuelEconomyChanged(percent = 20, improved = true), insight)
    }

    @Test
    fun `fuel economy insight reports worsened when recent consumption is higher`() {
        // older avg 8.0, recent avg 10.0 -> +25%
        val insight = VehicleInsightGenerator.fuelEconomyInsight(listOf(8.0, 8.0, 10.0, 10.0))
        assertEquals(VehicleInsightGenerator.Insight.FuelEconomyChanged(percent = 25, improved = false), insight)
    }

    @Test
    fun `maintenance cost insight is null with fewer than 6 months`() {
        assertNull(VehicleInsightGenerator.maintenanceCostInsight(listOf(0.0, 0.0, 0.0, 50.0, 50.0)))
    }

    @Test
    fun `maintenance cost insight is null when there was no prior spending to compare against`() {
        assertNull(VehicleInsightGenerator.maintenanceCostInsight(listOf(0.0, 0.0, 0.0, 50.0, 50.0, 50.0)))
    }

    @Test
    fun `maintenance cost insight reports increased spending`() {
        // older sum 100 (3 months), recent sum 300 (3 months) -> +200%
        val insight = VehicleInsightGenerator.maintenanceCostInsight(listOf(0.0, 50.0, 50.0, 100.0, 100.0, 100.0))
        assertEquals(VehicleInsightGenerator.Insight.MaintenanceCostChanged(percent = 200, increased = true), insight)
    }

    @Test
    fun `maintenance cost insight reports decreased spending`() {
        // older sum 300, recent sum 100 -> -67%
        val insight = VehicleInsightGenerator.maintenanceCostInsight(listOf(100.0, 100.0, 100.0, 50.0, 50.0, 0.0))
        assertEquals(VehicleInsightGenerator.Insight.MaintenanceCostChanged(percent = 67, increased = false), insight)
    }

    @Test
    fun `generate combines both insights when both are significant`() {
        val insights = VehicleInsightGenerator.generate(
            litersPer100KmChronological = listOf(10.0, 10.0, 8.0, 8.0),
            monthlyMaintenanceCostChronological = listOf(0.0, 50.0, 50.0, 100.0, 100.0, 100.0)
        )
        assertEquals(2, insights.size)
    }

    @Test
    fun `generate returns an empty list when nothing is significant`() {
        val insights = VehicleInsightGenerator.generate(emptyList(), emptyList())
        assertEquals(emptyList<VehicleInsightGenerator.Insight>(), insights)
    }
}
