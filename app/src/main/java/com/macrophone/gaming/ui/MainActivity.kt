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

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == ShellExecutor.SHIZUKU_REQUEST_CODE) {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Đã cấp quyền Shizuku thành công! Động cơ sẵn sàng 120Hz.", Toast.LENGTH_SHORT).show()
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
        registerShizukuListenerSafe()
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    override fun onDestroy() {
        unregisterShizukuListenerSafe()
        super.onDestroy()
    }

    private fun registerShizukuListenerSafe() {
        try {
            if (ShellExecutor.isShizukuRunning()) {
                Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
            }
        } catch (_: Throwable) {}
    }

    private fun unregisterShizukuListenerSafe() {
        try {
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
                    Toast.makeText(this, "Vui lòng cấp quyền 'Hiển thị trên ứng dụng khác' trước!", Toast.LENGTH_LONG).show()
                    openOverlaySettings()
                    return@setOnClickListener
                }
                TurboOverlayService.start(this)
                Toast.makeText(this, "Đã khởi động Game Turbo HUD! Hãy mở game Liên Quân.", Toast.LENGTH_LONG).show()
            }
            binding.root.postDelayed({ refreshStatuses() }, 300)
        }

        // Cấp quyền Cửa sổ nổi
        binding.btnGrantOverlay.setOnClickListener {
            openOverlaySettings()
        }

        // Cấp quyền Shizuku 1-chạm
        binding.btnGrantShizuku.setOnClickListener {
            if (!ShellExecutor.isShizukuRunning()) {
                Toast.makeText(
                    this,
                    "Shizuku chưa chạy! Hãy mở app Shizuku trên máy và bấm 'Khởi động' qua Wi-Fi trước.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (!ShellExecutor.hasShizukuPermission()) {
                ShellExecutor.requestShizukuPermission(this)
            } else {
                ShellExecutor.autoGrantPermissions(this)
                Toast.makeText(this, "Đã được cấp quyền Shizuku rồi!", Toast.LENGTH_SHORT).show()
                refreshStatuses()
            }
        }

        // Sao chép lệnh ADB
        binding.btnCopyAdb.setOnClickListener {
            val cmd = ShellExecutor.getAdbCommand(this)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
            Toast.makeText(this, "Đã sao chép lệnh ADB vào khay nhớ tạm!", Toast.LENGTH_SHORT).show()
        }

        // Mở cài đặt Infinix XOS (để mở khóa Restricted Settings)
        binding.btnUnlockInfinix.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, "Bấm nút 3 chấm (⋮) ở góc trên bên phải -> Chọn 'Cho phép cài đặt bị hạn chế'", Toast.LENGTH_LONG).show()
            } catch (_: Throwable) {
                Toast.makeText(this, "Không thể mở cài đặt ứng dụng.", Toast.LENGTH_SHORT).show()
            }
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
