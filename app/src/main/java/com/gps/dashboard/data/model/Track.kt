package com.gps.dashboard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                // "2026-06-29 14:30 骑行"
    val startTime: Long,             // UTC 毫秒
    val endTime: Long? = null,       // null = 正在录制
    val totalDistance: Float = 0f,   // 米
    val totalDuration: Long = 0L,   // 毫秒
    val avgSpeed: Float = 0f,       // m/s
    val maxSpeed: Float = 0f,       // m/s
    val pointCount: Int = 0,        // 原始点数
    val compressedPointCount: Int = 0, // 压缩后点数
    val avgAccuracy: Float = 0f,    // 米
)
