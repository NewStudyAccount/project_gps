package com.gps.dashboard.ui.component

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos

/**
 * GPS 坐标到屏幕像素的投影转换。
 *
 * 使用简化的线性投影，适合小范围轨迹（几公里内）。
 * 自动计算 bounding box 并居中显示。
 */
class TrackProjection(
    private val points: List<LatLng>,
    private val canvasWidth: Float,
    private val canvasHeight: Float,
    private val padding: Float = 40f,
) {
    data class LatLng(val latitude: Double, val longitude: Double)

    private val centerLat: Double
    private val centerLng: Double
    private val scale: Float

    init {
        if (points.isEmpty()) {
            centerLat = 0.0
            centerLng = 0.0
            scale = 1f
        } else {
            val lats = points.map { it.latitude }
            val lngs = points.map { it.longitude }
            val latSpan = lats.max() - lats.min()
            val lngSpan = lngs.max() - lngs.min()

            centerLat = (lats.max() + lats.min()) / 2
            centerLng = (lngs.max() + lngs.min()) / 2

            // 在当前纬度下，1°经度的实际距离
            val lngFactor = cos(Math.toRadians(centerLat))
            val effectiveLngSpan = lngSpan * lngFactor

            // 取较大维度来填满画布
            val maxSpan = maxOf(latSpan, effectiveLngSpan).coerceAtLeast(0.0001)
            val availableSize = minOf(canvasWidth, canvasHeight) - padding * 2
            scale = (availableSize / maxSpan).toFloat()
        }
    }

    /**
     * 将 GPS 坐标转换为屏幕坐标。
     */
    fun toScreen(lat: Double, lng: Double): Offset {
        val lngFactor = cos(Math.toRadians(centerLat))
        val x = ((lng - centerLng) * scale * lngFactor + canvasWidth / 2).toFloat()
        val y = ((centerLat - lat) * scale + canvasHeight / 2).toFloat()
        return Offset(x, y)
    }

    /**
     * 将屏幕坐标转换回 GPS 坐标（用于点击检测）。
     */
    fun toLatLng(screenX: Float, screenY: Float): LatLng {
        val lngFactor = cos(Math.toRadians(centerLat))
        val lng = (screenX - canvasWidth / 2) / (scale * lngFactor) + centerLng
        val lat = centerLat - (screenY - canvasHeight / 2) / scale
        return LatLng(lat, lng)
    }

    /**
     * 获取轨迹的边界范围（米）。
     */
    fun getBoundsInMeters(): Pair<Float, Float> {
        if (points.size < 2) return Pair(0f, 0f)
        val lats = points.map { it.latitude }
        val lngs = points.map { it.longitude }
        val latSpan = (lats.max() - lats.min()) * 111_319.9
        val lngSpan = (lngs.max() - lngs.min()) * 111_319.9 * cos(Math.toRadians(centerLat))
        return Pair(latSpan.toFloat(), lngSpan.toFloat())
    }
}
