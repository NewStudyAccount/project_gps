# GPS Dashboard — 技术设计

## 项目结构

```
app/src/main/java/com/gps/dashboard/
├── GpsApplication.kt
├── MainActivity.kt
├── data/
│   ├── model/
│   │   ├── GpsData.kt              # GPS 数据模型
│   │   ├── SatelliteInfo.kt        # 卫星信息模型
│   │   └── CompassData.kt          # 罗盘数据模型
│   ├── repository/
│   │   ├── LocationRepository.kt   # 定位数据源
│   │   ├── SensorRepository.kt     # 传感器数据源
│   │   └── SatelliteRepository.kt  # 卫星数据源
│   └── buffer/
│       └── RingBuffer.kt           # 环形缓冲区
├── ui/
│   ├── theme/
│   │   ├── Color.kt                # 配色方案
│   │   ├── Type.kt                 # 字体定义
│   │   └── Theme.kt                # 深色主题
│   ├── screen/
│   │   └── DashboardScreen.kt      # 主仪表盘页面
│   ├── component/
│   │   ├── CompassView.kt          # 罗盘组件 (Canvas)
│   │   ├── DataCard.kt             # 数据卡片组件
│   │   ├── AltitudeChart.kt        # 海拔历史图表 (Canvas)
│   │   ├── SatellitePanel.kt       # 卫星信号面板
│   │   ├── SignalBar.kt            # 单颗卫星信号条 (Canvas)
│   │   ├── AccuracyBar.kt          # 精度状态条
│   │   └── SpeedUnitPicker.kt      # 速度单位选择器
│   └── viewmodel/
│       └── GpsViewModel.kt         # 主 ViewModel
└── util/
    ├── CoordinateFormatter.kt      # 坐标格式化 (DMS)
    ├── SpeedConverter.kt           # 速度单位换算
    ├── MagneticDeclination.kt      # 磁偏角计算
    └── AccuracyEvaluator.kt        # 精度评估
```

## 数据模型

### GpsData

```kotlin
data class GpsData(
    val latitude: Double,           // 十进制度
    val longitude: Double,          // 十进制度
    val altitude: Double,           // 米
    val speed: Float,               // m/s (原始值)
    val bearing: Float,             // 度 (磁北)
    val accuracy: Float,            // 米
    val hdop: Float,                // 水平精度因子
    val fixType: FixType,           // 定位类型
    val provider: Set<String>,      // GPS/GLONASS/BDS 等
    val timestamp: Long,            // UTC 毫秒
    val trueBearing: Float,         // 度 (真北, 计算得出)
)

enum class FixType {
    NONE, FIX_2D, FIX_3D
}
```

### SatelliteInfo

```kotlin
data class SatelliteInfo(
    val prn: Int,                   // 卫星编号
    val cn0: Float,                 // 信噪比 dB
    val constellation: Constellation,
    val inUse: Boolean,             // 是否正在使用
    val elevation: Float,           // 仰角
    val azimuth: Float,             // 方位角
)

enum class Constellation {
    GPS, GLONASS, BEIDOU, GALILEO, QZSS, SBAS, OTHER
}
```

### CompassData

```kotlin
data class CompassData(
    val heading: Float,             // 真北方位角 0-360
    val accuracy: SensorAccuracy,   // 传感器精度
)

enum class SensorAccuracy {
    UNRELIABLE, LOW, MEDIUM, HIGH
}
```

## 核心组件设计

### 1. LocationRepository

```
职责: 封装 LocationManager，提供 LocationFlow

输入: LocationManager.requestLocationUpdates()
输出: Flow<Location>

关键逻辑:
├─ 请求 GPS_PROVIDER (优先) + NETWORK_PROVIDER (辅助)
├─ 最小更新间隔: 100ms (10Hz)
├─ 最小距离变化: 0m (每次都更新)
└─ Location 对象直接 emit

注意: 不使用 FusedLocationProviderClient，避免 GMS 依赖
```

### 2. SensorRepository

```
职责: 封装 SensorManager，提供罗盘方向 Flow

输入: 加速度计 + 磁力计原始数据
输出: Flow<Float> (真北方位角)

处理流程:
  加速度计 ──→ 低通滤波 ──→ getRotationMatrix()
  磁力计 ────→ 低通滤波 ──→        │
                                   ▼
                           getOrientation()
                                   │
                                   ▼
                          磁北方位角 (azimuth)
                                   │
                           GeomagneticField 修正
                                   │
                                   ▼
                          真北方位角 (emit)

滤波参数:
├─ 低通滤波 alpha: 0.15 (平滑传感器噪声)
├─ 采样率: SENSOR_DELAY_UI (~60Hz)
└─ 磁偏角: 使用 GeomagneticField(lat, lon, alt, time) 计算
```

### 3. CompassView (Canvas 绘制)

```
绘制层次 (从底到顶):
│
├─ Layer 1: 外圈刻度环
│  ├─ 半径: canvas.width * 0.45
│  ├─ 主刻度 (每30°): 粗线 + 度数文字
│  ├─ 次刻度 (每10°): 中等线
│  └─ 细刻度 (每5°): 短细线
│
├─ Layer 2: 方位字母
│  ├─ N/NE/E/SE/S/SW/W/NW 八方位
│  ├─ 位置: 刻度环内侧
│  ├─ N 用琥珀橙 (#FF8C00), 其余用青色 (#00E5FF)
│  └─ 字号: N 最大 (18sp), 其余次之 (14sp)
│
├─ Layer 3: 内圈装饰
│  ├─ 半径: canvas.width * 0.30
│  ├─ 颜色: #2A2A3A (微妙分隔)
│  └─ 可选: 填充微渐变
│
├─ Layer 4: 中心指针
│  ├─ 三角形指针, 指向上方
│  ├─ 颜色: 琥珀橙 + 发光效果 (drawCircle with alpha)
│  └─ 中心点: 小圆点
│
└─ Layer 5: 度数读数
   ├─ 位置: 罗盘下方
   ├─ 格式: "227° SW"
   └─ 字号: 24sp, 琥珀橙

旋转逻辑:
├─ 刻度盘整体旋转: -heading 度 (设备转, 刻度盘反向转)
├─ 指针固定朝上 (代表北方方向)
└─ 动画: Animatable + spring(stiffness=100f, dampingRatio=0.7f)
    确保平滑过渡, 避免 359°→1° 的跳跃 (需要处理角度环绕)
```

**角度环绕处理**:
```kotlin
// 计算最短旋转路径
fun shortestAngleDiff(current: Float, target: Float): Float {
    val diff = ((target - current + 540) % 360) - 180
    return diff
}
```

### 4. AltitudeChart (Canvas 绘制)

```
数据结构:
├─ RingBuffer<Float>(capacity=60)  // 60 个采样点
├─ 每秒采样一次 (非每次 GPS 更新)
└─ 存储海拔值 (米)

绘制:
├─ X 轴: 时间 (60 格, 左旧右新)
├─ Y 轴: 海拔 (自动缩放, 上下各留 10% padding)
├─ 线条: 贝塞尔曲线 (cubicTo)
├─ 线色: 青色 #00E5FF, 宽度 2dp
├─ 填充: 线下方渐变填充 (青色 alpha 0.3 → 0)
└─ 网格: 水平虚线 (浅灰, 2-3 条)
```

### 5. SatellitePanel

```
折叠态:
├─ 总览条: 横向信号强度条 (所有卫星叠加)
├─ 文字: "SAT 12/24  avg CN0: 38 dB"
└─ 展开指示: "tap to expand ▾"

展开态:
├─ 6 列网格显示每颗卫星
│  ├─ 每格: 卫星编号 + 信号强度柱
│  ├─ 信号柱高度: cn0 / 50 * maxHeight
│  └─ 信号柱颜色: cn0 按阈值渐变
├─ 底部统计: "GPS: 8  GLO: 3  BDS: 1"
└─ 收起指示: "tap to collapse ▴"

信号强度柱颜色:
├─ cn0 >= 35: 绿色 #00FF6A
├─ cn0 >= 20: 黄色 #FFD700
└─ cn0 < 20:  红色 #FF3D00

动画: AnimatedVisibility(expand/collapse) + 渐入渐出
```

### 6. SpeedUnitPicker

```
数据模型:
enum class SpeedUnit(val label: String, val factor: Float) {
    KMH("km/h", 3.6f),      // m/s → km/h
    MS("m/s", 1.0f),         // 原始值
    MPH("mph", 2.237f),      // m/s → mph
}

交互:
├─ 点击速度卡片 → 弹出 DropdownMenu
├─ 选中项高亮 (琥珀橙)
├─ 选择后自动关闭
└─ 数值换算: rawSpeed * unit.factor
```

## ViewModel 设计

```
GpsViewModel:
│
├─ 输入 (从 Repositories 收集):
│  ├─ locationRepository.locationFlow
│  ├─ sensorRepository.compassFlow
│  └─ satelliteRepository.satelliteFlow
│
├─ 状态:
│  ├─ gpsData: StateFlow<GpsData>
│  ├─ compassHeading: StateFlow<Float>
│  ├─ satellites: StateFlow<List<SatelliteInfo>>
│  ├─ altitudeHistory: StateFlow<List<Float>>  // 最近 60 个值
│  ├─ speedUnit: StateFlow<SpeedUnit>
│  └─ satelliteExpanded: StateFlow<Boolean>
│
├─ 计算派生:
│  ├─ trueBearing = bearing + magneticDeclination
│  ├─ formattedLatitude = toDMS(latitude, isLat=true)
│  ├─ formattedLongitude = toDMS(longitude, isLat=false)
│  ├─ formattedSpeed = speed * speedUnit.factor
│  ├─ accuracyLevel = evaluateAccuracy(accuracy)
│  ├─ satelliteInUse = satellites.filter { it.inUse }
│  └─ constellationStats = groupBy { it.constellation }
│
└─ 操作:
   ├─ setSpeedUnit(unit: SpeedUnit)
   └─ toggleSatellitePanel()
```

## 动画参数

| 动画 | 类型 | 参数 |
|------|------|------|
| 罗盘旋转 | Spring | stiffness=100f, dampingRatio=0.7f |
| 精度条颜色 | tween | duration=300ms |
| 卫星面板展开 | AnimatedVisibility | expandVertically + fadeIn, 250ms |
| 速度单位切换 | snap | 即时 |
| 海拔图表新点 | 无动画 | 直接追加 |

## 权限

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

运行时权限请求: 首次启动时请求，未授权时显示引导提示。
