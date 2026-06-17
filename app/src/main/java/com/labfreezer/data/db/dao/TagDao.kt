package com.labfreezer.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.labfreezer.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY name ASC")
    fun getAllFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag ORDER BY name ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tag WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tag WHERE id = :id")
    suspend fun deleteById(id: Long)
}
