# 调试指南

## 已修复的问题

### 1. 视觉反馈在非选取模式下不可见
**原因**：状态文本的 Y 坐标设置为 520px，超出了工具栏的 500px 高度范围。

**修复**：将状态文本移到工具栏顶部（从 Y=10 开始），字体大小改为 14px，左对齐显示。

**验证方法**：
1. 打开应用
2. 不点击"选取"按钮
3. 在工具栏左上角应该能看到"穿透:开"的白色文字
4. 选择一个位置后，应该能看到"位置已选取"
5. 点击"开始"后，应该能看到"○等待"或"●点击中"以及点击计数

---

## 需要调试的问题

### 2. 点击"开始"后一直显示"等待"状态，未触发点击

**可能的原因**：
1. 无障碍服务未正确启用
2. 位置数据未正确传递到 AutoClickService
3. AutoClickService 的 dispatchGesture 失败

**调试步骤**：

#### 步骤 1：检查无障碍服务是否启用
1. 打开手机"设置" → "无障碍" → "已安装的服务"
2. 找到"Demo"或"Auto Click Service"
3. 确保开关已打开
4. 如果未启用，请启用它

#### 步骤 2：查看日志
使用 adb logcat 查看详细日志：

```bash
adb logcat | grep -E "(FloatingBallView|FloatingWindowService|AutoClickService)"
```

**关键日志点**：

**选择位置时应该看到**：
```
FloatingBallView: Selection touch: local(...) screen(...)
FloatingBallView: Added position: (x,y)
FloatingWindowService: onPositionSelected: (x,y)
AutoClickService: Click position added: x, y
```

**点击开始时应该看到**：
```
FloatingBallView: Start button clicked, positions: 1
FloatingBallView: Position 0: (x,y)
FloatingBallView: Calling listener.onStartClicking()
FloatingWindowService: onStartClicking called
FloatingWindowService: AutoClickService start intent sent
AutoClickService: Starting auto click with 1 positions
AutoClickService: Position 0: (x, y)
AutoClickService: Clicking position 0: (x, y)
AutoClickService: Click performed at: x, y
```

**如果看到 "No click positions set - cannot start clicking"**：
- 说明位置数据未传递到 AutoClickService
- 检查 onPositionSelected 是否被调用
- 检查 AutoClickService 是否正确接收 add_position intent

**如果看到 "Click cancelled"**：
- 说明 dispatchGesture 失败
- 可能是无障碍服务权限不足
- 尝试重启应用和无障碍服务

#### 步骤 3：检查广播接收
点击开始后，应该看到：
```
AutoClickService: Click start notification sent for index: 0
AutoClickService: Click end notification sent for index: 0
```

如果看到这些日志，但 UI 仍显示"等待"：
- 说明 FloatingWindowService 的 BroadcastReceiver 未收到广播
- 检查 registerClickReceiver() 是否被调用

#### 步骤 4：测试简化场景
1. 打开应用
2. 点击"选取"
3. 点击屏幕上的任意位置（非工具栏）
4. 点击"开始"
5. 观察日志输出

---

## 常见问题

### Q: 无法选取位置
**A**: 确保点击"选取"按钮后，按钮变为蓝色，然后点击屏幕其他位置（非工具栏区域）。

### Q: 工具栏无法操作
**A**: 检查窗口大小是否正确。非选取模式下应该是 200x500，选取模式下应该是 MATCH_PARENT。

### Q: 点击事件未穿透
**A**: 检查 dispatchTouchEvent 是否正确返回 false。查看日志中的 "Outside toolbar, returning false" 信息。

---

## 预期行为

1. **初始状态**：工具栏左上角显示"穿透:开"
2. **点击选取**：按钮变蓝，工具栏左上角显示"穿透:关"
3. **选择位置**：屏幕上出现灰色圆圈，工具栏显示"位置已选取"
4. **点击开始**：
   - 开始按钮变蓝
   - 工具栏显示"○等待"
   - 每 0.15 秒，圆圈变黄，显示"●点击中"
   - 点击计数增加
   - 底层应用应该收到点击事件
5. **点击暂停**：暂停按钮变蓝，显示"已暂停(X次)"

---

## 如果问题仍然存在

请提供以下信息：
1. adb logcat 的完整输出（从选择位置到点击开始的所有日志）
2. 无障碍服务是否已启用的截图
3. 工具栏上显示的状态文字内容

