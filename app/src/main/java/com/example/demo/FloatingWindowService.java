package com.example.demo;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class FloatingWindowService extends Service {
    private static final String ACTION_SHOW = "show";
    private static final String ACTION_HIDE = "hide";
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String EXTRA_ACTION = "action";
    
    private WindowManager windowManager;
    private View floatingView;
    private FloatingBallView floatingBallView;
    private boolean isFloatingViewVisible = false;
    private WindowManager.LayoutParams layoutParams;
    private android.content.BroadcastReceiver clickReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra(EXTRA_ACTION);
            if (ACTION_SHOW.equals(action)) {
                showFloatingView();
            } else if (ACTION_HIDE.equals(action)) {
                hideFloatingView();
            }
        }
        return START_STICKY;
    }

    private void showFloatingView() {
        if (isFloatingViewVisible) {
            return;
        }

        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show();
            return;
        }

        floatingBallView = new FloatingBallView(this);
        floatingBallView.setOnFloatingBallListener(new FloatingBallView.OnFloatingBallListener() {
            @Override
            public void onSelectPosition() {
                // 切换选择位置模式
                // 需要根据当前状态决定是否扩展窗口
                if (floatingBallView.isSelectionMode()) {
                    // 进入选取模式：扩展窗口，不穿透
                    setSelectionMode(true);
                } else {
                    // 退出选取模式：恢复窗口大小，穿透
                    setSelectionMode(false);
                }
            }

            @Override
            public void onStartClicking() {
                // 开始自动连击
                android.util.Log.d("FloatingWindowService", "onStartClicking called");
                Intent serviceIntent = new Intent(FloatingWindowService.this, AutoClickService.class);
                serviceIntent.putExtra(EXTRA_ACTION, ACTION_START);
                startService(serviceIntent);
                android.util.Log.d("FloatingWindowService", "AutoClickService start intent sent");
                
                // 设置窗口为穿透模式，让点击能够作用到底层应用
                setClickThroughMode(true);
            }

            @Override
            public void onStopClicking() {
                // 停止自动连击
                Intent serviceIntent = new Intent(FloatingWindowService.this, AutoClickService.class);
                serviceIntent.putExtra(EXTRA_ACTION, ACTION_STOP);
                startService(serviceIntent);
                
                // 暂停时仍然保持穿透模式，不恢复正常
                // setClickThroughMode(false);
            }

            @Override
            public void onClose() {
                hideFloatingView();
            }

            @Override
            public void onPositionSelected(float x, float y) {
                // 通知AutoClickService添加新位置
                android.util.Log.d("FloatingWindowService", "onPositionSelected: (" + x + "," + y + ")");
                Intent serviceIntent = new Intent(FloatingWindowService.this, AutoClickService.class);
                serviceIntent.putExtra(EXTRA_ACTION, "add_position");
                serviceIntent.putExtra("x", x);
                serviceIntent.putExtra("y", y);
                startService(serviceIntent);
                
                // 选中位置后，扩展窗口到全屏以显示标记
                expandWindowForMarker();
            }

            @Override
            public void onPositionRemoved(int index) {
                // 通知AutoClickService移除位置
                Intent serviceIntent = new Intent(FloatingWindowService.this, AutoClickService.class);
                serviceIntent.putExtra(EXTRA_ACTION, "remove_position");
                serviceIntent.putExtra("index", index);
                startService(serviceIntent);
            }

            @Override
            public void onMoveToolbar(int deltaX, int deltaY) {
                // 移动工具栏
                moveToolbar(deltaX, deltaY);
            }

            @Override
            public void onSelectionModeChanged(boolean selectionMode) {
                // 更新窗口参数以支持选取模式
                setSelectionMode(selectionMode);
            }

            @Override
            public void showToast(String message) {
                // 显示Toast提示
                Toast.makeText(FloatingWindowService.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onClearPositions() {
                // 清空位置
                android.util.Log.d("FloatingWindowService", "onClearPositions called");
                Intent serviceIntent = new Intent(FloatingWindowService.this, AutoClickService.class);
                serviceIntent.putExtra(EXTRA_ACTION, "clear_positions");
                startService(serviceIntent);
                
                // 清空位置后，缩小窗口到工具栏大小
                shrinkWindowToToolbar();
            }
        });
        
        // 设置拖拽监听器
        floatingBallView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, android.view.DragEvent event) {
                switch (event.getAction()) {
                    case android.view.DragEvent.ACTION_DRAG_STARTED:
                        return true;
                    case android.view.DragEvent.ACTION_DRAG_LOCATION:
                        // 更新工具栏位置
                        updateToolbarPosition((int) event.getX(), (int) event.getY());
                        return true;
                    case android.view.DragEvent.ACTION_DRAG_ENDED:
                        return true;
                }
                return false;
            }
        });

        layoutParams = new WindowManager.LayoutParams(
                200, // 初始化为工具栏宽度
                500, // 初始化为工具栏高度
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = 0; // 左上角
        layoutParams.y = 0;

        windowManager.addView(floatingBallView, layoutParams);
        isFloatingViewVisible = true;
        
        // 注册广播接收器
        registerClickReceiver();
    }

    private void hideFloatingView() {
        if (floatingBallView != null && isFloatingViewVisible) {
            windowManager.removeView(floatingBallView);
            isFloatingViewVisible = false;
        }
    }

    private void updateToolbarPosition(int x, int y) {
        if (layoutParams != null && isFloatingViewVisible) {
            layoutParams.x = x;
            layoutParams.y = y;
            windowManager.updateViewLayout(floatingBallView, layoutParams);
        }
    }
    
    public void moveToolbar(int deltaX, int deltaY) {
        if (layoutParams != null && isFloatingViewVisible) {
            layoutParams.x += deltaX;
            layoutParams.y += deltaY;
            windowManager.updateViewLayout(floatingBallView, layoutParams);
        }
    }
    
    public void setSelectionMode(boolean selectionMode) {
        if (layoutParams != null && isFloatingViewVisible) {
            if (selectionMode) {
                // 选取模式：扩展窗口到全屏
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            } else {
                // 非选取模式：缩小窗口到工具栏大小，底层应用可操作
                layoutParams.width = 200;
                layoutParams.height = 500;
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            }
            windowManager.updateViewLayout(floatingBallView, layoutParams);
            android.util.Log.d("FloatingWindowService", "setSelectionMode: " + selectionMode + ", window size: " + layoutParams.width + "x" + layoutParams.height);
        }
    }
    
    public void setClickThroughMode(boolean clickThrough) {
        // 不需要改变窗口标志，由View的onTouchEvent处理穿透
        // 这个方法保留以保持接口兼容性
    }
    
    private void expandWindowForMarker() {
        // 扩展窗口到全屏以显示标记
        if (layoutParams != null && isFloatingViewVisible) {
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            windowManager.updateViewLayout(floatingBallView, layoutParams);
            android.util.Log.d("FloatingWindowService", "Window expanded for marker display");
        }
    }
    
    private void shrinkWindowToToolbar() {
        // 缩小窗口到工具栏大小
        if (layoutParams != null && isFloatingViewVisible) {
            layoutParams.width = 200;
            layoutParams.height = 500;
            windowManager.updateViewLayout(floatingBallView, layoutParams);
            android.util.Log.d("FloatingWindowService", "Window shrunk to toolbar size");
        }
    }

    private void registerClickReceiver() {
        clickReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                if ("com.example.demo.CLICK_START".equals(intent.getAction())) {
                    if (floatingBallView != null) {
                        floatingBallView.setCurrentlyClicking(true);
                    }
                } else if ("com.example.demo.CLICK_END".equals(intent.getAction())) {
                    if (floatingBallView != null) {
                        floatingBallView.setCurrentlyClicking(false);
                    }
                }
            }
        };
        
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction("com.example.demo.CLICK_START");
        filter.addAction("com.example.demo.CLICK_END");
        
        // Android 8.0+ 需要添加标志
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(clickReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(clickReceiver, filter);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingBallView != null && isFloatingViewVisible) {
            windowManager.removeView(floatingBallView);
        }
        if (clickReceiver != null) {
            try {
                unregisterReceiver(clickReceiver);
            } catch (Exception e) {
                // 忽略注销错误
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
