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
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 总览条
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
                    text = "SAT $inUseCount/${satellites.size}",
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
                    text = "avg CN0: ${"%.0f".format(averageCn0)} dB  ·  tap to expand",
                    fontSize = 11.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )
            }
        }

        // 展开内容
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
                    .padding(12.dp)
            ) {
                // 卫星网格
                val columns = 6
                val rows = satellites.chunked(columns)
                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (sat in row) {
                            SatelliteCell(sat, Modifier.weight(1f))
                        }
                        // 填充空位
                        repeat(columns - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 底部统计
                Text(
                    text = constellationStats.entries.joinToString("  ") { (const, count) ->
                        "${const.label}: $count"
                    },
                    fontSize = 12.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )
            }
        }
    }
}

@Composable
private fun SatelliteCell(sat: SatelliteInfo, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(2.dp)
    ) {
        Text(
            text = "%02d".format(sat.prn),
            fontSize = 11.sp,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )
        Spacer(modifier = Modifier.height(2.dp))

        val barHeight = 24.dp
        val fillRatio = (sat.cn0 / 50f).coerceIn(0f, 1f)
        val barColor = when {
            sat.cn0 >= 35f -> StatusGood
            sat.cn0 >= 20f -> StatusMedium
            else -> StatusBad
        }

        Box(
            modifier = Modifier
                .width(12.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(Border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillRatio)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}
