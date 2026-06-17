package com.labfreezer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.labfreezer.data.db.entity.StorageDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageDeviceDao {

    @Query("SELECT * FROM storage_device ORDER BY sort_order ASC, name ASC")
    fun getAllFlow(): Flow<List<StorageDeviceEntity>>

    @Query("SELECT * FROM storage_device ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<StorageDeviceEntity>

    @Query("SELECT * FROM storage_device WHERE id = :id")
    suspend fun getById(id: Long): StorageDeviceEntity?

    @Query("SELECT * FROM storage_device WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<StorageDeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: StorageDeviceEntity): Long

    @Update
    suspend fun update(device: StorageDeviceEntity)

    @Delete
    suspend fun delete(device: StorageDeviceEntity)

    @Query("DELETE FROM storage_device WHERE id = :id")
    suspend fun deleteById(id: Long)
}
