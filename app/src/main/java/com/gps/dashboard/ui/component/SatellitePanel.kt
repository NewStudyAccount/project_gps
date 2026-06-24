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
import androidx.compose.ui.text.style.TextAlign
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
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 地球图标
                Text(
                    text = "🌍",
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 按星座分组
                val grouped = satellites.groupBy { it.constellation }
                val columns = 6

                for ((constellation, sats) in grouped) {
                    // 轨道虚线
                    OrbitLine()

                    // 星座标签
                    Text(
                        text = constellation.label,
                        fontSize = 11.sp,
                        color = TextDim,
                        fontFamily = MonoFontFamily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 4.dp),
                    )

                    // 卫星卡片行
                    val rows = sats.chunked(columns)
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (sat in row) {
                                SatelliteCard(sat, Modifier.weight(1f))
                            }
                            // 填充空位
                            repeat(columns - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 底部统计
                Text(
                    text = constellationStats.entries.joinToString("  ") { (const, count) ->
                        "${const.label}: $count"
                    },
                    fontSize = 12.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 收起提示
                Text(
                    text = "点击收起 ▴",
                    fontSize = 11.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )
            }
        }
    }
}

@Composable
private fun OrbitLine(modifier: Modifier = Modifier) {
    Text(
        text = "┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈",
        fontSize = 10.sp,
        color = TextDim.copy(alpha = 0.5f),
        fontFamily = MonoFontFamily,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun SatelliteCard(sat: SatelliteInfo, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Background)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        // 状态指示器
        Text(
            text = if (sat.inUse) "◉" else "○",
            fontSize = 12.sp,
            color = if (sat.inUse) Primary else TextDim,
            fontFamily = MonoFontFamily,
        )

        // 卫星名称
        Text(
            text = "${sat.constellation.label} %02d".format(sat.prn),
            fontSize = 11.sp,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 信号强度条
        val barHeight = 16.dp
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

        Spacer(modifier = Modifier.height(2.dp))

        // CN0 值
        Text(
            text = "${sat.cn0.toInt()}dB",
            fontSize = 9.sp,
            color = TextDim,
            fontFamily = MonoFontFamily,
        )
    }
}
