package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.SamplePositionDao
import com.labfreezer.data.db.dao.SampleTagInfo
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.entity.SamplePositionEntity
import com.labfreezer.data.db.entity.StorageBoxEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SamplePositionRepository @Inject constructor(
    private val sampleDao: SamplePositionDao
) {
    fun getByBoxIdFlow(boxId: Long): Flow<List<SamplePositionEntity>> =
        sampleDao.getByBoxIdFlow(boxId)

    suspend fun getByBoxId(boxId: Long): List<SamplePositionEntity> =
        sampleDao.getByBoxId(boxId)

    suspend fun getById(id: Long): SamplePositionEntity? = sampleDao.getById(id)

    suspend fun getByPosition(boxId: Long, row: Int, col: Int): SamplePositionEntity? =
        sampleDao.getByPosition(boxId, row, col)

    suspend fun searchByName(query: String): List<SamplePositionEntity> =
        sampleDao.searchByName(query)

    // 全局搜索
    suspend fun searchWithPath(query: String): List<SampleWithPath> =
        sampleDao.searchWithPath(query)

    suspend fun searchWithPathByTags(query: String, tagIds: List<Long>): List<SampleWithPath> =
        sampleDao.searchWithPathByTags(query, tagIds, tagIds.size)

    // 设备范围搜索
    suspend fun searchWithPathByDevice(query: String, deviceId: Long): List<SampleWithPath> =
        sampleDao.searchWithPathByDevice(query, deviceId)

    suspend fun searchWithPathByDeviceAndTags(query: String, deviceId: Long, tagIds: List<Long>): List<SampleWithPath> =
        sampleDao.searchWithPathByDeviceAndTags(query, deviceId, tagIds, tagIds.size)

    // 层级范围搜索
    suspend fun searchWithPathByLayer(query: String, layerId: Long): List<SampleWithPath> =
        sampleDao.searchWithPathByLayer(query, layerId)

    suspend fun searchWithPathByLayerAndTags(query: String, layerId: Long, tagIds: List<Long>): List<SampleWithPath> =
        sampleDao.searchWithPathByLayerAndTags(query, layerId, tagIds, tagIds.size)

    // 盒子范围搜索
    suspend fun searchWithPathByBox(query: String, boxId: Long): List<SampleWithPath> =
        sampleDao.searchWithPathByBox(query, boxId)

    suspend fun searchWithPathByBoxAndTags(query: String, boxId: Long, tagIds: List<Long>): List<SampleWithPath> =
        sampleDao.searchWithPathByBoxAndTags(query, boxId, tagIds, tagIds.size)

    // 范围搜索：盒子实体
    suspend fun searchBoxesByDevice(query: String, deviceId: Long): List<StorageBoxEntity> =
        sampleDao.searchBoxesByDevice(query, deviceId)

    suspend fun searchBoxesByLayer(query: String, layerId: Long): List<StorageBoxEntity> =
        sampleDao.searchBoxesByLayer(query, layerId)

    // 批量标签查询
    suspend fun getTagsBySampleIds(sampleIds: List<Long>): List<SampleTagInfo> =
        sampleDao.getTagsBySampleIds(sampleIds)

    suspend fun getAllWithPath(): List<SampleWithPath> =
        sampleDao.getAllWithPath()

    suspend fun getAllWithPhoto(): List<SampleWithPath> =
        sampleDao.getAllWithPhoto()

    suspend fun getWithPhotoAndNameOrNote(): List<SampleWithPath> =
        sampleDao.getWithPhotoAndNameOrNote()

    suspend fun getWithPhotoButEmptyNameAndNote(): List<SamplePositionEntity> =
        sampleDao.getWithPhotoButEmptyNameAndNote()

    suspend fun countByBoxId(boxId: Long): Int = sampleDao.countByBoxId(boxId)

    suspend fun countByLayerId(layerId: Long): Int = sampleDao.countByLayerId(layerId)

    suspend fun countAll(): Int = sampleDao.countAll()

    suspend fun insert(position: SamplePositionEntity): Long = sampleDao.insert(position)

    suspend fun update(position: SamplePositionEntity) = sampleDao.update(position)

    suspend fun delete(position: SamplePositionEntity) = sampleDao.delete(position)

    suspend fun deleteById(id: Long) = sampleDao.deleteById(id)
}
