package com.rovena.garage.notifications

import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.MaintenanceEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ReminderEvaluatorTest {
    @Test
    fun maintenanceCanBecomeDueByMileageOrDate() {
        val now = 10_000L
        val record = MaintenanceEntity(
            vehicleId = 1,
            serviceType = "Oil",
            performedAt = 1,
            mileage = 10_000,
            cost = 20.0,
            nextDueMileage = 15_000,
            nextDueAt = 20_000
        )
        assertFalse(ReminderEvaluator.isMaintenanceDue(record, mileage = 14_999, now = 19_999))
        assertTrue(ReminderEvaluator.isMaintenanceDue(record, mileage = 15_000, now = 19_999))
        assertTrue(ReminderEvaluator.isMaintenanceDue(record, mileage = 14_999, now = 20_000))
    }

    @Test
    fun documentIsFlaggedInsideReminderHorizon() {
        val now = 1_000L
        val horizon = TimeUnit.DAYS.toMillis(7)
        val document = DocumentEntity(
            vehicleId = 1,
            title = "Insurance",
            category = "Policy",
            expiryAt = now + horizon,
            createdAt = now
        )
        assertTrue(ReminderEvaluator.isDocumentDueSoon(document, now, horizon))
        assertFalse(ReminderEvaluator.isDocumentDueSoon(document.copy(expiryAt = now + horizon + 1), now, horizon))
    }
}
