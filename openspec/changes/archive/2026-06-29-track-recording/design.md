# 轨迹记录与后台定位 — 技术设计

## 数据库设计 (Room)

### Track 实体

```kotlin
@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                // "2026-06-29 14:30 骑行"
    val startTime: Long,             // UTC 毫秒
    val endTime: Long? = null,       // null = 正在录制
    val totalDistance: Float = 0f,   // 米
    val totalDuration: Long = 0L,   // 毫秒
    val avgSpeed: Float = 0f,       // m/s
    val maxSpeed: Float = 0f,       // m/s
    val pointCount: Int = 0,        // 原始点数
    val compressedPointCount: Int = 0, // 压缩后点数
    val avgAccuracy: Float = 0f,    // 米
)
```

### TrackPoint 实体

```kotlin
@Entity(
    tableName = "track_points",
    foreignKeys = [ForeignKey(
        entity = Track::class,
        parentColumns = ["id"],
        childColumns = ["trackId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trackId")]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,               // 外键
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,            // 米
    val speed: Float,                // m/s
    val bearing: Float,              // 度
    val accuracy: Float,             // 米
    val timestamp: Long,             // UTC 毫秒
    val isOriginal: Boolean = true,  // true=原始点, false=压缩插入的点
)
```

### DAO

```kotlin
@Dao
interface TrackDao {
    @Insert
    suspend fun insert(track: Track): Long

    @Update
    suspend fun update(track: Track)

    @Delete
    suspend fun delete(track: Track)

    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: Long): Track?

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insert(point: TrackPoint): Long

    @Insert
    suspend fun insertAll(points: List<TrackPoint>)

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getByTrackId(trackId: Long): List<TrackPoint>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getByTrackIdFlow(trackId: Long): Flow<List<TrackPoint>>

    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: Long)

    @Query("SELECT COUNT(*) FROM track_points WHERE trackId = :trackId")
    suspend fun countByTrackId(trackId: Long): Int
}
```

### Database

```kotlin
@Database(entities = [Track::class, TrackPoint::class], version = 1)
abstract class TrackingDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao
}
```

## 共享定位状态 (LocationStateHolder)

应用级单例，Service 写入，ViewModel/Recorder/Notification 读取。

```kotlin
object LocationStateHolder {
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun update(location: Location) {
        _location.value = location
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }
}
```

**数据流**：

```
LocationForegroundService
    │
    │  LocationManager 回调
    ▼
LocationStateHolder.update(location)
    │
    ├──▶ GpsViewModel.locationFlow  → 仪表盘 UI
    ├──▶ TrackRecorder              → 写入 Room
    └──▶ TrackingNotificationManager → 更新通知
```

## Foreground Service 设计

### LocationForegroundService

```
生命周期:
  startService() ──▶ onCreate() ──▶ onStartCommand()
                                        │
                                        ├─ 注册 LocationManager
                                        ├─ 启动前台通知
                                        ├─ 更新 LocationStateHolder
                                        │
                                        ▼
                                   持续运行中
                                        │
  stopService() / stopSelf() ──▶ onDestroy()
                                    │
                                    ├─ 移除 LocationManager 监听
                                    └─ 停止前台通知
```

**关键实现要点**：

```kotlin
class LocationForegroundService : Service() {

    // 与 GpsViewModel 相同的 LocationManager 调用方式
    // 但使用 Service 的 Looper (而非 MainLooper)
    // 回调中更新 LocationStateHolder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification())
            ACTION_STOP_TRACK -> TrackRecorder.stopRecording()
            ACTION_STOP_SERVICE -> stopSelf()
        }
        return START_STICKY  // 被系统杀死后自动重启
    }
}
```

### Manifest 声明

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<service
    android:name=".service.LocationForegroundService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

### 通知管理

**两种通知状态**：

```
状态 1: 仅后台运行 (未录制)
┌─────────────────────────────────────┐
│ 🛰️ GPS 仪表盘 — 后台运行中          │
│ 12▲  3D FIX  12.3 km/h             │
│ [打开应用]                          │
└─────────────────────────────────────┘

状态 2: 后台运行 + 录制中
┌─────────────────────────────────────┐
│ 🔴 轨迹录制中                       │
│ 00:12:34 · 2.3km · 156点           │
│ [打开应用]           [停止录制]      │
└─────────────────────────────────────┘
```

使用 `NotificationCompat.Builder` + `PendingIntent` 实现按钮交互。

## 自适应采样器 (AdaptiveSampler)

```kotlin
data class SamplingConfig(
    val minIntervalMs: Long,    // 最短时间间隔
    val minDistanceM: Float,    // 最短记录距离
)

enum class MotionProfile {
    STATIONARY, WALKING, CYCLING, DRIVING
}

class AdaptiveSampler {

    private val configs = mapOf(
        MotionProfile.STATIONARY to SamplingConfig(30_000L, Float.MAX_VALUE),
        MotionProfile.WALKING    to SamplingConfig(5_000L, 5f),
        MotionProfile.CYCLING    to SamplingConfig(3_000L, 10f),
        MotionProfile.DRIVING    to SamplingConfig(1_000L, 20f),
    )

    private var lastRecordedTime = 0L
    private var lastRecordedLocation: Location? = null

    // 判断是否应该记录这个点
    fun shouldRecord(location: Location): Boolean {
        val now = System.currentTimeMillis()
        val profile = classifyMotion(location.speed)
        val config = configs[profile]!!

        // 时间间隔检查
        if (now - lastRecordedTime < config.minIntervalMs) return false

        // 距离检查 (静止状态跳过)
        if (profile != MotionProfile.STATIONARY) {
            val last = lastRecordedLocation ?: return true
            if (location.distanceTo(last) < config.minDistanceM) return false
        }

        return true
    }

    fun onRecorded(location: Location) {
        lastRecordedTime = System.currentTimeMillis()
        lastRecordedLocation = location
    }

    private fun classifyMotion(speed: Float): MotionProfile {
        return when {
            speed < 0.5f  -> MotionProfile.STATIONARY
            speed < 3f    -> MotionProfile.WALKING
            speed < 10f   -> MotionProfile.CYCLING
            else          -> MotionProfile.DRIVING
        }
    }
}
```

## 轨迹录制器 (TrackRecorder)

```kotlin
class TrackRecorder(
    private val db: TrackingDatabase,
    private val sampler: AdaptiveSampler,
) {
    enum class State { IDLE, RECORDING, PAUSED }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _currentTrackStats = MutableStateFlow(TrackStats())
    val currentTrackStats: StateFlow<TrackStats> = _currentTrackStats.asStateFlow()

    private var currentTrackId: Long? = null
    private var job: Job? = null

    // 从 LocationStateHolder 收集数据
    fun startRecording(scope: CoroutineScope) { ... }
    fun pauseRecording() { ... }
    fun resumeRecording() { ... }
    suspend fun stopRecording(): Long { ... }  // 返回 trackId
}

data class TrackStats(
    val duration: Long = 0L,
    val distance: Float = 0f,
    val pointCount: Int = 0,
    val currentSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val motionProfile: MotionProfile = MotionProfile.STATIONARY,
)
```

**录制流程**：

```
startRecording()
    │
    ├─ 在 Room 创建 Track 记录 (endTime=null)
    ├─ 启动协程，collect LocationStateHolder.location
    │
    ▼
每次 GPS 更新:
    │
    ├─ sampler.shouldRecord(location)?
    │   ├─ No  → 跳过
    │   └─ Yes → 创建 TrackPoint 写入 Room
    │            更新 _currentTrackStats
    │
    ▼
stopRecording()
    │
    ├─ 停止协程
    ├─ 读取所有 TrackPoint
    ├─ 执行 RDP 降采样 → 生成压缩点
    ├─ 更新 Track 记录 (endTime, 统计数据)
    └─ 返回 trackId
```

## 降采样算法 (TrackCompressor)

### Ramer-Douglas-Peucker

```kotlin
object TrackCompressor {

    /**
     * 对轨迹点列表执行 RDP 降采样
     * @param points 原始轨迹点 (按时间排序)
     * @param epsilon 最大允许偏差 (米), 默认 5m
     * @return 保留的关键点索引列表
     */
    fun simplify(points: List<TrackPoint>, epsilon: Double = 5.0): List<Int> {
        if (points.size <= 2) return points.indices.toList()

        // 找到距离首尾连线最远的点
        val first = points.first()
        val last = points.last()
        var maxDist = 0.0
        var maxIndex = 0

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        // 如果最大距离超过阈值，递归处理两段
        return if (maxDist > epsilon) {
            val left = simplify(points.subList(0, maxIndex + 1), epsilon)
            val right = simplify(points.subList(maxIndex, points.size), epsilon)
            (left + right.drop(1)).distinct().sorted()
        } else {
            // 所有点都在阈值内，只保留首尾
            listOf(0, points.size - 1)
        }
    }

    // 点到线段的垂直距离 (Haversine 公式计算真实距离)
    private fun perpendicularDistance(
        point: TrackPoint, lineStart: TrackPoint, lineEnd: TrackPoint
    ): Double { ... }
}
```

## Canvas 地图设计

### 坐标投影

```kotlin
class TrackProjection(
    private val points: List<LatLng>,
    private val canvasWidth: Float,
    private val canvasHeight: Float,
    private val padding: Float = 40f,
) {
    // 计算 bounding box
    private val latRange: Double  // 纬度范围
    private val lngRange: Double  // 经度范围
    private val centerLat: Double
    private val centerLng: Double
    private val scale: Float      // 像素/度

    init {
        val lats = points.map { it.latitude }
        val lngs = points.map { it.longitude }
        val latSpan = lats.max() - lats.min()
        val lngSpan = lngs.max() - lngs.min()

        centerLat = (lats.max() + lats.min()) / 2
        centerLng = (lngs.max() + lngs.min()) / 2

        // 在上海 (31°N), 1° 经度 ≈ 96km
        val lngFactor = cos(Math.toRadians(centerLat))
        val effectiveLngSpan = lngSpan * lngFactor

        // 取较大维度来填满画布
        val maxSpan = maxOf(latSpan, effectiveLngSpan)
        val availableSize = minOf(canvasWidth, canvasHeight) - padding * 2
        scale = (availableSize / maxSpan).toFloat()
    }

    fun toScreen(lat: Double, lng: Double): Offset {
        val x = (lng - centerLng) * scale * cos(Math.toRadians(centerLat)) +
                canvasWidth / 2
        val y = (centerLat - lat) * scale + canvasHeight / 2
        return Offset(x.toFloat(), y.toFloat())
    }
}
```

### 绘制层次

```
Canvas 绘制顺序 (从底到顶):

Layer 1: 背景
  └─ fillRect(Background) 全屏填充

Layer 2: 网格
  ├─ 等间距网格点 (间距随缩放级别变化)
  ├─ 颜色: #2A2A3A, 半径 1dp
  └─ 网格线: 水平 + 垂直虚线

Layer 3: 轨迹线
  ├─ drawPath(points → screen coords)
  ├─ 已走路段 (回溯模式): 灰色 #6B6B80, 2dp
  ├─ 待走路段: 琥珀橙 #FF8C00, 2dp
  └─ 圆角连接 (joinRound)

Layer 4: 关键点标记
  ├─ 起点: 绿色圆 #00FF6A, 半径 6dp
  ├─ 终点: 红色圆 #FF3D00, 半径 6dp
  └─ 转弯点 (回溯): 黄色三角 #FFD700

Layer 5: 当前位置
  ├─ 外圈: 琥珀橙脉冲圆 (alpha 动画)
  ├─ 内圈: 实心琥珀橙圆, 半径 5dp
  └─ 方向指针: 三角形, 指向 bearing 方向

Layer 6: 信息叠加
  ├─ 左下: 经纬度刻度
  ├─ 右下: 缩放比例尺
  └─ 轨迹点详情 (点击时弹出)
```

### 手势处理

```kotlin
// 使用 Compose 的 pointerInput 处理手势
Modifier.pointerInput(Unit) {
    detectTransformGestures { centroid, pan, zoom, _ ->
        // 双指缩放: 以 centroid 为中心缩放
        scale *= zoom
        // 单指拖拽: 平移视图
        offset += pan
    }
}

// 缩放范围: 0.5x ~ 20x
// 双击重置: scale=1, offset=0
```

## 回溯引擎 (BacktrackEngine)

### 核心数据结构

```kotlin
data class BacktrackState(
    val status: Status,              // TRACKING / DEVIATED / COMPLETE
    val nearestSegmentIndex: Int,    // 当前最近的轨迹段索引
    val projectionPoint: LatLng,     // 在轨迹上的投影点
    val distanceToTrack: Float,      // 距离轨迹的距离 (米)
    val nextTargetIndex: Int,        // 下一个目标点索引
    val nextTargetDistance: Float,    // 距下一个目标的距离 (米)
    val turnDirection: TurnDirection,// 转向方向
    val turnDistance: Float,         // 距转弯点的距离 (米)
    val remainingDistance: Float,     // 剩余总距离 (米)
    val progress: Float,             // 完成进度 0.0 ~ 1.0
)

enum class Status { TRACKING, DEVIATED, COMPLETE }
enum class TurnDirection { STRAIGHT, LEFT, RIGHT, U_TURN }
```

### 算法流程

```
每次 GPS 更新:

1. 找最近轨迹段
   ┌─────────────────────────────────────────┐
   │ for i in 0 until points.size - 1:       │
   │   d = pointToSegmentDistance(            │
   │         userPos, points[i], points[i+1]) │
   │   if d < minDist:                        │
   │     minDist = d                          │
   │     nearestIdx = i                       │
   └─────────────────────────────────────────┘

2. 计算投影点 (在线段上的最近点)
   ┌─────────────────────────────────────────┐
   │ A = points[i], B = points[i+1]          │
   │ t = dot(user-A, B-A) / |B-A|²           │
   │ t = clamp(t, 0, 1)                      │
   │ projection = A + t * (B - A)            │
   └─────────────────────────────────────────┘

3. 确定回溯目标
   ┌─────────────────────────────────────────┐
   │ 回溯方向: 沿轨迹反向                      │
   │ targetIdx = nearestIdx (走向前一个点)      │
   │ 如果 projection 更靠近 B:                 │
   │   targetIdx = nearestIdx + 1 → 先到 B    │
   │ 然后目标变为 points[targetIdx - 1]        │
   └─────────────────────────────────────────┘

4. 计算转向
   ┌─────────────────────────────────────────┐
   │ 在目标点处计算两段夹角:                    │
   │ v1 = target → prev                      │
   │ v2 = target → next (回溯方向的下一段)      │
   │ angle = atan2(cross(v1,v2), dot(v1,v2))  │
   │                                           │
   │ angle mapping:                            │
   │   |angle| < 30°  → STRAIGHT              │
   │   angle in [-120°, -30°] → LEFT           │
   │   angle in [30°, 120°]   → RIGHT          │
   │   |angle| > 120° → U_TURN                │
   └─────────────────────────────────────────┘

5. 偏离检测
   ┌─────────────────────────────────────────┐
   │ distance < 30m  → TRACKING (正常)        │
   │ distance 30~50m → TRACKING (接近偏离)    │
   │ distance > 50m  → DEVIATED               │
   │                                           │
   │ DEVIATED 时:                              │
   │   找轨迹上离用户最近的点                   │
   │   箭头指向该点方向                         │
   │   显示 "返回轨迹 XXm"                     │
   └─────────────────────────────────────────┘

6. 完成检测
   ┌─────────────────────────────────────────┐
   │ distance(user, 轨迹起点) < 20m → COMPLETE │
   └─────────────────────────────────────────┘
```

### 点到线段距离

```kotlin
fun pointToSegmentDistance(p: LatLng, a: LatLng, b: LatLng): Float {
    // 将经纬度转换为米 (局部近似)
    val px = (p.longitude - a.longitude) * lngToM
    val py = (p.latitude - a.latitude) * latToM
    val bx = (b.longitude - a.longitude) * lngToM
    val by = (b.latitude - a.latitude) * latToM

    val t = ((px * bx + py * by) / (bx * bx + by * by)).coerceIn(0.0, 1.0)
    val projX = t * bx
    val projY = t * by

    return sqrt((px - projX).pow(2) + (py - projY).pow(2)).toFloat()
}
```

## 导出设计

### GpxExporter

```kotlin
object GpxExporter {

    fun export(track: Track, points: List<TrackPoint>): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="GPS Dashboard"""")
            appendLine("""  xmlns="http://www.topografix.com/GPX/1/1">""")
            appendLine("  <metadata>")
            appendLine("    <name>${track.name}</name>")
            appendLine("    <time>${formatTime(track.startTime)}</time>")
            appendLine("  </metadata>")
            appendLine("  <trk>")
            appendLine("    <name>${track.name}</name>")
            appendLine("    <trkseg>")
            for (p in points) {
                appendLine("""      <trkpt lat="${p.latitude}" lon="${p.longitude}">""")
                appendLine("        <ele>${p.altitude}</ele>")
                appendLine("        <time>${formatTime(p.timestamp)}</time>")
                appendLine("        <speed>${p.speed}</speed>")
                appendLine("      </trkpt>")
            }
            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    // 保存到 Downloads 并返回 Uri
    suspend fun saveToFile(context: Context, track: Track, points: List<TrackPoint>): Uri { ... }
}
```

### KmlExporter

```kotlin
object KmlExporter {

    fun export(track: Track, points: List<TrackPoint>): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
            appendLine("  <Document>")
            appendLine("    <name>${track.name}</name>")
            appendLine("    <Placemark>")
            appendLine("      <name>${track.name}</name>")
            appendLine("      <LineString>")
            appendLine("        <coordinates>")
            for (p in points) {
                appendLine("          ${p.longitude},${p.latitude},${p.altitude}")
            }
            appendLine("        </coordinates>")
            appendLine("      </LineString>")
            appendLine("    </Placemark>")
            appendLine("  </Document>")
            appendLine("</kml>")
        }
    }
}
```

### 分享机制

```kotlin
// 使用 Android Share Sheet
fun shareTrack(context: Context, uri: Uri, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享轨迹"))
}
```

## 现有模块修改

### GpsViewModel 变更

```
Before:
  LocationRepository(application) → locationFlow → _gpsData

After:
  LocationStateHolder.location → _gpsData

原因: Service 和 ViewModel 不应各自创建 LocationManager 监听
      统一由 Service 采集，通过 LocationStateHolder 分发
```

### DashboardScreen 变更

```
新增状态收集:
  + recordingState (from TrackingViewModel)
  + trackStats (from TrackingViewModel)
  + isServiceRunning (from LocationStateHolder)

新增 UI 元素:
  + RecordingBar (录制时显示)
  + "开始录制" / "停止录制" 按钮
  + "历史轨迹" 按钮
  + Service 启动逻辑 (LaunchedEffect)
```

### build.gradle.kts 新增依赖

```kotlin
// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")  // 或 ksp

// 需要添加 kapt/ksp 插件
plugins {
    id("com.google.devtools.ksp") version "..." // 或 kapt
}
```

## 权限请求流程

```
启动 App
    │
    ├─ 检查 ACCESS_FINE_LOCATION
    │   └─ 未授权 → 请求 (已有逻辑)
    │
    ├─ 检查 POST_NOTIFICATIONS (Android 13+)
    │   └─ 未授权 → 请求
    │
    ├─ 检查 ACCESS_BACKGROUND_LOCATION (Android 10+)
    │   └─ 未授权 → 引导到设置页 (不能直接弹窗请求)
    │
    └─ 所有权限就绪 → 启动 Service + 显示主界面
```

**后台权限特殊处理**：Android 10+ 要求 `ACCESS_BACKGROUND_LOCATION` 必须在设置页手动授予，不能通过标准权限弹窗请求。需要一个引导弹窗解释原因。
