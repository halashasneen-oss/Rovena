package com.rovena.garage.data.local.database

import androidx.room.migration.Migration

/**
 * Central registry of Room migrations. Rovena ships with schema version 1, so
 * this list starts empty - add a `Migration(from, to)` here for every future
 * schema change and keep the corresponding exported schema JSON (under
 * app/schemas/) checked in, so Room can validate migrations in tests.
 *
 * Never use `fallbackToDestructiveMigration()` in production: a missing
 * migration must fail loudly rather than silently delete a user's vehicle
 * history.
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
