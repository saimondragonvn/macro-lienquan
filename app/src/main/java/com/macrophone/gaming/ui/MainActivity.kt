package com.macrophone.gaming.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.macrophone.gaming.R
import com.macrophone.gaming.core.ShellExecutor
import com.macrophone.gaming.databinding.ActivityMainBinding
import com.macrophone.gaming.service.TurboOverlayService
import com.macrophone.gaming.util.ShizukuHelper
import rikka.shizuku.Shizuku

/**
 * Màn hình chính Game Turbo Pro (Kiến trúc Mới):
 * - Quản trị và kiểm tra trạng thái quyền trực quan, tức thì.
 * - 1 Chạm để Bật/Tắt Game Turbo HUD.
 * - Hỗ trợ cấp quyền Shizuku / Root an toàn 100%, không bao giờ văng ứng dụng.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                refreshStatuses()
            }
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                refreshStatuses()
            }
        }
    }

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == ShellExecutor.SHIZUKU_REQUEST_CODE) {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Đã cấp quyền Shizuku thành công! Động cơ 120Hz sẵn sàng.", Toast.LENGTH_SHORT).show()
                    ShellExecutor.autoGrantPermissions(this)
                } else {
                    Toast.makeText(this, "Bạn đã từ chối cấp quyền Shizuku.", Toast.LENGTH_SHORT).show()
                }
                refreshStatuses()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvDeviceBadge.text = "Thiết bị: ${ShizukuHelper.getDeviceDisplayName()} • 120Hz Fast Combo"

        setupButtons()
        registerShizukuListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    override fun onDestroy() {
        unregisterShizukuListeners()
        super.onDestroy()
    }

    private fun registerShizukuListeners() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Throwable) {}
    }

    private fun unregisterShizukuListeners() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Throwable) {}
    }

    private fun setupButtons() {
        // Nút BẬT / TẮT GAME TURBO HUD
        binding.btnToggleTurbo.setOnClickListener {
            if (TurboOverlayService.isRunning) {
                TurboOverlayService.stop(this)
                Toast.makeText(this, "Đã tắt Game Turbo HUD", Toast.LENGTH_SHORT).show()
            } else {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Vui lòng cấp quyền 'Hiển thị trên ứng dụng khác' (Cửa sổ nổi) trước!", Toast.LENGTH_LONG).show()
                    openOverlaySettings()
                    return@setOnClickListener
                }
                TurboOverlayService.start(this)
                Toast.makeText(this, "Đã bật Game Turbo HUD! Nhìn vào mép màn hình để thấy tab [⚡ TURBO].", Toast.LENGTH_LONG).show()
            }
            binding.root.postDelayed({ refreshStatuses() }, 300)
        }

        // Cấp quyền Cửa sổ nổi
        binding.btnGrantOverlay.setOnClickListener {
            openOverlaySettings()
        }

        // Cấp quyền Shizuku 1-chạm
        binding.btnGrantShizuku.setOnClickListener {
            if (ShellExecutor.isShizukuRunning()) {
                if (ShellExecutor.hasShizukuPermission()) {
                    ShellExecutor.autoGrantPermissions(this)
                    Toast.makeText(this, "Đã cấp quyền Shizuku rồi! Động cơ sẵn sàng.", Toast.LENGTH_SHORT).show()
                    refreshStatuses()
                } else {
                    val ok = ShellExecutor.requestShizukuPermission(this)
                    if (!ok) {
                        Toast.makeText(this, "Không thể mở hộp thoại xin quyền Shizuku. Vui lòng kiểm tra lại dịch vụ Shizuku.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // Shizuku chưa chạy trên máy -> Thử mở app Shizuku cho người dùng
                val shizukuLaunchIntent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (shizukuLaunchIntent != null) {
                    Toast.makeText(
                        this,
                        "Đang mở ứng dụng Shizuku... Vui lòng bấm 'Khởi động' (qua Ghép nối Wi-Fi) trong app Shizuku!",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(shizukuLaunchIntent)
                } else {
                    Toast.makeText(
                        this,
                        "Chưa tìm thấy app Shizuku! Hãy cài đặt app Shizuku từ CH Play và khởi động qua Gỡ lỗi Wi-Fi.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Sao chép lệnh ADB
        binding.btnCopyAdb.setOnClickListener {
            val cmd = ShellExecutor.getAdbCommand(this)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
            Toast.makeText(this, "Đã sao chép lệnh ADB vào khay nhớ tạm!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openOverlaySettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (_: Throwable) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }
    }

    private fun refreshStatuses() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val engineReady = ShellExecutor.isEngineReady()
        val isServiceRunning = TurboOverlayService.isRunning

        // 1. Cửa sổ nổi
        if (hasOverlay) {
            binding.tvOverlayBadge.text = "ĐÃ CẤP"
            binding.tvOverlayBadge.setTextColor(ContextCompat.getColor(this, R.color.emerald_play))
            binding.btnGrantOverlay.text = "✓ Đã cấp quyền Cửa sổ nổi"
            binding.btnGrantOverlay.isEnabled = false
            binding.btnGrantOverlay.alpha = 0.6f
        } else {
            binding.tvOverlayBadge.text = "CHƯA CẤP"
            binding.tvOverlayBadge.setTextColor(ContextCompat.getColor(this, R.color.amber_warning))
            binding.btnGrantOverlay.text = "Cấp quyền Cửa sổ nổi"
            binding.btnGrantOverlay.isEnabled = true
            binding.btnGrantOverlay.alpha = 1.0f
        }

        // 2. Động cơ Shizuku / Root
        if (engineReady) {
            binding.tvShizukuBadge.text = "SẴN SÀNG (${ShellExecutor.getEngineModeName()})"
            binding.tvShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.emerald_play))
        } else {
            binding.tvShizukuBadge.text = "CHƯA CẤP"
            binding.tvShizukuBadge.setTextColor(ContextCompat.getColor(this, R.color.amber_warning))
        }

        // 3. Trạng thái toàn cục
        if (hasOverlay && engineReady) {
            binding.tvGlobalStatus.text = "SẴN SÀNG"
            binding.tvGlobalStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_play))
        } else {
            binding.tvGlobalStatus.text = "CẦN CẤP QUYỀN"
            binding.tvGlobalStatus.setTextColor(ContextCompat.getColor(this, R.color.amber_warning))
        }

        // 4. Nút Khởi động Turbo
        if (isServiceRunning) {
            binding.btnToggleTurbo.text = "⏹ TẮT GAME TURBO HUD"
            binding.btnToggleTurbo.setBackgroundColor(ContextCompat.getColor(this, R.color.crimson_stop))
            binding.btnToggleTurbo.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            binding.btnToggleTurbo.text = "⚡ BẬT GAME TURBO HUD"
            binding.btnToggleTurbo.setBackgroundColor(ContextCompat.getColor(this, R.color.cyan_neon))
            binding.btnToggleTurbo.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
        }
    }
}
