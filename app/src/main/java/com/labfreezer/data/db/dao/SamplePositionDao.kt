package com.labfreezer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.labfreezer.data.db.entity.SamplePositionEntity
import kotlinx.coroutines.flow.Flow

data class SampleWithPath(
    val sampleId: Long,
    val boxId: Long,
    val row: Int,
    val col: Int,
    val name: String?,
    val note: String?,
    val date: String?,
    val photoPath: String?,
    val deviceName: String,
    val layerName: String,
    val boxName: String
)

data class SampleTagInfo(
    val sampleId: Long,
    val tagId: Long,
    val tagName: String
)

@Dao
interface SamplePositionDao {

    @Query("SELECT * FROM sample_position WHERE box_id = :boxId ORDER BY row ASC, col ASC")
    fun getByBoxIdFlow(boxId: Long): Flow<List<SamplePositionEntity>>

    @Query("SELECT * FROM sample_position WHERE box_id = :boxId ORDER BY row ASC, col ASC")
    suspend fun getByBoxId(boxId: Long): List<SamplePositionEntity>

    @Query("SELECT * FROM sample_position WHERE id = :id")
    suspend fun getById(id: Long): SamplePositionEntity?

    @Query("SELECT * FROM sample_position WHERE box_id = :boxId AND row = :row AND col = :col")
    suspend fun getByPosition(boxId: Long, row: Int, col: Int): SamplePositionEntity?

    @Query("SELECT * FROM sample_position WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<SamplePositionEntity>

    // ==================== 全局搜索（所有范围） ====================

    @Query(
        "SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "WHERE sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%' OR sp.date LIKE '%' || :query || '%' " +
        "ORDER BY sp.name ASC"
    )
    suspend fun searchWithPath(query: String): List<SampleWithPath>

    @Query(
        "SELECT DISTINCT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "LEFT JOIN sample_tag st ON sp.id = st.sample_id " +
        "WHERE (sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%' OR sp.date LIKE '%' || :query || '%') " +
        "AND (:tagCount = 0 OR st.tag_id IN (:tagIds)) " +
        "ORDER BY sp.name ASC"
    )
    suspend fun searchWithPathByTags(query: String, tagIds: List<Long>, tagCount: Int): List<SampleWithPath>

    // ==================== 设备范围搜索 ====================

    @Query(
        "SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "WHERE (sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%' OR sp.date LIKE '%' || :query || '%') " +
        "AND sd.id = :deviceId " +
        "ORDER BY sp.name ASC"
    )
    suspend fun searchWithPathByDevice(query: String, deviceId: Long): List<SampleWithPath>

    @Query(
        "SELECT DISTINCT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "LEFT JOIN sample_tag st ON sp.id = st.sample_id " +
        "WHERE (sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%' OR sp.date LIKE '%' || :query || '%') " +
        "AND sd.id = :deviceId " +
        "AND (:tagCount = 0 OR st.tag_id IN (:tagIds)) " +
        "ORDER BY sp.name ASC"
    )
    suspend fun searchWithPathByDeviceAndTags(query: String, deviceId: Long, tagIds: List<Long>, tagCount: Int): List<SampleWithPath>

    // ==================== 层级范围搜索 ====================

    @Query(
        "SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "WHERE (sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%' OR sp.date LIKE '%' || :query || '%') " +
        "AND sl.id = :layerId " +
        "ORDER BY sp.name ASC"
    )
    suspend fun searchWithPathByLayer(query: String, layerId: Long): List<SampleWithPath>

    @Query(
        "SELECT DISTINCT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "LEFT JOIN sample_tag st ON sp.id = st.sample_id " +
        "WHERE (sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%' OR sp.date LIKE '%' || :query || '%') " +
        "AND sl.id = :layerId " +
        "AND (:tagCount = 0 OR st.tag_id IN (:tagIds)) " +
        "ORDER BY sp.name ASC"
    )
    suspend fun searchWithPathByLayerAndTags(query: String, layerId: Long, tagIds: List<Long>, tagCount: Int): List<SampleWithPath>

    // ==================== 盒子范围搜索 ====================

    @Query(
        "SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "WHERE (sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%' OR sp.date LIKE '%' || :query || '%') " +
        "AND sb.id = :boxId " +
        "ORDER BY sp.name ASC"
    )
    suspend fun searchWithPathByBox(query: String, boxId: Long): List<SampleWithPath>

    @Query(
        "SELECT DISTINCT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "LEFT JOIN sample_tag st ON sp.id = st.sample_id " +
        "WHERE (sp.name LIKE '%' || :query || '%' OR sp.note LIKE '%' || :query || '%' OR sp.date LIKE '%' || :query || '%') " +
        "AND sb.id = :boxId " +
        "AND (:tagCount = 0 OR st.tag_id IN (:tagIds)) " +
        "ORDER BY sp.name ASC"
    )
    suspend fun searchWithPathByBoxAndTags(query: String, boxId: Long, tagIds: List<Long>, tagCount: Int): List<SampleWithPath>

    // ==================== 范围搜索：盒子/层级（用于搜索结果中非样本实体的范围限制） ====================

    @Query("SELECT * FROM storage_box WHERE name LIKE '%' || :query || '%' AND layer_id IN (SELECT id FROM storage_layer WHERE device_id = :deviceId) ORDER BY name ASC")
    suspend fun searchBoxesByDevice(query: String, deviceId: Long): List<com.labfreezer.data.db.entity.StorageBoxEntity>

    @Query("SELECT * FROM storage_box WHERE name LIKE '%' || :query || '%' AND layer_id = :layerId ORDER BY name ASC")
    suspend fun searchBoxesByLayer(query: String, layerId: Long): List<com.labfreezer.data.db.entity.StorageBoxEntity>

    // ==================== 批量标签查询 ====================

    @Query(
        "SELECT st.sample_id AS sampleId, t.id AS tagId, t.name AS tagName " +
        "FROM sample_tag st INNER JOIN tag t ON st.tag_id = t.id " +
        "WHERE st.sample_id IN (:sampleIds)"
    )
    suspend fun getTagsBySampleIds(sampleIds: List<Long>): List<SampleTagInfo>

    @Query(
        "SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "ORDER BY sb.name ASC, sp.row ASC, sp.col ASC"
    )
    suspend fun getAllWithPath(): List<SampleWithPath>

    @Query(
        "SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "WHERE sp.photo_path IS NOT NULL " +
        "ORDER BY sd.name ASC, sl.name ASC, sb.name ASC, sp.row ASC, sp.col ASC"
    )
    suspend fun getAllWithPhoto(): List<SampleWithPath>

    @Query(
        "SELECT sp.id AS sampleId, sp.box_id AS boxId, sp.row, sp.col, " +
        "sp.name, sp.note, sp.date, sp.photo_path AS photoPath, " +
        "sd.name AS deviceName, sl.name AS layerName, sb.name AS boxName " +
        "FROM sample_position sp " +
        "INNER JOIN storage_box sb ON sp.box_id = sb.id " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "INNER JOIN storage_device sd ON sl.device_id = sd.id " +
        "WHERE sp.photo_path IS NOT NULL " +
        "AND ((sp.name IS NOT NULL AND sp.name != '') OR (sp.note IS NOT NULL AND sp.note != '')) " +
        "ORDER BY sd.name ASC, sl.name ASC, sb.name ASC, sp.row ASC, sp.col ASC"
    )
    suspend fun getWithPhotoAndNameOrNote(): List<SampleWithPath>

    @Query("SELECT * FROM sample_position WHERE photo_path IS NOT NULL AND (name IS NULL OR name = '') AND (note IS NULL OR note = '')")
    suspend fun getWithPhotoButEmptyNameAndNote(): List<SamplePositionEntity>

    @Query("SELECT COUNT(*) FROM sample_position WHERE box_id = :boxId")
    suspend fun countByBoxId(boxId: Long): Int

    @Query("SELECT COUNT(*) FROM sample_position WHERE box_id = :boxId AND photo_path IS NOT NULL")
    suspend fun countWithPhotoByBoxId(boxId: Long): Int

    @Query("SELECT COUNT(*) FROM sample_position WHERE box_id IN (SELECT id FROM storage_box WHERE layer_id = :layerId)")
    suspend fun countByLayerId(layerId: Long): Int

    @Query("SELECT COUNT(*) FROM sample_position")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: SamplePositionEntity): Long

    @Update
    suspend fun update(position: SamplePositionEntity)

    @Delete
    suspend fun delete(position: SamplePositionEntity)

    @Query("DELETE FROM sample_position WHERE id = :id")
    suspend fun deleteById(id: Long)
}
