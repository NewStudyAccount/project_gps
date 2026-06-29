# GPS Dashboard — 开发任务

## 阶段一：项目基础

- [ ] **1.1** 初始化 Android 项目
  - 创建 Gradle 项目结构 (Kotlin, Compose)
  - 配置 minSdk=26, targetSdk=35
  - 添加 Compose, Material3 依赖
  - 配置 JetBrains Mono 字体资源

- [ ] **1.2** 建立主题系统
  - 定义 Color.kt (完整配色方案)
  - 定义 Type.kt (JetBrains Mono 字体)
  - 定义 Theme.kt (深色主题, 无亮色)

## 阶段二：数据层

- [ ] **2.1** 创建数据模型
  - GpsData, FixType
  - SatelliteInfo, Constellation
  - CompassData, SensorAccuracy
  - SpeedUnit enum

- [ ] **2.2** 实现 RingBuffer
  - 泛型环形缓冲区
  - 容量 60
  - 支持 toList(), add(), isEmpty()

- [ ] **2.3** 实现 LocationRepository
  - 封装 LocationManager
  - 提供 locationFlow
  - GPS + NETWORK 双 provider

- [ ] **2.4** 实现 SensorRepository
  - 加速度计 + 磁力计数据合并
  - 低通滤波 (alpha=0.15)
  - GeomagneticField 真北修正
  - 提供 compassFlow 和 accuracyFlow

- [ ] **2.5** 实现 SatelliteRepository
  - GnssStatus.Callback 封装
  - 提取卫星信息列表
  - 提供 satelliteFlow

## 阶段三：工具类

- [ ] **3.1** 实现 CoordinateFormatter
  - toDMS(decimal, isLat) → "39°54'15.1" N"
  - 方向判断 (N/S, E/W)

- [ ] **3.2** 实现 SpeedConverter
  - m/s → km/h, m/s, mph 换算
  - format(speed, unit) → "12.3 km/h"

- [ ] **3.3** 实现 AccuracyEvaluator
  - evaluate(accuracy) → AccuracyLevel
  - GOOD (±0~5m), MEDIUM (±5~15m), BAD (±15m+)

## 阶段四：ViewModel

- [ ] **4.1** 实现 GpsViewModel
  - 收集三个 Repository 的 Flow
  - 派生状态 (格式化坐标, 真北方位, 精度等级)
  - 海拔历史环形缓冲区 (每秒采样)
  - speedUnit 状态
  - satelliteExpanded 状态
  - 权限状态管理

## 阶段五：UI 组件

- [ ] **5.1** 实现 DataCard 通用组件
  - Label + Value + Unit 布局
  - 统一样式 (背景/边框/圆角)

- [ ] **5.2** 实现 AccuracyBar 组件
  - 进度条绘制
  - 三色阈值
  - 颜色渐变动画

- [ ] **5.3** 实现 SpeedUnitPicker 组件
  - DropdownMenu 弹出
  - 单位选择逻辑
  - 选中高亮

- [ ] **5.4** 实现 CompassView 组件
  - Canvas 绘制刻度环
  - 方位字母
  - 中心指针 + 发光
  - 度数读数文字
  - Spring 旋转动画
  - 角度环绕处理

- [ ] **5.5** 实现 AltitudeChart 组件
  - Canvas 绘制贝塞尔曲线
  - 渐变填充
  - Y 轴自适应
  - 网格线

- [ ] **5.6** 实现 SatellitePanel 组件
  - 折叠态 (总览条 + 文字)
  - 展开态 (6 列网格 + 信号柱)
  - 底部星座统计
  - AnimatedVisibility 动画

## 阶段六：页面组装

- [ ] **6.1** 实现 DashboardScreen
  - 竖屏布局组装
  - 顶栏 (卫星数 + 定位类型)
  - 罗盘区
  - 坐标卡片区 (2x3 网格)
  - 海拔图表区
  - 卫星面板区
  - 底部元数据栏
  - ScrollableColumn 处理小屏适配

- [ ] **6.2** 实现权限请求界面
  - 首次启动权限请求
  - 未授权提示卡片
  - 跳转设置按钮

- [ ] **6.3** 实现 MainActivity
  - 设置 Compose content
  - 权限检查入口
  - 横竖屏锁定 (竖屏)

## 阶段七：打磨

- [ ] **7.1** 动画调优
  - 罗盘 spring 参数微调
  - 精度条颜色过渡
  - 卫星面板展开流畅度

- [ ] **7.2** 边界情况处理
  - GPS 信号丢失时的 UI 状态
  - 无卫星时的空状态
  - 海拔数据不足 60 点时的图表
  - 坐标为 0° 时的显示

- [ ] **7.3** 性能优化
  - Canvas 绘制性能
  - StateFlow 收集效率
  - 避免不必要的重组
