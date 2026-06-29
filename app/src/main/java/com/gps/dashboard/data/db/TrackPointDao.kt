package com.gps.dashboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gps.dashboard.data.model.TrackPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {

    @Insert
    suspend fun insert(point: TrackPoint): Long

    @Insert
    suspend fun insertAll(points: List<TrackPoint>)

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getByTrackId(trackId: Long): List<TrackPoint>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getByTrackIdFlow(trackId: Long): Flow<List<TrackPoint>>

    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: Long)

    @Query("SELECT COUNT(*) FROM track_points WHERE trackId = :trackId")
    suspend fun countByTrackId(trackId: Long): Int
}
