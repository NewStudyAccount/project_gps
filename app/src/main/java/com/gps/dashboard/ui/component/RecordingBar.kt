package com.gps.dashboard.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gps.dashboard.data.recorder.AdaptiveSampler
import com.gps.dashboard.data.recorder.TrackRecorder
import com.gps.dashboard.ui.theme.*

@Composable
fun RecordingBar(
    stats: TrackRecorder.TrackStats,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, StatusBad.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // 第一行：录制指示 + 时长 + 距离
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 脉冲红点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(StatusBad)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REC",
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = StatusBad,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatDuration(stats.duration),
                fontSize = 14.sp,
                color = TextPrimary,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = formatDistance(stats.distance),
                fontSize = 14.sp,
                color = Primary,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${stats.pointCount}点",
                fontSize = 12.sp,
                color = TextDim,
                fontFamily = MonoFontFamily,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 第二行：速度 + 运动状态
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "速度 ${"%.1f".format(stats.currentSpeed * 3.6f)} km/h",
                fontSize = 11.sp,
                color = TextDim,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "均速 ${"%.1f".format(stats.avgSpeed * 3.6f)} km/h",
                fontSize = 11.sp,
                color = TextDim,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stats.motionProfile.label,
                fontSize = 11.sp,
                color = Accent,
                fontFamily = MonoFontFamily,
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun formatDistance(meters: Float): String {
    return if (meters >= 1000) {
        "%.2f km".format(meters / 1000)
    } else {
        "%.0f m".format(meters)
    }
}
