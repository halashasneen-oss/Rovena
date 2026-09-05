package com.rovena.garage.data

import androidx.room.withTransaction
import com.rovena.garage.data.local.RovenaDatabase
import com.rovena.garage.data.local.VehicleEntity

class VehicleRepository(
    private val database: RovenaDatabase
) {
    private val dao = database.vehicleDao()

    val vehicles = dao.observeAll()

    suspend fun addVehicle(draft: VehicleDraft): Long {
        require(VehicleValidator.validate(draft) == null) { "Invalid vehicle draft" }
        val now = System.currentTimeMillis()

        return database.withTransaction {
            val isFirstVehicle = dao.count() == 0
            dao.insert(
                VehicleEntity(
                    make = draft.make.trim(),
                    model = draft.model.trim(),
                    year = draft.year,
                    mileage = draft.mileage,
                    fuelType = draft.fuelType.name,
                    nickname = draft.nickname.trim(),
                    plateNumber = draft.plateNumber.trim(),
                    vin = draft.vin.trim().uppercase(),
                    currencyCode = draft.currencyCode.trim().ifBlank { "JOD" }.uppercase(),
                    isPrimary = isFirstVehicle,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun setPrimary(id: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            if (dao.getById(id) == null) return@withTransaction
            dao.clearPrimary()
            dao.markPrimary(id, now)
        }
    }

    suspend fun deleteVehicle(id: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val target = dao.getById(id) ?: return@withTransaction
            dao.deleteById(id)
            if (target.isPrimary) {
                dao.getAllOnce().firstOrNull()?.let { next ->
                    dao.clearPrimary()
                    dao.markPrimary(next.id, now)
                }
            }
        }
    }
}
