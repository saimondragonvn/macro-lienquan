package com.macrophone.gaming.core

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Động cơ thực thi lệnh đặc quyền tối giản, siêu tốc (Game Turbo Ultra Fast Engine):
 * - Hỗ trợ cả Shizuku (Gỡ lỗi Wi-Fi) và Root (su).
 * - Gom chuỗi lệnh tap vào 1 tiến trình shell duy nhất giúp xả combo với độ trễ 0ms.
 * - Bọc ngoại lệ 100%, tuyệt đối không bao giờ làm crash ứng dụng ("Ứng dụng đã dừng").
 */
object ShellExecutor {

    private const val TAG = "ShellExecutor"
    const val SHIZUKU_REQUEST_CODE = 7788

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
     * Kiểm tra ứng dụng đã được cấp quyền Shizuku chưa
     */
    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Yêu cầu cấp quyền Shizuku an toàn
     */
    fun requestShizukuPermission(activity: Activity): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) return false
            if (hasShizukuPermission()) return true
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "requestShizukuPermission error: ${e.message}")
            false
        }
    }

    /**
     * Kiểm tra thiết bị có quyền Root không (nhanh chóng, không block main thread)
     */
    fun isRootAvailable(): Boolean {
        val rootPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        return rootPaths.any { File(it).exists() }
    }

    /**
     * Trạng thái sẵn sàng của động cơ Game Turbo (Shizuku hoặc Root)
     */
    fun isEngineReady(): Boolean {
        return (isShizukuRunning() && hasShizukuPermission()) || isRootAvailable()
    }

    /**
     * Lấy tên chế độ hoạt động hiện tại để hiển thị cho game thủ
     */
    fun getEngineModeName(): String {
        return when {
            isShizukuRunning() && hasShizukuPermission() -> "Shizuku (Gỡ lỗi Wi-Fi)"
            isRootAvailable() -> "Root (su)"
            isShizukuRunning() -> "Shizuku (Chưa cấp quyền)"
            else -> "Chưa kích hoạt"
        }
    }

    /**
     * Thực thi lệnh shell đặc quyền với Shizuku hoặc Root
     */
    fun executeCommand(cmd: String): Boolean {
        // 1. Ưu tiên Shizuku
        if (isShizukuRunning() && hasShizukuPermission()) {
            try {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                val proc = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? Process
                if (proc != null) {
                    val finished = proc.waitFor(3, TimeUnit.SECONDS)
                    val code = if (finished) proc.exitValue() else -1
                    proc.destroy()
                    if (code == 0) return true
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Shizuku exec failed: ${e.message}")
            }
        }

        // 2. Dự phòng bằng Root `su` (giả lập PC / máy đã root)
        if (isRootAvailable()) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val finished = proc.waitFor(3, TimeUnit.SECONDS)
                val code = if (finished) proc.exitValue() else -1
                proc.destroy()
                if (code == 0) return true
            } catch (e: Throwable) {
                Log.w(TAG, "Root exec failed: ${e.message}")
            }
        }

        return false
    }

    /**
     * Thực thi chuỗi Combo các điểm chạm (input tap X Y) siêu tốc 120Hz
     */
    fun executeCombo(points: List<Pair<Float, Float>>, delayBetweenMs: Long = 40): Boolean {
        if (points.isEmpty()) return false

        val sb = StringBuilder()
        for (i in points.indices) {
            val (x, y) = points[i]
            sb.append("input tap ").append(x.toInt()).append(" ").append(y.toInt()).append("; ")
            if (i < points.size - 1 && delayBetweenMs > 0) {
                val sec = String.format(Locale.US, "%.3f", delayBetweenMs / 1000.0)
                sb.append("sleep ").append(sec).append("; ")
            }
        }

        val fullCmd = sb.toString().trim().removeSuffix(";")
        return executeCommand(fullCmd)
    }

    /**
     * Tự động cấp các quyền hệ thống qua shell
     */
    fun autoGrantPermissions(context: Context): Boolean {
        val pkg = context.packageName
        val cmds = listOf(
            "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
            "appops set $pkg SYSTEM_ALERT_WINDOW allow",
            "dumpsys deviceidle whitelist +$pkg"
        )
        var successCount = 0
        for (c in cmds) {
            if (executeCommand(c)) successCount++
        }
        return successCount >= 1
    }

    /**
     * Chuỗi lệnh ADB dùng để người dùng sao chép dán vào LADB hoặc PC
     */
    fun getAdbCommand(context: Context): String {
        val pkg = context.packageName
        return "appops set $pkg SYSTEM_ALERT_WINDOW allow && dumpsys deviceidle whitelist +$pkg"
    }
}
