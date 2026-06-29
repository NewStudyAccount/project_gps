package com.gps.dashboard.backtrack

import com.gps.dashboard.ui.component.TrackProjection

data class BacktrackState(
    val status: Status = Status.TRACKING,
    val nearestSegmentIndex: Int = 0,
    val projectionPoint: TrackProjection.LatLng? = null,
    val distanceToTrack: Float = 0f,        // 米
    val nextTargetIndex: Int = 0,
    val nextTargetDistance: Float = 0f,       // 米
    val turnDirection: TurnDirection = TurnDirection.STRAIGHT,
    val turnDistance: Float = 0f,             // 米 (距转弯点)
    val remainingDistance: Float = 0f,        // 米
    val progress: Float = 0f,                 // 0.0 ~ 1.0
)

enum class Status {
    TRACKING,   // 正常回溯中
    DEVIATED,   // 偏离轨迹
    COMPLETE,   // 到达终点
}

enum class TurnDirection(val label: String, val arrow: String) {
    STRAIGHT("继续直行", "↑"),
    LEFT("左转", "←"),
    RIGHT("右转", "→"),
    U_TURN("掉头", "↻"),
}
