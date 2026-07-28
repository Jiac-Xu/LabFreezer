package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.StorageDeviceDao
import com.labfreezer.data.db.dao.StorageBoxDao
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.dao.StorageLayerDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceWithCount(
    val device: StorageDeviceEntity,
    val layerCount: Int
)

/**
 * 设备概要：包含「可见子节点」的计数。
 * 子节点可以是 Layer 或 Box（通过 hidden layer 直接挂载的盒子）。
 */
data class DeviceWithSummary(
    val device: StorageDeviceEntity,
    val visibleChildCount: Int,
    val hasBoxesDirect: Boolean   // 是否有直接挂在设备下的盒子（跳过层级）
)

@Singleton
class StorageDeviceRepository @Inject constructor(
    private val deviceDao: StorageDeviceDao,
    private val layerDao: StorageLayerDao,
    private val boxDao: StorageBoxDao
) {
    fun getAllFlow(): Flow<List<StorageDeviceEntity>> = deviceDao.getAllFlow()

    suspend fun searchByName(query: String): List<StorageDeviceEntity> = deviceDao.searchByName(query)

    suspend fun getAll(): List<StorageDeviceEntity> = deviceDao.getAll()

    suspend fun getAllHidden(): List<StorageDeviceEntity> = deviceDao.getAllHidden()

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

    /**
     * 获取设备概要列表，包含可见子节点（Layer + 直接挂载的 Box）的数量。
     */
    suspend fun getDevicesWithSummary(): List<DeviceWithSummary> {
        val devices = deviceDao.getAll()
        return devices.map { device ->
            val layerCount = layerDao.countByDeviceId(device.id)
            val directBoxes = boxDao.getBoxesByDeviceDirect(device.id)
            DeviceWithSummary(
                device = device,
                visibleChildCount = layerCount + directBoxes.size,
                hasBoxesDirect = directBoxes.isNotEmpty()
            )
        }
    }

    suspend fun insert(device: StorageDeviceEntity): Long = deviceDao.insert(device)

    suspend fun update(device: StorageDeviceEntity) = deviceDao.update(device)

    suspend fun delete(device: StorageDeviceEntity) = deviceDao.delete(device)

    suspend fun deleteById(id: Long) = deviceDao.deleteById(id)
}
