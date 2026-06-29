## Context

GPS 仪表盘使用 Jetpack Compose + Canvas 构建深色主题 UI。卫星数据通过 `SatelliteRepository` 的 `GnssStatus.Callback` 获取，每个卫星包含 `prn`、`cn0`、`constellation`、`inUse`、`elevation`（0-90°）、`azimuth`（0-360°）。

当前 `SatellitePanel.kt` 展开视图使用 emoji 🌍 + 网格卡片列表，需要重写为 Canvas 天球图。

主要涉及文件：
- `SatellitePanel.kt` — 卫星面板主组件（重写展开视图）
- `SkyPlotCanvas.kt` — 新增，Canvas 天球图绘制
- `Color.kt` — 新增星座颜色
- `GpsViewModel.kt` — 新增可见卫星总数统计

## Goals / Non-Goals

**Goals:**
- 用 Canvas 绘制线框地球（经纬线网格）作为天球图中心
- 将卫星按 azimuth/elevation 投影到圆形平面
- 圆点大小按 CN0 值缩放
- 左上角显示各星座已用/总数图例
- 保持现有深色航空/赛车风格

**Non-Goals:**
- 不添加动画效果
- 不显示卫星编号
- 不支持交互（点击、缩放）

## Decisions

### 1. 天球图投影模型

**决策**：等距方位投影（Azimuthal Equidistant Projection）

```
                N (0°/360°)
                │
                │  ● satellite (azimuth=340°, elevation=30°)
                │
  W (270°)──────┼────── E (90°)
                │
                │      ● satellite (azimuth=120°, elevation=60°)
                │
                S (180°)

  圆心 = 天顶 (elevation=90°)
  圆边 = 地平线 (elevation=0°)
```

**公式**：
```
r = R × (90 - elevation) / 90
x = cx + r × sin(azimuth_rad)
y = cy - r × cos(azimuth_rad)
```

其中 `R` 是天球图半径，`(cx, cy)` 是圆心。

**理由**：
- 经典天球图标准投影方式
- 直观：圆心=头顶，边缘=地平线
- 计算简单，性能好

### 2. 线框地球绘制

**决策**：在天球图中心绘制一个较小的线框球体（半径约为天球图的 25%），包含：
- 经线（每隔 30° 一条，共 12 条）
- 纬线（每隔 30° 一条，共 5 条：±60°、±30°、赤道）
- 线条使用 `TextDim` 颜色，alpha=0.3

**理由**：
- 经纬线网格是线框地球的经典表现
- 25% 半径不会遮挡太多卫星点
- 低透明度确保不干扰卫星信息

### 3. 卫星圆点编码

**决策**：
- **大小**：CN0 映射到 4dp-12dp 直径
  - CN0 < 15dB → 4dp（极弱）
  - CN0 15-25dB → 6dp（弱）
  - CN0 25-35dB → 8dp（中）
  - CN0 35-45dB → 10dp（强）
  - CN0 > 45dB → 12dp（极强）
- **颜色**：按星座分配（见下方颜色方案）
- **inUse 区分**：inUse 的卫星绘制实心圆，非 inUse 绘制空心圆（仅描边）

**理由**：
- 大小编码直观反映信号质量
- 实心/空心区分是否参与定位
- 无动画，静态渲染性能最优

### 4. 星座颜色方案

| 星座 | 颜色 | Hex |
|------|------|-----|
| GPS | Primary (橙) | #FF8C00 |
| GLONASS | Accent (青) | #00E5FF |
| BEIDOU | StatusGood (绿) | #00FF6A |
| GALILEO | StatusMedium (金) | #FFD700 |
| QZSS | 新增 (粉) | #FF6B9D |
| SBAS | 新增 (紫) | #9B59B6 |
| OTHER | TextDim (灰) | #6B6B80 |

**理由**：
- 复用现有主题色，保持风格统一
- 粉色和紫色补充色板，确保每个星座可区分
- 颜色在深色背景上对比度良好

### 5. 左上角图例面板

**决策**：半透明面板叠加在天球图左上角，显示：

```
┌───────────────┐
│ ▸ GPS    8/12 │
│ ▸ BDS    5/10 │
│ ▸ GLO    3/5  │
│ ▸ GAL    2/4  │
│ ▸ QZS    1/1  │
└───────────────┘
```

- 仅显示有卫星的星座（无卫星的不显示）
- 左侧色块对应星座颜色
- 背景使用 `Surface` 色 + 80% 不透明度

### 6. 底部汇总栏

**决策**：天球图下方显示：

```
▸ 19/32  CN0 32dB
```

- `19/32` = 已用/可见总数
- `CN0 32dB` = inUse 卫星平均 CN0
- 使用 `TextDim` 颜色，11sp

### 7. 天球图辅助元素

**决策**：
- 外圆边框：`Border` 颜色，1dp
- 方位刻度线：N/E/S/W 四个方向，`TextDim` 颜色，10sp
- 仰角参考圆：30° 和 60° 两个虚线圆，`Border` 颜色，alpha=0.3

**理由**：
- 方位刻度帮助方向识别
- 仰角参考圆帮助判断卫星高度
- 低透明度不干扰主信息

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 卫星点重叠（多颗卫星在同一位置） | 接受重叠，CN0 不同时大小有差异可部分区分 |
| 小屏幕上天球图太小 | 最小尺寸限制 200dp，必要时隐藏方位标签 |
| Canvas 绘制性能 | 静态渲染，仅在数据变化时重绘，无动画帧循环 |
| 非 inUse 卫星太多导致图面杂乱 | 非 inUse 卫星使用较低 alpha（0.4） |

## Open Questions

无
