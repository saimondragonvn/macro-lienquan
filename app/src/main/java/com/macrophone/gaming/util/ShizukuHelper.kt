package com.macrophone.gaming.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Tiện ích tương tác với Shizuku (Gỡ lỗi qua Wi-Fi) & Root (cho máy ảo / giả lập Android):
 * - Tự động cấp toàn bộ quyền mà không cần làm thủ công
 * - Hỗ trợ cả điện thoại thật (qua Shizuku) và giả lập PC (LDPlayer, BlueStacks, Nox qua Root)
 * - Tuyệt đối không gây crash ứng dụng ("Ứng dụng đã dừng")
 */
object ShizukuHelper {

    const val SHIZUKU_REQUEST_CODE = 8899
    private const val TAG = "ShizukuHelper"

    /**
     * Kiểm tra ứng dụng Shizuku có được cài đặt trên máy không
     */
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Kiểm tra dịch vụ Shizuku có đang chạy (đã kích hoạt qua Wi-Fi) không
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Kiểm tra thiết bị có quyền Root không (đặc biệt hữu ích cho giả lập LDPlayer, Nox, BlueStacks trên PC)
     */
    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            process.waitFor()
            process.exitValue() == 0
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Kiểm tra ứng dụng đã được cấp quyền truy cập Shizuku chưa
     */
    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Throwable) {
            Log.w(TAG, "checkSelfPermission exception: ${e.message}")
            false
        }
    }

    /**
     * Yêu cầu người dùng cấp quyền truy cập Shizuku (hiển thị popup Shizuku an toàn, không crash)
     */
    fun requestShizukuPermission(activity: Activity): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) return false
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return true

            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "requestShizukuPermission error: ${e.message}", e)
            false
        }
    }

    /**
     * Thực thi lệnh shell privileged (thử qua Shizuku trước, sau đó qua Root `su`)
     */
    fun executePrivilegedCommand(cmd: String): Boolean {
        // 1. Thử qua Shizuku nếu đang chạy và đã cấp quyền
        if (isShizukuRunning() && hasShizukuPermission()) {
            try {
                val proc = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                proc.waitFor()
                val code = proc.exitValue()
                proc.destroy()
                if (code == 0) return true
            } catch (e: Throwable) {
                Log.w(TAG, "Shizuku exec command failed: ${e.message}")
            }
        }

        // 2. Thử qua Root `su` (cho máy giả lập PC hoặc máy đã root)
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            proc.waitFor()
            val code = proc.exitValue()
            proc.destroy()
            if (code == 0) return true
        } catch (_: Throwable) {}

        return false
    }

    /**
     * Chạy toàn bộ các lệnh shell tự động cấp quyền (hỗ trợ cả Shizuku lẫn Root)
     */
    suspend fun grantAllPermissionsViaShizuku(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val hasShizuku = isShizukuRunning() && hasShizukuPermission()
        val hasRoot = isRootAvailable()

        if (!hasShizuku && !hasRoot) {
            if (isShizukuRunning() && !hasShizukuPermission()) {
                return@withContext Result.failure(Exception("Ứng dụng chưa được cấp quyền Shizuku! Hãy cho phép trong hộp thoại Shizuku."))
            }
            return@withContext Result.failure(Exception("Chưa kích hoạt Shizuku và máy chưa có Root! Hãy mở Shizuku hoặc bật Root trên giả lập."))
        }

        val pkg = context.packageName
        val serviceClass = "com.macrophone.gaming.service.MacroAccessibilityService"

        val commands = listOf(
            // 1. Mở khóa "Cài đặt bị hạn chế" (Restricted Settings) trên Android 13/14+
            "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
            // 2. Cấp quyền vẽ trên ứng dụng khác (SYSTEM_ALERT_WINDOW)
            "appops set $pkg SYSTEM_ALERT_WINDOW allow",
            // 3. Kích hoạt trực tiếp dịch vụ Trợ năng
            "settings put secure enabled_accessibility_services $pkg/$serviceClass",
            "settings put secure accessibility_enabled 1",
            // 4. Bỏ qua tối ưu hóa pin
            "dumpsys deviceidle whitelist +$pkg"
        )

        var successCount = 0
        for (cmd in commands) {
            if (executePrivilegedCommand(cmd)) {
                successCount++
            }
        }

        if (successCount >= 3) {
            val modeStr = if (hasShizuku) "Shizuku" else "Root Giả Lập"
            Result.success("Đã cấp toàn bộ quyền thành công qua $modeStr!")
        } else {
            Result.failure(Exception("Đã thực thi lệnh nhưng một số quyền chưa nhận được."))
        }
    }

    /**
     * Chuỗi lệnh ADB dùng cho người dùng muốn tự chạy qua Gỡ lỗi Wi-Fi (LADB, Termux, hoặc PC)
     */
    fun getAdbCommandsString(context: Context): String {
        val pkg = context.packageName
        val serviceClass = "com.macrophone.gaming.service.MacroAccessibilityService"
        return """
            appops set $pkg ACCESS_RESTRICTED_SETTINGS allow
            appops set $pkg SYSTEM_ALERT_WINDOW allow
            settings put secure enabled_accessibility_services $pkg/$serviceClass
            settings put secure accessibility_enabled 1
            dumpsys deviceidle whitelist +$pkg
        """.trimIndent()
    }

    /**
     * Mở Shizuku trên CH Play (Google Play Store)
     */
    fun openShizukuInPlayStore(context: Context) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=moe.shizuku.privileged.api")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Throwable) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        }
    }

    /**
     * Mở ứng dụng Shizuku đã cài đặt
     */
    fun openShizukuApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "Chưa cài Shizuku! Hãy tải từ CH Play trước.", Toast.LENGTH_LONG).show()
                openShizukuInPlayStore(context)
            }
        } catch (_: Throwable) {
            openShizukuInPlayStore(context)
        }
    }
}
