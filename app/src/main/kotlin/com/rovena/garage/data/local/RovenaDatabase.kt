package com.rovena.garage.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VehicleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RovenaDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    companion object {
        fun create(context: Context): RovenaDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                RovenaDatabase::class.java,
                "rovena2.db"
            ).build()
    }
}
