package com.gps.dashboard.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gps.dashboard.ui.component.TrackCanvas
import com.gps.dashboard.ui.theme.*
import com.gps.dashboard.ui.viewmodel.ReplayViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackReplayScreen(
    trackId: Long,
    viewModel: ReplayViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    LaunchedEffect(trackId) {
        viewModel.loadTrack(trackId)
    }

    val points by viewModel.latLngPoints.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val currentPoint = viewModel.getCurrentPoint()

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
                    viewModel.pause()
                    onBack()
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "轨迹回放",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                fontFamily = MonoFontFamily,
            )
        }

        // 地图区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Border, RoundedCornerShape(8.dp))
        ) {
            TrackCanvas(
                points = points,
                highlightedIndex = currentIndex,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 当前点信息
        if (currentPoint != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoText("速度", "%.1f km/h".format(currentPoint.speed * 3.6f))
                InfoText("海拔", "%.1f m".format(currentPoint.altitude))
                InfoText("精度", "±%.1f m".format(currentPoint.accuracy))
                InfoText("时间", formatTimestamp(currentPoint.timestamp))
            }
        }

        // 进度条
        if (points.size > 1) {
            val progress = currentIndex.toFloat() / (points.size - 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Primary)
                )
            }

            // 进度文字
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${currentIndex + 1} / ${points.size}",
                    fontSize = 11.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )
                Text(
                    text = "${points.size} 点",
                    fontSize = 11.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )
            }
        }

        // 控制条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 上一个
            ControlButton("◀◀") { viewModel.skipToPrevious() }

            // 播放/暂停
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Primary)
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isPlaying) "⏸" else "▶",
                    fontSize = 24.sp,
                    color = Background,
                )
            }

            // 下一个
            ControlButton("▶▶") { viewModel.skipToNext() }

            // 速度选择
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Surface)
                    .border(1.dp, Border, RoundedCornerShape(4.dp))
                    .clickable {
                        val newSpeed = when (speed) {
                            0.5f -> 1f
                            1f -> 2f
                            2f -> 4f
                            else -> 0.5f
                        }
                        viewModel.setSpeed(newSpeed)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${speed}x",
                    fontSize = 14.sp,
                    color = Accent,
                    fontFamily = MonoFontFamily,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InfoText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextDim,
            fontFamily = MonoFontFamily,
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )
    }
}

@Composable
private fun ControlButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextPrimary,
        )
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}
