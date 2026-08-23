package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.MaintenanceCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InspectionMaintenanceSuggesterTest {

    @Test
    fun `mechanical items with an unambiguous category are mapped`() {
        assertEquals(MaintenanceCategory.BRAKE_PADS, InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.BRAKES))
        assertEquals(MaintenanceCategory.TIRES, InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.TIRES))
        assertEquals(MaintenanceCategory.BATTERY, InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.BATTERY))
        assertEquals(MaintenanceCategory.SUSPENSION, InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.SUSPENSION))
        assertEquals(MaintenanceCategory.STEERING, InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.STEERING))
        assertEquals(MaintenanceCategory.ENGINE, InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.ENGINE))
        assertEquals(MaintenanceCategory.TRANSMISSION, InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.TRANSMISSION))
        assertEquals(MaintenanceCategory.COOLANT, InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.COOLING))
    }

    @Test
    fun `ambiguous FLUIDS item is deliberately not mapped`() {
        assertNull(InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.FLUIDS))
    }

    @Test
    fun `exterior and interior items are not mapped`() {
        assertNull(InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.PAINT))
        assertNull(InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.SEATS))
        assertNull(InspectionMaintenanceSuggester.suggestedCategory(InspectionItemKey.DASHBOARD))
    }
}
