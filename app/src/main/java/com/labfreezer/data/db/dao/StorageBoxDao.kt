package com.labfreezer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.labfreezer.data.db.entity.StorageBoxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageBoxDao {

    @Query("SELECT * FROM storage_box WHERE name LIKE '%' || :query || '%' ORDER BY sort_order ASC, name ASC")
    suspend fun searchByName(query: String): List<StorageBoxEntity>

    @Query("SELECT * FROM storage_box ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<StorageBoxEntity>

    @Query("SELECT * FROM storage_box WHERE layer_id = :layerId ORDER BY sort_order ASC, name ASC")
    fun getByLayerIdFlow(layerId: Long): Flow<List<StorageBoxEntity>>

    @Query("SELECT * FROM storage_box WHERE layer_id = :layerId ORDER BY sort_order ASC, name ASC")
    suspend fun getByLayerId(layerId: Long): List<StorageBoxEntity>

    @Query("SELECT * FROM storage_box WHERE id = :id")
    suspend fun getById(id: Long): StorageBoxEntity?

    @Query("SELECT * FROM storage_box WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<StorageBoxEntity?>

    @Query("SELECT COUNT(*) FROM storage_box WHERE layer_id = :layerId")
    suspend fun countByLayerId(layerId: Long): Int

    @Query("SELECT COUNT(*) FROM storage_box WHERE layer_id IN (SELECT id FROM storage_layer WHERE device_id = :deviceId)")
    suspend fun countByDeviceId(deviceId: Long): Int

    /**
     * 获取通过 hidden layer 直接挂在某设备下的盒子。
     * 用于「跳过层级」场景：设备下没有真实 Layer，只有通过 __hidden__ 占位 Layer 连接的 Box。
     */
    @Query(
        "SELECT sb.* FROM storage_box sb " +
        "INNER JOIN storage_layer sl ON sb.layer_id = sl.id " +
        "WHERE sl.device_id = :deviceId AND sl.name = '__hidden__' " +
        "ORDER BY sb.sort_order ASC, sb.name ASC"
    )
    suspend fun getBoxesByDeviceDirect(deviceId: Long): List<StorageBoxEntity>

    @Query("UPDATE storage_box SET layer_id = :targetLayerId WHERE layer_id = :sourceLayerId")
    suspend fun relinkBoxes(sourceLayerId: Long, targetLayerId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(box: StorageBoxEntity): Long

    @Update
    suspend fun update(box: StorageBoxEntity)

    @Delete
    suspend fun delete(box: StorageBoxEntity)

    @Query("DELETE FROM storage_box WHERE id = :id")
    suspend fun deleteById(id: Long)
}
