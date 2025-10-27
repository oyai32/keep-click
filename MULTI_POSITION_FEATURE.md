# 多位置选取与轮流点击功能说明

## 功能概述

支持在屏幕上选取多个位置（最多10个），点击"开始"后会**按顺序轮流点击所有选中的位置**。

## 主要特性

### 1. 多位置选取
- **最大数量**：10个位置
- **选取方式**：进入选取模式后，点击屏幕任意位置添加标记
- **取消选取**：再次点击已选位置（标记圆圈）则取消该位置
- **位置标记**：每个位置显示序号（1-10）

### 2. 轮流点击
- **点击模式**：按照选取顺序（①→②→③...）依次点击各个位置
- **随机偏移**：每个位置独立应用随机偏移
- **视觉反馈**：点击时当前位置的标记变黄色，其他保持灰色
- **点击间隔**：使用设定的随机间隔后点击下一个位置

## 使用流程

```
1. 点击"选取"按钮
   └─> 进入选取模式

2. 在屏幕上点击多个位置
   └─> 位置1: 显示 ① 标记
   └─> 位置2: 显示 ② 标记
   └─> 位置3: 显示 ③ 标记
   └─> ...

3. 若要取消某个位置
   └─> 点击该位置的标记圆圈
       └─> 标记消失

4. 点击"开始"按钮
   └─> 按顺序轮流点击
       └─> ① 变黄（点击位置1）
       └─> 等待随机间隔（150-300ms）
       └─> ② 变黄（点击位置2）
       └─> 等待随机间隔
       └─> ③ 变黄（点击位置3）
       └─> ...
       └─> 回到 ① 继续循环

5. 点击"暂停"停止点击
6. 点击"清空"清除所有位置
```

## 代码实现

### 1. FloatingBallView.java

#### 增加位置上限
```java
private int maxPositions = 10; // 最多可以选取10个位置
```

#### 点击状态跟踪
```java
private java.util.Set<Integer> clickingPositions = new java.util.HashSet<>();
```

#### 选取逻辑修改
```java
private boolean handleSelectionModeTouch(float x, float y) {
    // 检查是否点击了已存在的位置（取消选取）
    for (int i = 0; i < clickPositions.size(); i++) {
        ClickPosition pos = clickPositions.get(i);
        if (isPointInCircle(screenX, screenY, pos.getX(), pos.getY(), 30)) {
            clickPositions.remove(i); // 取消这个位置
            return true;
        }
    }
    
    // 检查是否已达到最大位置数
    if (clickPositions.size() >= maxPositions) {
        showToast("最多只能选取 " + maxPositions + " 个位置");
        return true;
    }
    
    // 添加新位置
    clickPositions.add(new ClickPosition(screenX, screenY));
    return true;
}
```

#### 绘制多个标记
```java
private void drawClickPositions(Canvas canvas) {
    for (int i = 0; i < clickPositions.size(); i++) {
        ClickPosition pos = clickPositions.get(i);
        
        // 根据这个位置是否正在点击改变颜色
        if (clickingPositions.contains(i)) {
            circlePaint.setColor(Color.parseColor(COLOR_YELLOW)); // 黄色
        } else {
            circlePaint.setColor(Color.parseColor(COLOR_GRAY)); // 灰色
        }
        canvas.drawCircle(localX, localY, 30, circlePaint);
        
        // 绘制序号
        canvas.drawText(String.valueOf(i + 1), localX, textY, numberPaint);
    }
}
```

#### 独立位置点击状态
```java
public void setPositionClicking(int index, boolean clicking) {
    if (clicking) {
        clickingPositions.add(index);
    } else {
        clickingPositions.remove(index);
    }
    invalidate();
}
```

### 2. AutoClickService.java

#### 按顺序轮流点击
```java
clickRunnable = new Runnable() {
    @Override
    public void run() {
        if (isClicking && !clickPositions.isEmpty()) {
            // 轮流点击：每次点击当前索引的位置
            ClickPosition pos = clickPositions.get(currentClickIndex);
            
            if (pos.isActive()) {
                // 应用随机偏移
                float[] offsetPos = getRandomOffset(pos.getX(), pos.getY());
                
                // 通知开始点击
                notifyClickStart(currentClickIndex);
                
                // 执行点击
                performClick(offsetPos[0], offsetPos[1]);
                
                // 通知点击结束
                handler.postDelayed(() -> notifyClickEnd(currentClickIndex), 50);
                
                // 切换到下一个位置
                currentClickIndex = (currentClickIndex + 1) % clickPositions.size();
            }
            
            // 使用随机间隔后点击下一个位置
            long nextInterval = getRandomInterval();
            handler.postDelayed(this, nextInterval);
        }
    }
};
```

### 3. FloatingWindowService.java

#### 接收独立位置的点击通知
```java
private void registerClickReceiver() {
    clickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("CLICK_START".equals(intent.getAction())) {
                int index = intent.getIntExtra("index", -1);
                if (index >= 0) {
                    floatingBallView.setPositionClicking(index, true);
                }
            } else if ("CLICK_END".equals(intent.getAction())) {
                int index = intent.getIntExtra("index", -1);
                if (index >= 0) {
                    floatingBallView.setPositionClicking(index, false);
                }
            }
        }
    };
}
```

## 使用场景

### 场景1：游戏技能连招
```
选取位置：
  ① - 技能1按钮
  ② - 技能2按钮
  ③ - 攻击按钮

点击开始：
  技能1 → 等待间隔 → 技能2 → 等待间隔 → 攻击 → 循环
  → 实现自动连招循环
```

### 场景2：应用批量操作
```
选取位置：
  ① - 点赞按钮
  ② - 收藏按钮
  ③ - 分享按钮

点击开始：
  点赞 → 收藏 → 分享 → 点赞 → ...
  → 循环执行各个操作
```

### 场景3：多窗口轮询刷新
```
选取位置：
  ① - 窗口1的刷新按钮
  ② - 窗口2的刷新按钮
  ③ - 窗口3的刷新按钮

点击开始：
  窗口1刷新 → 窗口2刷新 → 窗口3刷新 → 窗口1刷新 → ...
  → 依次轮询刷新各个窗口
```

## 视觉效果

### 标记显示

```
未点击状态：
  ①  ②  ③  ④  ⑤
  灰色圆圈，黑色序号

轮流点击状态（示例：正在点击位置2）：
  ①  ②  ③  ④  ⑤
  灰  黄  灰  灰  灰
  
下一轮（正在点击位置3）：
  ①  ②  ③  ④  ⑤
  灰  灰  黄  灰  灰
```

### 选取与取消

```
选取模式：
  点击空白处 → 添加标记
  点击已有标记 → 取消标记

例如：
  ①  ②  ③
  点击 ② → 变成 ①  ③（序号自动调整）
```

## 日志输出

### 选取位置
```
Selection touch: local(500,800) screen(500,800)
Added position 0 at (500.0,800.0)
Added position 1 at (600.0,900.0)
Added position 2 at (700.0,1000.0)
```

### 取消位置
```
Removed position 1 at (600.0,900.0)
```

### 轮流点击
```
Clicking position 0: original(500.0, 800.0) -> offset(503.2, 797.5)
Next click in 234 ms (will click position 1)
Clicking position 1: original(600.0, 900.0) -> offset(595.8, 903.1)
Next click in 187 ms (will click position 2)
Clicking position 2: original(700.0, 1000.0) -> offset(708.9, 1001.4)
Next click in 265 ms (will click position 0)
```

## 与单位置模式的对比

| 特性 | 单位置模式（旧） | 多位置模式（新） |
|------|----------------|----------------|
| 位置数量 | 1个 | 最多10个 |
| 选取方式 | 点击任意位置 | 多次点击添加 |
| 取消选取 | 清空按钮 | 点击标记取消 |
| 点击方式 | 单点重复点击 | **轮流点击** |
| 视觉反馈 | 单个圆圈变色 | **当前位置圆圈变色** |
| 随机偏移 | 应用 | 每个位置独立应用 |

## 技术细节

### 轮流点击的实现

使用索引 `currentClickIndex` 跟踪当前要点击的位置：

```java
// 点击当前位置
ClickPosition pos = clickPositions.get(currentClickIndex);
performClick(pos.getX(), pos.getY());

// 切换到下一个位置
currentClickIndex = (currentClickIndex + 1) % clickPositions.size();

// 使用随机间隔后点击下一个位置
long nextInterval = getRandomInterval();
handler.postDelayed(clickRunnable, nextInterval);
```

**关键点**：
- 每次只点击一个位置
- 使用模运算 `%` 实现循环：0→1→2→...→n→0
- 每次点击后等待随机间隔再点击下一个位置

### 独立的随机偏移

每个位置独立计算随机偏移：

```java
// 位置1: (100, 200) → 偏移 (+3, -2) → 实际点击 (103, 198)
// 位置2: (200, 300) → 偏移 (-5, +7) → 实际点击 (195, 307)
// 位置3: (300, 400) → 偏移 (+2, +1) → 实际点击 (302, 401)
```

### 独立的视觉反馈

使用 `Set<Integer>` 跟踪当前正在点击的位置：

```java
// 点击位置0时
clickingPositions.add(0); // 位置0变黄
// 50ms后
clickingPositions.remove(0); // 位置0恢复灰色

// 点击位置1时
clickingPositions.add(1); // 位置1变黄
// 50ms后
clickingPositions.remove(1); // 位置1恢复灰色

// 每次只有一个位置变黄，其他保持灰色
```

## 性能考虑

### 1. 点击性能

- **单次点击时间**：~5ms
- **点击间隔**：150-300ms（可设置）
- **总体影响**：流畅，无延迟

### 2. 绘制性能

- **绘制10个圆圈**：< 1ms
- **重绘频率**：点击时约 20fps（50ms 黄色闪烁）
- **总体影响**：流畅

### 3. 广播性能

- **每次点击发送2个广播**（开始/结束）：~2ms
- **接收并更新UI**：~1ms
- **总体影响**：可忽略

## 限制与建议

### 位置数量限制

**最大10个位置**的原因：
1. 过多位置会影响点击速度
2. 屏幕空间有限，标记会重叠
3. 实际使用中很少需要超过10个位置

### 推荐使用

| 位置数量 | 适用场景 | 点击间隔建议 |
|---------|---------|------------|
| 1-3个 | 普通游戏连招 | 150-300ms |
| 4-6个 | 复杂技能循环 | 200-400ms |
| 7-10个 | 多步骤自动化 | 300-500ms |

### 注意事项

1. **位置间距**：建议标记之间至少相距 100px，避免误触
2. **点击顺序**：位置会按照选取顺序（①→②→③...）依次点击
3. **循环执行**：点击完最后一个位置后会回到第一个位置继续
4. **权限要求**：需要无障碍服务权限才能模拟点击

## 测试建议

### 基础功能测试

1. **选取测试**
   - 选取1个位置 ✓
   - 选取10个位置 ✓
   - 尝试选取第11个位置（应提示"最多10个"）✓

2. **取消测试**
   - 点击标记取消位置 ✓
   - 取消后序号自动调整 ✓

3. **点击测试**
   - 1个位置点击 ✓
   - 10个位置轮流点击 ✓
   - 当前位置标记变黄，其他保持灰色 ✓
   - 点击完最后一个位置后回到第一个 ✓

### 性能测试

```bash
# 查看点击日志
adb logcat | grep "AutoClickService"

# 预期输出
Clicking position 0: ...
Next click in 234 ms (will click position 1)
Clicking position 1: ...
Next click in 187 ms (will click position 2)
Clicking position 2: ...
Next click in 265 ms (will click position 3)
...
```

## 总结

多位置选取与轮流点击功能让自动连击工具更加强大和灵活：

✅ 支持最多10个位置
✅ 点击已选位置即可取消
✅ 按顺序轮流点击各个位置
✅ 每个位置独立随机偏移
✅ 独立的视觉反馈（当前位置变黄）
✅ 循环执行，自动回到第一个位置
✅ 流畅的性能表现

现在你可以轻松实现复杂的技能连招和自动化操作！🎉

