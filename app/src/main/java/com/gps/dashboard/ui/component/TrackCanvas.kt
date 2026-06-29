package com.gps.dashboard.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.detectTransformGestures
import com.gps.dashboard.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canvas 自绘轨迹地图组件。
 *
 * 绘制层次:
 * 1. 背景
 * 2. 网格点
 * 3. 轨迹线
 * 4. 起点/终点标记
 * 5. 当前位置
 */
@Composable
fun TrackCanvas(
    points: List<TrackProjection.LatLng>,
    currentPosition: TrackProjection.LatLng? = null,
    currentBearing: Float = 0f,
    highlightedIndex: Int = -1,  // 回放时高亮到哪个点
    backtrackSegments: Pair<Int, Int>? = null,  // 回溯时 (已走段, 总段数)
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "position_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_radius",
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 20f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        val projection = TrackProjection(
            points = points,
            canvasWidth = size.width,
            canvasHeight = size.height,
        )

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scale, scale, pivot = Offset(size.width / 2, size.height / 2))
        }) {
            // Layer 1: 背景
            drawRect(Background)

            // Layer 2: 网格点
            drawGrid()

            // Layer 3: 轨迹线
            if (points.size >= 2) {
                drawTrackLine(points, projection, highlightedIndex, backtrackSegments)
            }

            // Layer 4: 起点/终点标记
            if (points.isNotEmpty()) {
                val startScreen = projection.toScreen(points.first().latitude, points.first().longitude)
                drawCircle(StatusGood, radius = 6f, center = startScreen)

                if (points.size > 1) {
                    val endScreen = projection.toScreen(points.last().latitude, points.last().longitude)
                    drawCircle(StatusBad, radius = 6f, center = endScreen)
                }
            }

            // Layer 5: 当前位置
            if (currentPosition != null) {
                val posScreen = projection.toScreen(currentPosition.latitude, currentPosition.longitude)
                drawCircle(Primary.copy(alpha = 0.3f), radius = pulseRadius, center = posScreen)
                drawCircle(Primary, radius = 5f, center = posScreen)

                // 方向指针
                if (currentBearing != 0f) {
                    drawBearingArrow(posScreen, currentBearing)
                }
            }

            // 高亮当前回放点
            if (highlightedIndex in points.indices) {
                val hlScreen = projection.toScreen(
                    points[highlightedIndex].latitude,
                    points[highlightedIndex].longitude
                )
                drawCircle(Primary, radius = 8f, center = hlScreen)
                drawCircle(Color.White, radius = 4f, center = hlScreen)
            }
        }
    }
}

private fun DrawScope.drawGrid() {
    val gridSpacing = 50f
    val gridColor = Border.copy(alpha = 0.3f)

    var x = 0f
    while (x < size.width) {
        drawCircle(gridColor, radius = 1f, center = Offset(x, 0f))
        x += gridSpacing
    }

    var y = 0f
    while (y < size.height) {
        drawCircle(gridColor, radius = 1f, center = Offset(0f, y))
        y += gridSpacing
    }
}

private fun DrawScope.drawTrackLine(
    points: List<TrackProjection.LatLng>,
    projection: TrackProjection,
    highlightedIndex: Int,
    backtrackSegments: Pair<Int, Int>?,
) {
    val path = Path()
    val first = projection.toScreen(points.first().latitude, points.first().longitude)
    path.moveTo(first.x, first.y)

    for (i in 1 until points.size) {
        val screen = projection.toScreen(points[i].latitude, points[i].longitude)
        path.lineTo(screen.x, screen.y)
    }

    // 绘制轨迹线
    val trackColor = if (backtrackSegments != null) {
        // 回溯模式: 已走灰色，未走琥珀橙
        Primary
    } else {
        Primary
    }

    drawPath(
        path = path,
        color = trackColor,
        style = Stroke(
            width = 3f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.drawBearingArrow(center: Offset, bearing: Float) {
    val arrowLength = 20f
    val angleRad = Math.toRadians(bearing.toDouble() - 90.0) // -90 因为 Canvas 0° 是右方
    val endX = center.x + (arrowLength * cos(angleRad)).toFloat()
    val endY = center.y + (arrowLength * sin(angleRad)).toFloat()

    drawLine(
        color = Primary,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
}
