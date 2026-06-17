package com.labfreezer.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sample_position",
    foreignKeys = [
        ForeignKey(
            entity = StorageBoxEntity::class,
            parentColumns = ["id"],
            childColumns = ["box_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["box_id"]),
        Index(value = ["name"]),
        Index(value = ["box_id", "row", "col"], unique = true)
    ]
)
data class SamplePositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "box_id")
    val boxId: Long,

    val row: Int,

    val col: Int,

    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,

    val name: String? = null,

    val note: String? = null,

    val date: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
