package com.labfreezer.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_box",
    foreignKeys = [
        ForeignKey(
            entity = StorageLayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["layer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["layer_id"])
    ]
)
data class StorageBoxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "layer_id")
    val layerId: Long,

    val name: String,

    val rows: Int,

    val cols: Int,

    val note: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
