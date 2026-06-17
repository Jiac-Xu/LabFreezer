package com.labfreezer.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.labfreezer.data.db.AppDatabase
import com.labfreezer.data.db.dao.DeviceTypeDao
import com.labfreezer.data.db.dao.SamplePositionDao
import com.labfreezer.data.db.dao.SampleTagDao
import com.labfreezer.data.db.dao.StorageBoxDao
import com.labfreezer.data.db.dao.StorageDeviceDao
import com.labfreezer.data.db.dao.StorageLayerDao
import com.labfreezer.data.db.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `device_type` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sort_order` INTEGER NOT NULL DEFAULT 0)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "labfreezer.db"
        ).addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration().build()
    }

    @Provides fun provideStorageDeviceDao(db: AppDatabase): StorageDeviceDao = db.storageDeviceDao()
    @Provides fun provideStorageLayerDao(db: AppDatabase): StorageLayerDao = db.storageLayerDao()
    @Provides fun provideStorageBoxDao(db: AppDatabase): StorageBoxDao = db.storageBoxDao()
    @Provides fun provideSamplePositionDao(db: AppDatabase): SamplePositionDao = db.samplePositionDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
    @Provides fun provideSampleTagDao(db: AppDatabase): SampleTagDao = db.sampleTagDao()
    @Provides fun provideDeviceTypeDao(db: AppDatabase): DeviceTypeDao = db.deviceTypeDao()
}
