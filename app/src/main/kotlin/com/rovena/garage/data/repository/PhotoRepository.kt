package com.rovena.garage.data.repository

import com.rovena.garage.data.local.dao.VehiclePhotoDao
import com.rovena.garage.data.local.entities.VehiclePhotoEntity
import com.rovena.garage.domain.model.PhotoLinkedType
import kotlinx.coroutines.flow.Flow

class PhotoRepository(private val photoDao: VehiclePhotoDao) {

    fun observeByVehicle(vehicleId: Long): Flow<List<VehiclePhotoEntity>> = photoDao.observeByVehicle(vehicleId)

    fun observeByLink(linkedType: PhotoLinkedType, linkedId: Long): Flow<List<VehiclePhotoEntity>> =
        photoDao.observeByLink(linkedType, linkedId)

    suspend fun getByLinkOnce(linkedType: PhotoLinkedType, linkedId: Long): List<VehiclePhotoEntity> =
        photoDao.getByLinkOnce(linkedType, linkedId)

    suspend fun add(photo: VehiclePhotoEntity): Long = photoDao.insert(photo)

    suspend fun delete(photo: VehiclePhotoEntity) = photoDao.delete(photo)

    suspend fun deleteAllForLink(linkedType: PhotoLinkedType, linkedId: Long) = photoDao.deleteByLink(linkedType, linkedId)
}
