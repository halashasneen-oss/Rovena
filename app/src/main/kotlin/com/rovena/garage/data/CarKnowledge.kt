package com.rovena.garage.data

import com.rovena.garage.data.local.MaintenanceEntity
import com.rovena.garage.data.local.VehicleEntity
import java.util.Locale

/** Data-backed maintenance guidance. These are generic intervals, not manufacturer schedules. */
enum class MaintenanceTask {
    ENGINE_OIL,
    AIR_FILTER,
    CABIN_FILTER,
    BRAKE_INSPECTION,
    BRAKE_FLUID,
    COOLANT,
    TRANSMISSION_FLUID,
    SPARK_PLUGS,
    TIRE_ROTATION
}

enum class MaintenancePlanStatus { OVERDUE, DUE_SOON, ON_TRACK }

data class MaintenancePlanItem(
    val task: MaintenanceTask,
    val intervalKm: Long,
    val intervalMonths: Int,
    val nextDueMileage: Long,
    val remainingKm: Long,
    val status: MaintenancePlanStatus,
    val basedOnRecordedService: Boolean
)

object MaintenancePlanEngine {
    private data class Rule(
        val task: MaintenanceTask,
        val intervalKm: Long,
        val intervalMonths: Int,
        val keywords: Set<String>
    )

    private val commonRules = listOf(
        Rule(MaintenanceTask.TIRE_ROTATION, 10_000, 12, setOf("tire", "tyre", "rotation", "اطار", "إطار", "كفر")),
        Rule(MaintenanceTask.CABIN_FILTER, 15_000, 12, setOf("cabin filter", "ac filter", "pollen", "فلتر مكيف", "فلتر المكيف")),
        Rule(MaintenanceTask.BRAKE_INSPECTION, 10_000, 12, setOf("brake", "pads", "فرامل", "بريك")),
        Rule(MaintenanceTask.BRAKE_FLUID, 40_000, 24, setOf("brake fluid", "زيت فرامل", "سائل فرامل")),
        Rule(MaintenanceTask.COOLANT, 40_000, 24, setOf("coolant", "radiator", "antifreeze", "مبرد", "رديتر", "راديتر"))
    )

    private val combustionRules = listOf(
        Rule(MaintenanceTask.ENGINE_OIL, 10_000, 12, setOf("oil", "engine oil", "زيت", "زيت محرك")),
        Rule(MaintenanceTask.AIR_FILTER, 15_000, 12, setOf("air filter", "فلتر هوا", "فلتر هواء")),
        Rule(MaintenanceTask.TRANSMISSION_FLUID, 60_000, 48, setOf("transmission", "gear oil", "atf", "زيت جير", "زيت قير"))
    )

    private val sparkRule = Rule(
        MaintenanceTask.SPARK_PLUGS,
        40_000,
        36,
        setOf("spark", "plug", "spark plugs", "بواجي", "بوجي")
    )

    fun suggestions(
        vehicle: VehicleEntity,
        maintenance: List<MaintenanceEntity>
    ): List<MaintenancePlanItem> {
        val fuel = vehicle.fuelType.uppercase(Locale.ROOT)
        val rules = buildList {
            addAll(commonRules)
            if (fuel != "ELECTRIC") addAll(combustionRules)
            if (fuel in setOf("GASOLINE", "HYBRID", "LPG")) add(sparkRule)
        }

        return rules.map { rule -> buildSuggestion(rule, vehicle.mileage, maintenance) }
            .sortedWith(compareBy<MaintenancePlanItem> { statusRank(it.status) }.thenBy { it.remainingKm })
    }

    private fun buildSuggestion(
        rule: Rule,
        currentMileage: Long,
        maintenance: List<MaintenanceEntity>
    ): MaintenancePlanItem {
        val last = maintenance
            .filter { matches(it.serviceType, rule.keywords) }
            .maxByOrNull { it.performedAt }

        val nextDue = if (last != null) {
            last.mileage + rule.intervalKm
        } else {
            genericMilestone(currentMileage, rule.intervalKm)
        }
        val remaining = nextDue - currentMileage
        val dueSoonThreshold = maxOf(500L, (rule.intervalKm * 10) / 100)
        val status = when {
            remaining < 0 -> MaintenancePlanStatus.OVERDUE
            remaining <= dueSoonThreshold -> MaintenancePlanStatus.DUE_SOON
            else -> MaintenancePlanStatus.ON_TRACK
        }

        return MaintenancePlanItem(
            task = rule.task,
            intervalKm = rule.intervalKm,
            intervalMonths = rule.intervalMonths,
            nextDueMileage = nextDue,
            remainingKm = remaining,
            status = status,
            basedOnRecordedService = last != null
        )
    }

    private fun genericMilestone(currentMileage: Long, intervalKm: Long): Long {
        if (currentMileage <= 0L) return intervalKm
        val remainder = currentMileage % intervalKm
        return if (remainder == 0L) currentMileage else currentMileage + (intervalKm - remainder)
    }

    private fun matches(value: String, keywords: Set<String>): Boolean {
        val normalized = value.lowercase(Locale.ROOT)
        return keywords.any { normalized.contains(it.lowercase(Locale.ROOT)) }
    }

    private fun statusRank(status: MaintenancePlanStatus): Int = when (status) {
        MaintenancePlanStatus.OVERDUE -> 0
        MaintenancePlanStatus.DUE_SOON -> 1
        MaintenancePlanStatus.ON_TRACK -> 2
    }
}

enum class WarningSeverity { INFO, CAUTION, STOP_SAFELY }

enum class WarningLightType {
    OIL_PRESSURE,
    COOLANT_TEMPERATURE,
    BRAKE_SYSTEM,
    BATTERY_CHARGING,
    CHECK_ENGINE,
    ABS
}

data class WarningLightGuide(val type: WarningLightType, val severity: WarningSeverity)

object WarningLightCatalog {
    val all = listOf(
        WarningLightGuide(WarningLightType.OIL_PRESSURE, WarningSeverity.STOP_SAFELY),
        WarningLightGuide(WarningLightType.COOLANT_TEMPERATURE, WarningSeverity.STOP_SAFELY),
        WarningLightGuide(WarningLightType.BRAKE_SYSTEM, WarningSeverity.STOP_SAFELY),
        WarningLightGuide(WarningLightType.BATTERY_CHARGING, WarningSeverity.CAUTION),
        WarningLightGuide(WarningLightType.CHECK_ENGINE, WarningSeverity.CAUTION),
        WarningLightGuide(WarningLightType.ABS, WarningSeverity.CAUTION)
    )
}

enum class SymptomUrgency { NORMAL, SOON, URGENT }

enum class CarSymptom {
    OVERHEATING,
    SHAKING_AT_IDLE,
    BRAKE_NOISE,
    STEERING_NOISE,
    HIGH_FUEL_USE
}

data class SymptomGuide(val symptom: CarSymptom, val urgency: SymptomUrgency)

object SymptomCatalog {
    val all = listOf(
        SymptomGuide(CarSymptom.OVERHEATING, SymptomUrgency.URGENT),
        SymptomGuide(CarSymptom.SHAKING_AT_IDLE, SymptomUrgency.SOON),
        SymptomGuide(CarSymptom.BRAKE_NOISE, SymptomUrgency.SOON),
        SymptomGuide(CarSymptom.STEERING_NOISE, SymptomUrgency.SOON),
        SymptomGuide(CarSymptom.HIGH_FUEL_USE, SymptomUrgency.NORMAL)
    )
}
