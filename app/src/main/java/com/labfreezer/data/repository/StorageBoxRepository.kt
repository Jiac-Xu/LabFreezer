package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.StorageBoxDao
import com.labfreezer.data.db.entity.StorageBoxEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageBoxRepository @Inject constructor(
    private val boxDao: StorageBoxDao
) {
    fun getByLayerIdFlow(layerId: Long): Flow<List<StorageBoxEntity>> =
        boxDao.getByLayerIdFlow(layerId)

    suspend fun getByLayerId(layerId: Long): List<StorageBoxEntity> =
        boxDao.getByLayerId(layerId)

    suspend fun getById(id: Long): StorageBoxEntity? = boxDao.getById(id)

    fun getByIdFlow(id: Long): Flow<StorageBoxEntity?> = boxDao.getByIdFlow(id)

    suspend fun countByLayerId(layerId: Long): Int = boxDao.countByLayerId(layerId)

    suspend fun countByDeviceId(deviceId: Long): Int = boxDao.countByDeviceId(deviceId)

    suspend fun insert(box: StorageBoxEntity): Long = boxDao.insert(box)

    suspend fun update(box: StorageBoxEntity) = boxDao.update(box)

    suspend fun delete(box: StorageBoxEntity) = boxDao.delete(box)

    suspend fun deleteById(id: Long) = boxDao.deleteById(id)
}
