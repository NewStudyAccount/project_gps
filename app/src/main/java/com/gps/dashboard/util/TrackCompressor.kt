package com.gps.dashboard.util

import com.gps.dashboard.data.model.TrackPoint
import kotlin.math.*

/**
 * Ramer-Douglas-Peucker 轨迹降采样算法。
 *
 * 保留"形状关键点"，去掉"近似直线上的中间点"。
 * 使用 Haversine 公式计算真实米制距离。
 */
object TrackCompressor {

    /**
     * 对轨迹点列表执行 RDP 降采样。
     *
     * @param points 原始轨迹点 (按时间排序)
     * @param epsilon 最大允许偏差 (米), 默认 5m
     * @return 保留的关键点索引列表
     */
    fun simplify(points: List<TrackPoint>, epsilon: Double = 5.0): List<Int> {
        if (points.size <= 2) return points.indices.toList()

        val keep = mutableSetOf<Int>()
        rdpRecursive(points, 0, points.size - 1, epsilon, keep)
        return keep.sorted()
    }

    private fun rdpRecursive(
        points: List<TrackPoint>,
        start: Int,
        end: Int,
        epsilon: Double,
        keep: MutableSet<Int>,
    ) {
        if (end - start <= 1) {
            keep.add(start)
            keep.add(end)
            return
        }

        var maxDist = 0.0
        var maxIndex = start

        for (i in start + 1 until end) {
            val dist = perpendicularDistance(points[i], points[start], points[end])
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        if (maxDist > epsilon) {
            rdpRecursive(points, start, maxIndex, epsilon, keep)
            rdpRecursive(points, maxIndex, end, epsilon, keep)
        } else {
            keep.add(start)
            keep.add(end)
        }
    }

    /**
     * 计算点到线段的垂直距离 (Haversine 公式计算真实米制距离)。
     */
    private fun perpendicularDistance(
        point: TrackPoint,
        lineStart: TrackPoint,
        lineEnd: TrackPoint,
    ): Double {
        // 将经纬度转换为米 (局部近似)
        val latToM = 111_319.9
        val lngToM = 111_319.9 * cos(Math.toRadians(point.latitude))

        val px = (point.longitude - lineStart.longitude) * lngToM
        val py = (point.latitude - lineStart.latitude) * latToM
        val bx = (lineEnd.longitude - lineStart.longitude) * lngToM
        val by = (lineEnd.latitude - lineStart.latitude) * latToM

        val lineLengthSq = bx * bx + by * by
        if (lineLengthSq == 0.0) {
            return sqrt(px * px + py * py)
        }

        val t = ((px * bx + py * by) / lineLengthSq).coerceIn(0.0, 1.0)
        val projX = t * bx
        val projY = t * by

        return sqrt((px - projX).pow(2) + (py - projY).pow(2))
    }
}
