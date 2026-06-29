package com.gps.dashboard.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps.dashboard.data.model.Constellation
import com.gps.dashboard.data.model.SatelliteInfo
import com.gps.dashboard.ui.theme.*

@Composable
fun SatellitePanel(
    satellites: List<SatelliteInfo>,
    expanded: Boolean,
    inUseCount: Int,
    averageCn0: Float,
    constellationStats: Map<Constellation, Int>,
    constellationTotalStats: Map<Constellation, Int>,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 总览条（保留原有折叠概览栏）
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable { onToggle() }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "卫星 $inUseCount/${satellites.size}",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontFamily = MonoFontFamily,
                )
                Spacer(modifier = Modifier.width(12.dp))

                // 总览信号条
                val fillRatio = if (satellites.isEmpty()) 0f
                else inUseCount.toFloat() / satellites.size

                val barColor = when {
                    averageCn0 >= 35f -> StatusGood
                    averageCn0 >= 20f -> StatusMedium
                    else -> StatusBad
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillRatio.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (expanded) "▴" else "▾",
                    fontSize = 14.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )
            }

            if (!expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "平均 CN0: ${"%.0f".format(averageCn0)} dB  ·  点击展开",
                    fontSize = 11.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )
            }
        }

        // 展开内容：天球图
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(250)) + fadeOut(tween(250)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
            ) {
                SkyPlotCanvas(
                    satellites = satellites,
                    inUseCount = inUseCount,
                    averageCn0 = averageCn0,
                    constellationStats = constellationStats,
                    constellationTotalStats = constellationTotalStats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp)
                        .aspectRatio(1f)
                )

                // 收起提示
                Text(
                    text = "点击收起 ▴",
                    fontSize = 11.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onToggle() },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
