package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.StorageDeviceDao
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.dao.StorageLayerDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceWithCount(
    val device: StorageDeviceEntity,
    val layerCount: Int
)

@Singleton
class StorageDeviceRepository @Inject constructor(
    private val deviceDao: StorageDeviceDao,
    private val layerDao: StorageLayerDao
) {
    fun getAllFlow(): Flow<List<StorageDeviceEntity>> = deviceDao.getAllFlow()

    suspend fun getAll(): List<StorageDeviceEntity> = deviceDao.getAll()

    suspend fun getById(id: Long): StorageDeviceEntity? = deviceDao.getById(id)

    fun getByIdFlow(id: Long): Flow<StorageDeviceEntity?> = deviceDao.getByIdFlow(id)

    suspend fun getDevicesWithCount(): List<DeviceWithCount> {
        val devices = deviceDao.getAll()
        return devices.map { device ->
            DeviceWithCount(
                device = device,
                layerCount = layerDao.countByDeviceId(device.id)
            )
        }
    }

    suspend fun insert(device: StorageDeviceEntity): Long = deviceDao.insert(device)

    suspend fun update(device: StorageDeviceEntity) = deviceDao.update(device)

    suspend fun delete(device: StorageDeviceEntity) = deviceDao.delete(device)

    suspend fun deleteById(id: Long) = deviceDao.deleteById(id)
}
