package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.StorageBoxDao
import com.labfreezer.data.db.dao.StorageLayerDao
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

    suspend fun insert(layer: StorageLayerEntity): Long = layerDao.insert(layer)

    suspend fun update(layer: StorageLayerEntity) = layerDao.update(layer)

    suspend fun delete(layer: StorageLayerEntity) = layerDao.delete(layer)

    suspend fun deleteById(id: Long) = layerDao.deleteById(id)
}
