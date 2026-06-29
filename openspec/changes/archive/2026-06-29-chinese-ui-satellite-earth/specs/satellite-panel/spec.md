## MODIFIED Requirements

### Requirement: 总览条

- 高度: 8dp
- 圆角: 4dp
- 背景: #2A2A3A
- 填充: 已用卫星数 / 可见卫星数 比例
- 填充色: 平均 CN0 对应颜色 (见下方阈值)
- 填充动画: 300ms 渐变

#### Scenario: 折叠态显示
- **WHEN** 卫星面板处于折叠状态
- **THEN** 显示 "卫星 {inUse}/{total}" 标签和信号强度条

### Requirement: 文字信息

- "卫星 {inUse}/{total}": 14sp, #E0E0E0
- "平均 CN0: {avg} dB": 12sp, #6B6B80
- "点击展开 ▾": 11sp, #6B6B80

#### Scenario: 折叠态文字
- **WHEN** 卫星面板处于折叠状态
- **THEN** 显示中文标签 "卫星"、"平均 CN0" 和 "点击展开"

### Requirement: 展开态布局

展开态 SHALL 显示地球图标、按星座分组的卫星卡片、底部统计。

#### Scenario: 展开态完整显示
- **WHEN** 用户点击展开卫星面板
- **THEN** 显示地球 emoji、虚线轨道、按星座分组的卫星卡片行、底部星座统计

### Requirement: 底部统计

- 格式: "GPS: 8  GLO: 3  BDS: 1"
- 12sp, #6B6B80
- 按 Constellation 分组统计 inUse 的卫星

#### Scenario: 显示星座统计
- **WHEN** 卫星面板展开
- **THEN** 底部显示各星座使用中卫星数量统计

### Requirement: 收起指示

- "点击收起 ▴": 11sp, #6B6B80

#### Scenario: 展开态收起提示
- **WHEN** 卫星面板处于展开状态
- **THEN** 显示中文 "点击收起" 提示文字
