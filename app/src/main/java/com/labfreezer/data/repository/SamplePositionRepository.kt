package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.SamplePositionDao
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.entity.SamplePositionEntity
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

    suspend fun searchWithPath(query: String): List<SampleWithPath> =
        sampleDao.searchWithPath(query)

    suspend fun searchWithPathByTags(query: String, tagIds: List<Long>): List<SampleWithPath> =
        sampleDao.searchWithPathByTags(query, tagIds, tagIds.size)

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
