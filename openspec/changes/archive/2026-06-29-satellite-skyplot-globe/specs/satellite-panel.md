## Capability: satellite-panel

### Description

卫星信号展示面板，支持折叠/展开两种状态。折叠态显示总览信息，展开态显示天球图（Sky Plot）可视化。

### Folded State

- 显示 `卫星 X/Y`（已用/可见总数）
- 信号强度进度条（颜色按平均 CN0 分级：≥35dB 绿色、≥20dB 黄色、<20dB 红色）
- 平均 CN0 值
- 点击展开/收起

### Expanded State (Sky Plot)

- **天球图**：Canvas 绘制的圆形天球图
  - 中心：线框地球（经纬线网格，半径 25%）
  - 外圆：地平线参考（1dp 边框）
  - 辅助线：30°/60° 仰角参考圆（虚线），N/E/S/W 方位标签
- **卫星点**：按 azimuth/elevation 投影
  - 圆点大小：CN0 映射 4dp-12dp
  - 实心圆 = inUse，空心圆 = 可见但未使用
  - 颜色按星座分配
- **左上角图例**：各星座颜色块 + 名称 + 已用/总数
- **底部汇总**：已用/可见总数 + 平均 CN0

### Data Dependencies

- `satellites: List<SatelliteInfo>` — 卫星列表（含 azimuth, elevation）
- `inUseCount: Int` — 已用卫星数
- `averageCn0: Float` — 平均 CN0
- `constellationStats: Map<Constellation, Int>` — 各星座已用数
- `constellationTotalStats: Map<Constellation, Int>` — 各星座可见总数（新增）
