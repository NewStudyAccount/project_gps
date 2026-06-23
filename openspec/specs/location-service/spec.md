# 定位服务 (Location & Sensor Services)

## 概述

封装 Android 系统定位和传感器服务，提供响应式数据流。

## LocationRepository

### 配置

| 参数 | 值 |
|------|-----|
| Provider | GPS_PROVIDER (优先), NETWORK_PROVIDER (辅助) |
| 最小更新间隔 | 100ms (10Hz) |
| 最小距离变化 | 0m |

### 输出

```kotlin
val locationFlow: Flow<Location>
```

### 权限检查

- 调用前需确认 `ACCESS_FINE_LOCATION` 已授权
- 未授权时 flow 不发射, 不抛异常

### 生命周期

- 在 ViewModel 的 viewModelScope 中收集
- ViewModel 销毁时自动取消

## SensorRepository

### 传感器

| 传感器 | 用途 | 采样率 |
|--------|------|--------|
| TYPE_ACCELEROMETER | 设备加速度 | SENSOR_DELAY_UI |
| TYPE_MAGNETIC_FIELD | 地磁场 | SENSOR_DELAY_UI |

### 处理流程

```
加速度计 ──→ 低通滤波 ──→ getRotationMatrix()
                                      │
磁力计 ────→ 低通滤波 ──→             │
                                      ▼
                              getOrientation()
                                      │
                                      ▼
                             磁北方位角 (azimuth)
                                      │
                              GeomagneticField 修正
                                      │
                                      ▼
                             真北方位角 → emit
```

### 低通滤波

```kotlin
filtered = filtered * (1 - alpha) + raw * alpha
alpha = 0.15f
```

### 磁偏角修正

```kotlin
val geoField = GeomagneticField(lat, lon, alt, timeMillis)
val trueHeading = magneticHeading + geoField.declination
// 归一化到 0-360
val normalized = (trueHeading + 360) % 360
```

- 需要当前经纬度和海拔来计算
- 磁偏角变化缓慢, 可在位置更新时重新计算
- 使用最近一次的 Location 数据

### 输出

```kotlin
val compassFlow: Flow<Float>  // 真北方位角 0-360
```

### 传感器精度

```kotlin
val accuracyFlow: Flow<SensorAccuracy>
// UNRELIABLE, LOW, MEDIUM, HIGH
// 来自 SENSOR_STATUS_ACCURACY
```

## SatelliteRepository

### API

- `LocationManager.registerGnssStatusCallback()` (API 24+)
- `GnssStatus` 对象包含所有可见卫星信息

### 数据提取

```kotlin
fun extractSatellites(status: GnssStatus): List<SatelliteInfo> {
    return (0 until status.satelliteCount).map { i ->
        SatelliteInfo(
            prn = status.getSvid(i),
            cn0 = status.getCn0Hz(i),
            constellation = mapConstellation(status.getConstellationType(i)),
            inUse = status.usedInFix(i),
            elevation = status.getElevationDegrees(i),
            azimuth = status.getAzimuthDegrees(i),
        )
    }
}
```

### 输出

```kotlin
val satelliteFlow: Flow<List<SatelliteInfo>>
```

### 更新频率

- GnssStatus.Callback 默认 ~1Hz 更新
- 足够满足 UI 需求

## 权限处理

### 所需权限

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 运行时请求

- MainActivity 中使用 `rememberLauncherForActivityResult`
- 首次启动时请求
- 未授权时显示提示卡片: "需要定位权限以显示 GPS 数据"
- 用户拒绝后不再自动请求, 提供手动跳转设置的按钮

### 权限状态

```kotlin
enum class PermissionState {
    GRANTED,        // 已授权
    DENIED,         // 拒绝 (可再次请求)
    PERMANENTLY_DENIED  // 永久拒绝 (需跳转设置)
}
```
