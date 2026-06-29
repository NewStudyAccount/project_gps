package com.gps.dashboard.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gps.dashboard.data.db.TrackingDatabase
import com.gps.dashboard.data.model.Track
import com.gps.dashboard.data.model.TrackPoint
import com.gps.dashboard.export.GpxExporter
import com.gps.dashboard.export.KmlExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 轨迹数据仓库，封装 TrackDao 和 TrackPointDao 的常用操作。
 */
class TrackRepository(private val db: TrackingDatabase) {

    private val trackDao = db.trackDao()
    private val trackPointDao = db.trackPointDao()

    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()

    suspend fun getTrackById(id: Long): Track? = trackDao.getById(id)

    suspend fun getTrackPoints(trackId: Long): List<TrackPoint> =
        trackPointDao.getByTrackId(trackId)

    fun getTrackPointsFlow(trackId: Long): Flow<List<TrackPoint>> =
        trackPointDao.getByTrackIdFlow(trackId)

    suspend fun deleteTrack(trackId: Long) {
        val track = trackDao.getById(trackId) ?: return
        trackPointDao.deleteByTrackId(trackId)
        trackDao.delete(track)
    }

    suspend fun deleteAll() {
        trackDao.deleteAll()
    }

    suspend fun getPointCount(trackId: Long): Int =
        trackPointDao.countByTrackId(trackId)

    /**
     * 导出轨迹为 GPX 文件并保存到 Downloads。
     */
    suspend fun exportGpx(context: Context, trackId: Long): Uri? = withContext(Dispatchers.IO) {
        val track = trackDao.getById(trackId) ?: return@withContext null
        val points = trackPointDao.getByTrackId(trackId)
        GpxExporter.saveToFile(context, track, points)
    }

    /**
     * 导出轨迹为 KML 文件并保存到 Downloads。
     */
    suspend fun exportKml(context: Context, trackId: Long): Uri? = withContext(Dispatchers.IO) {
        val track = trackDao.getById(trackId) ?: return@withContext null
        val points = trackPointDao.getByTrackId(trackId)
        KmlExporter.saveToFile(context, track, points)
    }

    /**
     * 分享轨迹文件。
     */
    fun shareTrack(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享轨迹"))
    }
}
