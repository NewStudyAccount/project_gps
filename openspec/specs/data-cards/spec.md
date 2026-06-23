# 数据卡片 (DataCard)

## 概述

统一风格的数据展示卡片，用于显示各项 GPS 数据。

## 组件结构

```
┌──────────────────────────────────────┐
│  LABEL           ← 小字, #00E5FF    │
│  VALUE           ← 大字, #FF8C00    │
│  UNIT/SUFFIX     ← 小字, #6B6B80    │
│  [可选: 状态条]                      │
└──────────────────────────────────────┘
```

## 通用规格

| 属性 | 值 |
|------|-----|
| 背景色 | #14141F |
| 边框 | 1dp, #2A2A3A |
| 圆角 | 8dp |
| 内边距 | 12dp |
| 卡片间距 | 8dp |

## 各卡片规格

### 纬度卡片

- Label: "LATITUDE", #00E5FF, 11sp
- Value: `39°54'15.1" N`, #FF8C00, 20sp
- 格式化: `CoordinateFormatter.toDMS(lat, isLat=true)`

### 经度卡片

- Label: "LONGITUDE", #00E5FF, 11sp
- Value: `116°24'26.6" E`, #FF8C00, 20sp
- 格式化: `CoordinateFormatter.toDMS(lon, isLat=false)`

### 海拔卡片

- Label: "ALTITUDE", #00E5FF, 11sp
- Value: `43.5`, #FF8C00, 24sp
- Unit: "m", #6B6B80, 14sp

### 速度卡片

- Label: "SPEED", #00E5FF, 11sp
- Value: `12.3`, #FF8C00, 24sp
- Unit: 当前单位标签, #6B6B80, 14sp, 可点击
- 交互: 点击弹出 SpeedUnitPicker

### 精度卡片

- Label: "ACCURACY", #00E5FF, 11sp
- Value: `± 3.2`, #FF8C00, 20sp
- Unit: "m", #6B6B80, 14sp
- 状态条: AccuracyBar 组件

### HDOP 卡片

- Label: "HDOP", #00E5FF, 11sp
- Value: `0.8`, #FF8C00, 24sp
- 无单位

## 速度单位选择器 (SpeedUnitPicker)

弹出式 DropdownMenu:

```
enum class SpeedUnit(val label: String, val factor: Float) {
    KMH("km/h", 3.6f),
    MS("m/s", 1.0f),
    MPH("mph", 2.237f),
}
```

- 弹出位置: 速度卡片下方
- 选中项: #FF8C00 背景高亮
- 未选中项: #14141F 背景
- 文字: #E0E0E0
- 选择后自动关闭

## 精度状态条 (AccuracyBar)

```
▓▓▓▓▓▓▓▓▓░░░░░░░░░░░
```

- 高度: 4dp
- 圆角: 2dp
- 填充比例: 基于精度值反比计算 (精度越好填充越多)
- 颜色阈值:
  - ±0~5m: #00FF6A (绿)
  - ±5~15m: #FFD700 (黄)
  - ±15m+: #FF3D00 (红)
- 颜色变化带 300ms 渐变动画

## 坐标格式化

DMS (度分秒) 格式:

```
输入: 39.9042 (十进制度)
输出: 39°54'15.1" N

规则:
1. 取绝对值
2. 度 = floor(abs)
3. 分 = floor((abs - 度) * 60)
4. 秒 = ((abs - 度) * 60 - 分) * 60, 保留一位小数
5. 方向: 纬度 N/S, 经度 E/W
```
