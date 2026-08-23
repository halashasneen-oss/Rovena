package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.PartDao
import com.rovena.garage.data.local.entities.PartEntity
import kotlinx.coroutines.flow.Flow

class PartRepository(private val partDao: PartDao) {

    fun observeByVehicle(vehicleId: Long): Flow<List<PartEntity>> = partDao.observeByVehicle(vehicleId)

    fun observeCount(vehicleId: Long): Flow<Int> = partDao.observeCount(vehicleId)

    fun observeWithWarranty(vehicleId: Long): Flow<List<PartEntity>> = partDao.observeWithWarranty(vehicleId)

    suspend fun addOrUpdate(part: PartEntity): Long =
        if (part.id == 0L) partDao.insert(part) else { partDao.update(part); part.id }

    suspend fun delete(part: PartEntity) = partDao.delete(part)
}
