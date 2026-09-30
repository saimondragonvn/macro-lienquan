package com.macrophone.gaming.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Tiện ích tương tác với Shizuku (Gỡ lỗi qua Wi-Fi) & Root:
 * - Tối ưu hóa đặc biệt cho Infinix Note 30 (Transsion XOS 13/14) và trình giả lập PC.
 * - Kiểm tra Root an toàn tức thì (không block Main UI thread).
 * - Bắt trọn 100% ngoại lệ, chống hoàn toàn hiện tượng văng ứng dụng ("Ứng dụng đã dừng").
 */
object ShizukuHelper {

    const val SHIZUKU_REQUEST_CODE = 8899
    private const val TAG = "ShizukuHelper"

    @Volatile
    private var cachedRootAvailable: Boolean? = null

    /**
     * Tự động nhận diện thiết bị Transsion (Infinix, Tecno, Itel) chạy XOS / HiOS
     */
    fun isTranssionDevice(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("infinix") || m.contains("tecno") || m.contains("itel") || m.contains("transsion") ||
                b.contains("infinix") || b.contains("tecno") || b.contains("itel")
    }

    /**
     * Lấy tên thương hiệu và model hiển thị đẹp mắt cho người dùng
     */
    fun getDeviceDisplayName(): String {
        val model = Build.MODEL
        val brand = Build.BRAND.replaceFirstChar { it.uppercase() }
        return if (model.startsWith(brand, ignoreCase = true)) {
            model
        } else {
            "$brand $model"
        }
    }

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
     * Kiểm tra thiết bị có quyền Root không.
     * TỐI ƯU HÓA: Kiểm tra sự tồn tại của file nhị phân trước, tránh chạy exec trên Main Thread làm đơ máy Infinix Note 30!
     */
    fun isRootAvailable(): Boolean {
        cachedRootAvailable?.let { return it }

        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        val hasBinary = rootPaths.any { File(it).exists() }
        if (!hasBinary) {
            cachedRootAvailable = false
            return false
        }

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val finished = process.waitFor(500, TimeUnit.MILLISECONDS)
            val isRoot = finished && process.exitValue() == 0
            process.destroy()
            cachedRootAvailable = isRoot
            isRoot
        } catch (_: Throwable) {
            cachedRootAvailable = false
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
                val proc = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? Process
                if (proc != null) {
                    val finished = proc.waitFor(3, TimeUnit.SECONDS)
                    val code = if (finished) proc.exitValue() else -1
                    proc.destroy()
                    if (code == 0) return true
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Shizuku exec command failed: ${e.message}")
            }
        }

        // 2. Thử qua Root `su` (cho máy giả lập PC hoặc máy đã root)
        if (isRootAvailable()) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val finished = proc.waitFor(3, TimeUnit.SECONDS)
                val code = if (finished) proc.exitValue() else -1
                proc.destroy()
                if (code == 0) return true
            } catch (_: Throwable) {}
        }

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
        val commands = listOf(
            // 1. Mở khóa "Cài đặt bị hạn chế" (Restricted Settings) trên Android 13/14 (Infinix XOS)
            "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
            // 2. Cấp quyền vẽ trên ứng dụng khác (SYSTEM_ALERT_WINDOW)
            "appops set $pkg SYSTEM_ALERT_WINDOW allow",
            // 3. Bỏ qua tối ưu hóa pin cho game mượt mà
            "dumpsys deviceidle whitelist +$pkg"
        )

        var successCount = 0
        for (cmd in commands) {
            if (executePrivilegedCommand(cmd)) {
                successCount++
            }
        }

        if (successCount >= 1) {
            val modeStr = if (hasShizuku) "Shizuku" else "Root"
            Result.success("Đã tự động mở khóa & kích hoạt Động cơ Game Turbo thành công qua $modeStr!")
        } else {
            Result.failure(Exception("Đã thực thi lệnh nhưng hệ thống chưa lưu. Hãy kiểm tra Shizuku."))
        }
    }

    /**
     * Chuỗi lệnh ADB dùng cho người dùng muốn tự chạy qua Gỡ lỗi Wi-Fi (LADB, Termux, hoặc PC)
     */
    fun getAdbCommandsString(context: Context): String {
        val pkg = context.packageName
        return """
            appops set $pkg ACCESS_RESTRICTED_SETTINGS allow
            appops set $pkg SYSTEM_ALERT_WINDOW allow
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
