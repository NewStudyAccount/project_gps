package com.gps.dashboard.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gps.dashboard.data.model.Track
import com.gps.dashboard.ui.theme.*
import com.gps.dashboard.ui.viewmodel.TrackListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackListScreen(
    viewModel: TrackListViewModel = viewModel(),
    onBack: () -> Unit = {},
    onNavigateToReplay: (Long) -> Unit = {},
    onNavigateToBacktrack: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<Long?>(null) }

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
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "📋 历史轨迹",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (tracks.isNotEmpty()) {
                Text(
                    text = "清空全部",
                    fontSize = 12.sp,
                    color = StatusBad,
                    fontFamily = MonoFontFamily,
                    modifier = Modifier.clickable { showDeleteAllDialog = true }
                )
            }
        }

        if (tracks.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "📍",
                    fontSize = 48.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无轨迹记录",
                    fontSize = 16.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "开始录制后，轨迹将显示在这里",
                    fontSize = 12.sp,
                    color = TextDim,
                    fontFamily = MonoFontFamily,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // 轨迹列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tracks, key = { it.id }) { track ->
                    TrackListItem(
                        track = track,
                        onReplay = { onNavigateToReplay(track.id) },
                        onBacktrack = { onNavigateToBacktrack(track.id) },
                        onExport = { showExportDialog = track.id },
                        onDelete = { viewModel.deleteTrack(track.id) },
                    )
                }
            }
        }
    }

    // 导出对话框
    showExportDialog?.let { trackId ->
        ExportDialog(
            onDismiss = { showExportDialog = null },
            onExportGpx = {
                showExportDialog = null
                viewModel.exportGpx(context, trackId) { uri ->
                    uri?.let { viewModel.shareTrack(context, it, "application/gpx+xml") }
                }
            },
            onExportKml = {
                showExportDialog = null
                viewModel.exportKml(context, trackId) { uri ->
                    uri?.let { viewModel.shareTrack(context, it, "application/vnd.google-earth.kml+xml") }
                }
            },
        )
    }

    // 清空确认对话框
    if (showDeleteAllDialog) {
        ConfirmDialog(
            title = "清空全部轨迹",
            message = "确定要删除所有轨迹记录吗？此操作不可撤销。",
            onConfirm = {
                showDeleteAllDialog = false
                viewModel.deleteAll()
            },
            onDismiss = { showDeleteAllDialog = false },
        )
    }
}

@Composable
private fun TrackListItem(
    track: Track,
    onReplay: () -> Unit,
    onBacktrack: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // 名称
        Text(
            text = track.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 统计信息
        Text(
            text = "${formatDistance(track.totalDistance)} · ${formatDuration(track.totalDuration)} · 均速 ${formatSpeed(track.avgSpeed)}",
            fontSize = 12.sp,
            color = TextDim,
            fontFamily = MonoFontFamily,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${track.pointCount} 点 · 精度 ±${"%.1f".format(track.avgAccuracy)}m",
            fontSize = 11.sp,
            color = TextDim,
            fontFamily = MonoFontFamily,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton("回放", onReplay)
            ActionButton("回溯", onBacktrack)
            ActionButton("导出", onExport)
            ActionButton("删除", onDelete, isDestructive = true)
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "删除轨迹",
            message = "确定要删除「${track.name}」吗？",
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isDestructive) StatusBad.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isDestructive) StatusBad else Primary,
            fontFamily = MonoFontFamily,
        )
    }
}

@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExportGpx: () -> Unit,
    onExportKml: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "导出格式",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary.copy(alpha = 0.1f))
                    .clickable { onExportGpx() }
                    .padding(12.dp),
            ) {
                Text(
                    text = "GPX (通用 GPS 格式)",
                    fontSize = 14.sp,
                    color = Primary,
                    fontFamily = MonoFontFamily,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent.copy(alpha = 0.1f))
                    .clickable { onExportKml() }
                    .padding(12.dp),
            ) {
                Text(
                    text = "KML (Google Earth 格式)",
                    fontSize = 14.sp,
                    color = Accent,
                    fontFamily = MonoFontFamily,
                )
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = MonoFontFamily,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = TextDim,
                fontFamily = MonoFontFamily,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "取消",
                        fontSize = 14.sp,
                        color = TextDim,
                        fontFamily = MonoFontFamily,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusBad)
                        .clickable { onConfirm() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "确认",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Background,
                        fontFamily = MonoFontFamily,
                    )
                }
            }
        }
    }
}

private fun formatDistance(meters: Float): String {
    return if (meters >= 1000) "%.2f km".format(meters / 1000) else "%.0f m".format(meters)
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%d:%02d:%02d".format(hours, minutes, seconds)
}

private fun formatSpeed(mps: Float): String {
    return "%.1f km/h".format(mps * 3.6f)
}
