## MODIFIED Requirements

### Requirement: 纬度卡片

- Label: "纬度", #00E5FF, 11sp
- Value: `39°54'15.1" N`, #FF8C00, 20sp
- 格式化: `CoordinateFormatter.toDMS(lat, isLat=true)`

#### Scenario: 显示纬度
- **WHEN** GPS 数据可用
- **THEN** 卡片标签显示 "纬度"，值显示格式化的纬度坐标

### Requirement: 经度卡片

- Label: "经度", #00E5FF, 11sp
- Value: `116°24'26.6" E`, #FF8C00, 20sp
- 格式化: `CoordinateFormatter.toDMS(lon, isLat=false)`

#### Scenario: 显示经度
- **WHEN** GPS 数据可用
- **THEN** 卡片标签显示 "经度"，值显示格式化的经度坐标

### Requirement: 海拔卡片

- Label: "海拔", #00E5FF, 11sp
- Value: `43.5`, #FF8C00, 24sp
- Unit: "m", #6B6B80, 14sp

#### Scenario: 显示海拔
- **WHEN** GPS 数据可用
- **THEN** 卡片标签显示 "海拔"，值显示海拔高度

### Requirement: 速度卡片

- Label: "速度", #00E5FF, 11sp
- Value: `12.3`, #FF8C00, 24sp
- Unit: 当前单位标签, #6B6B80, 14sp, 可点击
- 交互: 点击弹出 SpeedUnitPicker

#### Scenario: 显示速度
- **WHEN** GPS 数据可用
- **THEN** 卡片标签显示 "速度"，值显示当前速度

### Requirement: 精度卡片

- Label: "精度", #00E5FF, 11sp
- Value: `± 3.2`, #FF8C00, 20sp
- Unit: "m", #6B6B80, 14sp
- 状态条: AccuracyBar 组件

#### Scenario: 显示精度
- **WHEN** GPS 数据可用
- **THEN** 卡片标签显示 "精度"，值显示精度范围

### Requirement: HDOP 卡片

- Label: "HDOP", #00E5FF, 11sp
- Value: `0.8`, #FF8C00, 24sp
- 无单位

#### Scenario: 显示 HDOP
- **WHEN** GPS 数据可用
- **THEN** 卡片标签显示 "HDOP"，值显示 HDOP 数值
