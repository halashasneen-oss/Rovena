package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServicePlanCatalogTest {

    @Test
    fun `every plan has a unique id and at least two categories`() {
        assertTrue(ServicePlanCatalog.PLANS.isNotEmpty())
        val ids = ServicePlanCatalog.PLANS.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
        ServicePlanCatalog.PLANS.forEach { plan ->
            assertTrue(plan.categories.size >= 2, "${plan.id} should bundle at least 2 categories")
            assertEquals(plan.categories.distinct().size, plan.categories.size, "${plan.id} should not repeat a category")
        }
    }
}
