package com.rovena.garage.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VehicleEntity::class,
        MaintenanceEntity::class,
        FuelEntryEntity::class,
        ExpenseEntity::class,
        DocumentEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class RovenaDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun recordDao(): RecordDao

    companion object {
        private const val DATABASE_NAME = "rovena_auto_v1.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `maintenance_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `serviceType` TEXT NOT NULL,
                        `performedAt` INTEGER NOT NULL,
                        `mileage` INTEGER NOT NULL,
                        `cost` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        `nextDueMileage` INTEGER,
                        `nextDueAt` INTEGER,
                        FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_vehicleId` ON `maintenance_records` (`vehicleId`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `fuel_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `filledAt` INTEGER NOT NULL,
                        `mileage` INTEGER NOT NULL,
                        `liters` REAL NOT NULL,
                        `totalCost` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_entries_vehicleId` ON `fuel_entries` (`vehicleId`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `expense_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `spentAt` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_entries_vehicleId` ON `expense_entries` (`vehicleId`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `vehicle_documents` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `vehicleId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `expiryAt` INTEGER,
                        `notes` TEXT NOT NULL,
                        `fileUri` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_documents_vehicleId` ON `vehicle_documents` (`vehicleId`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `maintenance_records` ADD COLUMN `attachmentUri` TEXT")
                db.execSQL("ALTER TABLE `fuel_entries` ADD COLUMN `attachmentUri` TEXT")
                db.execSQL("ALTER TABLE `expense_entries` ADD COLUMN `attachmentUri` TEXT")
            }
        }

        fun create(context: Context): RovenaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                RovenaDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
    }
}
