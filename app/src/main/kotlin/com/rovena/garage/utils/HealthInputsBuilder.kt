package com.rovena.garage.utils

import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.data.repository.InspectionConditionScores
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.usecase.HealthScoreCalculator

/**
 * Single source of truth for turning a vehicle's raw maintenance/document/inspection
 * records into [HealthScoreCalculator.VehicleHealthInputs] - shared by every screen and
 * PDF generator that computes a Health Score, so overdue/service-currency logic (and
 * the engine/transmission service categories it keys off) never drifts out of sync
 * between call sites.
 */
object HealthInputsBuilder {

    private val ENGINE_SERVICE_CATEGORIES = setOf(MaintenanceCategory.ENGINE_OIL, MaintenanceCategory.OIL_FILTER, MaintenanceCategory.ENGINE)
    private val TRANSMISSION_SERVICE_CATEGORIES = setOf(MaintenanceCategory.TRANSMISSION_FLUID, MaintenanceCategory.TRANSMISSION)

    fun build(
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceRecordEntity>,
        documents: List<DocumentEntity>,
        conditionScores: InspectionConditionScores,
        nowMillis: Long = System.currentTimeMillis()
    ): HealthScoreCalculator.VehicleHealthInputs {
        val overdue = maintenance.count { record ->
            (record.nextDueMileageKm != null && record.nextDueMileageKm <= vehicle.currentMileageKm) ||
                (record.nextDueDateMillis != null && record.nextDueDateMillis <= nowMillis)
        }
        val tracked = maintenance.count { it.nextDueMileageKm != null || it.nextDueDateMillis != null }
        val lastMaintenanceDate = maintenance.maxOfOrNull { it.dateMillis }
        val daysSinceLastMaintenance = lastMaintenanceDate?.let { ((nowMillis - it) / 86_400_000L).toInt() }
        val hasExpiredDoc = documents.any { it.expiryDateMillis != null && it.expiryDateMillis < nowMillis }

        fun serviceUpToDate(categories: Set<MaintenanceCategory>): Boolean? {
            val relevant = maintenance.filter { it.category in categories && (it.nextDueMileageKm != null || it.nextDueDateMillis != null) }
            if (relevant.isEmpty()) return null
            return relevant.none {
                (it.nextDueMileageKm != null && it.nextDueMileageKm <= vehicle.currentMileageKm) ||
                    (it.nextDueDateMillis != null && it.nextDueDateMillis <= nowMillis)
            }
        }

        return HealthScoreCalculator.VehicleHealthInputs(
            daysSinceLastMaintenance = daysSinceLastMaintenance,
            overdueMaintenanceCount = if (tracked > 0) overdue else null,
            totalActiveMaintenanceItems = if (tracked > 0) tracked else null,
            brakesConditionScore = conditionScores.brakes,
            tiresConditionScore = conditionScores.tires,
            batteryConditionScore = conditionScores.battery,
            fluidsConditionScore = conditionScores.fluids,
            engineServiceUpToDate = serviceUpToDate(ENGINE_SERVICE_CATEGORIES),
            transmissionServiceUpToDate = serviceUpToDate(TRANSMISSION_SERVICE_CATEGORIES),
            hasExpiredDocument = if (documents.isNotEmpty()) hasExpiredDoc else null,
            hasAnyTrackedDocument = documents.isNotEmpty()
        )
    }

    fun calculate(
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceRecordEntity>,
        documents: List<DocumentEntity>,
        conditionScores: InspectionConditionScores,
        nowMillis: Long = System.currentTimeMillis()
    ): HealthScoreCalculator.Result =
        HealthScoreCalculator.fromVehicleInputs(build(vehicle, maintenance, documents, conditionScores, nowMillis))
}
