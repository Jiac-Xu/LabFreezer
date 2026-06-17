package com.labfreezer.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sample_tag",
    foreignKeys = [
        ForeignKey(
            entity = SamplePositionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sample_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sample_id"]),
        Index(value = ["tag_id"]),
        Index(value = ["sample_id", "tag_id"], unique = true)
    ]
)
data class SampleTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sample_id")
    val sampleId: Long,
    @ColumnInfo(name = "tag_id")
    val tagId: Long
)
