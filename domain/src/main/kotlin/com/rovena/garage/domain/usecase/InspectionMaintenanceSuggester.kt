package com.rovena.garage.domain.usecase

import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.MaintenanceCategory

/**
 * Maps an inspection item flagged PROBLEM onto a suggested [MaintenanceCategory],
 * so the app can offer - never silently create - a follow-up reminder (spec:
 * Inspection -> Maintenance task suggestion flow). Only mechanical items with
 * an unambiguous single maintenance category are mapped; `FLUIDS` is
 * deliberately excluded (it could mean coolant, brake fluid, or oil level -
 * guessing which would be exactly the kind of fabricated specificity this
 * app avoids), and every exterior/interior item is excluded since they
 * aren't a mechanical "service this" concern in the same sense.
 */
object InspectionMaintenanceSuggester {

    private val MAPPING: Map<InspectionItemKey, MaintenanceCategory> = mapOf(
        InspectionItemKey.BRAKES to MaintenanceCategory.BRAKE_PADS,
        InspectionItemKey.TIRES to MaintenanceCategory.TIRES,
        InspectionItemKey.BATTERY to MaintenanceCategory.BATTERY,
        InspectionItemKey.SUSPENSION to MaintenanceCategory.SUSPENSION,
        InspectionItemKey.STEERING to MaintenanceCategory.STEERING,
        InspectionItemKey.ENGINE to MaintenanceCategory.ENGINE,
        InspectionItemKey.TRANSMISSION to MaintenanceCategory.TRANSMISSION,
        InspectionItemKey.COOLING to MaintenanceCategory.COOLANT
    )

    fun suggestedCategory(key: InspectionItemKey): MaintenanceCategory? = MAPPING[key]
}
