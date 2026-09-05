package com.rovena.garage.notifications

import com.rovena.garage.data.FuelAnalytics
import com.rovena.garage.data.VehicleHealthEngine
import com.rovena.garage.data.local.DocumentEntity
import com.rovena.garage.data.local.FuelEntryEntity
import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import kotlin.math.abs

enum class SmartReminderKind {
    MAINTENANCE_OVERDUE,
    MAINTENANCE_SOON,
    DOCUMENT_EXPIRED,
    DOCUMENT_SOON,
    FUEL_EFFICIENCY_DROP
}

data class SmartReminder(
    val key: String,
    val priority: Int,
    val kind: SmartReminderKind,
    val vehicleName: String,
    val itemLabel: String = "",
    val percent: Int = 0
)

object SmartReminderEngine {
    fun evaluateVehicle(
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceEntity>,
        documents: List<DocumentEntity>,
        fuel: List<FuelEntryEntity>,
        now: Long = System.currentTimeMillis()
    ): List<SmartReminder> {
        val name = vehicle.nickname.ifBlank { "${vehicle.make} ${vehicle.model}" }
        val reminders = mutableListOf<SmartReminder>()

        maintenance.forEach { record ->
            when {
                VehicleHealthEngine.isMaintenanceOverdue(record, vehicle.mileage, now) -> {
                    reminders += SmartReminder(
                        key = "maintenance_overdue:${vehicle.id}:${record.id}",
                        priority = 100,
                        kind = SmartReminderKind.MAINTENANCE_OVERDUE,
                        vehicleName = name,
                        itemLabel = record.serviceType
                    )
                }
                VehicleHealthEngine.isMaintenanceDueSoon(
                    record = record,
                    mileage = vehicle.mileage,
                    now = now,
                    distanceWindowKm = 500,
                    timeWindowMillis = java.util.concurrent.TimeUnit.DAYS.toMillis(7)
                ) -> {
                    reminders += SmartReminder(
                        key = "maintenance_soon:${vehicle.id}:${record.id}",
                        priority = 80,
                        kind = SmartReminderKind.MAINTENANCE_SOON,
                        vehicleName = name,
                        itemLabel = record.serviceType
                    )
                }
            }
        }

        documents.forEach { document ->
            when {
                VehicleHealthEngine.isDocumentExpired(document, now) -> {
                    reminders += SmartReminder(
                        key = "document_expired:${vehicle.id}:${document.id}",
                        priority = 95,
                        kind = SmartReminderKind.DOCUMENT_EXPIRED,
                        vehicleName = name,
                        itemLabel = document.title
                    )
                }
                VehicleHealthEngine.isDocumentExpiringSoon(
                    document = document,
                    now = now,
                    horizonMillis = java.util.concurrent.TimeUnit.DAYS.toMillis(7)
                ) -> {
                    reminders += SmartReminder(
                        key = "document_soon:${vehicle.id}:${document.id}",
                        priority = 75,
                        kind = SmartReminderKind.DOCUMENT_SOON,
                        vehicleName = name,
                        itemLabel = document.title
                    )
                }
            }
        }

        val fuelInsights = FuelAnalytics.analyze(fuel)
        if (VehicleHealthEngine.isMeaningfulFuelDrop(fuelInsights)) {
            reminders += SmartReminder(
                key = "fuel_efficiency_drop:${vehicle.id}",
                priority = 55,
                kind = SmartReminderKind.FUEL_EFFICIENCY_DROP,
                vehicleName = name,
                percent = abs(fuelInsights.trendPercent ?: 0.0).toInt().coerceAtLeast(1)
            )
        }

        return reminders.sortedWith(compareByDescending<SmartReminder> { it.priority }.thenBy { it.key })
    }
}
