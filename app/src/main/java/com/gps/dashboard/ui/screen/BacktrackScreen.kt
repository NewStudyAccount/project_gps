package com.gps.dashboard.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gps.dashboard.backtrack.BacktrackState
import com.gps.dashboard.backtrack.Status
import com.gps.dashboard.backtrack.TurnDirection
import com.gps.dashboard.ui.component.TrackCanvas
import com.gps.dashboard.ui.theme.*
import com.gps.dashboard.ui.viewmodel.BacktrackViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BacktrackScreen(
    trackId: Long,
    viewModel: BacktrackViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    LaunchedEffect(trackId) {
        viewModel.loadTrack(trackId)
    }

    val latLngPoints by viewModel.latLngPoints.collectAsState()
    val backtrackState by viewModel.backtrackState.collectAsState()
    val isBacktracking by viewModel.isBacktracking.collectAsState()
    var showMap by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "◀",
                fontSize = 18.sp,
                color = Primary,
                fontFamily = MonoFontFamily,
                modifier = Modifier.clickable {
                    viewModel.stopBacktrack()
                    onBack()
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isBacktracking) "回溯中" else "轨迹回溯",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBacktracking) StatusGood else Primary,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (showMap) "切换指引" else "切换地图",
                fontSize = 12.sp,
                color = Accent,
                fontFamily = MonoFontFamily,
                modifier = Modifier.clickable { showMap = !showMap }
            )
        }

        if (!isBacktracking) {
            // 未开始回溯 - 显示开始按钮
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "↩",
                    fontSize = 64.sp,
                    color = Primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "沿原路返回",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = MonoFontFamily,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "将引导你沿录制的轨迹原路返回起点",
                    fontSize = 13.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary)
                        .clickable { viewModel.startBacktrack() }
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "开始回溯",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Background,
                        fontFamily = MonoFontFamily,
                    )
                }
            }
        } else if (backtrackState.status == Status.COMPLETE) {
            // 回溯完成
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "✓",
                    fontSize = 64.sp,
                    color = StatusGood,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "已到达起点",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusGood,
                    fontFamily = MonoFontFamily,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "返回",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontFamily = MonoFontFamily,
                    )
                }
            }
        } else if (showMap) {
            // 地图模式
            MapMode(latLngPoints, backtrackState)
        } else {
            // 指引模式
            GuidanceMode(backtrackState)
        }
    }
}

@Composable
private fun GuidanceMode(state: BacktrackState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 状态指示
        val statusColor = when (state.status) {
            Status.TRACKING -> if (state.distanceToTrack > 30f) StatusMedium else StatusGood
            Status.DEVIATED -> StatusBad
            Status.COMPLETE -> StatusGood
        }
        val statusText = when (state.status) {
            Status.TRACKING -> if (state.distanceToTrack > 30f) "接近偏离" else "✓ 在轨迹上"
            Status.DEVIATED -> "⚠️ 偏离轨迹"
            Status.COMPLETE -> "✓ 已到达"
        }

        Text(
            text = statusText,
            fontSize = 14.sp,
            color = statusColor,
            fontFamily = MonoFontFamily,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 大箭头指向目标
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Surface)
                .border(2.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(100.dp)),
            contentAlignment = Alignment.Center,
        ) {
            GuidanceArrow(
                direction = state.turnDirection,
                distance = state.nextTargetDistance,
                statusColor = statusColor,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 转向提示
        Text(
            text = state.turnDirection.arrow,
            fontSize = 48.sp,
            color = Primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.turnDirection.label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "前方 ${formatDistance(state.nextTargetDistance)}",
            fontSize = 14.sp,
            color = TextDim,
            fontFamily = MonoFontFamily,
        )

        Spacer(modifier = Modifier.weight(1f))

        // 偏离提示
        if (state.status == Status.DEVIATED) {
            Text(
                text = "← 返回轨迹 ${formatDistance(state.distanceToTrack)}",
                fontSize = 16.sp,
                color = StatusBad,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 底部统计
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem("剩余距离", formatDistance(state.remainingDistance))
            StatItem("进度", "%.0f%%".format(state.progress * 100))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 停止按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(StatusBad.copy(alpha = 0.1f))
                .clickable { /* 由上层处理 */ }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "结束回溯",
                fontSize = 14.sp,
                color = StatusBad,
                fontFamily = MonoFontFamily,
            )
        }
    }
}

@Composable
private fun GuidanceArrow(
    direction: TurnDirection,
    distance: Float,
    statusColor: Color,
) {
    Canvas(modifier = Modifier.size(120.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val arrowLength = size.width * 0.35f

        // 根据方向旋转箭头
        val angle = when (direction) {
            TurnDirection.STRAIGHT -> -90.0  // 向上
            TurnDirection.LEFT -> -180.0     // 向左
            TurnDirection.RIGHT -> 0.0       // 向右
            TurnDirection.U_TURN -> 90.0     // 向下
        }

        val angleRad = Math.toRadians(angle)
        val endX = center.x + (arrowLength * cos(angleRad)).toFloat()
        val endY = center.y + (arrowLength * sin(angleRad)).toFloat()

        // 箭头线
        drawLine(
            color = statusColor,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 6f,
        )

        // 箭头头部
        val headLength = 20f
        val headAngle1 = Math.toRadians(angle + 150)
        val headAngle2 = Math.toRadians(angle - 150)
        drawLine(
            color = statusColor,
            start = Offset(endX, endY),
            end = Offset(
                endX + (headLength * cos(headAngle1)).toFloat(),
                endY + (headLength * sin(headAngle1)).toFloat()
            ),
            strokeWidth = 4f,
        )
        drawLine(
            color = statusColor,
            start = Offset(endX, endY),
            end = Offset(
                endX + (headLength * cos(headAngle2)).toFloat(),
                endY + (headLength * sin(headAngle2)).toFloat()
            ),
            strokeWidth = 4f,
        )
    }
}

@Composable
private fun MapMode(
    points: List<com.gps.dashboard.ui.component.TrackProjection.LatLng>,
    state: BacktrackState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
    ) {
        TrackCanvas(
            points = points,
            highlightedIndex = state.nextTargetIndex,
            backtrackSegments = Pair(state.nearestSegmentIndex, points.size - 1),
            modifier = Modifier.fillMaxSize(),
        )

        // 底部信息栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Background.copy(alpha = 0.9f))
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("剩余", formatDistance(state.remainingDistance))
                StatItem("下一转弯", formatDistance(state.nextTargetDistance))
                StatItem(
                    "状态",
                    when (state.status) {
                        Status.TRACKING -> "✓ 正常"
                        Status.DEVIATED -> "⚠ 偏离"
                        Status.COMPLETE -> "✓ 到达"
                    }
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextDim,
            fontFamily = MonoFontFamily,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )
    }
}

private fun formatDistance(meters: Float): String {
    return if (meters >= 1000) "%.2f km".format(meters / 1000) else "%.0f m".format(meters)
}
