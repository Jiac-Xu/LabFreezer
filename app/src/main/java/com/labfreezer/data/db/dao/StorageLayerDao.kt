package com.labfreezer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.labfreezer.data.db.entity.StorageLayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageLayerDao {

    @Query("SELECT * FROM storage_layer WHERE name LIKE '%' || :query || '%' AND name != '__hidden__' ORDER BY sort_order ASC, name ASC")
    suspend fun searchByName(query: String): List<StorageLayerEntity>

    @Query("SELECT * FROM storage_layer WHERE name != '__hidden__' ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<StorageLayerEntity>

    @Query("SELECT * FROM storage_layer WHERE device_id = :deviceId AND name != '__hidden__' ORDER BY sort_order ASC, name ASC")
    fun getByDeviceIdFlow(deviceId: Long): Flow<List<StorageLayerEntity>>

    @Query("SELECT * FROM storage_layer WHERE device_id = :deviceId AND name != '__hidden__' ORDER BY sort_order ASC, name ASC")
    suspend fun getByDeviceId(deviceId: Long): List<StorageLayerEntity>

    @Query("SELECT * FROM storage_layer WHERE device_id = :deviceId ORDER BY sort_order ASC, name ASC")
    suspend fun getByDeviceIdAll(deviceId: Long): List<StorageLayerEntity>

    @Query("SELECT * FROM storage_layer WHERE device_id = :deviceId AND name = '__hidden__' ORDER BY id ASC LIMIT 1")
    suspend fun getHiddenLayerByDeviceId(deviceId: Long): StorageLayerEntity?

    @Query("SELECT * FROM storage_layer WHERE id = :id")
    suspend fun getById(id: Long): StorageLayerEntity?

    @Query("SELECT COUNT(*) FROM storage_layer WHERE device_id = :deviceId AND name != '__hidden__'")
    suspend fun countByDeviceId(deviceId: Long): Int

    @Query("UPDATE storage_layer SET device_id = :targetDeviceId WHERE device_id = :sourceDeviceId")
    suspend fun relinkLayers(sourceDeviceId: Long, targetDeviceId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(layer: StorageLayerEntity): Long

    @Update
    suspend fun update(layer: StorageLayerEntity)

    @Delete
    suspend fun delete(layer: StorageLayerEntity)

    @Query("DELETE FROM storage_layer WHERE id = :id")
    suspend fun deleteById(id: Long)
}
