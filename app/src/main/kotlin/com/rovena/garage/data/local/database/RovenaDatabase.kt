package com.rovena.garage.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rovena.garage.data.local.dao.AppSettingsDao
import com.rovena.garage.data.local.dao.BackupMetadataDao
import com.rovena.garage.data.local.dao.DocumentDao
import com.rovena.garage.data.local.dao.ExpenseDao
import com.rovena.garage.data.local.dao.FuelDao
import com.rovena.garage.data.local.dao.InspectionDao
import com.rovena.garage.data.local.dao.InspectionItemDao
import com.rovena.garage.data.local.dao.MaintenanceDao
import com.rovena.garage.data.local.dao.PartDao
import com.rovena.garage.data.local.dao.ReminderDao
import com.rovena.garage.data.local.dao.TimelineDao
import com.rovena.garage.data.local.dao.VehicleDao
import com.rovena.garage.data.local.dao.VehicleNoteDao
import com.rovena.garage.data.local.dao.VehiclePhotoDao
import com.rovena.garage.data.local.entities.AppSettingsEntity
import com.rovena.garage.data.local.entities.BackupMetadataEntity
import com.rovena.garage.data.local.entities.DocumentEntity
import com.rovena.garage.data.local.entities.ExpenseEntity
import com.rovena.garage.data.local.entities.FuelRecordEntity
import com.rovena.garage.data.local.entities.InspectionEntity
import com.rovena.garage.data.local.entities.InspectionItemEntity
import com.rovena.garage.data.local.entities.MaintenanceRecordEntity
import com.rovena.garage.data.local.entities.PartEntity
import com.rovena.garage.data.local.entities.ReminderEntity
import com.rovena.garage.data.local.entities.TimelineEventEntity
import com.rovena.garage.data.local.entities.VehicleEntity
import com.rovena.garage.data.local.entities.VehicleNoteEntity
import com.rovena.garage.data.local.entities.VehiclePhotoEntity

@Database(
    entities = [
        VehicleEntity::class,
        MaintenanceRecordEntity::class,
        FuelRecordEntity::class,
        ExpenseEntity::class,
        DocumentEntity::class,
        InspectionEntity::class,
        InspectionItemEntity::class,
        ReminderEntity::class,
        TimelineEventEntity::class,
        VehiclePhotoEntity::class,
        AppSettingsEntity::class,
        BackupMetadataEntity::class,
        VehicleNoteEntity::class,
        PartEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RovenaDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun fuelDao(): FuelDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun documentDao(): DocumentDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun inspectionItemDao(): InspectionItemDao
    abstract fun reminderDao(): ReminderDao
    abstract fun timelineDao(): TimelineDao
    abstract fun vehiclePhotoDao(): VehiclePhotoDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun backupMetadataDao(): BackupMetadataDao
    abstract fun vehicleNoteDao(): VehicleNoteDao
    abstract fun partDao(): PartDao

    companion object {
        const val DATABASE_NAME = "rovena.db"

        @Volatile
        private var instance: RovenaDatabase? = null

        fun getInstance(context: Context): RovenaDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        /** Closes and drops the cached instance so the underlying .db file can be safely replaced (used by Restore). */
        @Synchronized
        fun closeInstance() {
            instance?.close()
            instance = null
        }

        private fun build(context: Context): RovenaDatabase =
            Room.databaseBuilder(context.applicationContext, RovenaDatabase::class.java, DATABASE_NAME)
                // Deliberately no destructive fallback: a bad migration must fail loudly
                // rather than silently wipe a user's vehicle history.
                .addMigrations(*Migrations.ALL)
                .build()
    }
}
