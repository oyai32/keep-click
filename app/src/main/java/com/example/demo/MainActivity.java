package com.example.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_ACCESSIBILITY_PERMISSION = 1002;

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

        Button startFloatingButton = findViewById(R.id.startFloatingButton);
        Button stopFloatingButton = findViewById(R.id.stopFloatingButton);
        Button checkPermissionsButton = findViewById(R.id.checkPermissionsButton);

        startFloatingButton.setOnClickListener(v -> startFloatingWindow());
        stopFloatingButton.setOnClickListener(v -> stopFloatingWindow());
        checkPermissionsButton.setOnClickListener(v -> checkAndRequestPermissions());
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
}