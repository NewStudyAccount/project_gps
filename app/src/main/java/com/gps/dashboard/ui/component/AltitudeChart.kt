package com.gps.dashboard.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps.dashboard.ui.theme.*

@Composable
fun AltitudeChart(
    history: List<Float>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "ALT 60s",
            style = MetaText.copy(fontSize = 10.sp),
            modifier = Modifier.align(androidx.compose.ui.Alignment.End)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            if (history.size < 2) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = TextDim.toArgb()
                        textSize = 12.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    drawText("No data", size.width / 2, size.height / 2 + 4.dp.toPx(), paint)
                }
                return@Canvas
            }

            val minVal = history.min()
            val maxVal = history.max()
            val range = (maxVal - minVal).coerceAtLeast(1f)
            val padding = range * 0.1f
            val yMin = minVal - padding
            val yMax = maxVal + padding
            val yRange = yMax - yMin

            val stepX = size.width / (history.size - 1).toFloat()

            // 网格线
            val gridCount = 3
            for (i in 0..gridCount) {
                val y = size.height * i / gridCount
                drawLine(
                    color = Border,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                )
            }

            // 贝塞尔曲线
            val path = Path()
            val points = history.mapIndexed { index, value ->
                val x = index * stepX
                val y = size.height - ((value - yMin) / yRange) * size.height
                Offset(x, y)
            }

            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val controlX1 = (prev.x + curr.x) / 2
                path.cubicTo(controlX1, prev.y, controlX1, curr.y, curr.x, curr.y)
            }

            // 线条
            drawPath(
                path = path,
                color = Accent,
                style = Stroke(2.dp.toPx())
            )

            // 渐变填充
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Accent.copy(alpha = 0.3f),
                        Accent.copy(alpha = 0.0f),
                    ),
                    startY = 0f,
                    endY = size.height,
                )
            )

            // Y 轴标签
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = TextDim.toArgb()
                    textSize = 9.sp.toPx()
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                drawText("%.0fm".format(yMax), 2f, 10.dp.toPx(), paint)
                drawText("%.0fm".format(yMin), 2f, size.height - 2.dp.toPx(), paint)
            }
        }
    }
}
