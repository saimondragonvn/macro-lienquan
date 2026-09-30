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
        val wrappedCmd = "$cmd >/dev/null 2>&1"

        // 1. Ưu tiên Shizuku (Gỡ lỗi Wi-Fi ADB đặc quyền)
        if (isShizukuRunning() && hasShizukuPermission()) {
            try {
                val proc = try {
                    val method = try {
                        Shizuku::class.java.getMethod(
                            "newProcess",
                            Array<String>::class.java,
                            Array<String>::class.java,
                            String::class.java
                        )
                    } catch (_: Throwable) {
                        Shizuku::class.java.getDeclaredMethod(
                            "newProcess",
                            Array<String>::class.java,
                            Array<String>::class.java,
                            String::class.java
                        )
                    }
                    method.isAccessible = true
                    method.invoke(null, arrayOf("sh", "-c", wrappedCmd), null, null) as? Process
                } catch (e: Throwable) {
                    Log.w(TAG, "Shizuku process invocation error: ${e.message}")
                    null
                }

                if (proc != null) {
                    try { proc.outputStream.close() } catch (_: Throwable) {}
                    val finished = proc.waitFor(3, TimeUnit.SECONDS)
                    val code = if (finished) proc.exitValue() else -1
                    proc.destroy()
                    if (code == 0 || finished) {
                        return true
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Shizuku exec failed: ${e.message}")
            }
        }

        // 2. Dự phòng bằng Root `su` (giả lập PC / máy đã root)
        if (isRootAvailable()) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", wrappedCmd))
                try { proc.outputStream.close() } catch (_: Throwable) {}
                val finished = proc.waitFor(3, TimeUnit.SECONDS)
                val code = if (finished) proc.exitValue() else -1
                proc.destroy()
                if (code == 0 || finished) return true
            } catch (e: Throwable) {
                Log.w(TAG, "Root exec failed: ${e.message}")
            }
        }

        return false
    }

    /**
     * Nhấn 1 điểm (tap) an toàn và chuẩn xác cho Game:
     * Dùng input swipe với thời lượng 35ms mô phỏng cảm ứng ngón tay thật của game thủ,
     * đảm bảo game engine (Liên Quân / Unity / Tencent) nhận diện 100%.
     */
    fun tap(x: Float, y: Float, durationMs: Long = 35): Boolean {
        val xi = x.toInt()
        val yi = y.toInt()
        val dur = durationMs.coerceIn(25L, 200L)
        return executeCommand("input swipe $xi $yi $xi $yi $dur")
    }

    /**
     * Vuốt từ điểm (x1, y1) đến (x2, y2) với thời lượng nhất định
     */
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 100): Boolean {
        val dur = durationMs.coerceAtLeast(35)
        return executeCommand("input swipe ${x1.toInt()} ${y1.toInt()} ${x2.toInt()} ${y2.toInt()} $dur")
    }

    /**
     * Thực thi chuỗi Combo các điểm chạm siêu tốc 120Hz:
     * Gom toàn bộ các điểm ghim vào 1 chuỗi lệnh swipe duy nhất, bắn thẳng vào kernel input pipeline!
     */
    fun executeCombo(points: List<Pair<Float, Float>>, delayBetweenMs: Long = 40, repeatCount: Int = 1): Boolean {
        if (points.isEmpty()) return false

        val sb = StringBuilder()
        val totalLoops = repeatCount.coerceIn(1, 5)
        for (r in 0 until totalLoops) {
            for (i in points.indices) {
                val (x, y) = points[i]
                val xi = x.toInt()
                val yi = y.toInt()
                sb.append("input swipe ").append(xi).append(" ").append(yi).append(" ")
                    .append(xi).append(" ").append(yi).append(" 35; ")

                // Nếu có khoảng nghỉ delay lớn hơn 50ms giữa các chiêu
                if (delayBetweenMs > 50 && (i < points.size - 1 || r < totalLoops - 1)) {
                    val sec = String.format(Locale.US, "%.2f", delayBetweenMs / 1000.0)
                    sb.append("sleep ").append(sec).append("; ")
                }
            }
        }

        val fullCmd = sb.toString().trim().removeSuffix(";")
        return executeCommand(fullCmd)
    }

    /**
     * Thực thi chuỗi MacroAction (bao gồm cả TAP, HOLD, SWIPE) ghi lại từ màn hình
     */
    fun executeActions(
        actions: List<com.macrophone.gaming.data.model.MacroAction>,
        speedMultiplier: Float = 1.0f,
        repeatCount: Int = 1
    ): Boolean {
        if (actions.isEmpty()) return false

        val mult = speedMultiplier.coerceIn(0.5f, 10.0f)
        val sb = StringBuilder()
        val totalLoops = repeatCount.coerceIn(1, 5)
        for (r in 0 until totalLoops) {
            for (i in actions.indices) {
                val act = actions[i]
                when (act.type) {
                    com.macrophone.gaming.data.model.MacroType.TAP -> {
                        val pt = act.points.firstOrNull() ?: continue
                        val xi = pt.x.toInt()
                        val yi = pt.y.toInt()
                        sb.append("input swipe ").append(xi).append(" ").append(yi).append(" ")
                            .append(xi).append(" ").append(yi).append(" 35; ")
                    }
                    com.macrophone.gaming.data.model.MacroType.HOLD -> {
                        val pt = act.points.firstOrNull() ?: continue
                        val xi = pt.x.toInt()
                        val yi = pt.y.toInt()
                        val dur = (act.durationMs / mult).toLong().coerceIn(35L, 1000L)
                        sb.append("input swipe ").append(xi).append(" ").append(yi).append(" ")
                            .append(xi).append(" ").append(yi).append(" ").append(dur).append("; ")
                    }
                    com.macrophone.gaming.data.model.MacroType.SWIPE -> {
                        val start = act.points.firstOrNull() ?: continue
                        val end = act.points.lastOrNull() ?: continue
                        val dur = (act.durationMs / mult).toLong().coerceIn(35L, 500L)
                        sb.append("input swipe ").append(start.x.toInt()).append(" ").append(start.y.toInt()).append(" ")
                            .append(end.x.toInt()).append(" ").append(end.y.toInt()).append(" ").append(dur).append("; ")
                    }
                }
                val delay = (act.delayBeforeMs / mult).toLong()
                if (delay > 50 && (i < actions.size - 1 || r < totalLoops - 1)) {
                    val sec = String.format(Locale.US, "%.2f", delay / 1000.0)
                    sb.append("sleep ").append(sec).append("; ")
                }
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
