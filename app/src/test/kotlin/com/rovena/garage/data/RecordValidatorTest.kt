package com.rovena.garage.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordValidatorTest {
    @Test
    fun maintenanceRequiresServiceAndValidMileage() {
        assertTrue(
            RecordValidator.validMaintenance(
                MaintenanceDraft(serviceType = "Oil change", mileage = 50_000, cost = 25.0, nextDueMileage = 55_000)
            )
        )
        assertFalse(
            RecordValidator.validMaintenance(
                MaintenanceDraft(serviceType = "", mileage = 50_000, cost = 25.0)
            )
        )
        assertFalse(
            RecordValidator.validMaintenance(
                MaintenanceDraft(serviceType = "Oil change", mileage = 50_000, cost = 25.0, nextDueMileage = 49_000)
            )
        )
    }

    @Test
    fun fuelRequiresPositiveLiters() {
        assertTrue(RecordValidator.validFuel(FuelDraft(mileage = 50_100, liters = 30.5, totalCost = 28.0)))
        assertFalse(RecordValidator.validFuel(FuelDraft(mileage = 50_100, liters = 0.0, totalCost = 28.0)))
    }

    @Test
    fun expenseAndDocumentRequireMeaningfulFields() {
        assertTrue(RecordValidator.validExpense(ExpenseDraft(category = "Insurance", amount = 120.0)))
        assertFalse(RecordValidator.validExpense(ExpenseDraft(category = "", amount = 120.0)))
        assertTrue(RecordValidator.validDocument(DocumentDraft(title = "Insurance", category = "Policy")))
        assertFalse(RecordValidator.validDocument(DocumentDraft(title = "Insurance", category = "")))
    }
}
