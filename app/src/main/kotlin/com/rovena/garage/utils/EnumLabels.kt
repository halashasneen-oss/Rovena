package com.rovena.garage.utils

import androidx.annotation.StringRes
import com.rovena.garage.R
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.DocumentType
import com.rovena.garage.domain.model.DueStatus
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.HealthCategory
import com.rovena.garage.domain.model.HealthStatus
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.InspectionItemStatus
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.domain.model.TransmissionType
import com.rovena.garage.domain.usecase.InputValidator

/** Central lookup from every fixed-vocabulary enum to its localized string resource. */
object EnumLabels {

    @StringRes
    fun of(type: FuelType): Int = when (type) {
        FuelType.PETROL -> R.string.fuel_type_petrol
        FuelType.DIESEL -> R.string.fuel_type_diesel
        FuelType.HYBRID -> R.string.fuel_type_hybrid
        FuelType.PLUG_IN_HYBRID -> R.string.fuel_type_plugin_hybrid
        FuelType.ELECTRIC -> R.string.fuel_type_electric
        FuelType.LPG -> R.string.fuel_type_lpg
        FuelType.OTHER -> R.string.fuel_type_other
    }

    @StringRes
    fun of(type: TransmissionType): Int = when (type) {
        TransmissionType.MANUAL -> R.string.transmission_manual
        TransmissionType.AUTOMATIC -> R.string.transmission_automatic
        TransmissionType.CVT -> R.string.transmission_cvt
        TransmissionType.DCT -> R.string.transmission_dct
        TransmissionType.OTHER -> R.string.transmission_other
    }

    @StringRes
    fun of(category: MaintenanceCategory): Int = when (category) {
        MaintenanceCategory.ENGINE_OIL -> R.string.maintenance_category_engine_oil
        MaintenanceCategory.OIL_FILTER -> R.string.maintenance_category_oil_filter
        MaintenanceCategory.AIR_FILTER -> R.string.maintenance_category_air_filter
        MaintenanceCategory.CABIN_FILTER -> R.string.maintenance_category_cabin_filter
        MaintenanceCategory.FUEL_FILTER -> R.string.maintenance_category_fuel_filter
        MaintenanceCategory.SPARK_PLUGS -> R.string.maintenance_category_spark_plugs
        MaintenanceCategory.BRAKE_PADS -> R.string.maintenance_category_brake_pads
        MaintenanceCategory.BRAKE_DISCS -> R.string.maintenance_category_brake_discs
        MaintenanceCategory.TIRES -> R.string.maintenance_category_tires
        MaintenanceCategory.BATTERY -> R.string.maintenance_category_battery
        MaintenanceCategory.COOLANT -> R.string.maintenance_category_coolant
        MaintenanceCategory.TRANSMISSION_FLUID -> R.string.maintenance_category_transmission_fluid
        MaintenanceCategory.TIMING_BELT -> R.string.maintenance_category_timing_belt
        MaintenanceCategory.SERPENTINE_BELT -> R.string.maintenance_category_serpentine_belt
        MaintenanceCategory.SUSPENSION -> R.string.maintenance_category_suspension
        MaintenanceCategory.STEERING -> R.string.maintenance_category_steering
        MaintenanceCategory.AC -> R.string.maintenance_category_ac
        MaintenanceCategory.ELECTRICAL -> R.string.maintenance_category_electrical
        MaintenanceCategory.ENGINE -> R.string.maintenance_category_engine
        MaintenanceCategory.TRANSMISSION -> R.string.maintenance_category_transmission
        MaintenanceCategory.OTHER -> R.string.maintenance_category_other
    }

    @StringRes
    fun of(category: ExpenseCategory): Int = when (category) {
        ExpenseCategory.FUEL -> R.string.expense_category_fuel
        ExpenseCategory.MAINTENANCE -> R.string.expense_category_maintenance
        ExpenseCategory.REPAIRS -> R.string.expense_category_repairs
        ExpenseCategory.INSURANCE -> R.string.expense_category_insurance
        ExpenseCategory.REGISTRATION -> R.string.expense_category_registration
        ExpenseCategory.TIRES -> R.string.expense_category_tires
        ExpenseCategory.CAR_WASH -> R.string.expense_category_car_wash
        ExpenseCategory.PARKING -> R.string.expense_category_parking
        ExpenseCategory.FINES -> R.string.expense_category_fines
        ExpenseCategory.PARTS -> R.string.expense_category_parts
        ExpenseCategory.ACCESSORIES -> R.string.expense_category_accessories
        ExpenseCategory.INSPECTION -> R.string.expense_category_inspection
        ExpenseCategory.OTHER -> R.string.expense_category_other
    }

    @StringRes
    fun of(type: DocumentType): Int = when (type) {
        DocumentType.REGISTRATION -> R.string.document_type_registration
        DocumentType.INSURANCE -> R.string.document_type_insurance
        DocumentType.INSPECTION -> R.string.document_type_inspection
        DocumentType.PURCHASE_CONTRACT -> R.string.document_type_purchase_contract
        DocumentType.MAINTENANCE_INVOICE -> R.string.document_type_maintenance_invoice
        DocumentType.RECEIPT -> R.string.document_type_receipt
        DocumentType.OTHER -> R.string.document_type_other
    }

    @StringRes
    fun of(key: InspectionItemKey): Int = when (key) {
        InspectionItemKey.PAINT -> R.string.inspection_item_paint
        InspectionItemKey.BODY -> R.string.inspection_item_body
        InspectionItemKey.DOORS -> R.string.inspection_item_doors
        InspectionItemKey.EXT_WINDOWS -> R.string.inspection_item_windows
        InspectionItemKey.LIGHTS -> R.string.inspection_item_lights
        InspectionItemKey.MIRRORS -> R.string.inspection_item_mirrors
        InspectionItemKey.TIRES -> R.string.inspection_item_tires
        InspectionItemKey.WHEELS -> R.string.inspection_item_wheels
        InspectionItemKey.CHASSIS -> R.string.inspection_item_chassis
        InspectionItemKey.SEATS -> R.string.inspection_item_seats
        InspectionItemKey.DASHBOARD -> R.string.inspection_item_dashboard
        InspectionItemKey.INT_AC -> R.string.inspection_item_ac
        InspectionItemKey.INT_WINDOWS -> R.string.inspection_item_windows
        InspectionItemKey.AUDIO -> R.string.inspection_item_audio
        InspectionItemKey.ELECTRONICS -> R.string.inspection_item_electronics
        InspectionItemKey.INTERIOR_CONDITION -> R.string.inspection_item_interior_condition
        InspectionItemKey.ENGINE -> R.string.inspection_item_engine
        InspectionItemKey.TRANSMISSION -> R.string.inspection_item_transmission
        InspectionItemKey.BRAKES -> R.string.inspection_item_brakes
        InspectionItemKey.SUSPENSION -> R.string.inspection_item_suspension
        InspectionItemKey.STEERING -> R.string.inspection_item_steering
        InspectionItemKey.COOLING -> R.string.inspection_item_cooling
        InspectionItemKey.FLUIDS -> R.string.inspection_item_fluids
        InspectionItemKey.BATTERY -> R.string.inspection_item_battery
    }

    @StringRes
    fun of(status: InspectionItemStatus): Int = when (status) {
        InspectionItemStatus.GOOD -> R.string.inspection_status_good
        InspectionItemStatus.ATTENTION -> R.string.inspection_status_attention
        InspectionItemStatus.PROBLEM -> R.string.inspection_status_problem
        InspectionItemStatus.UNKNOWN -> R.string.inspection_status_unknown
    }

    @StringRes
    fun of(status: HealthStatus): Int = when (status) {
        HealthStatus.EXCELLENT -> R.string.health_status_excellent
        HealthStatus.GOOD -> R.string.health_status_good
        HealthStatus.FAIR -> R.string.health_status_fair
        HealthStatus.ATTENTION_NEEDED -> R.string.health_status_attention_needed
        HealthStatus.CRITICAL -> R.string.health_status_critical
        HealthStatus.NOT_ENOUGH_DATA -> R.string.health_status_not_enough_data
    }

    @StringRes
    fun of(category: HealthCategory): Int = when (category) {
        HealthCategory.MAINTENANCE_RECENCY -> R.string.health_category_maintenance_recency
        HealthCategory.OVERDUE_MAINTENANCE -> R.string.health_category_overdue_maintenance
        HealthCategory.BRAKES -> R.string.health_category_brakes
        HealthCategory.TIRES -> R.string.health_category_tires
        HealthCategory.BATTERY -> R.string.health_category_battery
        HealthCategory.FLUIDS -> R.string.health_category_fluids
        HealthCategory.ENGINE_SERVICE -> R.string.health_category_engine_service
        HealthCategory.TRANSMISSION_SERVICE -> R.string.health_category_transmission_service
        HealthCategory.DOCUMENTATION -> R.string.health_category_documentation
    }

    @StringRes
    fun of(status: DueStatus): Int = when (status) {
        DueStatus.UPCOMING -> R.string.due_status_upcoming
        DueStatus.DUE_SOON -> R.string.due_status_due_soon
        DueStatus.DUE -> R.string.due_status_due
        DueStatus.OVERDUE -> R.string.due_status_overdue
    }

    @StringRes
    fun of(basis: ReminderBasis): Int = when (basis) {
        ReminderBasis.MILEAGE -> R.string.reminder_basis_mileage
        ReminderBasis.DATE -> R.string.reminder_basis_date
        ReminderBasis.BOTH -> R.string.reminder_basis_both
    }

    fun currencySymbolOrCode(currency: AppCurrency, customCode: String?): String = when (currency) {
        AppCurrency.CUSTOM -> customCode?.takeIf { it.isNotBlank() } ?: "?"
        else -> currency.code
    }

    @StringRes
    fun of(error: InputValidator.Error): Int = when (error) {
        InputValidator.Error.REQUIRED -> R.string.error_required
        InputValidator.Error.NEGATIVE_MILEAGE -> R.string.error_invalid_mileage
        InputValidator.Error.NEGATIVE_COST -> R.string.error_negative_cost
        InputValidator.Error.NOT_POSITIVE_QUANTITY -> R.string.error_not_positive_quantity
        InputValidator.Error.NOT_POSITIVE_ENGINE_SIZE -> R.string.error_not_positive_engine_size
        InputValidator.Error.IMPLAUSIBLE_DATE -> R.string.error_implausible_date
        InputValidator.Error.INVALID_YEAR -> R.string.error_invalid_year
        InputValidator.Error.INVALID_VIN -> R.string.error_invalid_vin
    }

    /**
     * The 3-letter code to permanently stamp onto a new financial record (spec:
     * currency architecture - every record stores its currency at creation
     * time). Unlike [currencySymbolOrCode], this never returns a placeholder
     * like "?": a blank custom code falls back to JOD rather than persisting
     * something unusable.
     */
    fun effectiveCurrencyCode(currency: AppCurrency, customCode: String?): String = when (currency) {
        AppCurrency.CUSTOM -> customCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: AppCurrency.JOD.code
        else -> currency.code
    }
}
