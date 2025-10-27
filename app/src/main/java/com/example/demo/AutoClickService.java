package com.example.demo;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.List;

public class AutoClickService extends AccessibilityService {
    private static final String TAG = "AutoClickService";
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clickRunnable;
    private boolean isClicking = false;
    private List<ClickPosition> clickPositions = new ArrayList<>();
    private int currentClickIndex = 0;
    private long minClickInterval = 150; // 最小间隔，默认150ms
    private long maxClickInterval = 300; // 最大间隔，默认300ms
    private int randomOffset = 10; // 随机偏移半径，默认5px
    private java.util.Random random = new java.util.Random();
    
    public static class ClickPosition {
        private float x;
        private float y;
        private boolean isActive;
        
        public ClickPosition(float x, float y) {
            this.x = x;
            this.y = y;
            this.isActive = true;
        }
        
        public float getX() { return x; }
        public float getY() { return y; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { this.isActive = active; }
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不需要处理特定事件
    }
    
    @Override
    public void onInterrupt() {
        stopClicking();
    }
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "=== AutoClickService connected ===");
        
        // 检查服务配置
        android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            Log.d(TAG, "Service capabilities:");
            Log.d(TAG, "  - Can retrieve window content: " + info.getCanRetrieveWindowContent());
            
            // 检查是否可以执行手势（Android 7.0+）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                // 注意：AccessibilityServiceInfo 没有直接的 canPerformGestures 方法
                // 但我们可以通过 capabilities 检查
                int capabilities = info.getCapabilities();
                boolean canPerformGestures = (capabilities & android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) != 0;
                
                if (canPerformGestures) {
                    Log.d(TAG, "  - ✓ Can perform gestures: YES");
                } else {
                    Log.e(TAG, "  - ✗ Can perform gestures: NO (THIS IS THE PROBLEM!)");
                    Log.e(TAG, "  - Please disable and re-enable the accessibility service");
                }
            }
            
            Log.d(TAG, "  - Event types: " + info.eventTypes);
            Log.d(TAG, "  - Feedback type: " + info.feedbackType);
            Log.d(TAG, "  - Flags: " + info.flags);
            Log.d(TAG, "  - Capabilities: " + info.getCapabilities());
        } else {
            Log.e(TAG, "Service info is null!");
        }
        
        // 检查是否支持手势
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Log.d(TAG, "✓ Device supports gesture dispatch (API >= 24)");
        } else {
            Log.e(TAG, "✗ Device does NOT support gesture dispatch (API < 24)");
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("start".equals(action)) {
                startClicking();
            } else if ("stop".equals(action)) {
                stopClicking();
            } else if ("add_position".equals(action)) {
                float x = intent.getFloatExtra("x", -1);
                float y = intent.getFloatExtra("y", -1);
                if (x >= 0 && y >= 0) {
                    addClickPosition(x, y);
                }
            } else if ("remove_position".equals(action)) {
                int index = intent.getIntExtra("index", -1);
                if (index >= 0) {
                    removeClickPosition(index);
                }
            } else if ("clear_positions".equals(action)) {
                clearClickPositions();
            } else if ("update_interval".equals(action)) {
                long minInterval = intent.getLongExtra("min_interval", 150);
                long maxInterval = intent.getLongExtra("max_interval", 300);
                updateClickInterval(minInterval, maxInterval);
            } else if ("update_settings".equals(action)) {
                long minInterval = intent.getLongExtra("min_interval", 150);
                long maxInterval = intent.getLongExtra("max_interval", 300);
                int offset = intent.getIntExtra("random_offset", 10);
                updateSettings(minInterval, maxInterval, offset);
            }
        }
        return START_STICKY;
    }
    
    
    public void addClickPosition(float x, float y) {
        ClickPosition pos = new ClickPosition(x, y);
        this.clickPositions.add(pos);
        Log.d(TAG, "Click position added: " + x + ", " + y);
    }
    
    public void removeClickPosition(int index) {
        if (index >= 0 && index < clickPositions.size()) {
            clickPositions.remove(index);
            Log.d(TAG, "Click position removed at index: " + index);
        }
    }
    
    public void clearClickPositions() {
        clickPositions.clear();
        Log.d(TAG, "All click positions cleared.");
    }
    
    public void updateClickInterval(long minInterval, long maxInterval) {
        this.minClickInterval = minInterval;
        this.maxClickInterval = maxInterval;
        Log.d(TAG, "Click interval updated: " + minInterval + " - " + maxInterval + " ms");
    }
    
    public void updateSettings(long minInterval, long maxInterval, int offset) {
        this.minClickInterval = minInterval;
        this.maxClickInterval = maxInterval;
        this.randomOffset = offset;
        Log.d(TAG, "Settings updated: interval " + minInterval + " - " + maxInterval + " ms, offset " + offset + " px");
    }
    
    private long getRandomInterval() {
        Log.d(TAG, "getRandomInterval: min=" + minClickInterval + ", max=" + maxClickInterval);
        if (minClickInterval == maxClickInterval) {
            return minClickInterval;
        }
        // 生成 [minClickInterval, maxClickInterval] 范围内的随机数
        long range = maxClickInterval - minClickInterval + 1;
        long result = minClickInterval + (long)(random.nextDouble() * range);
        Log.d(TAG, "Generated interval: " + result + "ms");
        return result;
    }
    
    private float[] getRandomOffset(float x, float y) {
        if (randomOffset == 0) {
            return new float[]{x, y};
        }
        // 在圆形范围内生成随机偏移
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = random.nextDouble() * randomOffset;
        float offsetX = (float)(radius * Math.cos(angle));
        float offsetY = (float)(radius * Math.sin(angle));
        return new float[]{x + offsetX, y + offsetY};
    }
    
    public void startClicking() {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "=== startClicking() called at: " + startTime);
        
        if (isClicking) {
            Log.d(TAG, "Already clicking, ignoring start request");
            return;
        }
        
        if (clickPositions.isEmpty()) {
            Log.e(TAG, "No click positions set - cannot start clicking");
            return;
        }
        
        isClicking = true;
        currentClickIndex = 0;
        Log.d(TAG, "Starting auto click with " + clickPositions.size() + " positions");
        
        // 打印所有位置
        for (int i = 0; i < clickPositions.size(); i++) {
            ClickPosition pos = clickPositions.get(i);
            Log.d(TAG, "Position " + i + ": (" + pos.getX() + ", " + pos.getY() + ")");
        }
        
        clickRunnable = new Runnable() {
            @Override
            public void run() {
                if (isClicking && !clickPositions.isEmpty()) {
                    // 轮流点击：每次点击当前索引的位置
                    ClickPosition pos = clickPositions.get(currentClickIndex);
                    
                    if (pos.isActive()) {
                        // 1. 先执行点击
                        long clickTime = System.currentTimeMillis();
                        float[] offsetPos = getRandomOffset(pos.getX(), pos.getY());
                        Log.d(TAG, "[" + clickTime + "] Clicking position " + currentClickIndex + ": original(" + pos.getX() + ", " + pos.getY() + ") -> offset(" + offsetPos[0] + ", " + offsetPos[1] + ")");
                        performClick(offsetPos[0], offsetPos[1]);
                        
                        // 2. 切换到下一个位置
                        currentClickIndex = (currentClickIndex + 1) % clickPositions.size();
                        
                        // 3. 等待随机间隔后再点击下一个位置
                        long nextInterval = getRandomInterval();
                        Log.d(TAG, "Next click in " + nextInterval + " ms (will click position " + currentClickIndex + ")");
                        handler.postDelayed(this, nextInterval);
                    } else {
                        Log.d(TAG, "Position " + currentClickIndex + " is not active, skipping");
                        // 跳过不活动的位置，立即尝试下一个
                        currentClickIndex = (currentClickIndex + 1) % clickPositions.size();
                        handler.post(this);
                    }
                } else {
                    Log.d(TAG, "Stopping click runnable - isClicking: " + isClicking + ", positions: " + clickPositions.size());
                }
            }
        };
        
        // 立即执行第一次点击（窗口已通过 View.post() 确保稳定）
        long postTime = System.currentTimeMillis();
        handler.post(clickRunnable);
        Log.d(TAG, "=== Click runnable posted at: " + postTime + " (executing immediately after window stabilization)");
    }
    
    public void stopClicking() {
        if (!isClicking) {
            return;
        }
        
        isClicking = false;
        if (clickRunnable != null) {
            handler.removeCallbacks(clickRunnable);
        }
        Log.d(TAG, "Stopped auto click");
    }
    
    private void performClick(float x, float y) {
        Log.d(TAG, "performClick called at: (" + x + ", " + y + ")");
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                Path path = new Path();
                path.moveTo(x, y);
                
                // 增加手势持续时间到 50ms，模拟真实点击
                GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 50);
                GestureDescription gesture = new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();
                
                Log.d(TAG, "Dispatching gesture at: (" + x + ", " + y + ")");
                boolean dispatched = dispatchGesture(gesture, new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        Log.d(TAG, "✓ Click COMPLETED at: (" + x + ", " + y + ")");
                    }
                    
                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.e(TAG, "✗ Click CANCELLED at: (" + x + ", " + y + ")");
                    }
                }, null);
                
                Log.d(TAG, "dispatchGesture returned: " + dispatched);
                
                if (!dispatched) {
                    Log.e(TAG, "Failed to dispatch gesture - accessibility service may not be properly enabled");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error performing click: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "Device API level too low (< N), cannot perform gesture");
        }
    }
}
