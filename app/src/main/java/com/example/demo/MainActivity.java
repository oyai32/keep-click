package com.example.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_ACCESSIBILITY_PERMISSION = 1002;
    private static final String PREFS_NAME = "AutoClickerPrefs";
    private static final String KEY_MIN_INTERVAL = "min_interval";
    private static final String KEY_MAX_INTERVAL = "max_interval";
    
    private EditText minIntervalInput;
    private EditText maxIntervalInput;
    private TextView currentIntervalText;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        Button startFloatingButton = findViewById(R.id.startFloatingButton);
        Button stopFloatingButton = findViewById(R.id.stopFloatingButton);
        Button saveIntervalButton = findViewById(R.id.saveIntervalButton);
        
        minIntervalInput = findViewById(R.id.minIntervalInput);
        maxIntervalInput = findViewById(R.id.maxIntervalInput);
        currentIntervalText = findViewById(R.id.currentIntervalText);

        startFloatingButton.setOnClickListener(v -> startFloatingWindow());
        stopFloatingButton.setOnClickListener(v -> stopFloatingWindow());
        saveIntervalButton.setOnClickListener(v -> saveIntervalSettings());
        
        // 加载保存的设置
        loadIntervalSettings();
    }

    private void startFloatingWindow() {
        if (checkPermissions()) {
            Intent serviceIntent = new Intent(this, FloatingWindowService.class);
            serviceIntent.putExtra("action", "show");
            startService(serviceIntent);
            Toast.makeText(this, "浮动窗口已启动", Toast.LENGTH_SHORT).show();
        } else {
            checkAndRequestPermissions();
        }
    }

    private void stopFloatingWindow() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        serviceIntent.putExtra("action", "hide");
        startService(serviceIntent);
        Toast.makeText(this, "浮动窗口已关闭", Toast.LENGTH_SHORT).show();
    }

    private boolean checkPermissions() {
        boolean overlayPermission = true;
        boolean accessibilityPermission = true;

        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlayPermission = Settings.canDrawOverlays(this);
        }

        // 检查无障碍服务权限
        String settingValue = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        accessibilityPermission = settingValue != null && 
                settingValue.contains(getPackageName() + "/" + AutoClickService.class.getName());

        return overlayPermission && accessibilityPermission;
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            requestAccessibilityPermission();
        }
    }

    private void requestAccessibilityPermission() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, REQUEST_ACCESSIBILITY_PERMISSION);
        Toast.makeText(this, "请在无障碍服务中启用自动连击服务", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                requestAccessibilityPermission();
            } else {
                Toast.makeText(this, "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_ACCESSIBILITY_PERMISSION) {
            if (checkPermissions()) {
                Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "请确保已启用无障碍服务", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void loadIntervalSettings() {
        long minInterval = sharedPreferences.getLong(KEY_MIN_INTERVAL, 150);
        long maxInterval = sharedPreferences.getLong(KEY_MAX_INTERVAL, 300);
        
        minIntervalInput.setText(String.valueOf(minInterval));
        maxIntervalInput.setText(String.valueOf(maxInterval));
        updateCurrentIntervalText(minInterval, maxInterval);
    }
    
    private void saveIntervalSettings() {
        String minStr = minIntervalInput.getText().toString().trim();
        String maxStr = maxIntervalInput.getText().toString().trim();
        
        if (minStr.isEmpty() || maxStr.isEmpty()) {
            Toast.makeText(this, "请输入完整的间隔时间", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            long minInterval = Long.parseLong(minStr);
            long maxInterval = Long.parseLong(maxStr);
            
            // 验证输入
            if (minInterval < 50) {
                Toast.makeText(this, "最小间隔不能小于 50ms", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (minInterval > maxInterval) {
                Toast.makeText(this, "最小值不能大于最大值", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (maxInterval > 10000) {
                Toast.makeText(this, "最大间隔不能超过 10000ms", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 保存设置
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(KEY_MIN_INTERVAL, minInterval);
            editor.putLong(KEY_MAX_INTERVAL, maxInterval);
            editor.apply();
            
            // 更新 AutoClickService 的间隔设置
            Intent serviceIntent = new Intent(this, AutoClickService.class);
            serviceIntent.putExtra("action", "update_interval");
            serviceIntent.putExtra("min_interval", minInterval);
            serviceIntent.putExtra("max_interval", maxInterval);
            startService(serviceIntent);
            
            updateCurrentIntervalText(minInterval, maxInterval);
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
            
            android.util.Log.d("MainActivity", "Interval settings saved: " + minInterval + " - " + maxInterval + " ms");
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateCurrentIntervalText(long minInterval, long maxInterval) {
        if (minInterval == maxInterval) {
            currentIntervalText.setText("当前点击间隔：固定 " + minInterval + " ms");
        } else {
            currentIntervalText.setText("当前点击间隔：" + minInterval + " - " + maxInterval + " ms（随机）");
        }
    }
}