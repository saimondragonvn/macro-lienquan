package com.macrophone.gaming.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.macrophone.gaming.R
import com.macrophone.gaming.databinding.ActivitySetupWizardBinding
import com.macrophone.gaming.util.PermissionUtils
import com.macrophone.gaming.util.ShizukuHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * Trình hướng dẫn cấp quyền kiểu Panda Touch Pro:
 * Hướng dẫn từng bước rõ ràng, trực quan, tự động kiểm tra trạng thái.
 *
 * 4 bước:
 * 1. Cài Shizuku từ CH Play
 * 2. Bật Gỡ lỗi không dây (Wireless Debugging)
 * 3. Ghép nối (Pairing) và khởi động Shizuku
 * 4. Nhấn nút để tự động cấp TẤT CẢ quyền
 */
class SetupWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupWizardBinding

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == ShizukuHelper.SHIZUKU_REQUEST_CODE &&
            grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            runAutoGrant()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SetupWizardActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        try {
            if (ShizukuHelper.isShizukuRunning()) {
                Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
            }
        } catch (_: Throwable) {}
    }

    override fun onResume() {
        super.onResume()
        refreshStepStatuses()
    }

    override fun onDestroy() {
        try {
            if (ShizukuHelper.isShizukuRunning()) {
                Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
            }
        } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Bước 1: Cài Shizuku
        binding.btnStep1Action.setOnClickListener {
            ShizukuHelper.openShizukuInPlayStore(this)
        }

        // Bước 2: Mở cài đặt Developer Options
        binding.btnStep2Action.setOnClickListener {
            PermissionUtils.openDeveloperOptions(this)
        }

        // Bước 3: Mở Shizuku app để ghép nối
        binding.btnStep3Action.setOnClickListener {
            ShizukuHelper.openShizukuApp(this)
        }

        // Bước 4: Cấp tất cả quyền
        binding.btnStep4Action.setOnClickListener {
            if (!ShizukuHelper.isShizukuRunning()) {
                Toast.makeText(this, "Shizuku chưa khởi động! Hãy hoàn thành bước 3.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!ShizukuHelper.hasShizukuPermission()) {
                ShizukuHelper.requestShizukuPermission(this)
                return@setOnClickListener
            }
            runAutoGrant()
        }

        // Nút Refresh kiểm tra lại trạng thái
        binding.btnRefreshStatus.setOnClickListener {
            refreshStepStatuses()
            Toast.makeText(this, "Đã cập nhật trạng thái!", Toast.LENGTH_SHORT).show()
        }

        // Nút Hoàn tất
        binding.btnFinish.setOnClickListener {
            finish()
        }
    }

    private fun refreshStepStatuses() {
        // Step 1: Shizuku installed?
        val shizukuInstalled = ShizukuHelper.isShizukuInstalled(this)
        updateStepStatus(binding.ivStep1Status, binding.btnStep1Action, shizukuInstalled)

        // Step 2: Developer options / Wireless debugging (không kiểm tra chính xác được, check Shizuku running)
        val shizukuRunning = ShizukuHelper.isShizukuRunning()
        updateStepStatus(binding.ivStep2Status, binding.btnStep2Action, shizukuRunning)

        // Step 3: Shizuku running & paired
        updateStepStatus(binding.ivStep3Status, binding.btnStep3Action, shizukuRunning)

        // Step 4: All permissions granted
        val allGranted = PermissionUtils.hasOverlayPermission(this) &&
                PermissionUtils.isIgnoringBatteryOptimizations(this)
        updateStepStatus(binding.ivStep4Status, binding.btnStep4Action, allGranted)

        // Show finish button when all done
        if (shizukuRunning && allGranted) {
            binding.btnFinish.visibility = View.VISIBLE
            binding.tvFinalStatus.visibility = View.VISIBLE
            binding.tvFinalStatus.text = "✅ Tất cả đã sẵn sàng! Bạn có thể bắt đầu sử dụng Macro Gaming."
            binding.tvFinalStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_play))
        } else {
            binding.btnFinish.visibility = View.GONE
            binding.tvFinalStatus.visibility = View.GONE
        }
    }

    private fun updateStepStatus(statusIcon: View, actionButton: View, isComplete: Boolean) {
        if (isComplete) {
            (statusIcon as? android.widget.ImageView)?.setImageResource(R.drawable.ic_check)
            (statusIcon as? android.widget.ImageView)?.setColorFilter(
                ContextCompat.getColor(this, R.color.emerald_play)
            )
            actionButton.alpha = 0.5f
        } else {
            (statusIcon as? android.widget.ImageView)?.setImageResource(R.drawable.ic_warning)
            (statusIcon as? android.widget.ImageView)?.setColorFilter(
                ContextCompat.getColor(this, R.color.amber_warning)
            )
            actionButton.alpha = 1.0f
        }
    }

    private fun runAutoGrant() {
        binding.btnStep4Action.isEnabled = false
        binding.btnStep4Action.text = "Đang cấp quyền..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = ShizukuHelper.grantAllPermissionsViaShizuku(this@SetupWizardActivity)
            binding.progressBar.visibility = View.GONE
            binding.btnStep4Action.isEnabled = true
            binding.btnStep4Action.text = "⚡ Cấp TẤT CẢ quyền"

            result.onSuccess {
                Toast.makeText(this@SetupWizardActivity, "Đã cấp toàn bộ quyền thành công!", Toast.LENGTH_LONG).show()
                delay(300)
                refreshStepStatuses()
            }.onFailure { err ->
                Toast.makeText(this@SetupWizardActivity, "Lỗi: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
