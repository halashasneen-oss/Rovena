package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.usecase.PriorityEngine.AttentionItem
import com.rovena.garage.domain.usecase.PriorityEngine.AttentionSourceType
import com.rovena.garage.domain.usecase.PriorityEngine.VehicleStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PriorityEngineTest {

    private fun item(status: DueStatus, title: String = "x", remainingKm: Int? = null, remainingDays: Long? = null) =
        AttentionItem(AttentionSourceType.MAINTENANCE, 1, title, status, remainingKm, remainingDays)

    @Test
    fun `no candidates yields healthy status and empty list`() {
        val result = PriorityEngine.build(emptyList())
        assertTrue(result.items.isEmpty())
        assertEquals(VehicleStatus.HEALTHY, result.vehicleStatus)
    }

    @Test
    fun `only upcoming items are filtered out entirely and status is healthy`() {
        val result = PriorityEngine.build(listOf(item(DueStatus.UPCOMING), item(DueStatus.UPCOMING)))
        assertTrue(result.items.isEmpty())
        assertEquals(VehicleStatus.HEALTHY, result.vehicleStatus)
    }

    @Test
    fun `any overdue item makes vehicle status urgent`() {
        val result = PriorityEngine.build(listOf(item(DueStatus.DUE_SOON), item(DueStatus.OVERDUE)))
        assertEquals(VehicleStatus.URGENT, result.vehicleStatus)
    }

    @Test
    fun `due or due-soon without overdue makes vehicle status attention`() {
        val result = PriorityEngine.build(listOf(item(DueStatus.DUE_SOON)))
        assertEquals(VehicleStatus.ATTENTION, result.vehicleStatus)
    }

    @Test
    fun `items are ranked most urgent first, upcoming excluded`() {
        val result = PriorityEngine.build(
            listOf(
                item(DueStatus.UPCOMING, title = "far off"),
                item(DueStatus.DUE_SOON, title = "due soon"),
                item(DueStatus.OVERDUE, title = "overdue"),
                item(DueStatus.DUE, title = "due")
            )
        )
        assertEquals(listOf("overdue", "due", "due soon"), result.items.map { it.title })
    }

    @Test
    fun `within the same status, smaller remaining km or days sorts first`() {
        val result = PriorityEngine.build(
            listOf(
                item(DueStatus.DUE_SOON, title = "500km", remainingKm = 500),
                item(DueStatus.DUE_SOON, title = "100km", remainingKm = 100)
            )
        )
        assertEquals(listOf("100km", "500km"), result.items.map { it.title })
    }
}
