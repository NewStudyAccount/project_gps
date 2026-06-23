# 罗盘组件 (CompassView)

## 概述

自定义 Canvas 绘制的圆形罗盘，显示真北方位，带阻尼旋转动画。

## 输入

| 参数 | 类型 | 说明 |
|------|------|------|
| heading | Float | 真北方位角 0-360° |
| modifier | Modifier | Compose 修饰符 |

## 绘制规格

### 外圈刻度环

- 半径: `canvasWidth * 0.45`
- 主刻度 (每 30°): 线长 12dp, 线宽 2dp, 颜色 #E0E0E0
- 次刻度 (每 10°): 线长 8dp, 线宽 1.5dp, 颜色 #6B6B80
- 细刻度 (每 5°): 线长 5dp, 线宽 1dp, 颜色 #2A2A3A

### 方位字母

- 位置: 刻度环内侧 20dp
- 八方位: N/NE/E/SE/S/SW/W/NW
- N 使用 #FF8C00 (琥珀橙), 字号 18sp, 加粗
- 其余使用 #00E5FF (青色), 字号 14sp
- 旋转跟随刻度盘

### 内圈装饰

- 半径: `canvasWidth * 0.30`
- 圆环: 宽 1dp, 颜色 #2A2A3A
- 内部填充: 微渐变 #14141F → #0A0A0F

### 中心指针

- 形状: 等腰三角形, 底宽 16dp, 高 24dp
- 颜色: #FF8C00
- 发光: 底部 drawCircle, 半径 8dp, alpha 0.3, 颜色 #FF8C00
- 中心点: 半径 4dp 圆, 颜色 #FF8C00

### 度数读数

- 位置: 罗盘下方 16dp
- 格式: `"227° SW"`
- 字号: 24sp
- 颜色: #FF8C00

## 旋转行为

- 刻度盘旋转角度: `-heading` 度 (设备顺时针转 → 刻度盘逆时针转)
- 指针固定朝上 (代表北方)
- 动画: `Animatable` + Spring
  - stiffness: 100f
  - dampingRatio: 0.7f
- 角度环绕处理: 计算最短旋转路径, 避免 359→1 的长路径跳转

```kotlin
fun shortestAngleDiff(current: Float, target: Float): Float {
    return ((target - current + 540) % 360) - 180
}
```

## 约束

- 组件应为正方形 (宽=高)
- 最小尺寸: 200dp
- 最大尺寸: 屏幕宽度 * 0.85
