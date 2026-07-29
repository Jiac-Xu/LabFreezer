package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.StorageBoxDao
import com.labfreezer.data.db.dao.StorageLayerDao
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class LayerWithCount(
    val layer: StorageLayerEntity,
    val boxCount: Int
)

@Singleton
class StorageLayerRepository @Inject constructor(
    private val layerDao: StorageLayerDao,
    private val boxDao: StorageBoxDao
) {
    fun getByDeviceIdFlow(deviceId: Long): Flow<List<StorageLayerEntity>> =
        layerDao.getByDeviceIdFlow(deviceId)

    suspend fun searchByName(query: String): List<StorageLayerEntity> = layerDao.searchByName(query)

    suspend fun getAll(): List<StorageLayerEntity> = layerDao.getAll()

    suspend fun getByDeviceId(deviceId: Long): List<StorageLayerEntity> =
        layerDao.getByDeviceId(deviceId)

    suspend fun getByDeviceIdAll(deviceId: Long): List<StorageLayerEntity> =
        layerDao.getByDeviceIdAll(deviceId)

    suspend fun getById(id: Long): StorageLayerEntity? = layerDao.getById(id)

    suspend fun countByDeviceId(deviceId: Long): Int = layerDao.countByDeviceId(deviceId)

    suspend fun getLayersWithCount(deviceId: Long): List<LayerWithCount> {
        val layers = layerDao.getByDeviceId(deviceId)
        return layers.map { layer ->
            LayerWithCount(
                layer = layer,
                boxCount = boxDao.countByLayerId(layer.id)
            )
        }
    }

    /**
     * 获取某设备下所有非 hidden layer，
     * 每个 layer 附带其包含的盒子数和通过 hidden 子层挂载的盒子数。
     */
    suspend fun getVisibleLayerSummary(deviceId: Long): List<LayerWithCount> {
        val layers = layerDao.getByDeviceId(deviceId)
        return layers.map { layer ->
            LayerWithCount(
                layer = layer,
                boxCount = boxDao.countByLayerId(layer.id)
            )
        }
    }

    /**
     * 获取某设备下所有通过 hidden layer 直接挂载的盒子。
     * 用于「跳过层级」场景。
     */
    suspend fun getBoxesUnderHiddenLayer(deviceId: Long): List<StorageBoxEntity> {
        return boxDao.getBoxesByDeviceDirect(deviceId)
    }

    suspend fun insert(layer: StorageLayerEntity): Long = layerDao.insert(layer)

    suspend fun update(layer: StorageLayerEntity) = layerDao.update(layer)

    suspend fun delete(layer: StorageLayerEntity) = layerDao.delete(layer)

    suspend fun deleteById(id: Long) = layerDao.deleteById(id)
}
