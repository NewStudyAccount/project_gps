package com.gps.dashboard.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.gps.dashboard.data.model.Track
import com.gps.dashboard.data.model.TrackPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * KML 格式导出器。
 * 生成标准 KML 格式文件，兼容 Google Earth 等应用。
 */
object KmlExporter {

    fun export(track: Track, points: List<TrackPoint>): String {
        val sb = StringBuilder()

        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
        sb.appendLine("  <Document>")
        sb.appendLine("    <name>${escapeXml(track.name)}</name>")
        sb.appendLine("    <description>距离 ${formatDistance(track.totalDistance)} · 用时 ${formatDuration(track.totalDuration)}</description>")

        // 样式定义
        sb.appendLine("    <Style id=\"trackStyle\">")
        sb.appendLine("      <LineStyle>")
        sb.appendLine("        <color>ff008cff</color>")  // ABGR: 琥珀橙
        sb.appendLine("        <width>3</width>")
        sb.appendLine("      </LineStyle>")
        sb.appendLine("    </Style>")

        sb.appendLine("    <Placemark>")
        sb.appendLine("      <name>${escapeXml(track.name)}</name>")
        sb.appendLine("      <styleUrl>#trackStyle</styleUrl>")
        sb.appendLine("      <LineString>")
        sb.appendLine("        <altitudeMode>clampToGround</altitudeMode>")
        sb.appendLine("        <coordinates>")

        for (p in points) {
            sb.appendLine("          ${p.longitude},${p.latitude},${p.altitude}")
        }

        sb.appendLine("        </coordinates>")
        sb.appendLine("      </LineString>")
        sb.appendLine("    </Placemark>")

        // 起点标记
        if (points.isNotEmpty()) {
            val start = points.first()
            sb.appendLine("    <Placemark>")
            sb.appendLine("      <name>起点</name>")
            sb.appendLine("      <Point>")
            sb.appendLine("        <coordinates>${start.longitude},${start.latitude},${start.altitude}</coordinates>")
            sb.appendLine("      </Point>")
            sb.appendLine("    </Placemark>")
        }

        // 终点标记
        if (points.size > 1) {
            val end = points.last()
            sb.appendLine("    <Placemark>")
            sb.appendLine("      <name>终点</name>")
            sb.appendLine("      <Point>")
            sb.appendLine("        <coordinates>${end.longitude},${end.latitude},${end.altitude}</coordinates>")
            sb.appendLine("      </Point>")
            sb.appendLine("    </Placemark>")
        }

        sb.appendLine("  </Document>")
        sb.appendLine("</kml>")

        return sb.toString()
    }

    /**
     * 保存 KML 文件到公共 Downloads 目录，返回 Uri。
     */
    fun saveToFile(context: Context, track: Track, points: List<TrackPoint>): Uri? {
        val kmlContent = export(track, points)
        val fileName = buildFileName(track)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.google-earth.kml+xml")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(kmlContent.toByteArray(Charsets.UTF_8))
        }

        return uri
    }

    private fun buildFileName(track: Track): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        val dateStr = sdf.format(Date(track.startTime))
        return "GPS_轨迹_${dateStr}.kml"
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
}
