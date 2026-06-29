package com.gps.dashboard.backtrack

import com.gps.dashboard.ui.component.TrackProjection
import kotlin.math.*

/**
 * 回溯引擎：沿已录制轨迹反向导航的核心算法。
 *
 * 算法流程:
 * 1. 找最近轨迹段
 * 2. 计算投影点
 * 3. 确定回溯目标
 * 4. 计算转向方向
 * 5. 偏离检测
 * 6. 完成检测
 */
class BacktrackEngine(private val trackPoints: List<TrackProjection.LatLng>) {

    companion object {
        private const val DEVIATION_WARNING = 30f   // 接近偏离 (米)
        private const val DEVIATION_THRESHOLD = 50f // 偏离阈值 (米)
        private const val ARRIVAL_THRESHOLD = 20f   // 到达终点阈值 (米)
        private const val TURN_ANGLE_THRESHOLD = 30f // 转向角度阈值 (度)
    }

    // 预计算的经纬度到米的转换因子
    private val latToM = 111_319.9
    private val lngToM = if (trackPoints.isNotEmpty()) {
        111_319.9 * cos(Math.toRadians(trackPoints.first().latitude))
    } else {
        111_319.9
    }

    /**
     * 根据用户当前位置更新回溯状态。
     */
    fun update(userLat: Double, userLng: Double): BacktrackState {
        if (trackPoints.size < 2) {
            return BacktrackState(status = Status.COMPLETE)
        }

        // 1. 找最近轨迹段
        val nearest = findNearestSegment(userLat, userLng)

        // 2. 计算投影点
        val projection = projectOnSegment(
            userLat, userLng,
            trackPoints[nearest.segmentIndex],
            trackPoints[nearest.segmentIndex + 1]
        )

        // 3. 确定回溯方向和目标
        val (nextTarget, remainingDist) = calculateBacktrackTarget(nearest.segmentIndex, projection)

        // 4. 计算转向
        val turn = calculateTurn(nextTarget)

        // 5. 偏离检测
        val distanceToTrack = nearest.distance
        val status = when {
            isArrived(userLat, userLng) -> Status.COMPLETE
            distanceToTrack > DEVIATION_THRESHOLD -> Status.DEVIATED
            else -> Status.TRACKING
        }

        // 6. 进度计算
        val totalLength = calculateTotalLength()
        val walkedLength = calculateWalkedLength(nearest.segmentIndex, projection)
        val progress = if (totalLength > 0) (walkedLength / totalLength).coerceIn(0f, 1f) else 0f

        return BacktrackState(
            status = status,
            nearestSegmentIndex = nearest.segmentIndex,
            projectionPoint = projection,
            distanceToTrack = distanceToTrack,
            nextTargetIndex = nextTarget,
            nextTargetDistance = calculateDistance(
                userLat, userLng,
                trackPoints[nextTarget].latitude, trackPoints[nextTarget].longitude
            ),
            turnDirection = turn.direction,
            turnDistance = turn.distance,
            remainingDistance = remainingDist,
            progress = progress,
        )
    }

    /**
     * 找到离用户最近的轨迹段。
     */
    private fun findNearestSegment(userLat: Double, userLng: Double): NearestSegment {
        var minDist = Float.MAX_VALUE
        var minIndex = 0

        for (i in 0 until trackPoints.size - 1) {
            val dist = pointToSegmentDistance(
                userLat, userLng,
                trackPoints[i].latitude, trackPoints[i].longitude,
                trackPoints[i + 1].latitude, trackPoints[i + 1].longitude
            )
            if (dist < minDist) {
                minDist = dist
                minIndex = i
            }
        }

        return NearestSegment(minIndex, minDist)
    }

    /**
     * 计算点在线段上的投影点。
     */
    private fun projectOnSegment(
        userLat: Double, userLng: Double,
        segStart: TrackProjection.LatLng,
        segEnd: TrackProjection.LatLng,
    ): TrackProjection.LatLng {
        val dx = (segEnd.longitude - segStart.longitude) * lngToM
        val dy = (segEnd.latitude - segStart.latitude) * latToM
        val px = (userLng - segStart.longitude) * lngToM
        val py = (userLat - segStart.latitude) * latToM

        val lineLengthSq = dx * dx + dy * dy
        if (lineLengthSq == 0.0) return segStart

        val t = ((px * dx + py * dy) / lineLengthSq).coerceIn(0.0, 1.0)
        return TrackProjection.LatLng(
            segStart.latitude + t * (segEnd.latitude - segStart.latitude),
            segStart.longitude + t * (segEnd.longitude - segStart.longitude)
        )
    }

    /**
     * 计算回溯目标点和剩余距离。
     * 回溯方向：沿轨迹反向行走。
     */
    private fun calculateBacktrackTarget(
        nearestIndex: Int,
        projection: TrackProjection.LatLng,
    ): Pair<Int, Float> {
        // 计算从最近段到起点的剩余距离
        var remaining = calculateDistance(
            projection.latitude, projection.longitude,
            trackPoints[nearestIndex + 1].latitude, trackPoints[nearestIndex + 1].longitude
        )
        for (i in nearestIndex + 1 until trackPoints.size - 1) {
            remaining += calculateDistance(
                trackPoints[i].latitude, trackPoints[i].longitude,
                trackPoints[i + 1].latitude, trackPoints[i + 1].longitude
            )
        }

        // 回溯目标：走向当前段的起点（反向）
        // 如果投影点更靠近段起点，目标就是 nearestIndex
        // 否则目标是 nearestIndex + 1
        val distToStart = calculateDistance(
            projection.latitude, projection.longitude,
            trackPoints[nearestIndex].latitude, trackPoints[nearestIndex].longitude
        )
        val distToEnd = calculateDistance(
            projection.latitude, projection.longitude,
            trackPoints[nearestIndex + 1].latitude, trackPoints[nearestIndex + 1].longitude
        )

        val target = if (distToStart < distToEnd) nearestIndex else nearestIndex + 1
        return Pair(target, remaining)
    }

    /**
     * 计算转向方向。
     */
    private fun calculateTurn(targetIndex: Int): TurnInfo {
        if (targetIndex <= 0 || targetIndex >= trackPoints.size - 1) {
            return TurnInfo(TurnDirection.STRAIGHT, 0f)
        }

        val prev = trackPoints[targetIndex - 1]
        val current = trackPoints[targetIndex]
        val next = trackPoints[targetIndex + 1]

        // 计算两段的方位角
        val bearing1 = calculateBearing(prev, current)
        val bearing2 = calculateBearing(current, next)

        // 计算角度差
        var angleDiff = bearing2 - bearing1
        if (angleDiff > 180) angleDiff -= 360
        if (angleDiff < -180) angleDiff += 360

        val direction = when {
            abs(angleDiff) < TURN_ANGLE_THRESHOLD -> TurnDirection.STRAIGHT
            angleDiff > 0 -> TurnDirection.RIGHT
            else -> TurnDirection.LEFT
        }

        return TurnInfo(direction, 0f)
    }

    /**
     * 计算两点间的方位角（度）。
     */
    private fun calculateBearing(from: TrackProjection.LatLng, to: TrackProjection.LatLng): Double {
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /**
     * 判断是否到达终点。
     */
    private fun isArrived(userLat: Double, userLng: Double): Boolean {
        val startPoint = trackPoints.first()
        val dist = calculateDistance(userLat, userLng, startPoint.latitude, startPoint.longitude)
        return dist < ARRIVAL_THRESHOLD
    }

    /**
     * 计算点到线段的距离（米）。
     */
    private fun pointToSegmentDistance(
        px: Double, py: Double,
        ax: Double, ay: Double,
        bx: Double, by: Double,
    ): Float {
        val dx = (bx - ax) * lngToM
        val dy = (by - ay) * latToM
        val ppx = (px - ax) * lngToM
        val ppy = (py - ay) * latToM

        val lineLengthSq = dx * dx + dy * dy
        if (lineLengthSq == 0.0) return sqrt(ppx * ppx + ppy * ppy).toFloat()

        val t = ((ppx * dx + ppy * dy) / lineLengthSq).coerceIn(0.0, 1.0)
        val projX = t * dx
        val projY = t * dy

        return sqrt((ppx - projX).pow(2) + (ppy - projY).pow(2)).toFloat()
    }

    /**
     * 计算两点间距离（米）。
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val dx = (lng2 - lng1) * lngToM
        val dy = (lat2 - lat1) * latToM
        return sqrt(dx * dx + dy * dy).toFloat()
    }

    /**
     * 计算轨迹总长度。
     */
    private fun calculateTotalLength(): Float {
        var total = 0f
        for (i in 0 until trackPoints.size - 1) {
            total += calculateDistance(
                trackPoints[i].latitude, trackPoints[i].longitude,
                trackPoints[i + 1].latitude, trackPoints[i + 1].longitude
            )
        }
        return total
    }

    /**
     * 计算已走过的长度。
     */
    private fun calculateWalkedLength(nearestIndex: Int, projection: TrackProjection.LatLng): Float {
        var walked = 0f
        // 从起点到最近段的投影点
        for (i in 0 until nearestIndex) {
            walked += calculateDistance(
                trackPoints[i].latitude, trackPoints[i].longitude,
                trackPoints[i + 1].latitude, trackPoints[i + 1].longitude
            )
        }
        // 加上最近段起点到投影点的距离
        walked += calculateDistance(
            trackPoints[nearestIndex].latitude, trackPoints[nearestIndex].longitude,
            projection.latitude, projection.longitude
        )
        return walked
    }

    private data class NearestSegment(
        val segmentIndex: Int,
        val distance: Float,
    )

    private data class TurnInfo(
        val direction: TurnDirection,
        val distance: Float,
    )
}
