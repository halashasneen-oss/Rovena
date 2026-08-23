package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.MaintenanceCategory

/**
 * Built-in bundles of related maintenance categories (spec: Service Plans),
 * e.g. a routine "Minor Service" is really an oil change + oil filter + air
 * filter all done together. Applying one lets the user log every item in
 * the bundle from a single small form (shared date/mileage/workshop) instead
 * of repeating the full maintenance form N times for one visit.
 *
 * Deliberately a small fixed catalog rather than user-defined custom plans -
 * that would need its own CRUD screen and storage for a feature whose actual
 * value is mostly in *not* re-typing the same routine bundle by hand.
 */
object ServicePlanCatalog {

    data class ServicePlan(val id: String, val categories: List<MaintenanceCategory>)

    val PLANS: List<ServicePlan> = listOf(
        ServicePlan("minor_service", listOf(MaintenanceCategory.ENGINE_OIL, MaintenanceCategory.OIL_FILTER, MaintenanceCategory.AIR_FILTER)),
        ServicePlan(
            "major_service",
            listOf(
                MaintenanceCategory.ENGINE_OIL, MaintenanceCategory.OIL_FILTER, MaintenanceCategory.AIR_FILTER,
                MaintenanceCategory.CABIN_FILTER, MaintenanceCategory.SPARK_PLUGS
            )
        ),
        ServicePlan("brake_service", listOf(MaintenanceCategory.BRAKE_PADS, MaintenanceCategory.BRAKE_DISCS))
    )
}
