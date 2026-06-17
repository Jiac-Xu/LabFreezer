package com.labfreezer.data.repository

import com.labfreezer.data.db.dao.DeviceTypeDao
import com.labfreezer.data.db.entity.DeviceTypeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTypeRepository @Inject constructor(
    private val dao: DeviceTypeDao
) {
    fun getAllFlow(): Flow<List<DeviceTypeEntity>> = dao.getAllFlow()

    suspend fun getAll(): List<DeviceTypeEntity> = dao.getAll()

    suspend fun insert(type: DeviceTypeEntity): Long = dao.insert(type)

    suspend fun delete(type: DeviceTypeEntity) = dao.delete(type)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
