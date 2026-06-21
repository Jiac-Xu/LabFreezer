package com.labfreezer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.labfreezer.data.db.entity.SampleTagEntity
import com.labfreezer.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleTagDao {
    @Query("SELECT t.* FROM tag t INNER JOIN sample_tag st ON t.id = st.tag_id WHERE st.sample_id = :sampleId ORDER BY t.name ASC")
    fun getTagsBySampleIdFlow(sampleId: Long): Flow<List<TagEntity>>

    @Query("SELECT t.* FROM tag t INNER JOIN sample_tag st ON t.id = st.tag_id WHERE st.sample_id = :sampleId ORDER BY t.name ASC")
    suspend fun getTagsBySampleId(sampleId: Long): List<TagEntity>

    @Query("SELECT sp.* FROM sample_position sp INNER JOIN sample_tag st ON sp.id = st.sample_id WHERE st.tag_id = :tagId ORDER BY sp.name ASC")
    suspend fun getSamplesByTagId(tagId: Long): List<com.labfreezer.data.db.entity.SamplePositionEntity>

    @Query(
        "SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN sample_tag st ON sp.id = st.sample_id " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "WHERE st.tag_id = :tagId " +
        "ORDER BY sp.name ASC"
    )
    suspend fun getSamplesWithPathByTagId(tagId: Long): List<SampleWithPath>

    @Query("SELECT COUNT(*) FROM sample_tag WHERE tag_id = :tagId")
    suspend fun countSamplesByTagId(tagId: Long): Int

    @Query("SELECT COUNT(*) FROM sample_tag WHERE sample_id = :sampleId AND tag_id = :tagId")
    suspend fun countBySampleAndTag(sampleId: Long, tagId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sampleTag: SampleTagEntity): Long

    @Query("DELETE FROM sample_tag WHERE sample_id = :sampleId AND tag_id = :tagId")
    suspend fun deleteBySampleAndTag(sampleId: Long, tagId: Long)

    @Query("DELETE FROM sample_tag WHERE sample_id = :sampleId")
    suspend fun deleteAllBySampleId(sampleId: Long)
}
