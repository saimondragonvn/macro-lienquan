package com.macrophone.gaming.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
 * - Hướng dẫn từng bước rõ ràng, trực quan, tự động kiểm tra trạng thái.
 * - Tương thích hoàn hảo cả trên điện thoại thật (Shizuku) lẫn trình giả lập PC (LDPlayer, Nox, BlueStacks qua Root).
 * - Sử dụng Shizuku Sticky Listener an toàn, không bao giờ gây crash ("Ứng dụng đã dừng").
 */
class SetupWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupWizardBinding
    private val TAG = "SetupWizardActivity"

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            refreshStepStatuses()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            refreshStepStatuses()
        }
    }

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == ShizukuHelper.SHIZUKU_REQUEST_CODE) {
            runOnUiThread {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@SetupWizardActivity, "Đã cấp quyền Shizuku thành công!", Toast.LENGTH_SHORT).show()
                    runAutoGrant()
                } else {
                    Toast.makeText(this@SetupWizardActivity, "Bạn đã từ chối quyền Shizuku!", Toast.LENGTH_SHORT).show()
                }
                refreshStepStatuses()
            }
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
        registerShizukuListenersSafely()
    }

    override fun onResume() {
        super.onResume()
        refreshStepStatuses()
    }

    override fun onDestroy() {
        unregisterShizukuListenersSafely()
        super.onDestroy()
    }

    private fun registerShizukuListenersSafely() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            Log.w(TAG, "Shizuku listener registration: ${e.message}")
        }
    }

    private fun unregisterShizukuListenersSafely() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Throwable) {}
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

        // Bước 4: Cấp tất cả quyền (hỗ trợ cả Shizuku lẫn Root giả lập)
        binding.btnStep4Action.setOnClickListener {
            val isRoot = ShizukuHelper.isRootAvailable()
            val isShizukuRunning = ShizukuHelper.isShizukuRunning()

            if (isRoot) {
                // Trên máy ảo giả lập PC (LDPlayer, BlueStacks, Nox) đã có Root
                runAutoGrant()
                return@setOnClickListener
            }

            if (!isShizukuRunning) {
                Toast.makeText(this, "Shizuku chưa chạy! Hãy hoàn thành bước 3 hoặc bật Root trên giả lập.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!ShizukuHelper.hasShizukuPermission()) {
                val requested = ShizukuHelper.requestShizukuPermission(this)
                if (!requested) {
                    Toast.makeText(this, "Không thể mở hộp thoại xin quyền Shizuku. Hãy thử mở lại Shizuku.", Toast.LENGTH_SHORT).show()
                }
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
        val hasRoot = ShizukuHelper.isRootAvailable()
        val shizukuRunning = ShizukuHelper.isShizukuRunning()
        val shizukuInstalled = ShizukuHelper.isShizukuInstalled(this)

        // Step 1: Shizuku installed (hoặc máy đã root)
        updateStepStatus(binding.ivStep1Status, binding.btnStep1Action, shizukuInstalled || hasRoot)

        // Step 2 & Step 3: Service available
        updateStepStatus(binding.ivStep2Status, binding.btnStep2Action, shizukuRunning || hasRoot)
        updateStepStatus(binding.ivStep3Status, binding.btnStep3Action, shizukuRunning || hasRoot)

        // Step 4: All system permissions granted
        val allGranted = PermissionUtils.hasOverlayPermission(this) &&
                PermissionUtils.isIgnoringBatteryOptimizations(this)
        updateStepStatus(binding.ivStep4Status, binding.btnStep4Action, allGranted)

        // Hiển thị trạng thái hoàn tất
        if ((shizukuRunning || hasRoot) && allGranted) {
            binding.btnFinish.visibility = View.VISIBLE
            binding.tvFinalStatus.visibility = View.VISIBLE
            val mode = if (hasRoot) "Root Giả Lập" else "Shizuku"
            binding.tvFinalStatus.text = "✅ Đã sẵn sàng qua $mode! Bạn có thể bắt đầu sử dụng Macro Gaming."
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
            actionButton.alpha = 0.6f
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
        binding.btnStep4Action.text = "Đang tự động cấp quyền..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = ShizukuHelper.grantAllPermissionsViaShizuku(this@SetupWizardActivity)
            binding.progressBar.visibility = View.GONE
            binding.btnStep4Action.isEnabled = true
            binding.btnStep4Action.text = "⚡ Cấp TẤT CẢ quyền"

            result.onSuccess { msg ->
                Toast.makeText(this@SetupWizardActivity, msg, Toast.LENGTH_LONG).show()
                delay(300)
                refreshStepStatuses()
            }.onFailure { err ->
                Toast.makeText(this@SetupWizardActivity, "Thông báo: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
