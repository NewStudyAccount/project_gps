package com.gps.dashboard.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_points",
    foreignKeys = [ForeignKey(
        entity = Track::class,
        parentColumns = ["id"],
        childColumns = ["trackId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trackId")]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,               // 外键
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,            // 米
    val speed: Float,                // m/s
    val bearing: Float,              // 度
    val accuracy: Float,             // 米
    val timestamp: Long,             // UTC 毫秒
    val isOriginal: Boolean = true,  // true=原始点, false=压缩插入的点
)
