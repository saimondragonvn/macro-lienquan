package com.macrophone.gaming.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.macrophone.gaming.R
import com.macrophone.gaming.databinding.ActivityMainBinding
import com.macrophone.gaming.ui.adapter.MacroPresetAdapter
import com.macrophone.gaming.util.PermissionUtils
import com.macrophone.gaming.util.ShizukuHelper
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * Màn hình chính (MainActivity):
 * - Quản trị và tự động cấp quyền qua Shizuku / Gỡ lỗi Wi-Fi
 * - Kiểm tra trạng thái cấp quyền (Trợ năng, Vẽ trên màn hình, Tối ưu pin, Thông báo)
 * - Quản lý danh sách Macro Presets
 * - Khởi động / tắt Floating Widget Controller
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var presetAdapter: MacroPresetAdapter

    // Lắng nghe kết quả yêu cầu quyền Shizuku
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == ShizukuHelper.SHIZUKU_REQUEST_CODE) {
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                executeShizukuGrant()
            } else {
                Toast.makeText(this, "Bạn đã từ chối cấp quyền Shizuku!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        setupShizukuListeners()
        observeViewModel()
        requestNotificationPermissionIfNeeded()

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Throwable) {}
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun setupRecyclerView() {
        presetAdapter = MacroPresetAdapter(
            items = emptyList(),
            activeId = null,
            onSelect = { sequence ->
                viewModel.selectMacro(sequence)
            },
            onQuickPlay = { sequence ->
                if (!viewModel.permissions.value.hasAccessibility) {
                    Toast.makeText(this, "Vui lòng cấp quyền Trợ năng trước khi phát!", Toast.LENGTH_SHORT).show()
                    showAccessibilityGuidanceDialog()
                } else {
                    viewModel.playMacro(sequence)
                    Toast.makeText(this, "Đang phát: ${sequence.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = { sequence ->
                viewModel.deleteMacro(sequence)
            }
        )

        binding.rvPresets.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = presetAdapter
        }
    }

    private fun setupListeners() {
        binding.btnGrantAccessibility.setOnClickListener {
            showAccessibilityGuidanceDialog()
        }

        binding.btnGrantOverlay.setOnClickListener {
            PermissionUtils.openOverlaySettings(this)
        }

        binding.btnGrantBattery.setOnClickListener {
            PermissionUtils.openBatteryOptimizationSettings(this)
        }

        binding.btnResetDefaultPresets.setOnClickListener {
            viewModel.resetDefaults()
            Toast.makeText(this, "Đã khôi phục các combo mẫu", Toast.LENGTH_SHORT).show()
        }

        binding.btnToggleFloatingWidget.setOnClickListener {
            val perms = viewModel.permissions.value
            if (!perms.hasOverlay) {
                Toast.makeText(this, "Vui lòng cấp quyền 'Display over other apps' trước!", Toast.LENGTH_SHORT).show()
                PermissionUtils.openOverlaySettings(this)
                return@setOnClickListener
            }
            if (!perms.hasAccessibility) {
                Toast.makeText(this, "Vui lòng bật 'Accessibility Service' trước!", Toast.LENGTH_SHORT).show()
                showAccessibilityGuidanceDialog()
                return@setOnClickListener
            }

            viewModel.toggleFloatingService(this)
        }
    }

    /**
     * Cài đặt tương tác cho tính năng Shizuku & Gỡ lỗi qua Wi-Fi
     */
    private fun setupShizukuListeners() {
        binding.btnGrantShizuku.setOnClickListener {
            executeShizukuGrant()
        }

        binding.btnCopyAdb.setOnClickListener {
            copyAdbCommands()
        }
    }

    private fun executeShizukuGrant() {
        if (!ShizukuHelper.isShizukuRunning()) {
            showShizukuNotRunningDialog()
            return
        }

        if (!ShizukuHelper.hasShizukuPermission()) {
            Toast.makeText(this, "Đang xin quyền truy cập Shizuku...", Toast.LENGTH_SHORT).show()
            ShizukuHelper.requestShizukuPermission(this)
            return
        }

        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Đang tự động chạy lệnh cấp quyền...", Toast.LENGTH_SHORT).show()
            val result = ShizukuHelper.grantAllPermissionsViaShizuku(this@MainActivity)
            result.onSuccess { msg ->
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Thành công!")
                    .setMessage("Đã tự động mở khóa Restricted Settings, kích hoạt Trợ năng và cấp quyền Vẽ màn hình qua Shizuku!")
                    .setPositiveButton("Tuyệt vời", null)
                    .show()
                viewModel.refreshData()
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, err.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showShizukuNotRunningDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Shizuku chưa được khởi động!")
            .setMessage(
                "Để kích hoạt Shizuku bằng Gỡ lỗi qua Wi-Fi (Không cần máy tính):\n\n" +
                "1. Tải ứng dụng 'Shizuku' từ CH Play (hoặc GitHub).\n" +
                "2. Vào Cài đặt điện thoại > Tùy chọn nhà phát triển > Bật 'Gỡ lỗi không dây' (Wireless Debugging).\n" +
                "3. Mở Shizuku > Chọn 'Ghép nối' (Pairing) > Nhập mã 6 số từ Gỡ lỗi không dây.\n" +
                "4. Nhấn 'Khởi động' (Start) trong Shizuku.\n" +
                "5. Quay lại app này nhấn nút 'Cấp quyền Shizuku' là XONG NGAY!"
            )
            .setPositiveButton("Sao chép lệnh ADB thủ công") { _, _ ->
                copyAdbCommands()
            }
            .setNegativeButton("Đã hiểu", null)
            .show()
    }

    private fun copyAdbCommands() {
        val cmds = ShizukuHelper.getAdbCommandsString(this)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Macro ADB Commands", cmds)
        clipboard.setPrimaryClip(clip)

        MaterialAlertDialogBuilder(this)
            .setTitle("Đã sao chép 5 dòng lệnh ADB!")
            .setMessage(
                "Bạn có thể dán toàn bộ lệnh này vào ứng dụng LADB (Gỡ lỗi Wi-Fi ngay trên điện thoại) hoặc Command Prompt trên PC:\n\n" +
                cmds + "\n\n" +
                "Lệnh này sẽ tự động:\n" +
                "✓ Mở khóa Cài đặt bị hạn chế (Restricted settings)\n" +
                "✓ Kích hoạt Trợ năng (Accessibility Service)\n" +
                "✓ Cấp quyền Cửa sổ nổi (SYSTEM_ALERT_WINDOW)"
            )
            .setPositiveButton("Đã hiểu", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.permissions.collect { perms ->
                        updatePermissionUi(perms)
                    }
                }

                launch {
                    combine(viewModel.macroList, viewModel.activeMacro) { list, active ->
                        Pair(list, active)
                    }.collect { (list, active) ->
                        presetAdapter.updateData(list, active?.id)
                    }
                }

                launch {
                    viewModel.isFloatingServiceRunning.collect { isRunning ->
                        if (isRunning) {
                            binding.btnToggleFloatingWidget.text = getString(R.string.btn_stop_dock)
                            binding.btnToggleFloatingWidget.setBackgroundColor(
                                ContextCompat.getColor(this@MainActivity, R.color.crimson_stop)
                            )
                            binding.btnToggleFloatingWidget.setIconResource(R.drawable.ic_stop)
                        } else {
                            binding.btnToggleFloatingWidget.text = getString(R.string.btn_start_dock)
                            binding.btnToggleFloatingWidget.setBackgroundColor(
                                ContextCompat.getColor(this@MainActivity, R.color.cyan_neon)
                            )
                            binding.btnToggleFloatingWidget.setIconResource(R.drawable.ic_play)
                        }
                    }
                }
            }
        }
    }

    private fun updatePermissionUi(perms: PermissionState) {
        if (perms.hasAccessibility) {
            binding.ivAccStatus.setImageResource(R.drawable.ic_check)
            binding.ivAccStatus.setColorFilter(ContextCompat.getColor(this, R.color.emerald_play))
            binding.btnGrantAccessibility.text = getString(R.string.btn_granted)
            binding.btnGrantAccessibility.isEnabled = false
            binding.btnGrantAccessibility.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_surface_elevated))
            binding.btnGrantAccessibility.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            binding.ivAccStatus.setImageResource(R.drawable.ic_warning)
            binding.ivAccStatus.setColorFilter(ContextCompat.getColor(this, R.color.amber_warning))
            binding.btnGrantAccessibility.text = getString(R.string.btn_grant)
            binding.btnGrantAccessibility.isEnabled = true
            binding.btnGrantAccessibility.setBackgroundColor(ContextCompat.getColor(this, R.color.cyan_neon))
            binding.btnGrantAccessibility.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
        }

        if (perms.hasOverlay) {
            binding.ivOverlayStatus.setImageResource(R.drawable.ic_check)
            binding.ivOverlayStatus.setColorFilter(ContextCompat.getColor(this, R.color.emerald_play))
            binding.btnGrantOverlay.text = getString(R.string.btn_granted)
            binding.btnGrantOverlay.isEnabled = false
            binding.btnGrantOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_surface_elevated))
            binding.btnGrantOverlay.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            binding.ivOverlayStatus.setImageResource(R.drawable.ic_warning)
            binding.ivOverlayStatus.setColorFilter(ContextCompat.getColor(this, R.color.amber_warning))
            binding.btnGrantOverlay.text = getString(R.string.btn_grant)
            binding.btnGrantOverlay.isEnabled = true
            binding.btnGrantOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.cyan_neon))
            binding.btnGrantOverlay.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
        }

        if (perms.isBatteryOptimized) {
            binding.ivBatteryStatus.setImageResource(R.drawable.ic_check)
            binding.ivBatteryStatus.setColorFilter(ContextCompat.getColor(this, R.color.emerald_play))
            binding.btnGrantBattery.text = getString(R.string.btn_granted)
            binding.btnGrantBattery.isEnabled = false
            binding.btnGrantBattery.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_surface_elevated))
            binding.btnGrantBattery.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            binding.ivBatteryStatus.setImageResource(R.drawable.ic_warning)
            binding.ivBatteryStatus.setColorFilter(ContextCompat.getColor(this, R.color.amber_warning))
            binding.btnGrantBattery.text = getString(R.string.btn_grant)
            binding.btnGrantBattery.isEnabled = true
            binding.btnGrantBattery.setBackgroundColor(ContextCompat.getColor(this, R.color.cyan_neon))
            binding.btnGrantBattery.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
        }
    }

    private fun showAccessibilityGuidanceDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Kích hoạt Dịch vụ Trợ năng")
            .setMessage(
                "Để Macro tự động bấm chiêu trong game, bạn cần gạt BẬT 'Macro Gaming Combo Service'.\n\n" +
                "💡 MẸO NHANH: Bạn có thể dùng nút [⚡ Cấp quyền Shizuku] ở đầu trang để kích hoạt tự động 1-chạm mà không cần làm thủ công!\n\n" +
                "⚠️ HOẶC MỞ KHÓA THỦ CÔNG (ANDROID 13/14+):\n" +
                "Nếu công tắc Trợ năng bị MỜ (báo 'Cài đặt bị hạn chế'):\n" +
                "1. Nhấn nút [Mở Cài đặt ứng dụng] bên dưới.\n" +
                "2. Bấm vào dấu 3 chấm (⋮) ở góc trên bên phải màn hình.\n" +
                "3. Chọn 'Cho phép cài đặt bị hạn chế' (Allow restricted settings).\n" +
                "4. Xác nhận mở khóa vân tay / mã PIN.\n" +
                "5. Quay lại đây và nhấn [Đến Cài đặt Trợ năng] để gạt BẬT!"
            )
            .setPositiveButton("Đến Cài đặt Trợ năng") { _, _ ->
                PermissionUtils.openAccessibilitySettings(this)
            }
            .setNeutralButton("Mở Cài đặt ứng dụng (Mở khóa)") { _, _ ->
                PermissionUtils.openAppDetailsSettings(this)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasNotificationPermission(this)) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
