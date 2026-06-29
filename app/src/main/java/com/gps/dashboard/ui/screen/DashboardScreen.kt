package com.gps.dashboard.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.gps.dashboard.GpsApplication
import com.gps.dashboard.data.location.LocationStateHolder
import com.gps.dashboard.data.recorder.TrackRecorder
import com.gps.dashboard.ui.component.*
import com.gps.dashboard.ui.theme.*
import com.gps.dashboard.ui.viewmodel.GpsViewModel
import com.gps.dashboard.ui.viewmodel.TrackingViewModel
import com.gps.dashboard.util.AccuracyLevel
import androidx.compose.foundation.clickable

@Composable
fun DashboardScreen(
    viewModel: GpsViewModel = viewModel(),
    trackingViewModel: TrackingViewModel = viewModel(),
    onNavigateToTrackList: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val permissionGranted by viewModel.permissionGranted.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionResult(fineGranted)
    }

    // 检查权限
    LaunchedEffect(Unit) {
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        viewModel.onPermissionResult(fine == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    // 权限就绪时启动后台定位 Service
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            val app = context.applicationContext as GpsApplication
            if (!LocationStateHolder.isServiceRunning.value) {
                app.startLocationService()
            }
        }
    }

    if (!permissionGranted) {
        PermissionScreen(
            onRequestPermission = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
        return
    }

    // 收集录制状态
    val recordingState by trackingViewModel.recordingState.collectAsState()
    val trackStats by trackingViewModel.trackStats.collectAsState()
    val isRecording = recordingState == TrackRecorder.State.RECORDING
    val isPaused = recordingState == TrackRecorder.State.PAUSED

    // 收集状态
    val gpsData by viewModel.gpsData.collectAsState()
    val compassHeading by viewModel.compassHeading.collectAsState()
    val satellites by viewModel.satellites.collectAsState()
    val altitudeHistory by viewModel.altitudeHistory.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()
    val satelliteExpanded by viewModel.satelliteExpanded.collectAsState()
    val formattedLatitude by viewModel.formattedLatitude.collectAsState()
    val formattedLongitude by viewModel.formattedLongitude.collectAsState()
    val accuracyLevel by viewModel.accuracyLevel.collectAsState()
    val accuracyRatio by viewModel.accuracyRatio.collectAsState()
    val satelliteInUseCount by viewModel.satelliteInUseCount.collectAsState()
    val constellationStats by viewModel.constellationStats.collectAsState()
    val constellationTotalStats by viewModel.constellationTotalStats.collectAsState()
    val averageCn0 by viewModel.averageCn0.collectAsState()
    val fixTypeText by viewModel.fixTypeText.collectAsState()
    val providerText by viewModel.providerText.collectAsState()
    val utcTimeText by viewModel.utcTimeText.collectAsState()
    val hdopText by viewModel.hdopText.collectAsState()
    val altitudeText by viewModel.altitudeText.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 顶栏
        TopBar(
            satelliteCount = satellites.size,
            inUseCount = satelliteInUseCount,
            fixType = fixTypeText,
            provider = providerText,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 罗盘
        CompassView(
            heading = compassHeading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 坐标卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataCard(
                label = "纬度",
                value = formattedLatitude,
                modifier = Modifier.weight(1f),
            )
            DataCard(
                label = "经度",
                value = formattedLongitude,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 海拔 + 速度
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataCard(
                label = "海拔",
                value = altitudeText,
                unit = "m",
                modifier = Modifier.weight(1f),
            )
            DataCard(
                label = "速度",
                value = "%.1f".format(gpsData.speed * speedUnit.factor),
                modifier = Modifier.weight(1f),
                extraContent = {
                    Spacer(modifier = Modifier.height(4.dp))
                    SpeedUnitPicker(
                        currentUnit = speedUnit,
                        onUnitSelected = { viewModel.setSpeedUnit(it) }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 精度 + HDOP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataCard(
                label = "精度",
                value = "± ${"%.1f".format(gpsData.accuracy)}",
                unit = "m",
                modifier = Modifier.weight(1f),
                extraContent = {
                    Spacer(modifier = Modifier.height(6.dp))
                    AccuracyBar(
                        level = accuracyLevel,
                        ratio = accuracyRatio,
                    )
                }
            )
            DataCard(
                label = "HDOP",
                value = hdopText,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 海拔历史图表
        AltitudeChart(
            history = altitudeHistory,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Surface)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 卫星面板
        SatellitePanel(
            satellites = satellites,
            expanded = satelliteExpanded,
            inUseCount = satelliteInUseCount,
            averageCn0 = averageCn0,
            constellationStats = constellationStats,
            constellationTotalStats = constellationTotalStats,
            onToggle = { viewModel.toggleSatellitePanel() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 录制状态条 (仅录制时显示)
        if (isRecording || isPaused) {
            RecordingBar(
                stats = trackStats,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 录制按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRecording) StatusBad else Surface)
                    .border(
                        1.dp,
                        if (isRecording) StatusBad else Border,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        when (recordingState) {
                            TrackRecorder.State.IDLE -> trackingViewModel.startRecording()
                            TrackRecorder.State.RECORDING -> trackingViewModel.stopRecording()
                            TrackRecorder.State.PAUSED -> trackingViewModel.stopRecording()
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (recordingState) {
                        TrackRecorder.State.IDLE -> "● 开始录制"
                        TrackRecorder.State.RECORDING -> "■ 停止录制"
                        TrackRecorder.State.PAUSED -> "■ 停止录制"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) Background else Primary,
                    fontFamily = MonoFontFamily,
                )
            }

            // 历史轨迹按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface)
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable { onNavigateToTrackList() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "📋 历史轨迹",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontFamily = MonoFontFamily,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 底部元数据
        BottomMeta(
            utcTime = utcTimeText,
            provider = providerText,
            fixType = fixTypeText,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TopBar(
    satelliteCount: Int,
    inUseCount: Int,
    fixType: String,
    provider: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "◉ GPS 仪表盘",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            fontFamily = MonoFontFamily,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$inUseCount▲",
            fontSize = 12.sp,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = fixType,
            fontSize = 12.sp,
            color = if (fixType == "3D FIX") StatusGood else StatusBad,
            fontFamily = MonoFontFamily,
        )
    }
}

@Composable
private fun BottomMeta(
    utcTime: String,
    provider: String,
    fixType: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "UTC+8 $utcTime",
            fontSize = 12.sp,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "WGS-84  ·  $provider  ·  $fixType",
            fontSize = 11.sp,
            color = TextDim,
            fontFamily = MonoFontFamily,
        )
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "◉",
            fontSize = 64.sp,
            color = Primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "GPS 仪表盘",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = MonoFontFamily,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "需要定位权限以显示 GPS 数据",
            fontSize = 14.sp,
            color = TextDim,
            fontFamily = MonoFontFamily,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Primary)
                .clickable { onRequestPermission() }
                .padding(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Text(
                text = "授予权限",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Background,
                fontFamily = MonoFontFamily,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .clickable { onOpenSettings() }
                .padding(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Text(
                text = "打开设置",
                fontSize = 14.sp,
                color = TextDim,
                fontFamily = MonoFontFamily,
            )
        }
    }
}
