package com.macrophone.gaming.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Tiện ích tương tác với Shizuku (Gỡ lỗi qua Wi-Fi / Wireless Debugging):
 * - Tự động cấp quyền Trợ năng (Accessibility) và bỏ qua Restricted Settings
 * - Cấp quyền Vẽ trên màn hình (SYSTEM_ALERT_WINDOW)
 * - Cung cấp chuỗi lệnh ADB một chạm cho người dùng LADB hoặc máy tính
 */
object ShizukuHelper {

    const val SHIZUKU_REQUEST_CODE = 8899

    /**
     * Kiểm tra ứng dụng Shizuku có được cài đặt trên máy không
     */
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kiểm tra dịch vụ Shizuku có đang chạy (đã kích hoạt qua Wi-Fi) không
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Kiểm tra app đã được cấp quyền truy cập Shizuku chưa
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
            false
        }
    }

    /**
     * Yêu cầu quyền truy cập Shizuku
     */
    fun requestShizukuPermission(activity: Activity) {
        if (!isShizukuRunning()) return
        try {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        } catch (_: Throwable) {}
    }

    /**
     * Chạy toàn bộ các lệnh shell cấp quyền thông qua Shizuku
     */
    suspend fun grantAllPermissionsViaShizuku(context: Context): Result<String> = withContext(Dispatchers.IO) {
        if (!isShizukuRunning()) {
            return@withContext Result.failure(Exception("Shizuku chưa được kích hoạt qua Wi-Fi trên máy!"))
        }

        if (!hasShizukuPermission()) {
            return@withContext Result.failure(Exception("Ứng dụng chưa được cấp quyền sử dụng Shizuku!"))
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

        val outputLog = StringBuilder()

        try {
            for (cmd in commands) {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    outputLog.append(line).append("\n")
                }
                process.waitFor()
            }
            Result.success("Đã cấp toàn bộ quyền thành công qua Shizuku!")
        } catch (e: Throwable) {
            Result.failure(Exception("Lỗi khi thực thi lệnh Shizuku: ${e.message}"))
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
}
