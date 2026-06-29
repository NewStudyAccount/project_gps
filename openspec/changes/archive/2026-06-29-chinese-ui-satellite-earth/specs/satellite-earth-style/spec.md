## ADDED Requirements

### Requirement: 地球图标展示
卫星面板展开时 SHALL 在顶部居中显示地球 emoji 🌍，尺寸为 48dp。

#### Scenario: 展开面板显示地球
- **WHEN** 用户点击卫星概览栏展开面板
- **THEN** 面板顶部居中显示 🌍 emoji，尺寸 48dp

### Requirement: 虚线轨道连接
每个星座分组 SHALL 有一条虚线轨道从地球图标延伸至卫星卡片行。

#### Scenario: 轨道线显示
- **WHEN** 卫星面板展开且该星座有可见卫星
- **THEN** 显示从地球图标下方到该星座卫星行的虚线轨道

#### Scenario: 无卫星时隐藏轨道
- **WHEN** 某星座没有可见卫星
- **THEN** 不显示该星座的轨道线和卡片

### Requirement: 按星座分组展示
卫星卡片 SHALL 按星座分组排列，每组为一行，每行最多 6 个卡片。

#### Scenario: 多星座分组
- **WHEN** 有 GPS、BDS、GLONASS 三个星座的卫星
- **THEN** 显示三行卡片，每行对应一个星座

#### Scenario: 单星座多卫星换行
- **WHEN** 某星座有超过 6 颗卫星
- **THEN** 该星座的卫星卡片换行显示

### Requirement: 卫星卡片内容
每张卫星卡片 SHALL 显示状态指示器、卫星名称、信号强度条和 CN0 值。

#### Scenario: 使用中卫星卡片
- **WHEN** 卫星 inUse = true
- **THEN** 状态指示器显示 ◉，颜色为主题色

#### Scenario: 未使用卫星卡片
- **WHEN** 卫星 inUse = false
- **THEN** 状态指示器显示 ○，颜色为灰色

#### Scenario: 卫星名称格式
- **WHEN** 显示卫星卡片
- **THEN** 名称格式为 `{星座缩写} {PRN两位数}`，如 GPS 01、BDS 05

### Requirement: 信号强度条颜色分级
信号强度条颜色 SHALL 根据 CN0 值分级显示。

#### Scenario: 强信号
- **WHEN** CN0 >= 35 dB
- **THEN** 信号条颜色为绿色 #00FF6A

#### Scenario: 中等信号
- **WHEN** 20 <= CN0 < 35 dB
- **THEN** 信号条颜色为黄色 #FFD700

#### Scenario: 弱信号
- **WHEN** CN0 < 20 dB
- **THEN** 信号条颜色为红色 #FF3D00
