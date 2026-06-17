package com.labfreezer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.labfreezer.data.db.dao.DeviceTypeDao
import com.labfreezer.data.db.dao.SamplePositionDao
import com.labfreezer.data.db.dao.SampleTagDao
import com.labfreezer.data.db.dao.StorageBoxDao
import com.labfreezer.data.db.dao.StorageDeviceDao
import com.labfreezer.data.db.dao.StorageLayerDao
import com.labfreezer.data.db.dao.TagDao
import com.labfreezer.data.db.entity.DeviceTypeEntity
import com.labfreezer.data.db.entity.SamplePositionEntity
import com.labfreezer.data.db.entity.SampleTagEntity
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.entity.TagEntity
@Database(
    entities = [
        StorageDeviceEntity::class,
        StorageLayerEntity::class,
        StorageBoxEntity::class,
        SamplePositionEntity::class,
        TagEntity::class,
        SampleTagEntity::class,
        DeviceTypeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceTypeDao(): DeviceTypeDao
    abstract fun storageDeviceDao(): StorageDeviceDao
    abstract fun storageLayerDao(): StorageLayerDao
    abstract fun storageBoxDao(): StorageBoxDao
    abstract fun samplePositionDao(): SamplePositionDao
    abstract fun tagDao(): TagDao
    abstract fun sampleTagDao(): SampleTagDao
}
