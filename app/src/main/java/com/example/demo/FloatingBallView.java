package com.example.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;

public class FloatingBallView extends View {
    private static final String COLOR_RED = "#FF6B6B";
    private static final String COLOR_GRAY = "#CCCCCC";
    private static final String COLOR_DARK_GRAY = "#95A5A6";
    private static final String COLOR_BUTTON_DEFAULT = "#6750A4"; // 按钮默认背景色（紫色）
    private static final String COLOR_BUTTON_ACTIVE = "#FCC908"; // 按钮选中背景色（青蓝色）
    
    private Paint toolbarPaint;
    private Paint textPaint;
    private Paint buttonPaint;
    private Paint circlePaint;
    private RectF toolbarRect;
    private RectF selectButtonRect;
    private RectF startButtonRect;
    private RectF pauseButtonRect;
    private RectF clearButtonRect;
    private RectF scheduleButtonRect;
    private RectF settingsButtonRect;
    private RectF closeButtonRect;
    
    private boolean isSelectionMode = false;
    private boolean isClicking = false;
    private boolean isPaused = false; // 是否处于暂停状态
    private boolean isScheduled = false; // 是否已预约
    private String scheduledTime = ""; // 预约时间显示
    
    // 触摸坐标（用于绘制十字准星）
    private float lastTouchX = 0;
    private float lastTouchY = 0;
    
    // 位置管理
    private List<ClickPosition> clickPositions = new ArrayList<>();
    private int maxPositions = 10; // 最多可以选取10个位置
    
    private OnFloatingBallListener listener;
    
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
    
    public interface OnFloatingBallListener {
        void onSelectPosition();
        void onStartClicking();
        void onStopClicking();
        void onClose();
        void onPositionSelected(float x, float y);
        void onPositionRemoved(int index);
        void onSelectionModeChanged(boolean selectionMode);
        void showToast(String message);
        void onClearPositions();
        void onSettings();
        void onSchedule();
        void onCancelSchedule();
    }
    
    public FloatingBallView(Context context) {
        super(context);
        init();
    }
    
    public FloatingBallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        toolbarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        toolbarPaint.setColor(Color.parseColor("#80000000"));
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor(COLOR_GRAY));
        
        toolbarRect = new RectF();
        selectButtonRect = new RectF();
        startButtonRect = new RectF();
        pauseButtonRect = new RectF();
        clearButtonRect = new RectF();
        scheduleButtonRect = new RectF();
        settingsButtonRect = new RectF();
        closeButtonRect = new RectF();
    }
    
    public void setOnFloatingBallListener(OnFloatingBallListener listener) {
        this.listener = listener;
    }
    
    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        invalidate();
        
        // 通知服务更新窗口参数
        if (listener != null) {
            listener.onSelectionModeChanged(selectionMode);
        }
    }
    
    public boolean isSelectionMode() {
        return isSelectionMode;
    }
    
    public void setClicking(boolean clicking) {
        this.isClicking = clicking;
        invalidate();
    }
    
    public void setPaused(boolean paused) {
        this.isPaused = paused;
        invalidate();
    }
    
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制工具栏（始终在左上角固定位置）
        drawToolbar(canvas);
        
        // 绘制选择的位置圆圈
        drawClickPositions(canvas);
        
        if (isSelectionMode) {
            // 选择模式 - 显示十字准星
            drawCrosshair(canvas);
        }
    }
    
    private void drawToolbar(Canvas canvas) {
        // 工具栏尺寸
        int toolbarWidth = 200;
        int toolbarHeight = 700; // 7个按钮 * 100px
        
        // 工具栏背景
        toolbarRect.set(0, 0, toolbarWidth, toolbarHeight);
        canvas.drawRoundRect(toolbarRect, 8, 8, toolbarPaint);
        
        // 绘制按钮
        drawToolbarButtons(canvas, toolbarWidth, toolbarHeight);
    }
    
    private void drawToolbarButtons(Canvas canvas, int width, int height) {
        int buttonHeight = 100; // 每个按钮100px高
        int buttonWidth = 200;  // 每个按钮200px宽
        
        // 选取按钮（第1个）
        selectButtonRect.set(0, 0, buttonWidth, buttonHeight);
        buttonPaint.setColor(isSelectionMode ? Color.parseColor(COLOR_BUTTON_ACTIVE) : Color.parseColor(COLOR_BUTTON_DEFAULT));
        canvas.drawRoundRect(selectButtonRect, 4, 4, buttonPaint);
        canvas.drawText("选取", buttonWidth/2, buttonHeight/2 + 6, textPaint);
        
        // 开始按钮（第2个）
        startButtonRect.set(0, buttonHeight, buttonWidth, buttonHeight * 2);
        buttonPaint.setColor(isClicking ? Color.parseColor(COLOR_BUTTON_ACTIVE) : Color.parseColor(COLOR_BUTTON_DEFAULT));
        canvas.drawRoundRect(startButtonRect, 4, 4, buttonPaint);
        canvas.drawText("开始", buttonWidth/2, buttonHeight * 1.5f + 6, textPaint);
        
        // 暂停按钮（第3个）
        pauseButtonRect.set(0, buttonHeight * 2, buttonWidth, buttonHeight * 3);
        buttonPaint.setColor(isPaused ? Color.parseColor(COLOR_BUTTON_ACTIVE) : Color.parseColor(COLOR_BUTTON_DEFAULT));
        canvas.drawRoundRect(pauseButtonRect, 4, 4, buttonPaint);
        canvas.drawText("暂停", buttonWidth/2, buttonHeight * 2.5f + 6, textPaint);
        
        // 清空按钮（第4个）
        clearButtonRect.set(0, buttonHeight * 3, buttonWidth, buttonHeight * 4);
        buttonPaint.setColor(Color.parseColor(COLOR_BUTTON_DEFAULT));
        canvas.drawRoundRect(clearButtonRect, 4, 4, buttonPaint);
        canvas.drawText("清空", buttonWidth/2, buttonHeight * 3.5f + 6, textPaint);
        
        // 预约按钮（第5个）
        scheduleButtonRect.set(0, buttonHeight * 4, buttonWidth, buttonHeight * 5);
        buttonPaint.setColor(isScheduled ? Color.parseColor(COLOR_BUTTON_ACTIVE) : Color.parseColor(COLOR_BUTTON_DEFAULT));
        canvas.drawRoundRect(scheduleButtonRect, 4, 4, buttonPaint);
        
        // 绘制预约按钮文字和时间
        Paint smallTextPaint = new Paint(textPaint);
        if (isScheduled && !scheduledTime.isEmpty()) {
            // 已预约：显示"已预约"和预约时间
            smallTextPaint.setTextSize(28);
            canvas.drawText("已预约", buttonWidth/2, buttonHeight * 4.3f + 6, smallTextPaint);
            smallTextPaint.setTextSize(18);
            canvas.drawText(scheduledTime, buttonWidth/2, buttonHeight * 4.7f + 6, smallTextPaint);
        } else {
            // 未预约：只显示"预约"
            canvas.drawText("预约", buttonWidth/2, buttonHeight * 4.5f + 6, textPaint);
        }
        
        // 设置按钮（第6个，深灰色）
        settingsButtonRect.set(0, buttonHeight * 5, buttonWidth, buttonHeight * 6);
        buttonPaint.setColor(Color.parseColor(COLOR_DARK_GRAY));
        canvas.drawRoundRect(settingsButtonRect, 4, 4, buttonPaint);
        canvas.drawText("设置", buttonWidth/2, buttonHeight * 5.5f + 6, textPaint);
        
        // 关闭按钮（第7个，最下方，红色）
        closeButtonRect.set(0, buttonHeight * 6, buttonWidth, buttonHeight * 7);
        buttonPaint.setColor(Color.parseColor(COLOR_RED));
        canvas.drawRoundRect(closeButtonRect, 4, 4, buttonPaint);
        canvas.drawText("关闭", buttonWidth/2, buttonHeight * 6.5f + 6, textPaint);
    }
    
    private void drawClickPositions(Canvas canvas) {
        // 获取View在屏幕上的位置
        int[] location = new int[2];
        getLocationOnScreen(location);
        
        for (int i = 0; i < clickPositions.size(); i++) {
            ClickPosition pos = clickPositions.get(i);
            if (pos.isActive()) {
                // 将全局屏幕坐标转换为View内的相对坐标
                float localX = pos.getX() - location[0];
                float localY = pos.getY() - location[1];
                
                // 始终使用灰色
                circlePaint.setColor(Color.parseColor(COLOR_GRAY)); // 半透明灰色
                canvas.drawCircle(localX, localY, 30, circlePaint); // 30px 半径
                
                // 绘制序号
                Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                numberPaint.setColor(Color.BLACK);
                numberPaint.setTextSize(20);
                numberPaint.setTextAlign(Paint.Align.CENTER);
                numberPaint.setStyle(Paint.Style.FILL);
                
                // 计算文字位置（居中）
                Paint.FontMetrics fontMetrics = numberPaint.getFontMetrics();
                float textY = localY - (fontMetrics.ascent + fontMetrics.descent) / 2;
                
                canvas.drawText(String.valueOf(i + 1), localX, textY, numberPaint);
            }
        }
    }
    
    private void drawCrosshair(Canvas canvas) {
        // 绘制十字准星
        textPaint.setColor(Color.RED);
        textPaint.setStrokeWidth(3);
        // 这里需要获取当前触摸位置来绘制十字准星
        if (lastTouchX > 0 && lastTouchY > 0) {
            canvas.drawLine(lastTouchX - 20, lastTouchY, lastTouchX + 20, lastTouchY, textPaint);
            canvas.drawLine(lastTouchX, lastTouchY - 20, lastTouchX, lastTouchY + 20, textPaint);
        }
        textPaint.setStrokeWidth(1);
        textPaint.setColor(Color.WHITE);
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        android.util.Log.d("FloatingBallView", "Touch event: " + event.getAction() + " at (" + x + "," + y + ") selectionMode=" + isSelectionMode);
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                boolean handled = handleTouchDown(x, y);
                android.util.Log.d("FloatingBallView", "ACTION_DOWN handled: " + handled);
                return handled;
            case MotionEvent.ACTION_MOVE:
                if (isSelectionMode) {
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate(); // 重绘十字准星
                    return true;
                }
                // 不在选取模式，返回false让事件穿透
                return false;
            case MotionEvent.ACTION_UP:
                // 返回false让事件穿透
                return false;
        }
        return false;
    }
    
    private boolean handleTouchDown(float x, float y) {
        // 始终优先检查工具栏按钮点击
        // 工具栏固定在左上角200x700区域（7个按钮）
        if (x >= 0 && x <= 200 && y >= 0 && y <= 700) {
            // 点击在工具栏区域内，尝试处理按钮点击
            boolean handled = handleButtonTouch(x, y);
            if (handled) {
                // 按钮被点击，消费事件
                return true;
            } else if (isSelectionMode) {
                // 在选取模式下，工具栏空白区域也不穿透
                return true;
            } else {
                // 不在选取模式，工具栏空白区域穿透
                return false;
            }
        } else if (isSelectionMode) {
            // 点击在工具栏外部，且在选取模式下，处理位置选取
            return handleSelectionModeTouch(x, y);
        } else {
            // 不在工具栏区域，且不在选取模式，返回false让事件穿透
            return false;
        }
    }
    
    private boolean handleSelectionModeTouch(float x, float y) {
        // 获取工具栏在屏幕上的位置
        int[] location = new int[2];
        getLocationOnScreen(location);
        
        // 计算全局屏幕坐标
        float screenX = location[0] + x;
        float screenY = location[1] + y;
        
        // 调试信息
        android.util.Log.d("FloatingBallView", "Selection touch: local(" + x + "," + y + ") screen(" + screenX + "," + screenY + ")");
        android.util.Log.d("FloatingBallView", "Toolbar location: (" + location[0] + "," + location[1] + ")");
        
        // 检查是否点击了已存在的位置圆圈（点击相同位置则取消）
        for (int i = 0; i < clickPositions.size(); i++) {
            ClickPosition pos = clickPositions.get(i);
            if (pos.isActive() && isPointInCircle(screenX, screenY, pos.getX(), pos.getY(), 30)) {
                // 点击了已存在的位置，取消这个位置
                clickPositions.remove(i);
                if (listener != null) {
                    listener.onPositionRemoved(i);
                }
                invalidate();
                android.util.Log.d("FloatingBallView", "Removed position " + i + " at (" + screenX + "," + screenY + ")");
                return true;
            }
        }
        
        // 检查是否已达到最大位置数
        if (clickPositions.size() >= maxPositions) {
            if (listener != null) {
                listener.showToast("最多只能选取 " + maxPositions + " 个位置");
            }
            return true;
        }
        
        // 添加新位置
        ClickPosition newPos = new ClickPosition(screenX, screenY);
        clickPositions.add(newPos);
        if (listener != null) {
            listener.onPositionSelected(screenX, screenY);
        }
        invalidate();
        android.util.Log.d("FloatingBallView", "Added position " + (clickPositions.size() - 1) + " at (" + screenX + "," + screenY + ")");
        
        return true;
    }
    
    private boolean handleButtonTouch(float x, float y) {
        if (selectButtonRect.contains(x, y)) {
            handleSelectButtonClick();
            return true;
        } else if (startButtonRect.contains(x, y) && !isClicking) {
            handleStartButtonClick();
            return true;
        } else if (pauseButtonRect.contains(x, y)) {
            handlePauseButtonClick();
            return true;
        } else if (clearButtonRect.contains(x, y)) {
            handleClearButtonClick();
            return true;
        } else if (scheduleButtonRect.contains(x, y)) {
            handleScheduleButtonClick();
            return true;
        } else if (settingsButtonRect.contains(x, y)) {
            handleSettingsButtonClick();
            return true;
        } else if (closeButtonRect.contains(x, y)) {
            handleCloseButtonClick();
            return true;
        }
        return false;
    }
    
    private boolean isPointInCircle(float px, float py, float cx, float cy, float radius) {
        float distance = (float) Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
        return distance <= radius;
    }
    
    private void handleSelectButtonClick() {
        // 如果已预约，取消预约（需求5）
        if (isScheduled) {
            cancelSchedule();
        }
        
        // 需求1：点击选取后，所有按钮都处于未激活状态
        setClicking(false);
        setPaused(false);
        
        // 切换选取模式
        setSelectionMode(!isSelectionMode);
        if (listener != null) {
            listener.onSelectPosition();
        }
        invalidate();
    }
    
    private void handleStartButtonClick() {
        // 如果已预约，取消预约（需求5）
        if (isScheduled) {
            cancelSchedule();
        }
        
        android.util.Log.d("FloatingBallView", "Start button clicked, positions: " + clickPositions.size());
        
        if (clickPositions.isEmpty()) {
            // 没有已选位置，提示用户
            android.util.Log.d("FloatingBallView", "No positions selected, showing toast");
            if (listener != null) {
                listener.showToast("请先选取位置");
            }
            return;
        }
        
        // 打印所有位置
        for (int i = 0; i < clickPositions.size(); i++) {
            ClickPosition pos = clickPositions.get(i);
            android.util.Log.d("FloatingBallView", "Position " + i + ": (" + pos.getX() + "," + pos.getY() + ")");
        }
        
        // 退出选取模式
        setSelectionMode(false);
        // 进入点击模式
        setClicking(true);
        setPaused(false);
        android.util.Log.d("FloatingBallView", "Calling listener.onStartClicking()");
        if (listener != null) {
            listener.onStartClicking();
        }
    }
    
    private void handlePauseButtonClick() {
        // 只有开始状态下才能点击暂停（需求6）
        if (!isClicking) {
            if (listener != null) {
                listener.showToast("请先开始");
            }
            return;
        }
        
        // 退出选取模式
        setSelectionMode(false);
        // 停止点击，进入暂停状态
        setClicking(false);
        setPaused(true);
        if (listener != null) {
            listener.onStopClicking();
        }
    }
    
    private void handleClearButtonClick() {
        // 如果已预约，取消预约（需求5）
        if (isScheduled) {
            cancelSchedule();
        }
        
        // 清空位置，恢复所有按钮状态
        clearAllPositions();
        setSelectionMode(false);
        setClicking(false);
        setPaused(false);
        if (listener != null) {
            listener.onClearPositions();
            listener.onStopClicking(); // 停止点击并设置穿透
            listener.showToast("已清空");
        }
    }
    
    private void handleSettingsButtonClick() {
        // 如果已预约，取消预约（需求5）
        if (isScheduled) {
            cancelSchedule();
        }
        
        android.util.Log.d("FloatingBallView", "Settings button clicked");
        
        // 如果正在点击，先暂停
        if (isClicking) {
            setClicking(false);
            setPaused(true);
            if (listener != null) {
                listener.onStopClicking();
            }
            android.util.Log.d("FloatingBallView", "Auto-clicking paused before opening settings");
        }
        
        // 打开设置界面
        if (listener != null) {
            listener.onSettings();
        }
    }
    
    private void handleCloseButtonClick() {
        // 如果已预约，取消预约（需求5）
        if (isScheduled) {
            cancelSchedule();
        }
        
        // 关闭浮窗前先停止点击和清理状态
        android.util.Log.d("FloatingBallView", "Close button clicked");
        
        // 停止点击
        if (isClicking) {
            setClicking(false);
            if (listener != null) {
                listener.onStopClicking();
            }
        }
        
        // 清理状态
        setSelectionMode(false);
        setPaused(false);
        
        // 关闭浮窗
        if (listener != null) {
            listener.onClose();
        }
    }
    
    public void clearAllPositions() {
        clickPositions.clear();
        invalidate();
    }
    
    private void handleScheduleButtonClick() {
        // 需求7：再次点击预约按钮，取消预约
        if (isScheduled) {
            cancelSchedule();
            return;
        }
        
        // 新需求2：开始状态下不能点击预约按钮
        if (isClicking) {
            if (listener != null) {
                listener.showToast("开始状态下不能预约");
            }
            return;
        }
        
        // 需求2：需要已选取位置后才能点击
        if (clickPositions.isEmpty()) {
            if (listener != null) {
                listener.showToast("请先选取位置");
            }
            return;
        }
        
        // 新需求3：点击预约后，若此时已有选中的位置，则选取按钮处于未激活状态
        setSelectionMode(false);
        
        // 新需求4：暂停状态下，若点击预约按钮，暂停按钮变为未激活状态
        setPaused(false);
        
        // 激活预约
        if (listener != null) {
            listener.onSchedule();
        }
    }
    
    private void cancelSchedule() {
        isScheduled = false;
        scheduledTime = "";
        if (listener != null) {
            listener.onCancelSchedule();
        }
        invalidate();
    }
    
    public void setScheduledTime(String time) {
        this.scheduledTime = time;
        this.isScheduled = !time.isEmpty();
        invalidate();
    }
    
    public boolean isScheduled() {
        return isScheduled;
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // 在非选取模式下，只有工具栏区域接收触摸
        float x = event.getX();
        float y = event.getY();
        
        android.util.Log.d("FloatingBallView", "dispatchTouchEvent: (" + x + "," + y + ") isSelectionMode=" + isSelectionMode);
        
        if (!isSelectionMode) {
            // 非选取模式：只有工具栏区域（200×700）接收触摸
            if (x < 0 || x > 200 || y < 0 || y > 700) {
                // 工具栏外部，不处理触摸事件
                android.util.Log.d("FloatingBallView", "Outside toolbar, returning false (should penetrate)");
                return false;
            }
        }
        
        // 选取模式或工具栏区域内，正常处理
        android.util.Log.d("FloatingBallView", "Inside toolbar or selection mode, calling super");
        return super.dispatchTouchEvent(event);
    }
}
