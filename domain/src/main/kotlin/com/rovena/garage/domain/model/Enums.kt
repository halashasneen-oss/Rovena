package com.rovena.garage.domain.model

/**
 * Shared enums used across the domain layer. These are pure Kotlin (no Android
 * dependency) so they can be unit tested on a plain JVM and shared between the
 * Room entities (data layer) and the UI (presentation layer).
 */

enum class FuelType {
    PETROL, DIESEL, HYBRID, PLUG_IN_HYBRID, ELECTRIC, LPG, OTHER
}

enum class TransmissionType {
    MANUAL, AUTOMATIC, CVT, DCT, OTHER
}

enum class MaintenanceCategory {
    ENGINE_OIL, OIL_FILTER, AIR_FILTER, CABIN_FILTER, FUEL_FILTER, SPARK_PLUGS,
    BRAKE_PADS, BRAKE_DISCS, TIRES, BATTERY, COOLANT, TRANSMISSION_FLUID,
    TIMING_BELT, SERPENTINE_BELT, SUSPENSION, STEERING, AC, ELECTRICAL,
    ENGINE, TRANSMISSION, OTHER
}

enum class ExpenseCategory {
    FUEL, MAINTENANCE, REPAIRS, INSURANCE, REGISTRATION, TIRES, CAR_WASH,
    PARKING, FINES, PARTS, ACCESSORIES, INSPECTION, OTHER
}

enum class DocumentType {
    REGISTRATION, INSURANCE, INSPECTION, PURCHASE_CONTRACT, MAINTENANCE_INVOICE,
    RECEIPT, OTHER
}

enum class InspectionCategoryGroup { EXTERIOR, INTERIOR, MECHANICAL }

enum class InspectionItemStatus { GOOD, ATTENTION, PROBLEM, UNKNOWN }

enum class ReminderBasis { MILEAGE, DATE, BOTH }

enum class TimelineEventType { FUEL, MAINTENANCE, EXPENSE, DOCUMENT, INSPECTION, REMINDER, VEHICLE_UPDATE }

enum class HealthStatus { EXCELLENT, GOOD, FAIR, ATTENTION_NEEDED, CRITICAL, NOT_ENOUGH_DATA }

enum class DueStatus { UPCOMING, DUE_SOON, DUE, OVERDUE }

enum class HealthCategory {
    MAINTENANCE_RECENCY,
    OVERDUE_MAINTENANCE,
    BRAKES,
    TIRES,
    BATTERY,
    FLUIDS,
    ENGINE_SERVICE,
    TRANSMISSION_SERVICE,
    DOCUMENTATION
}

enum class AppCurrency(val code: String) {
    JOD("JOD"), USD("USD"), EUR("EUR"), GBP("GBP"),
    SAR("SAR"), AED("AED"), EGP("EGP"), TRY("TRY"), CUSTOM("CUSTOM")
}

enum class DistanceUnit { KM, MILES }

enum class FuelEconomyUnit { L_100KM, MPG }

enum class AppLanguage(val tag: String) { ENGLISH("en"), ARABIC("ar"), FRENCH("fr"), SPANISH("es") }

enum class AppThemeMode { LIGHT, DARK, SYSTEM }
