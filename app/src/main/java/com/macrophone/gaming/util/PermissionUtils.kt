package com.macrophone.gaming.util

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

/**
 * Tiện ích kiểm tra và yêu cầu các quyền hệ thống trên Android 11+ (API 30+)
 */
object PermissionUtils {

    /**
     * Kiểm tra quyền vẽ trên ứng dụng khác (SYSTEM_ALERT_WINDOW)
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Mở màn hình cài đặt cấp quyền vẽ trên ứng dụng khác
     */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Kiểm tra Accessibility Service của ứng dụng đã được bật trong Settings chưa
     */
    fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<out AccessibilityService>
    ): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            val component = service.resolveInfo.serviceInfo
            if (component.packageName == context.packageName && component.name == serviceClass.name) {
                return true
            }
        }
        return false
    }

    /**
     * Mở màn hình Cài đặt Trợ năng (Accessibility Settings)
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Kiểm tra ứng dụng có nằm trong danh sách bỏ qua tối ưu hóa pin không
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Yêu cầu bỏ qua tối ưu hóa pin để đảm bảo combo không bị hệ thống kill ngầm khi chơi game
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    /**
     * Kiểm tra quyền gửi thông báo (Android 13 / API 33+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Mở màn hình Thông tin ứng dụng (App Info / Details) để người dùng mở khóa
     * "Cài đặt bị hạn chế" (Restricted Settings) trên Android 13/14+
     */
    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Mở màn hình Tùy chọn nhà phát triển (Developer Options)
     */
    fun openDeveloperOptions(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            // Fallback: mở cài đặt chung nếu Developer Options chưa bật
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Mở màn hình quản lý Cửa sổ thả nổi của Infinix (Phone Master)
     */
    fun openTranssionFloatingSettings(context: Context): Boolean {
        val intents = listOf(
            Intent().apply {
                component = android.content.ComponentName(
                    "com.transsion.phonemaster",
                    "com.cyin.himgr.widget.dialog.PermissionActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent().apply {
                component = android.content.ComponentName(
                    "com.transsion.phonemaster",
                    "com.transsion.phonemaster.MainActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: Throwable) {}
        }
        return false
    }
}
