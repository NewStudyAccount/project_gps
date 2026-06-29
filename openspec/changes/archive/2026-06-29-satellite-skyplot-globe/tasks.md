## 1. 颜色与数据准备

- [x] 1.1 在 `Color.kt` 中新增星座颜色：`ConstellationQzss = Color(0xFFFF6B9D)`、`ConstellationSbas = Color(0xFF9B59B6)`
- [x] 1.2 在 `GpsViewModel.kt` 中新增 `constellationTotalStats: StateFlow<Map<Constellation, Int>>`，统计各星座可见卫星总数（非仅 inUse）
- [x] 1.3 在 `Constellation` 枚举中新增 `color` 属性，返回对应的星座颜色

## 2. SkyPlotCanvas 组件

- [x] 2.1 创建 `SkyPlotCanvas.kt` 文件，定义 `SkyPlotCanvas` Composable
- [x] 2.2 实现天球图外圆绘制（`Border` 颜色，1dp 描边）
- [x] 2.3 实现方位刻度线绘制（N/E/S/W 标签，`TextDim` 颜色）
- [x] 2.4 实现仰角参考圆绘制（30°、60° 虚线圆）
- [x] 2.5 实现线框地球绘制（经纬线网格，中心 25% 半径）
- [x] 2.6 实现卫星投影计算函数：`azimuth + elevation → (x, y)` 等距方位投影
- [x] 2.7 实现卫星圆点绘制（CN0 → 大小映射，inUse → 实心/空心）
- [x] 2.8 实现左上角图例面板（星座颜色块 + 名称 + 已用/总数）
- [x] 2.9 实现底部汇总栏（已用/可见总数 + 平均 CN0）

## 3. SatellitePanel 重构

- [x] 3.1 保留 `SatellitePanel` 的折叠概览栏（总览条不变）
- [x] 3.2 替换展开视图内容：移除旧的卡片列表，嵌入 `SkyPlotCanvas`
- [x] 3.3 将 `constellationStats` 参数改为同时传递 `constellationTotalStats`
- [x] 3.4 调整展开动画（保留现有的 expandVertically + fadeIn）

## 4. DashboardScreen 适配

- [x] 4.1 更新 `DashboardScreen.kt` 中 `SatellitePanel` 调用，传入新增的 `constellationTotalStats` 参数

## 5. 测试与验证

- [x] 5.1 验证天球图正确显示线框地球（经纬线可见）
- [x] 5.2 验证卫星点按 azimuth/elevation 正确投影
- [x] 5.3 验证圆点大小随 CN0 值变化
- [x] 5.4 验证 inUse 卫星为实心圆，非 inUse 为空心圆
- [x] 5.5 验证左上角图例颜色与卫星点颜色一致
- [x] 5.6 验证底部汇总数据正确
- [x] 5.7 验证折叠/展开动画正常
