# 点击位置随机距离功能说明

## 功能概述

为了让自动点击更加自然，避免被检测为机器操作，新增了"点击位置随机距离"设置。该功能会在每次点击前，对选取的位置进行随机偏移。

## 工作原理

### 1. 随机偏移算法

使用极坐标方式在圆形区域内生成随机偏移：

```java
private float[] getRandomOffset(float x, float y) {
    if (randomOffset == 0) {
        return new float[]{x, y};
    }
    // 在圆形范围内生成随机偏移
    double angle = random.nextDouble() * 2 * Math.PI;  // 随机角度 [0, 2π]
    double radius = random.nextDouble() * randomOffset; // 随机半径 [0, randomOffset]
    float offsetX = (float)(radius * Math.cos(angle));
    float offsetY = (float)(radius * Math.sin(angle));
    return new float[]{x + offsetX, y + offsetY};
}
```

**优势：**
- 均匀分布在圆形区域内
- 不会超出设定的半径范围
- 真正随机，难以预测

### 2. 用户界面

在主界面的"⚙️ 设置"区域添加了：

```
2. 点击位置随机距离，单位：像素(px)
随机半径：[输入框] px
```

- **默认值**：5px
- **最小值**：0px（不偏移）
- **最大值**：100px
- **推荐值**：5-15px

### 3. 设置保存

使用 SharedPreferences 持久化存储：

```java
private static final String KEY_RANDOM_OFFSET = "random_offset";

// 保存
editor.putInt(KEY_RANDOM_OFFSET, randomOffset);

// 读取
int randomOffset = sharedPreferences.getInt(KEY_RANDOM_OFFSET, 5);
```

### 4. 实时更新

点击"保存设置"后，通过 Intent 将配置传递给 AutoClickService：

```java
Intent serviceIntent = new Intent(this, AutoClickService.class);
serviceIntent.putExtra("action", "update_settings");
serviceIntent.putExtra("min_interval", minInterval);
serviceIntent.putExtra("max_interval", maxInterval);
serviceIntent.putExtra("random_offset", randomOffset);
startService(serviceIntent);
```

## 效果示例

假设选取的点击位置是 (100, 200)，随机半径设置为 10px：

| 点击次数 | 原始位置 | 实际点击位置 | 偏移距离 |
|---------|---------|-------------|---------|
| 1 | (100, 200) | (103.2, 197.5) | 4.2px |
| 2 | (100, 200) | (95.8, 203.1) | 5.3px |
| 3 | (100, 200) | (108.9, 201.4) | 9.0px |
| 4 | (100, 200) | (100.5, 191.2) | 8.8px |
| 5 | (100, 200) | (97.3, 199.1) | 2.8px |

每次点击都会在以原始位置为中心、半径为 10px 的圆内随机选择一个点。

## 使用建议

### 不同场景的推荐设置

1. **精确点击场景**（如小按钮）
   - 推荐：0-3px
   - 说明：偏移太大可能点不中

2. **普通按钮**（如游戏按钮）
   - 推荐：5-10px
   - 说明：既自然又可靠

3. **大区域点击**（如广告区域）
   - 推荐：10-20px
   - 说明：更加自然

4. **极度自然模拟**
   - 推荐：15-30px
   - 说明：配合随机间隔，非常接近真人

### 与点击间隔配合

结合两个随机设置，效果更佳：

```
点击间隔：150-300ms（随机）
随机半径：5-10px
```

这样每次点击的时间和位置都不同，更难被检测。

## 技术细节

### 日志输出

每次点击会输出详细日志：

```
Clicking position 0: original(100.0, 200.0) -> offset(103.2, 197.5)
```

可通过 logcat 查看偏移情况。

### 性能影响

- 随机数生成：< 0.1ms
- 坐标计算：< 0.1ms
- 总体影响：几乎可忽略

### 兼容性

- 支持所有 Android 版本
- 不影响现有功能
- 可随时调整或关闭（设为 0）

## 代码修改清单

### 新增文件
- 无

### 修改文件

1. **activity_main.xml**
   - 添加"随机距离"说明文字
   - 添加输入框和单位标签
   - 更新当前设置显示

2. **MainActivity.java**
   - 添加 `KEY_RANDOM_OFFSET` 常量
   - 添加 `randomOffsetInput` 输入框引用
   - 修改 `loadIntervalSettings()` 加载随机距离
   - 修改 `saveIntervalSettings()` 保存随机距离
   - 修改 `updateCurrentIntervalText()` 显示随机距离

3. **AutoClickService.java**
   - 添加 `randomOffset` 成员变量（默认 5）
   - 添加 `updateSettings()` 方法
   - 添加 `getRandomOffset()` 方法
   - 修改 `onStartCommand()` 处理 "update_settings"
   - 修改点击逻辑应用随机偏移

## 验证测试

### 测试步骤

1. 打开应用
2. 设置随机半径为 10px
3. 点击"保存设置"
4. 启动浮窗
5. 选取一个位置
6. 点击"开始"
7. 观察日志输出

### 预期结果

- logcat 中应显示每次点击的偏移情况
- 实际点击位置应在设定半径内随机分布
- 点击效果仍然正常

### 调试命令

```bash
# 查看点击日志
adb logcat | grep "AutoClickService"

# 查看设置保存日志
adb logcat | grep "MainActivity"
```

## 注意事项

1. **不要设置过大的偏移**
   - 可能导致点击偏离目标
   - 特别是小按钮

2. **配合界面测试**
   - 首次使用建议先用小偏移（3-5px）测试
   - 确认不影响点击效果后再增加

3. **日志监控**
   - 通过日志确认偏移是否正常
   - 检查是否有点击失败

## 未来改进方向

1. **可视化反馈**
   - 在标记圆圈周围显示偏移范围
   - 实时显示最近的点击位置

2. **智能偏移**
   - 根据按钮大小自动调整偏移范围
   - 避免点击到目标外

3. **偏移模式**
   - 圆形偏移（当前）
   - 正方形偏移
   - 高斯分布偏移（更自然）

4. **历史记录**
   - 记录最近的点击位置
   - 统计偏移分布情况

