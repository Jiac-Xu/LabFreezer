package com.labfreezer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.labfreezer.data.db.entity.DeviceTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceTypeDao {
    @Query("SELECT * FROM device_type ORDER BY sort_order")
    fun getAllFlow(): Flow<List<DeviceTypeEntity>>

    @Query("SELECT * FROM device_type ORDER BY sort_order")
    suspend fun getAll(): List<DeviceTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: DeviceTypeEntity): Long

    @Delete
    suspend fun delete(type: DeviceTypeEntity)

    @Query("DELETE FROM device_type WHERE id = :id")
    suspend fun deleteById(id: Long)
}
