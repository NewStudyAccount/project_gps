package com.gps.dashboard.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps.dashboard.data.model.Constellation
import com.gps.dashboard.data.model.SatelliteInfo
import com.gps.dashboard.ui.theme.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 天球图 Canvas 组件
 *
 * 等距方位投影：圆心=天顶(elevation=90°)，圆边=地平线(elevation=0°)
 * azimuth → 角度（0°=N, 顺时针）
 * elevation → 距圆心距离
 */
@Composable
fun SkyPlotCanvas(
    satellites: List<SatelliteInfo>,
    inUseCount: Int,
    averageCn0: Float,
    constellationStats: Map<Constellation, Int>,
    constellationTotalStats: Map<Constellation, Int>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = min(canvasWidth, canvasHeight) / 2f
            val cx = canvasWidth / 2f
            val cy = canvasHeight / 2f

            // 1. 绘制天球图外圆
            drawCircle(
                color = Border,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. 绘制仰角参考圆（30°、60°）
            drawElevationCircles(cx, cy, radius)

            // 3. 绘制方位刻度线和标签
            drawAzimuthLabels(cx, cy, radius, density)

            // 4. 绘制线框地球（中心 25% 半径）
            drawWireframeGlobe(cx, cy, radius * 0.25f)

            // 5. 绘制卫星点
            drawSatellites(satellites, cx, cy, radius)

            // 6. 绘制左上角图例
            drawLegend(constellationStats, constellationTotalStats, density)

            // 7. 绘制底部汇总
            drawSummary(inUseCount, satellites.size, averageCn0, density)
        }
    }
}

/**
 * 绘制仰角参考圆（30° 和 60°）
 */
private fun DrawScope.drawElevationCircles(cx: Float, cy: Float, radius: Float) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))

    // 60° 仰角圆（距圆心 1/3 半径）
    drawCircle(
        color = Border.copy(alpha = 0.3f),
        radius = radius * (90f - 60f) / 90f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.dp.toPx(), pathEffect = dashEffect)
    )

    // 30° 仰角圆（距圆心 2/3 半径）
    drawCircle(
        color = Border.copy(alpha = 0.3f),
        radius = radius * (90f - 30f) / 90f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.dp.toPx(), pathEffect = dashEffect)
    )
}

/**
 * 绘制方位刻度线和 N/E/S/W 标签
 */
private fun DrawScope.drawAzimuthLabels(
    cx: Float,
    cy: Float,
    radius: Float,
    density: androidx.compose.ui.unit.Density,
) {
    val labels = listOf(
        "N" to 0f,
        "E" to 90f,
        "S" to 180f,
        "W" to 270f,
    )

    val labelRadius = radius + 12.dp.toPx()
    val tickLength = 6.dp.toPx()

    for ((label, azimuthDeg) in labels) {
        val azimuthRad = Math.toRadians(azimuthDeg.toDouble())

        // 刻度线
        val innerX = cx + (radius - tickLength) * sin(azimuthRad).toFloat()
        val innerY = cy - (radius - tickLength) * cos(azimuthRad).toFloat()
        val outerX = cx + radius * sin(azimuthRad).toFloat()
        val outerY = cy - radius * cos(azimuthRad).toFloat()

        drawLine(
            color = TextDim,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 1.dp.toPx()
        )

        // 标签
        val labelX = cx + labelRadius * sin(azimuthRad).toFloat()
        val labelY = cy - labelRadius * cos(azimuthRad).toFloat()

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = TextDim.hashCode()
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.MONOSPACE
                isAntiAlias = true
            }
            drawText(label, labelX, labelY + 4.dp.toPx(), paint)
        }
    }
}

/**
 * 绘制线框地球（经纬线网格）
 */
private fun DrawScope.drawWireframeGlobe(cx: Float, cy: Float, globeRadius: Float) {
    val lineColor = TextDim.copy(alpha = 0.3f)
    val strokeWidth = 0.5.dp.toPx()

    // 外圆
    drawCircle(
        color = lineColor,
        radius = globeRadius,
        center = Offset(cx, cy),
        style = Stroke(width = strokeWidth)
    )

    // 经线（每隔 30°，共 6 条，每条是半个椭圆）
    for (i in 0 until 6) {
        val angleDeg = i * 30f
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val ellipseWidth = globeRadius * cos(angleRad).toFloat()

        // 画椭圆弧（经线）
        val path = androidx.compose.ui.graphics.Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    cx - kotlin.math.abs(ellipseWidth),
                    cy - globeRadius,
                    cx + kotlin.math.abs(ellipseWidth),
                    cy + globeRadius
                )
            )
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = strokeWidth)
        )
    }

    // 纬线（每隔 30°：±60°、±30°、赤道）
    val latitudes = listOf(-60f, -30f, 0f, 30f, 60f)
    for (lat in latitudes) {
        val latRad = Math.toRadians(lat.toDouble())
        val r = globeRadius * cos(latRad).toFloat()
        val yOffset = -globeRadius * sin(latRad).toFloat()

        if (lat == 0f) {
            // 赤道用实线，稍粗
            drawLine(
                color = TextDim.copy(alpha = 0.4f),
                start = Offset(cx - globeRadius, cy + yOffset),
                end = Offset(cx + globeRadius, cy + yOffset),
                strokeWidth = strokeWidth
            )
        } else {
            // 其他纬线用虚线
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            drawLine(
                color = lineColor,
                start = Offset(cx - r, cy + yOffset),
                end = Offset(cx + r, cy + yOffset),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )
        }
    }
}

/**
 * 绘制卫星点
 */
private fun DrawScope.drawSatellites(
    satellites: List<SatelliteInfo>,
    cx: Float,
    cy: Float,
    skyRadius: Float,
) {
    for (sat in satellites) {
        val (x, y) = projectSatellite(sat, cx, cy, skyRadius)

        // CN0 → 圆点大小映射
        val dotRadius = cn0ToRadius(sat.cn0).dp.toPx()

        if (sat.inUse) {
            // 实心圆
            drawCircle(
                color = sat.constellation.color,
                radius = dotRadius,
                center = Offset(x, y)
            )
        } else {
            // 空心圆（仅描边）
            drawCircle(
                color = sat.constellation.color.copy(alpha = 0.4f),
                radius = dotRadius,
                center = Offset(x, y),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/**
 * 等距方位投影：将卫星的 azimuth/elevation 映射到屏幕坐标
 *
 * @return Pair<screenX, screenY>
 */
private fun projectSatellite(
    sat: SatelliteInfo,
    cx: Float,
    cy: Float,
    skyRadius: Float,
): Pair<Float, Float> {
    // elevation 90° = 圆心，0° = 圆边
    val r = skyRadius * (90f - sat.elevation.coerceIn(0f, 90f)) / 90f
    val azimuthRad = Math.toRadians(sat.azimuth.toDouble())

    val x = cx + r * sin(azimuthRad).toFloat()
    val y = cy - r * cos(azimuthRad).toFloat()

    return x to y
}

/**
 * CN0 → 圆点半径映射（单位 dp）
 */
private fun cn0ToRadius(cn0: Float): Float = when {
    cn0 < 15f -> 4f
    cn0 < 25f -> 6f
    cn0 < 35f -> 8f
    cn0 < 45f -> 10f
    else -> 12f
}

/**
 * 绘制左上角图例面板
 */
private fun DrawScope.drawLegend(
    constellationStats: Map<Constellation, Int>,
    constellationTotalStats: Map<Constellation, Int>,
    density: androidx.compose.ui.unit.Density,
) {
    // 只显示有卫星的星座
    val entries = constellationTotalStats.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }

    if (entries.isEmpty()) return

    val panelX = 0f
    val panelY = 0f
    val lineHeight = with(density) { 18.dp.toPx() }
    val padding = with(density) { 8.dp.toPx() }
    val colorBlockSize = with(density) { 8.dp.toPx() }
    val panelWidth = with(density) { 90.dp.toPx() }
    val panelHeight = padding * 2 + entries.size * lineHeight

    // 背景
    drawRoundRect(
        color = Surface.copy(alpha = 0.85f),
        topLeft = Offset(panelX, panelY),
        size = androidx.compose.ui.geometry.Size(panelWidth, panelHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
    )

    // 边框
    drawRoundRect(
        color = Border,
        topLeft = Offset(panelX, panelY),
        size = androidx.compose.ui.geometry.Size(panelWidth, panelHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )

    // 文字
    val textPaint = android.graphics.Paint().apply {
        textSize = with(density) { 11.sp.toPx() }
        typeface = android.graphics.Typeface.MONOSPACE
        isAntiAlias = true
    }

    for ((index, entry) in entries.withIndex()) {
        val y = panelY + padding + index * lineHeight + lineHeight * 0.7f

        // 色块
        drawCircle(
            color = entry.key.color,
            radius = colorBlockSize / 2,
            center = Offset(panelX + padding + colorBlockSize / 2, y - 3.dp.toPx())
        )

        // 星座名称 + 已用/总数
        val usedCount = constellationStats[entry.key] ?: 0
        val label = "${entry.key.label}  $usedCount/${entry.value}"

        textPaint.color = TextPrimary.hashCode()
        drawContext.canvas.nativeCanvas.drawText(
            label,
            panelX + padding + colorBlockSize + 6.dp.toPx(),
            y,
            textPaint
        )
    }
}

/**
 * 绘制底部汇总栏
 */
private fun DrawScope.drawSummary(
    inUseCount: Int,
    totalCount: Int,
    averageCn0: Float,
    density: androidx.compose.ui.unit.Density,
) {
    val summaryY = size.height - with(density) { 24.dp.toPx() }
    val summaryText = "▸ $inUseCount/$totalCount  CN0 ${"%.0f".format(averageCn0)}dB"

    val textPaint = android.graphics.Paint().apply {
        color = TextDim.hashCode()
        textSize = with(density) { 11.sp.toPx() }
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.MONOSPACE
        isAntiAlias = true
    }

    drawContext.canvas.nativeCanvas.drawText(
        summaryText,
        size.width / 2f,
        summaryY,
        textPaint
    )
}
