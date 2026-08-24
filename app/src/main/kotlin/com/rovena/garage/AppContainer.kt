package com.rovena.garage

import android.content.Context
import com.rovena.garage.data.local.database.RovenaDatabase
import com.rovena.garage.data.repository.BackupMetadataRepository
import com.rovena.garage.data.repository.DocumentRepository
import com.rovena.garage.data.repository.ExpenseRepository
import com.rovena.garage.data.repository.FuelRepository
import com.rovena.garage.data.repository.HealthScoreHistoryRepository
import com.rovena.garage.data.repository.InspectionRepository
import com.rovena.garage.data.repository.MaintenanceRepository
import com.rovena.garage.data.repository.PartRepository
import com.rovena.garage.data.repository.PhotoRepository
import com.rovena.garage.data.repository.ReminderRepository
import com.rovena.garage.data.repository.SettingsRepository
import com.rovena.garage.data.repository.TimelineRepository
import com.rovena.garage.data.repository.TimelineSyncer
import com.rovena.garage.data.repository.UserPreferences
import com.rovena.garage.data.repository.VehicleNoteRepository
import com.rovena.garage.data.repository.VehicleRepository

/**
 * Hand-rolled dependency container (spec asks for minimal, stable
 * dependencies - no Hilt/Dagger). Every repository is a cheap, stateless
 * wrapper around Room DAOs, so simple lazy singletons are enough; there's no
 * need for a full DI framework's graph validation or code generation here.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: RovenaDatabase by lazy { RovenaDatabase.getInstance(appContext) }

    private val timelineSyncer: TimelineSyncer by lazy { TimelineSyncer(database.timelineDao()) }

    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepository(database.vehicleDao(), timelineSyncer, database, documentRepository, expenseRepository, photoRepository)
    }

    val maintenanceRepository: MaintenanceRepository by lazy {
        MaintenanceRepository(database.maintenanceDao(), database.vehicleDao(), timelineSyncer)
    }

    val fuelRepository: FuelRepository by lazy {
        FuelRepository(database.fuelDao(), database.vehicleDao(), timelineSyncer)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(database.expenseDao(), timelineSyncer)
    }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepository(database.documentDao(), database.reminderDao(), timelineSyncer, database)
    }

    val inspectionRepository: InspectionRepository by lazy {
        InspectionRepository(database.inspectionDao(), database.inspectionItemDao(), timelineSyncer, database, photoRepository)
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao(), timelineSyncer)
    }

    val timelineRepository: TimelineRepository by lazy {
        TimelineRepository(database.timelineDao())
    }

    val photoRepository: PhotoRepository by lazy {
        PhotoRepository(database.vehiclePhotoDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(database.appSettingsDao())
    }

    val backupMetadataRepository: BackupMetadataRepository by lazy {
        BackupMetadataRepository(database.backupMetadataDao())
    }

    val vehicleNoteRepository: VehicleNoteRepository by lazy {
        VehicleNoteRepository(database.vehicleNoteDao())
    }

    val partRepository: PartRepository by lazy {
        PartRepository(database.partDao(), database.reminderDao(), database)
    }

    val healthScoreHistoryRepository: HealthScoreHistoryRepository by lazy {
        HealthScoreHistoryRepository(database.healthScoreSnapshotDao())
    }

    val userPreferences: UserPreferences by lazy { UserPreferences(appContext) }
}
