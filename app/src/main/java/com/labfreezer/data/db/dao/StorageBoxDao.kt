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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(box: StorageBoxEntity): Long

    @Update
    suspend fun update(box: StorageBoxEntity)

    @Delete
    suspend fun delete(box: StorageBoxEntity)

    @Query("DELETE FROM storage_box WHERE id = :id")
    suspend fun deleteById(id: Long)
}
