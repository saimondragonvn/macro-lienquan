package com.macrophone.gaming.ui

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
import com.macrophone.gaming.R
import com.macrophone.gaming.databinding.ActivityMainBinding
import com.macrophone.gaming.ui.adapter.MacroPresetAdapter
import com.macrophone.gaming.util.PermissionUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Màn hình chính (MainActivity):
 * - Kiểm tra trạng thái cấp quyền (Trợ năng, Vẽ trên màn hình, Tối ưu pin, Thông báo)
 * - Quản lý danh sách Macro Presets
 * - Khởi động / tắt Floating Widget Controller
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var presetAdapter: MacroPresetAdapter

    // Xin quyền thông báo trên Android 13+
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
        observeViewModel()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật lại trạng thái quyền mỗi khi quay lại app từ Settings
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
                    PermissionUtils.openAccessibilitySettings(this)
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
            PermissionUtils.openAccessibilitySettings(this)
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
                PermissionUtils.openAccessibilitySettings(this)
                return@setOnClickListener
            }

            viewModel.toggleFloatingService(this)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Lắng nghe cập nhật quyền
                launch {
                    viewModel.permissions.collect { perms ->
                        updatePermissionUi(perms)
                    }
                }

                // Lắng nghe danh sách Macro và Active Macro
                launch {
                    combine(viewModel.macroList, viewModel.activeMacro) { list, active ->
                        Pair(list, active)
                    }.collect { (list, active) ->
                        presetAdapter.updateData(list, active?.id)
                    }
                }

                // Lắng nghe trạng thái chạy của Floating Service
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
        // 1. Accessibility Service Card
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

        // 2. Overlay Permission Card
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

        // 3. Battery Optimization Card
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasNotificationPermission(this)) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
