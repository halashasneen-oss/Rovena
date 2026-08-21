package com.rovena.garage.data.local.database

import androidx.room.TypeConverter
import com.rovena.garage.domain.model.AppCurrency
import com.rovena.garage.domain.model.AppLanguage
import com.rovena.garage.domain.model.AppThemeMode
import com.rovena.garage.domain.model.BackupRecordType
import com.rovena.garage.domain.model.DistanceUnit
import com.rovena.garage.domain.model.DocumentType
import com.rovena.garage.domain.model.ExpenseCategory
import com.rovena.garage.domain.model.FuelEconomyUnit
import com.rovena.garage.domain.model.FuelType
import com.rovena.garage.domain.model.InspectionCategoryGroup
import com.rovena.garage.domain.model.InspectionItemKey
import com.rovena.garage.domain.model.InspectionItemStatus
import com.rovena.garage.domain.model.MaintenanceCategory
import com.rovena.garage.domain.model.PhotoLinkedType
import com.rovena.garage.domain.model.ReminderBasis
import com.rovena.garage.domain.model.TimelineEventType
import com.rovena.garage.domain.model.TransmissionType

/**
 * All Room type converters live in one place. Every custom enum is stored as
 * its `name` (a plain String column), which keeps the schema human-readable
 * when inspecting the .db file directly and is stable across reordering enum
 * constants (unlike storing ordinal Ints).
 */
class Converters {

    @TypeConverter fun fromFuelType(v: FuelType): String = v.name
    @TypeConverter fun toFuelType(v: String): FuelType = enumValueOf(v)

    @TypeConverter fun fromTransmissionType(v: TransmissionType): String = v.name
    @TypeConverter fun toTransmissionType(v: String): TransmissionType = enumValueOf(v)

    @TypeConverter fun fromMaintenanceCategory(v: MaintenanceCategory): String = v.name
    @TypeConverter fun toMaintenanceCategory(v: String): MaintenanceCategory = enumValueOf(v)

    @TypeConverter fun fromMaintenanceCategoryNullable(v: MaintenanceCategory?): String? = v?.name
    @TypeConverter fun toMaintenanceCategoryNullable(v: String?): MaintenanceCategory? = v?.let { enumValueOf<MaintenanceCategory>(it) }

    @TypeConverter fun fromExpenseCategory(v: ExpenseCategory): String = v.name
    @TypeConverter fun toExpenseCategory(v: String): ExpenseCategory = enumValueOf(v)

    @TypeConverter fun fromDocumentType(v: DocumentType): String = v.name
    @TypeConverter fun toDocumentType(v: String): DocumentType = enumValueOf(v)

    @TypeConverter fun fromInspectionCategoryGroup(v: InspectionCategoryGroup): String = v.name
    @TypeConverter fun toInspectionCategoryGroup(v: String): InspectionCategoryGroup = enumValueOf(v)

    @TypeConverter fun fromInspectionItemKey(v: InspectionItemKey): String = v.name
    @TypeConverter fun toInspectionItemKey(v: String): InspectionItemKey = enumValueOf(v)

    @TypeConverter fun fromInspectionItemStatus(v: InspectionItemStatus): String = v.name
    @TypeConverter fun toInspectionItemStatus(v: String): InspectionItemStatus = enumValueOf(v)

    @TypeConverter fun fromReminderBasis(v: ReminderBasis): String = v.name
    @TypeConverter fun toReminderBasis(v: String): ReminderBasis = enumValueOf(v)

    @TypeConverter fun fromTimelineEventType(v: TimelineEventType): String = v.name
    @TypeConverter fun toTimelineEventType(v: String): TimelineEventType = enumValueOf(v)

    @TypeConverter fun fromPhotoLinkedType(v: PhotoLinkedType): String = v.name
    @TypeConverter fun toPhotoLinkedType(v: String): PhotoLinkedType = enumValueOf(v)

    @TypeConverter fun fromAppThemeMode(v: AppThemeMode): String = v.name
    @TypeConverter fun toAppThemeMode(v: String): AppThemeMode = enumValueOf(v)

    @TypeConverter fun fromAppLanguage(v: AppLanguage): String = v.name
    @TypeConverter fun toAppLanguage(v: String): AppLanguage = enumValueOf(v)

    @TypeConverter fun fromDistanceUnit(v: DistanceUnit): String = v.name
    @TypeConverter fun toDistanceUnit(v: String): DistanceUnit = enumValueOf(v)

    @TypeConverter fun fromFuelEconomyUnit(v: FuelEconomyUnit): String = v.name
    @TypeConverter fun toFuelEconomyUnit(v: String): FuelEconomyUnit = enumValueOf(v)

    @TypeConverter fun fromAppCurrency(v: AppCurrency): String = v.name
    @TypeConverter fun toAppCurrency(v: String): AppCurrency = enumValueOf(v)

    @TypeConverter fun fromBackupRecordType(v: BackupRecordType): String = v.name
    @TypeConverter fun toBackupRecordType(v: String): BackupRecordType = enumValueOf(v)
}
