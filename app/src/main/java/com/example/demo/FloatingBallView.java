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
    private static final String COLOR_BLUE = "#4ECDC4";
    private static final String COLOR_RED = "#FF6B6B";
    private static final String COLOR_GRAY = "#CCCCCC";
    private static final String COLOR_YELLOW = "#FFFF00";
    private static final String COLOR_DARK_GRAY = "#95A5A6";
    private static final String COLOR_ACTIVE_BLUE = "#2196F3";
    
    private Paint toolbarPaint;
    private Paint textPaint;
    private Paint buttonPaint;
    private Paint circlePaint;
    private RectF toolbarRect;
    private RectF selectButtonRect;
    private RectF startButtonRect;
    private RectF pauseButtonRect;
    private RectF moveButtonRect;
    private RectF clearButtonRect;
    
    private boolean isSelectionMode = false;
    private boolean isClicking = false;
    private boolean isPaused = false; // 是否处于暂停状态
    private boolean isDragging = false;
    private boolean isCurrentlyClicking = false; // 当前是否正在执行点击
    private int clickCount = 0; // 点击计数
    private float lastTouchX = 0;
    private float lastTouchY = 0;
    
    // 位置管理
    private List<ClickPosition> clickPositions = new ArrayList<>();
    private int maxPositions = 1; // 只能选取一个位置
    private int currentClickIndex = 0;
    
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
        void onMoveToolbar(int deltaX, int deltaY);
        void onSelectionModeChanged(boolean selectionMode);
        void showToast(String message);
        void onClearPositions();
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
        buttonPaint.setColor(Color.parseColor(COLOR_BLUE));
        
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor(COLOR_GRAY));
        
        toolbarRect = new RectF();
        selectButtonRect = new RectF();
        startButtonRect = new RectF();
        pauseButtonRect = new RectF();
        moveButtonRect = new RectF();
        clearButtonRect = new RectF();
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
    
    public void setClickPosition(float x, float y) {
        // 这个方法在新实现中不再需要，因为位置管理已经改为使用clickPositions列表
        // 保留此方法以保持接口兼容性，但不执行任何操作
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
        // 在选取模式下，工具栏固定在左上角，尺寸为200x500
        // 在正常模式下，工具栏占满整个View
        int toolbarWidth = 200;
        int toolbarHeight = 500;
        
        // 工具栏背景
        toolbarRect.set(0, 0, toolbarWidth, toolbarHeight);
        canvas.drawRoundRect(toolbarRect, 8, 8, toolbarPaint);
        
        // 绘制按钮
        drawToolbarButtons(canvas, toolbarWidth, toolbarHeight);
        
        // 绘制穿透状态（调试用）
        drawPenetrationStatus(canvas);
    }
    
    private void drawPenetrationStatus(Canvas canvas) {
        Paint statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statusPaint.setColor(Color.WHITE);
        statusPaint.setTextSize(14);
        statusPaint.setTextAlign(Paint.Align.LEFT);
        
        int leftMargin = 10; // 左边距
        int topMargin = 10; // 上边距
        
        // 在工具栏顶部显示状态信息
        int currentY = topMargin;
        
        // 第一行：穿透状态（调试用）
        String status = isSelectionMode ? "穿透:关" : "穿透:开";
        canvas.drawText(status, leftMargin, currentY, statusPaint);
        currentY += 20;
        
        // 第二行：点击状态
        if (isClicking) {
            String clickStatus = isCurrentlyClicking ? "●点击中" : "○等待";
            statusPaint.setColor(isCurrentlyClicking ? Color.parseColor(COLOR_YELLOW) : Color.WHITE);
            statusPaint.setTextSize(14);
            canvas.drawText(clickStatus, leftMargin, currentY, statusPaint);
            currentY += 20;
            
            // 第三行：点击计数
            statusPaint.setColor(Color.WHITE);
            statusPaint.setTextSize(14);
            canvas.drawText("已点击:" + clickCount, leftMargin, currentY, statusPaint);
        } else if (isPaused) {
            statusPaint.setColor(Color.parseColor(COLOR_ACTIVE_BLUE));
            statusPaint.setTextSize(14);
            canvas.drawText("已暂停(" + clickCount + "次)", leftMargin, currentY, statusPaint);
        } else if (!clickPositions.isEmpty()) {
            statusPaint.setColor(Color.parseColor(COLOR_GRAY));
            statusPaint.setTextSize(14);
            canvas.drawText("位置已选取", leftMargin, currentY, statusPaint);
        }
    }
    
    private void drawToolbarButtons(Canvas canvas, int width, int height) {
        int buttonHeight = 100; // 每个按钮100px高
        int buttonWidth = 200;  // 每个按钮200px宽
        
        // 选取按钮
        selectButtonRect.set(0, 0, buttonWidth, buttonHeight);
        buttonPaint.setColor(isSelectionMode ? Color.parseColor(COLOR_ACTIVE_BLUE) : Color.parseColor(COLOR_GRAY));
        canvas.drawRoundRect(selectButtonRect, 4, 4, buttonPaint);
        canvas.drawText("选取", buttonWidth/2, buttonHeight/2 + 6, textPaint);
        
        // 开始按钮
        startButtonRect.set(0, buttonHeight, buttonWidth, buttonHeight * 2);
        buttonPaint.setColor(isClicking ? Color.parseColor(COLOR_ACTIVE_BLUE) : Color.parseColor(COLOR_GRAY));
        canvas.drawRoundRect(startButtonRect, 4, 4, buttonPaint);
        canvas.drawText("开始", buttonWidth/2, buttonHeight * 1.5f + 6, textPaint);
        
        // 暂停按钮
        pauseButtonRect.set(0, buttonHeight * 2, buttonWidth, buttonHeight * 3);
        buttonPaint.setColor(isPaused ? Color.parseColor(COLOR_ACTIVE_BLUE) : Color.parseColor(COLOR_GRAY));
        canvas.drawRoundRect(pauseButtonRect, 4, 4, buttonPaint);
        canvas.drawText("暂停", buttonWidth/2, buttonHeight * 2.5f + 6, textPaint);
        
        // 清空按钮
        clearButtonRect.set(0, buttonHeight * 3, buttonWidth, buttonHeight * 4);
        buttonPaint.setColor(Color.parseColor(COLOR_RED));
        canvas.drawRoundRect(clearButtonRect, 4, 4, buttonPaint);
        canvas.drawText("清空", buttonWidth/2, buttonHeight * 3.5f + 6, textPaint);
        
        // 移动按钮
        moveButtonRect.set(0, buttonHeight * 4, buttonWidth, buttonHeight * 5);
        buttonPaint.setColor(Color.parseColor(COLOR_DARK_GRAY));
        canvas.drawRoundRect(moveButtonRect, 4, 4, buttonPaint);
        canvas.drawText("移动", buttonWidth/2, buttonHeight * 4.5f + 6, textPaint);
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
                
                // 根据是否正在点击改变颜色
                if (isCurrentlyClicking) {
                    circlePaint.setColor(Color.parseColor(COLOR_YELLOW)); // 黄色
                } else {
                    circlePaint.setColor(Color.parseColor(COLOR_GRAY)); // 半透明灰色
                }
                canvas.drawCircle(localX, localY, 15, circlePaint);
                
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
                lastTouchTime = System.currentTimeMillis();
                boolean handled = handleTouchDown(x, y);
                android.util.Log.d("FloatingBallView", "ACTION_DOWN handled: " + handled);
                return handled;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    handleDrag(x, y);
                    return true;
                } else if (isSelectionMode) {
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate(); // 重绘十字准星
                    return true;
                }
                // 不在拖拽或选取模式，返回false让事件穿透
                return false;
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    isDragging = false;
                    return true;
                }
                // 不在拖拽，返回false让事件穿透
                return false;
        }
        return false;
    }
    
    private boolean handleTouchDown(float x, float y) {
        // 始终优先检查工具栏按钮点击
        // 工具栏固定在左上角200x500区域
        if (x >= 0 && x <= 200 && y >= 0 && y <= 500) {
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
        
        // 检查是否点击了已存在的位置圆圈
        if (!clickPositions.isEmpty()) {
            ClickPosition pos = clickPositions.get(0);
            if (pos.isActive() && isPointInCircle(screenX, screenY, pos.getX(), pos.getY(), 15)) {
                // 点击了已存在的位置，取消选中
                clickPositions.clear();
                if (listener != null) {
                    listener.onPositionRemoved(0);
                }
                invalidate();
                return true;
            }
        }
        
        // 清空旧位置，添加新位置（只保留一个位置）
        clickPositions.clear();
        ClickPosition newPos = new ClickPosition(screenX, screenY);
        clickPositions.add(newPos);
        if (listener != null) {
            // 先清空服务中的旧位置
            listener.onClearPositions();
            // 再添加新位置
            listener.onPositionSelected(screenX, screenY);
        }
        invalidate();
        android.util.Log.d("FloatingBallView", "Added position: (" + screenX + "," + screenY + ")");
        
        // 选取完成后，退出选取模式，恢复窗口大小
        // setSelectionMode(false);
        
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
        } else if (moveButtonRect.contains(x, y)) {
            // 移动按钮：短按开始拖拽，长按也拖拽
            isDragging = true;
            return true;
        }
        return false;
    }
    
    private boolean isLongPress() {
        // 简单的长按检测，实际应用中可以使用更精确的检测
        return System.currentTimeMillis() - lastTouchTime > 500;
    }
    
    private long lastTouchTime = 0;
    
    private void handleDrag(float x, float y) {
        // 计算移动距离
        float deltaX = x - lastTouchX;
        float deltaY = y - lastTouchY;
        
        // 更新最后触摸位置
        lastTouchX = x;
        lastTouchY = y;
        
        // 通知父窗口移动工具栏
        if (listener != null) {
            listener.onMoveToolbar((int)deltaX, (int)deltaY);
        }
    }
    
    private boolean isPointInCircle(float px, float py, float cx, float cy, float radius) {
        float distance = (float) Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
        return distance <= radius;
    }
    
    private void handleSelectButtonClick() {
        // 切换选取模式
        setSelectionMode(!isSelectionMode);
        if (listener != null) {
            listener.onSelectPosition();
        }
        invalidate();
    }
    
    private void handleStartButtonClick() {
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
        clickCount = 0; // 重置计数器
        currentClickIndex = 0;
        android.util.Log.d("FloatingBallView", "Calling listener.onStartClicking()");
        if (listener != null) {
            listener.onStartClicking();
        }
    }
    
    private void handlePauseButtonClick() {
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
        // 清空位置，恢复所有按钮状态
        clearAllPositions();
        setSelectionMode(false);
        setClicking(false);
        setPaused(false);
        clickCount = 0; // 重置计数器
        if (listener != null) {
            listener.onClearPositions();
            listener.onStopClicking(); // 停止点击并设置穿透
            listener.showToast("已清空");
        }
    }
    
    // 新增方法
    public void nextClickPosition() {
        if (!clickPositions.isEmpty()) {
            currentClickIndex = (currentClickIndex + 1) % clickPositions.size();
            invalidate();
        }
    }
    
    public ClickPosition getCurrentClickPosition() {
        if (!clickPositions.isEmpty() && currentClickIndex < clickPositions.size()) {
            return clickPositions.get(currentClickIndex);
        }
        return null;
    }
    
    public List<ClickPosition> getAllClickPositions() {
        return new ArrayList<>(clickPositions);
    }
    
    public void clearAllPositions() {
        clickPositions.clear();
        invalidate();
    }
    
    public void setCurrentlyClicking(boolean clicking) {
        this.isCurrentlyClicking = clicking;
        if (clicking) {
            // 每次开始点击时增加计数
            clickCount++;
        }
        invalidate();
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // 在非选取模式下，只有工具栏区域接收触摸
        float x = event.getX();
        float y = event.getY();
        
        android.util.Log.d("FloatingBallView", "dispatchTouchEvent: (" + x + "," + y + ") isSelectionMode=" + isSelectionMode);
        
        if (!isSelectionMode) {
            // 非选取模式：只有工具栏区域（200×500）接收触摸
            if (x < 0 || x > 200 || y < 0 || y > 500) {
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
