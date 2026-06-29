package com.gps.dashboard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gps.dashboard.MainActivity
import com.gps.dashboard.R

/**
 * 管理轨迹录制相关通知。
 *
 * 两种通知模式:
 * 1. 后台运行 (未录制): 简单状态通知
 * 2. 录制中: 实时数据 + 停止按钮
 */
class TrackingNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "gps_tracking_channel"
        const val NOTIFICATION_ID = 1002
    }

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        createNotificationChannel()
    }

    /**
     * 构建"录制中"通知，含实时数据和停止按钮。
     */
    fun buildRecordingNotification(
        duration: String,
        distance: String,
        pointCount: Int,
        avgSpeed: String,
    ): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            context, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🔴 轨迹录制中")
            .setContentText("$duration · $distance · ${pointCount}点")
            .setSubText("均速 $avgSpeed")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_launcher_foreground, "停止录制", pendingStop)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    /**
     * 更新录制中通知的数据。
     */
    fun updateRecordingNotification(
        duration: String,
        distance: String,
        pointCount: Int,
        avgSpeed: String,
    ) {
        val notification = buildRecordingNotification(duration, distance, pointCount, avgSpeed)
        manager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 移除录制通知。
     */
    fun cancelRecording() {
        manager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "轨迹录制",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "轨迹录制状态通知"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
