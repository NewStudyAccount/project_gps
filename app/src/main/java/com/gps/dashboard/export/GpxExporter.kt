package com.gps.dashboard.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.gps.dashboard.data.model.Track
import com.gps.dashboard.data.model.TrackPoint
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GPX 格式导出器。
 * 生成标准 GPX 1.1 格式文件，兼容大多数 GPS 应用。
 */
object GpxExporter {

    fun export(track: Track, points: List<TrackPoint>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val sb = StringBuilder()

        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="GPS Dashboard"""")
        sb.appendLine("""  xmlns="http://www.topografix.com/GPX/1/1"""")
        sb.appendLine("""  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"""")
        sb.appendLine("""  xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">""")

        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>${escapeXml(track.name)}</name>")
        sb.appendLine("    <time>${sdf.format(Date(track.startTime))}</time>")
        sb.appendLine("  </metadata>")

        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${escapeXml(track.name)}</name>")
        sb.appendLine("    <desc>距离 ${formatDistance(track.totalDistance)} · 用时 ${formatDuration(track.totalDuration)} · 均速 ${formatSpeed(track.avgSpeed)}</desc>")
        sb.appendLine("    <trkseg>")

        for (p in points) {
            sb.appendLine("""      <trkpt lat="${p.latitude}" lon="${p.longitude}">""")
            sb.appendLine("        <ele>${"%.1f".format(p.altitude)}</ele>")
            sb.appendLine("        <time>${sdf.format(Date(p.timestamp))}</time>")
            sb.appendLine("        <speed>${"%.2f".format(p.speed)}</speed>")
            sb.appendLine("      </trkpt>")
        }

        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")

        return sb.toString()
    }

    /**
     * 保存 GPX 文件到公共 Downloads 目录，返回 Uri。
     */
    fun saveToFile(context: Context, track: Track, points: List<TrackPoint>): Uri? {
        val gpxContent = export(track, points)
        val fileName = buildFileName(track)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/gpx+xml")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(gpxContent.toByteArray(Charsets.UTF_8))
        }

        return uri
    }

    private fun buildFileName(track: Track): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        val dateStr = sdf.format(Date(track.startTime))
        return "GPS_轨迹_${dateStr}.gpx"
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun formatDistance(meters: Float): String {
        return if (meters >= 1000) "%.2fkm".format(meters / 1000) else "%.0fm".format(meters)
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    private fun formatSpeed(mps: Float): String {
        return "%.1fkm/h".format(mps * 3.6f)
    }
}
