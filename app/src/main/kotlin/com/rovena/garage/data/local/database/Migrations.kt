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

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
