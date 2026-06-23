package com.gps.dashboard.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps.dashboard.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CompassView(
    heading: Float,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val animatedHeading = remember { Animatable(0f) }
    var currentTarget by remember { mutableFloatStateOf(0f) }

    // 处理角度环绕
    LaunchedEffect(heading) {
        val diff = ((heading - currentTarget + 540) % 360) - 180
        currentTarget += diff
        scope.launch {
            animatedHeading.animateTo(
                targetValue = currentTarget,
                animationSpec = spring(
                    stiffness = 100f,
                    dampingRatio = 0.7f,
                )
            )
        }
    }

    val displayHeading = ((animatedHeading.value % 360) + 360) % 360

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width * 0.45f
            val innerRadius = size.width * 0.30f

            // 旋转画布
            rotate(-animatedHeading.value, center) {
                // 外圈刻度环
                for (i in 0 until 360 step 5) {
                    val angleRad = Math.toRadians(i.toDouble() - 90).toFloat()
                    val isMain = i % 30 == 0
                    val isSub = i % 10 == 0

                    val lineLength = when {
                        isMain -> 12.dp.toPx()
                        isSub -> 8.dp.toPx()
                        else -> 5.dp.toPx()
                    }
                    val lineWidth = when {
                        isMain -> 2.dp.toPx()
                        isSub -> 1.5.dp.toPx()
                        else -> 1.dp.toPx()
                    }
                    val color = when {
                        isMain -> TextPrimary
                        isSub -> TextDim
                        else -> Border
                    }

                    val start = Offset(
                        center.x + (outerRadius - lineLength) * kotlin.math.cos(angleRad),
                        center.y + (outerRadius - lineLength) * kotlin.math.sin(angleRad)
                    )
                    val end = Offset(
                        center.x + outerRadius * kotlin.math.cos(angleRad),
                        center.y + outerRadius * kotlin.math.sin(angleRad)
                    )

                    drawLine(color, start, end, lineWidth)
                }

                // 方位字母
                val directions = listOf(
                    0 to "N", 45 to "NE", 90 to "E", 135 to "SE",
                    180 to "S", 225 to "SW", 270 to "W", 315 to "NW"
                )
                for ((deg, label) in directions) {
                    val angleRad = Math.toRadians(deg.toDouble() - 90).toFloat()
                    val textRadius = outerRadius - 24.dp.toPx()
                    val x = center.x + textRadius * kotlin.math.cos(angleRad)
                    val y = center.y + textRadius * kotlin.math.sin(angleRad)

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            this.color = if (label == "N") Primary.toArgb() else Accent.toArgb()
                            textSize = if (label == "N") 18.sp.toPx() else 14.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.MONOSPACE
                            isFakeBoldText = label == "N"
                        }
                        drawText(label, x, y + 6.dp.toPx(), paint)
                    }
                }

                // 内圈装饰
                drawCircle(
                    color = Border,
                    radius = innerRadius,
                    center = center,
                    style = Stroke(1.dp.toPx())
                )
            }

            // 中心指针 (固定朝上 = 北方)
            val pointerPath = Path().apply {
                moveTo(center.x, center.y - 20.dp.toPx())
                lineTo(center.x - 8.dp.toPx(), center.y + 4.dp.toPx())
                lineTo(center.x + 8.dp.toPx(), center.y + 4.dp.toPx())
                close()
            }
            drawPath(pointerPath, Primary)

            // 发光效果
            drawCircle(
                color = Primary.copy(alpha = 0.2f),
                radius = 10.dp.toPx(),
                center = center
            )

            // 中心点
            drawCircle(
                color = Primary,
                radius = 4.dp.toPx(),
                center = center
            )
        }

        // 度数读数
        val directionLabel = when {
            displayHeading < 22.5f || displayHeading >= 337.5f -> "N"
            displayHeading < 67.5f -> "NE"
            displayHeading < 112.5f -> "E"
            displayHeading < 157.5f -> "SE"
            displayHeading < 202.5f -> "S"
            displayHeading < 247.5f -> "SW"
            displayHeading < 292.5f -> "W"
            else -> "NW"
        }
        Text(
            text = "%.0f° %s".format(displayHeading, directionLabel),
            style = CompassDegree,
            textAlign = TextAlign.Center,
        )
    }
}
