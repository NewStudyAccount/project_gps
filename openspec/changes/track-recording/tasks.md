# 轨迹记录与后台定位 — 开发任务

## 阶段一：基础设施 (后台 Service + 共享状态)

- [x] **1.1** 添加权限声明和依赖
  - AndroidManifest.xml 添加 FOREGROUND_SERVICE、FOREGROUND_SERVICE_LOCATION、POST_NOTIFICATIONS、ACCESS_BACKGROUND_LOCATION 权限
  - AndroidManifest.xml 声明 LocationForegroundService (foregroundServiceType="location")
  - build.gradle.kts 添加 Room 依赖 + KSP 插件
  - 同步 Gradle 验证编译通过

- [x] **1.2** 实现 LocationStateHolder 单例
  - 创建 `data/location/LocationStateHolder.kt`
  - `location: StateFlow<Location?>` 由 Service 写入
  - `isServiceRunning: StateFlow<Boolean>` 追踪 Service 状态
  - 提供 `update(location)` 和 `setServiceRunning()` 方法

- [x] **1.3** 实现 LocationForegroundService
  - 创建 `service/LocationForegroundService.kt`
  - 继承 `Service`，`onStartCommand` 中注册 LocationManager
  - GPS 回调中更新 LocationStateHolder
  - `START_STICKY` 策略确保被杀后重启
  - 支持 ACTION_START / ACTION_STOP_SERVICE intent

- [x] **1.4** 实现通知管理
  - 创建 `service/TrackingNotificationManager.kt`
  - 构建"后台运行中"通知 (含"打开应用"按钮)
  - 构建"录制中"通知 (含实时数据 + "停止录制"按钮)
  - NotificationChannel 创建 (IMPORTANCE_LOW)
  - Service 中调用 `startForeground()` + 通知更新

- [x] **1.5** 修改 GpsViewModel 使用 LocationStateHolder
  - 移除 `LocationRepository` 的直接使用
  - 改为从 `LocationStateHolder.location` 收集数据
  - 保留 SensorRepository 和 SatelliteRepository 不变
  - 验证仪表盘 UI 行为不变

- [x] **1.6** 实现 Service 生命周期管理
  - DashboardScreen 中添加 LaunchedEffect：权限就绪时启动 Service
  - GpsApplication 中添加 Service 启动/停止辅助方法
  - 处理 Activity 重建时 Service 已在运行的情况
  - 验证：启动 app → 通知出现 → 切后台 → 通知仍在 → 回来 → 数据继续

## 阶段二：数据库 + 轨迹录制

- [x] **2.1** 定义 Room 实体和 DAO
  - 创建 `data/model/Track.kt` (Entity)
  - 创建 `data/model/TrackPoint.kt` (Entity + ForeignKey)
  - 创建 `data/db/TrackDao.kt` (CRUD + Flow 查询)
  - 创建 `data/db/TrackPointDao.kt` (按 trackId 查询 + 批量插入)
  - 创建 `data/db/TrackingDatabase.kt` (RoomDatabase)

- [x] **2.2** 实现 GpsApplication 初始化数据库
  - 在 `GpsApplication` 中创建 `TrackingDatabase` 单例
  - 通过 companion object 或 Hilt 暴露实例
  - 验证数据库创建成功 (无崩溃)

- [x] **2.3** 实现 AdaptiveSampler
  - 创建 `data/recorder/AdaptiveSampler.kt`
  - 实现 MotionProfile 枚举 (STATIONARY/WALKING/CYCLING/DRIVING)
  - 实现 `classifyMotion(speed)` 速度分类
  - 实现 `shouldRecord(location)` 时间+距离双重判断
  - 实现 `onRecorded(location)` 更新状态
  - 编写简单测试验证各速度区间的采样逻辑

- [x] **2.4** 实现 TrackRecorder
  - 创建 `data/recorder/TrackRecorder.kt`
  - 实现 `startRecording()` 创建 Track + 启动收集协程
  - 实现 `pauseRecording()` / `resumeRecording()` 暂停/恢复
  - 实现 `stopRecording()` 停止 + 更新 Track 统计
  - 每次 shouldRecord 通过时写入 TrackPoint 到 Room
  - 暴露 `state: StateFlow<State>` 和 `currentTrackStats: StateFlow<TrackStats>`

- [x] **2.5** 实现 TrackRepository
  - 创建 `data/repository/TrackRepository.kt`
  - 封装 TrackDao 和 TrackPointDao 的常用操作
  - `getAllTracks(): Flow<List<Track>>`
  - `getTrackPoints(trackId): List<TrackPoint>`
  - `deleteTrack(trackId)` 同时删除关联点
  - `deleteAll()` 清空所有轨迹

- [x] **2.6** 实现录制状态 UI (RecordingBar)
  - 创建 `ui/component/RecordingBar.kt`
  - 显示：录制时长、距离、点数、当前速度、运动状态
  - 红色脉冲动画表示录制中
  - 仅在录制状态时显示 (AnimatedVisibility)

- [x] **2.7** 实现 DashboardScreen 录制集成
  - 创建 `ui/viewmodel/TrackingViewModel.kt`
  - 在 DashboardScreen 底部添加"开始录制"/"暂停"/"停止"按钮
  - 在数据卡片上方插入 RecordingBar
  - 按钮状态与录制状态机联动
  - 录制中启动/停止 Service 通知更新

## 阶段三：降采样 + 导出

- [x] **3.1** 实现 RDP 降采样算法
  - 创建 `util/TrackCompressor.kt`
  - 实现 `simplify(points, epsilon)` 递归 RDP
  - 实现 `perpendicularDistance(point, lineStart, lineEnd)` 点到线段距离
  - 使用 Haversine 公式计算真实米制距离
  - `stopRecording()` 中调用，压缩后标记 `isOriginal=false`

- [x] **3.2** 实现 GPX 导出
  - 创建 `export/GpxExporter.kt`
  - 实现 `export(track, points): String` 生成 GPX XML
  - 实现 `saveToFile(context, track, points): Uri` 保存到 Downloads
  - 时间格式: ISO 8601 UTC

- [x] **3.3** 实现 KML 导出
  - 创建 `export/KmlExporter.kt`
  - 实现 `export(track, points): String` 生成 KML XML
  - 实现 `saveToFile(context, track, points): Uri`

- [x] **3.4** 实现分享功能
  - TrackRepository 中添加 `shareTrack(context, trackId, format)` 方法
  - 使用 `Intent.ACTION_SEND` + `Intent.createChooser`
  - 支持 GPX / KML 格式选择
  - FileProvider 配置 (AndroidManifest.xml + file_paths.xml)

## 阶段四：历史轨迹列表

- [x] **4.1** 实现 TrackListScreen
  - 创建 `ui/viewmodel/TrackListViewModel.kt`
  - 创建 `ui/screen/TrackListScreen.kt`
  - 列表项：轨迹名称、距离、时长、均速、点数
  - 操作按钮：回放、回溯、导出、删除
  - 空状态提示
  - 删除确认对话框

- [x] **4.2** 实现导航路由
  - 添加 Navigation Compose 依赖
  - 定义路由：Dashboard / TrackList / TrackDetail / TrackReplay / Backtrack
  - DashboardScreen "历史轨迹" 按钮 → TrackListScreen
  - 实现返回栈管理

## 阶段五：Canvas 轨迹地图

- [x] **5.1** 实现坐标投影
  - 创建 `ui/component/TrackProjection.kt`
  - 输入：轨迹点列表 + Canvas 尺寸
  - 输出：`toScreen(lat, lng): Offset`
  - 自动计算 bounding box + padding
  - 处理经度随纬度的缩放 (cos 校正)

- [x] **5.2** 实现 TrackCanvas 基础绘制
  - 创建 `ui/component/TrackCanvas.kt`
  - Layer 1: 背景填充 (#0A0A0F)
  - Layer 2: 网格点 (#2A2A3A)
  - Layer 3: 轨迹线 (Path + drawPath, #FF8C00, 2dp)
  - Layer 4: 起点/终点标记
  - Layer 5: 当前位置脉冲圆

- [x] **5.3** 实现 TrackCanvas 手势交互
  - 双指缩放 (scale 范围 0.5x ~ 20x)
  - 单指拖拽平移
  - 双击重置视图
  - 缩放以触摸中心为锚点
  - 缩放时网格密度自适应调整

- [x] **5.4** 实现轨迹点信息弹窗
  - 点击轨迹点时显示详情：时间、速度、海拔、精度
  - Popup 或 Canvas 内绘制浮层
  - 点击空白处关闭

## 阶段六：轨迹回放

- [x] **6.1** 实现 ReplayViewModel
  - 创建 `ui/viewmodel/ReplayViewModel.kt`
  - 加载指定 trackId 的所有点
  - 管理回放状态：播放/暂停/跳转
  - 回放速度控制 (0.5x/1x/2x/4x)
  - 暴露 `currentIndex: StateFlow<Int>` 控制地图显示

- [x] **6.2** 实现 TrackReplayScreen
  - 创建 `ui/screen/TrackReplayScreen.kt`
  - 全屏 TrackCanvas (回放模式)
  - 已走轨迹: 灰色, 未走轨迹: 琥珀橙
  - 当前位置高亮 + 移动动画
  - 顶部信息栏：轨迹名称、当前时间点

- [x] **6.3** 实现回放控制条
  - 创建 `ui/component/ReplayControls.kt`
  - 播放/暂停按钮
  - 进度条 (可拖拽跳转)
  - 速度选择 (0.5x/1x/2x/4x)
  - 当前时间 / 总时长显示
  - 上一个/下一个关键点跳转

## 阶段七：轨迹回溯

- [x] **7.1** 实现 BacktrackEngine 核心算法
  - 创建 `backtrack/BacktrackEngine.kt`
  - 实现 `pointToSegmentDistance()` 点到线段距离
  - 实现 `findNearestSegment()` 找最近轨迹段
  - 实现 `calculateProjection()` 计算投影点
  - 实现 `calculateTurn()` 计算转向方向和距离
  - 实现 `detectDeviated()` 偏离检测

- [x] **7.2** 实现 BacktrackViewModel
  - 创建 `ui/viewmodel/BacktrackViewModel.kt`
  - 加载轨迹点，反转回溯方向
  - 每次 GPS 更新调用 BacktrackEngine
  - 暴露 `backtrackState: StateFlow<BacktrackState>`
  - 管理回溯生命周期 (启动/暂停/结束)

- [x] **7.3** 实现 BacktrackScreen 仪表盘指引模式
  - 创建 `ui/screen/BacktrackScreen.kt`
  - 大箭头指向下一个目标点 (Canvas 绘制)
  - 距离显示 (下一目标 + 剩余总距离)
  - 转向提示文字 ("前方 200m 右转")
  - 状态指示 (绿色=在轨迹上, 黄色=接近偏离, 红色=偏离)
  - "切换地图" 按钮

- [x] **7.4** 实现 BacktrackScreen 地图模式
  - TrackCanvas 回溯模式绘制
  - 已走路段: 虚线灰色
  - 待走路段: 实线琥珀橙
  - 当前位置 + 方向指针
  - 下一个目标点标记
  - "切换指引" 按钮

- [x] **7.5** 实现偏离处理 UI
  - 偏离状态下的专用界面
  - 大箭头指向最近轨迹点
  - "返回轨迹 XXm" 文字
  - 距离轨迹点过远时提供"放弃回溯"选项

## 阶段八：集成与打磨

- [x] **8.1** 权限请求流程完善
  - Android 13+ POST_NOTIFICATIONS 运行时请求
  - ACCESS_BACKGROUND_LOCATION 引导弹窗 (解释为何需要后台定位)
  - 权限未授予时的降级处理 (仅前台定位)
  - 权限状态检查与 UI 联动

- [x] **8.2** 电池优化引导
  - 检测是否在电池优化白名单中
  - 引导弹窗引导用户关闭电池优化 (厂商兼容)
  - 提供"稍后"选项，不阻塞使用

- [x] **8.3** 边界情况处理
  - GPS 信号丢失时 RecordingBar 显示"信号弱"
  - 轨迹点不足 (≤2) 时禁用回放/回溯
  - 回溯过程中 GPS 信号丢失 → 保持最后状态
  - 录制中 app 被强制停止 → 数据不丢失 (Room 事务)
  - 空轨迹自动清理 (录制后立即停止且无移动)

- [x] **8.4** 性能优化
  - TrackCanvas 大量点视口裁剪 (只绘制可见区域)
  - Room 批量插入使用事务 (insertAll)
  - 回溯算法中限制扫描范围 (只检查最近 N 段)
  - 避免不必要的 StateFlow 重订阅

- [x] **8.5** UI 样式统一
  - 所有新增页面使用现有主题色 (Background/Surface/Primary/Border)
  - 字体统一 JetBrains Mono
  - 按钮/卡片样式与 DashboardScreen 一致
  - 新增中文字符串资源化

- [x] **8.6** 整体功能测试
  - 后台运行: 启动→切后台→5分钟后回来→数据连续
  - 轨迹录制: 开始→行走→暂停→恢复→停止→查看轨迹
  - 导出: 选择轨迹→导出GPX→分享到微信→对方能打开
  - 回放: 选择轨迹→播放→暂停→跳转→变速
  - 回溯: 选择轨迹→开始回溯→行走→偏离→返回→到达终点
  - 权限: 首次安装→逐步授权→后台定位生效
