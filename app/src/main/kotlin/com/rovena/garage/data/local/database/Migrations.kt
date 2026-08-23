package com.rovena.garage.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Central registry of Room migrations. Every future schema change gets a new
 * `Migration(from, to)` here, added to [ALL], and the corresponding exported
 * schema JSON (under app/schemas/) kept checked in so Room can validate
 * migrations in tests.
 *
 * Never use `fallbackToDestructiveMigration()` in production: a missing
 * migration must fail loudly rather than silently delete a user's vehicle
 * history.
 */
object Migrations {

    /** Adds PIN failed-attempt lockout tracking and stored PIN length (spec #10) to app_settings. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_settings ADD COLUMN pinLength INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN pinFailedAttempts INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN pinLockoutUntilMillis INTEGER DEFAULT NULL")
        }
    }

    /** Adds the vehicle_notes table (spec: Vehicle Notes - freeform, non-diagnostic notes). */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vehicle_notes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `vehicleId` INTEGER NOT NULL,
                    `text` TEXT NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    `updatedAtMillis` INTEGER NOT NULL,
                    FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_notes_vehicleId` ON `vehicle_notes` (`vehicleId`)")
        }
    }

    /** Adds the parts table (spec: Parts History + Warranty Tracking). */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `parts` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `vehicleId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `installedDateMillis` INTEGER NOT NULL,
                    `installedMileageKm` INTEGER DEFAULT NULL,
                    `warrantyExpiryDateMillis` INTEGER DEFAULT NULL,
                    `warrantyExpiryMileageKm` INTEGER DEFAULT NULL,
                    `notes` TEXT DEFAULT NULL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_parts_vehicleId` ON `parts` (`vehicleId`)")
        }
    }

    /** Adds staged-notification tracking to reminders (spec: 30/14/7/3/1-day reminder schedule). */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reminders ADD COLUMN lastNotifiedStageDays INTEGER DEFAULT NULL")
        }
    }

    /** Adds reminder category (spec: per-category notification toggles) and the tiered-severity toggle columns on app_settings. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reminders ADD COLUMN category TEXT NOT NULL DEFAULT 'GENERAL'")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN notifyCriticalEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN notifyImportantEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN notifyUpcomingEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN notifyDocumentCategoryEnabled INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE app_settings ADD COLUMN notifyGeneralCategoryEnabled INTEGER NOT NULL DEFAULT 1")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
