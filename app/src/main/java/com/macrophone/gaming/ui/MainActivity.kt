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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.macrophone.gaming.R
import com.macrophone.gaming.core.ShellExecutor
import com.macrophone.gaming.data.MacroConfigStorage
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
    private lateinit var configStorage: MacroConfigStorage

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

        configStorage = MacroConfigStorage(this)
        binding.tvDeviceBadge.text = "Thiết bị: ${ShizukuHelper.getDeviceDisplayName()} • 120Hz Fast Combo"

        setupButtons()
        setupProfileViews()
        registerShizukuListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
        renderProfileChips()
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
        // 🚀 1-Chạm MỞ GAME ĐANG CHỌN & BẬT GAME TURBO
        binding.btnLaunchTurboAndGame.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Vui lòng cấp quyền 'Hiển thị trên ứng dụng khác' (Cửa sổ nổi) trước!", Toast.LENGTH_LONG).show()
                openOverlaySettings()
                return@setOnClickListener
            }

            if (!TurboOverlayService.isRunning) {
                TurboOverlayService.start(this)
            }

            launchCurrentGame()
            binding.root.postDelayed({ refreshStatuses() }, 300)
        }

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
                Toast.makeText(this, "Đã bật Game Turbo! Vuốt thông báo xuống & bấm [⚡ MỞ MENU] để cài đặt & ghi combo.", Toast.LENGTH_LONG).show()
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

    /**
     * Khởi tạo và đồng bộ giao diện Hồ Sơ Game trên Dashboard
     */
    private fun setupProfileViews() {
        renderProfileChips()

        binding.btnMainAddGameProfile.setOnClickListener {
            showAddGameProfileDialog()
        }
    }

    private fun renderProfileChips() {
        val container = binding.layoutMainProfileChips
        container.removeAllViews()

        val profiles = configStorage.getAllProfiles()
        val active = configStorage.getActiveProfile()

        binding.tvMainActiveProfileDetail.text = "Đang chọn: ${active.iconEmoji} ${active.name} • ${active.triggers.size} nút combo đã gán"
        binding.btnLaunchTurboAndGame.text = "🚀 MỞ ${active.name.uppercase()}"

        val density = resources.displayMetrics.density
        val padH = (14 * density).toInt()
        val padV = (8 * density).toInt()
        val marginEnd = (8 * density).toInt()

        profiles.forEach { profile ->
            val isSelected = (profile.id == active.id)
            val chip = TextView(this).apply {
                text = "${profile.iconEmoji} ${profile.name}"
                textSize = 11.5f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(padH, padV, padH, padV)
                setBackgroundResource(if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
                setTextColor(ContextCompat.getColor(this@MainActivity, if (isSelected) R.color.bg_dark else R.color.text_primary))
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    this.marginEnd = marginEnd
                }
                setOnClickListener {
                    if (profile.id != active.id) {
                        configStorage.setActiveProfileId(profile.id)
                        renderProfileChips()
                        Toast.makeText(this@MainActivity, "🎮 Đã chọn hồ sơ: [${profile.name}]", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            container.addView(chip)
        }
    }

    private fun showAddGameProfileDialog() {
        val input = EditText(this).apply {
            hint = "Tên game (VD: Free Fire, Tốc Chiến, Genshin)"
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cyan_neon))
            setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            setBackgroundResource(R.drawable.bg_turbo_btn_secondary)
            setPadding(32, 24, 32, 24)
        }

        val container = FrameLayout(this).apply {
            setPadding(40, 20, 40, 10)
            addView(input)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("🎮 Thêm Hồ Sơ Game Mới")
            .setMessage("Mỗi game sẽ lưu trữ riêng biệt danh sách nút combo và vị trí nút.")
            .setView(container)
            .setPositiveButton("Tạo Game") { _, _ ->
                val name = input.text?.toString()?.trim() ?: ""
                if (name.isNotEmpty()) {
                    configStorage.createProfile(name)
                    renderProfileChips()
                    Toast.makeText(this, "✨ Đã tạo và chọn hồ sơ: [$name]", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /**
     * Khởi động game theo hồ sơ đang chọn
     */
    private fun launchCurrentGame() {
        val active = configStorage.getActiveProfile()
        val pm = packageManager
        val targetPackages = when {
            active.id == MacroConfigStorage.DEFAULT_PROFILE_FREEFIRE_ID || active.name.contains("Free Fire", ignoreCase = true) ->
                listOf("com.dts.freefireth", "com.dts.freefiremax")
            active.id == MacroConfigStorage.DEFAULT_PROFILE_WILDRIFT_ID || active.name.contains("Tốc Chiến", ignoreCase = true) || active.name.contains("Wild Rift", ignoreCase = true) ->
                listOf("com.riotgames.league.wildriftvn", "com.riotgames.league.wildrift")
            else -> listOf(
                active.packageName ?: "",
                "com.garena.game.kgvn",
                "com.levelinfinite.sgameGlobal",
                "com.garena.game.kgtw",
                "com.garena.game.kgth"
            ).filter { it.isNotEmpty() }
        }

        for (pkg in targetPackages) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                configStorage.lastGamePackage = pkg
                Toast.makeText(this, "🚀 Đang mở ${active.name}...", Toast.LENGTH_SHORT).show()
                return
            }
        }

        launchLienQuanGame()
    }

    /**
     * Khởi động Liên Quân Mobile tự động
     */
    private fun launchLienQuanGame() {
        val packages = listOf(
            "com.garena.game.kgvn",         // Liên Quân Mobile Garena VN
            "com.levelinfinite.sgameGlobal", // Honor of Kings / AoV Global
            "com.garena.game.kgtw",         // AoV Đài Loan
            "com.garena.game.kgth"          // AoV Thái Lan
        )
        val pm = packageManager
        for (pkg in packages) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Toast.makeText(this, "🎮 Đang khởi động Liên Quân Mobile kèm Game Turbo!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        Toast.makeText(this, "Không tìm thấy game Liên Quân trên máy! Đang mở Google Play...", Toast.LENGTH_LONG).show()
        try {
            val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.garena.game.kgvn")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(playIntent)
        } catch (_: Throwable) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.garena.game.kgvn")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(webIntent)
        }
    }
}
