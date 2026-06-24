## 1. 标题汉化

- [x] 1.1 修改 DashboardScreen.kt 中 TopBar 标题为 "GPS 仪表盘"
- [x] 1.2 修改 DashboardScreen.kt 中 DataCard 标签为中文（纬度、经度、海拔、速度、精度）
- [x] 1.3 修改 DashboardScreen.kt 中 PermissionScreen 文案为中文
- [x] 1.4 修改 SatellitePanel.kt 中概览栏标签为中文（卫星、平均 CN0、点击展开）

## 2. 时间时区调整

- [x] 2.1 修改 GpsViewModel.kt 中 utcTimeText 时区为 UTC+8
- [x] 2.2 修改 DashboardScreen.kt 中 BottomMeta 时间标签为 "UTC+8"

## 3. 卫星面板重构

- [x] 3.1 创建 SatelliteCard 组件（状态指示器、名称、信号条、CN0 值）
- [x] 3.2 创建 OrbitLine 组件（虚线轨道）
- [x] 3.3 重构 SatellitePanel 展开态布局（地球 emoji + 轨道 + 卡片分组）
- [x] 3.4 实现按星座分组逻辑（groupBy constellation）
- [x] 3.5 实现卫星名称格式化（{星座} {PRN两位数}）
- [x] 3.6 修改底部统计标签为中文格式

## 4. 测试与验证

- [x] 4.1 验证所有标题显示为中文
- [x] 4.2 验证时间显示为 UTC+8 时区
- [x] 4.3 验证卫星面板地球样式显示正确
- [x] 4.4 验证卫星卡片按星座分组显示
- [x] 4.5 验证信号强度条颜色分级正确
