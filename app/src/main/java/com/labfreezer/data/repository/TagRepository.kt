package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.SampleTagDao
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.dao.TagDao
import com.labfreezer.data.db.entity.SampleTagEntity
import com.labfreezer.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val sampleTagDao: SampleTagDao
) {
    fun getAllFlow(): Flow<List<TagEntity>> = tagDao.getAllFlow()
    suspend fun getAll(): List<TagEntity> = tagDao.getAll()
    suspend fun getById(id: Long): TagEntity? = tagDao.getById(id)
    suspend fun getByName(name: String): TagEntity? = tagDao.getByName(name)
    suspend fun insert(tag: TagEntity): Long = tagDao.insert(tag)
    suspend fun update(tag: TagEntity) = tagDao.update(tag)
    suspend fun delete(tag: TagEntity) = tagDao.delete(tag)

    fun getTagsBySampleIdFlow(sampleId: Long): Flow<List<TagEntity>> =
        sampleTagDao.getTagsBySampleIdFlow(sampleId)

    suspend fun getTagsBySampleId(sampleId: Long): List<TagEntity> =
        sampleTagDao.getTagsBySampleId(sampleId)

    suspend fun getSamplesWithPathByTagId(tagId: Long): List<SampleWithPath> =
        sampleTagDao.getSamplesWithPathByTagId(tagId)

    suspend fun addTagToSample(sampleId: Long, tagId: Long) {
        if (sampleTagDao.countBySampleAndTag(sampleId, tagId) == 0) {
            sampleTagDao.insert(SampleTagEntity(sampleId = sampleId, tagId = tagId))
        }
    }

    suspend fun removeTagFromSample(sampleId: Long, tagId: Long) {
        sampleTagDao.deleteBySampleAndTag(sampleId, tagId)
    }

    suspend fun setSampleTags(sampleId: Long, tagIds: List<Long>) {
        sampleTagDao.deleteAllBySampleId(sampleId)
        tagIds.forEach { tagId ->
            sampleTagDao.insert(SampleTagEntity(sampleId = sampleId, tagId = tagId))
        }
    }
}
