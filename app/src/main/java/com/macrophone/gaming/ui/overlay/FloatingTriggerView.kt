package com.macrophone.gaming.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.macrophone.gaming.R
import kotlin.math.abs

/**
 * Nút Tròn Kích Hoạt Combo Nổi Trên Màn Hình Game (Floating Trigger Button):
 * - Kéo thả tự do đến mọi vị trí thuận ngón tay (gần nút đánh thường hoặc góc phải).
 * - CHẠM VÀO NÚT: Bắn ngay chuỗi combo chiêu đã gán với độ trễ siêu thấp 0ms!
 * - NHẤN GIỮ LÂU: Mở Menu Cài Đặt Combo (Tốc độ delay, Lặp lại, Hiện lại ghim trên game, Xóa).
 */
import com.macrophone.gaming.data.model.MacroAction

class FloatingTriggerView(
    private val context: Context,
    private val windowManager: WindowManager,
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    val points: List<Pair<Float, Float>> = emptyList(),
    val actions: List<MacroAction> = emptyList(),
    initialX: Int,
    initialY: Int,
    var delayBetweenMs: Long = 40,
    var repeatCount: Int = 1,
    var opacityPercent: Int = 85,
    var speedMultiplier: Float = 1.0f,
    private val onTrigger: (FloatingTriggerView) -> Unit,
    private val onRestorePins: ((List<Pair<Float, Float>>) -> Unit)? = null,
    var onPositionChanged: ((FloatingTriggerView) -> Unit)? = null,
    var onConfigChanged: ((FloatingTriggerView) -> Unit)? = null,
    private val onDelete: (FloatingTriggerView) -> Unit
) {

    val view: View = LayoutInflater.from(
        androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_MacroGaming)
    ).inflate(R.layout.view_floating_macro_button, null)
    private val tvLabel: TextView = view.findViewById(R.id.tvMacroBtnLabel)
    private val ivIcon: ImageView = view.findViewById(R.id.ivMacroBtnIcon)
    private val ivDelete: ImageView = view.findViewById(R.id.ivMacroBtnDelete)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private var configDialogView: View? = null

    val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = initialX
        y = initialY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    init {
        tvLabel.text = name.take(4)
        updateOpacity(opacityPercent)
        (view as? android.view.ViewGroup)?.isMotionEventSplittingEnabled = true
        setupTouchListener()

        ivDelete.setOnClickListener {
            destroy()
            onDelete(this)
        }

        try {
            windowManager.addView(view, params)
        } catch (_: Throwable) {}
    }

    fun updateOpacity(percent: Int) {
        opacityPercent = percent.coerceIn(20, 100)
        view.alpha = opacityPercent / 100f
    }

    fun setVisible(visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun rename(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            name = trimmed
            tvLabel.text = name.take(4)
            onConfigChanged?.invoke(this)
        }
    }

    fun showRenameDialog() {
        try {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_MacroGaming)
            val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.view_rename_dialog, null)

            val dialogParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val etInput = dialogView.findViewById<android.widget.EditText>(R.id.etRenameInput)
            val btnConfirm = dialogView.findViewById<TextView>(R.id.btnRenameConfirm)
            val btnCancel = dialogView.findViewById<TextView>(R.id.btnRenameCancel)

            etInput?.setText(name)
            etInput?.selectAll()

            val closeRenameDialog = {
                try {
                    if (dialogView.isAttachedToWindow) {
                        windowManager.removeViewImmediate(dialogView)
                    } else {
                        windowManager.removeView(dialogView)
                    }
                } catch (_: Throwable) {}
            }

            btnConfirm?.setOnClickListener {
                val newName = etInput?.text?.toString()?.trim() ?: ""
                if (newName.isNotEmpty()) {
                    rename(newName)
                    Toast.makeText(context, "✅ Đã đổi tên thành: $name", Toast.LENGTH_SHORT).show()
                }
                closeRenameDialog()
            }

            btnCancel?.setOnClickListener {
                closeRenameDialog()
            }

            windowManager.addView(dialogView, dialogParams)
        } catch (_: Throwable) {}
    }

    private fun setupTouchListener() {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private var downTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        downTime = System.currentTimeMillis()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        val dragThreshold = 8 * context.resources.displayMetrics.density

                        if (!isDragging && (abs(dx) > dragThreshold || abs(dy) > dragThreshold)) {
                            isDragging = true
                        }

                        if (isDragging) {
                            params.x = (initialX + dx).toInt()
                            params.y = (initialY + dy).toInt()
                            if (view.isAttachedToWindow) {
                                try {
                                    windowManager.updateViewLayout(view, params)
                                } catch (_: Throwable) {}
                            }
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - downTime
                        if (isDragging) {
                            // 3. Kéo thả di chuyển vị trí nút -> Lưu vị trí ngay lập tức!
                            onPositionChanged?.invoke(this@FloatingTriggerView)
                        } else if (duration < 350) {
                            // 1. Chạm nhanh -> KÍCH HOẠT COMBO VÀO GAME NGAY TỨC THÌ!
                            flashFeedback()
                            onTrigger(this@FloatingTriggerView)
                        } else if (duration >= 450) {
                            // 2. Nhấn giữ lâu mà không kéo -> Mở Menu Cài Đặt Combo!
                            vibrate(60)
                            showConfigDialog()
                        }
                        isDragging = false
                        return true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            onPositionChanged?.invoke(this@FloatingTriggerView)
                        }
                        isDragging = false
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun showConfigDialog() {
        if (configDialogView != null) return

        try {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_MacroGaming)
            configDialogView = LayoutInflater.from(themedContext).inflate(R.layout.view_trigger_config_dialog, null)

            val dialogParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val tvDialogComboName = configDialogView!!.findViewById<TextView>(R.id.tvDialogComboName)
            val tvDialogPointsSummary = configDialogView!!.findViewById<TextView>(R.id.tvDialogPointsSummary)
            val btnDialogClose = configDialogView!!.findViewById<TextView>(R.id.btnDialogClose)

            val btnOptDelayFast = configDialogView!!.findViewById<TextView>(R.id.btnOptDelayFast)
            val btnOptDelayNormal = configDialogView!!.findViewById<TextView>(R.id.btnOptDelayNormal)
            val btnOptDelaySlow = configDialogView!!.findViewById<TextView>(R.id.btnOptDelaySlow)

            val btnRepeat1 = configDialogView!!.findViewById<TextView>(R.id.btnRepeat1)
            val btnRepeat2 = configDialogView!!.findViewById<TextView>(R.id.btnRepeat2)
            val btnRepeat3 = configDialogView!!.findViewById<TextView>(R.id.btnRepeat3)

            val btnOpacity30 = configDialogView!!.findViewById<TextView>(R.id.btnOpacity30)
            val btnOpacity50 = configDialogView!!.findViewById<TextView>(R.id.btnOpacity50)
            val btnOpacity80 = configDialogView!!.findViewById<TextView>(R.id.btnOpacity80)
            val btnOpacity100 = configDialogView!!.findViewById<TextView>(R.id.btnOpacity100)

            val btnDialogTestRun = configDialogView!!.findViewById<TextView>(R.id.btnDialogTestRun)
            val btnDialogRestorePins = configDialogView!!.findViewById<TextView>(R.id.btnDialogRestorePins)
            val btnDialogDelete = configDialogView!!.findViewById<TextView>(R.id.btnDialogDelete)

            tvDialogComboName?.text = "CÀI ĐẶT COMBO [$name]"
            tvDialogPointsSummary?.text = if (actions.isNotEmpty()) {
                "${actions.size} thao tác thực tế • Game Turbo 120Hz"
            } else {
                "${points.size} chiêu đã gán • Tọa độ chuẩn Liên Quân"
            }

            val tvDialogSpeedBadge = configDialogView!!.findViewById<TextView>(R.id.tvDialogSpeedBadge)
            val etDialogSpeedMultiplier = configDialogView!!.findViewById<android.widget.EditText>(R.id.etDialogSpeedMultiplier)
            val btnConfigSpeed1 = configDialogView!!.findViewById<TextView>(R.id.btnConfigSpeed1)
            val btnConfigSpeed2 = configDialogView!!.findViewById<TextView>(R.id.btnConfigSpeed2)
            val btnConfigSpeed5 = configDialogView!!.findViewById<TextView>(R.id.btnConfigSpeed5)
            val btnConfigSpeed10 = configDialogView!!.findViewById<TextView>(R.id.btnConfigSpeed10)
            val btnConfigSpeed20 = configDialogView!!.findViewById<TextView>(R.id.btnConfigSpeed20)

            val updateSpeedMultiplierUI = {
                val formatted = if (speedMultiplier % 1f == 0f) "${speedMultiplier.toInt()}" else "$speedMultiplier"
                tvDialogSpeedBadge?.text = "x$formatted"

                val is1 = speedMultiplier == 1.0f
                val is2 = speedMultiplier == 2.0f
                val is5 = speedMultiplier == 5.0f
                val is10 = speedMultiplier == 10.0f
                val is20 = speedMultiplier == 20.0f

                btnConfigSpeed1?.setBackgroundResource(if (is1) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnConfigSpeed1?.setTextColor(ContextCompat.getColor(context, if (is1) R.color.bg_dark else R.color.text_primary))

                btnConfigSpeed2?.setBackgroundResource(if (is2) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnConfigSpeed2?.setTextColor(ContextCompat.getColor(context, if (is2) R.color.bg_dark else R.color.text_primary))

                btnConfigSpeed5?.setBackgroundResource(if (is5) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnConfigSpeed5?.setTextColor(ContextCompat.getColor(context, if (is5) R.color.bg_dark else R.color.text_primary))

                btnConfigSpeed10?.setBackgroundResource(if (is10) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnConfigSpeed10?.setTextColor(ContextCompat.getColor(context, if (is10) R.color.bg_dark else R.color.text_primary))

                btnConfigSpeed20?.setBackgroundResource(if (is20) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnConfigSpeed20?.setTextColor(ContextCompat.getColor(context, if (is20) R.color.bg_dark else R.color.text_primary))
            }

            val initialSpeedStr = if (speedMultiplier % 1f == 0f) "${speedMultiplier.toInt()}" else "$speedMultiplier"
            etDialogSpeedMultiplier?.setText(initialSpeedStr)
            updateSpeedMultiplierUI()

            etDialogSpeedMultiplier?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val num = s?.toString()?.toFloatOrNull()
                    if (num != null && num > 0) {
                        speedMultiplier = num.coerceIn(0.5f, 20.0f)
                        val formatted = if (speedMultiplier % 1f == 0f) "${speedMultiplier.toInt()}" else "$speedMultiplier"
                        tvDialogSpeedBadge?.text = "x$formatted"
                        onConfigChanged?.invoke(this@FloatingTriggerView)
                    }
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            btnConfigSpeed1?.setOnClickListener {
                speedMultiplier = 1.0f
                etDialogSpeedMultiplier?.setText("1")
                updateSpeedMultiplierUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnConfigSpeed2?.setOnClickListener {
                speedMultiplier = 2.0f
                etDialogSpeedMultiplier?.setText("2")
                updateSpeedMultiplierUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnConfigSpeed5?.setOnClickListener {
                speedMultiplier = 5.0f
                etDialogSpeedMultiplier?.setText("5")
                updateSpeedMultiplierUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnConfigSpeed10?.setOnClickListener {
                speedMultiplier = 10.0f
                etDialogSpeedMultiplier?.setText("10")
                updateSpeedMultiplierUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnConfigSpeed20?.setOnClickListener {
                speedMultiplier = 20.0f
                etDialogSpeedMultiplier?.setText("20")
                updateSpeedMultiplierUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }

            val updateDelayUI = {
                btnOptDelayFast?.setBackgroundResource(if (delayBetweenMs <= 35) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnOptDelayFast?.setTextColor(ContextCompat.getColor(context, if (delayBetweenMs <= 35) R.color.bg_dark else R.color.text_primary))

                btnOptDelayNormal?.setBackgroundResource(if (delayBetweenMs in 36..80) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnOptDelayNormal?.setTextColor(ContextCompat.getColor(context, if (delayBetweenMs in 36..80) R.color.bg_dark else R.color.text_primary))

                btnOptDelaySlow?.setBackgroundResource(if (delayBetweenMs > 80) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnOptDelaySlow?.setTextColor(ContextCompat.getColor(context, if (delayBetweenMs > 80) R.color.bg_dark else R.color.text_primary))
            }

            val updateRepeatUI = {
                btnRepeat1?.setBackgroundResource(if (repeatCount == 1) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnRepeat1?.setTextColor(ContextCompat.getColor(context, if (repeatCount == 1) R.color.bg_dark else R.color.text_primary))

                btnRepeat2?.setBackgroundResource(if (repeatCount == 2) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnRepeat2?.setTextColor(ContextCompat.getColor(context, if (repeatCount == 2) R.color.bg_dark else R.color.text_primary))

                btnRepeat3?.setBackgroundResource(if (repeatCount >= 3) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnRepeat3?.setTextColor(ContextCompat.getColor(context, if (repeatCount >= 3) R.color.bg_dark else R.color.text_primary))
            }

            val updateOpacityUI = {
                btnOpacity30?.setBackgroundResource(if (opacityPercent <= 35) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnOpacity30?.setTextColor(ContextCompat.getColor(context, if (opacityPercent <= 35) R.color.bg_dark else R.color.text_primary))

                btnOpacity50?.setBackgroundResource(if (opacityPercent in 36..65) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnOpacity50?.setTextColor(ContextCompat.getColor(context, if (opacityPercent in 36..65) R.color.bg_dark else R.color.text_primary))

                btnOpacity80?.setBackgroundResource(if (opacityPercent in 66..85) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnOpacity80?.setTextColor(ContextCompat.getColor(context, if (opacityPercent in 66..85) R.color.bg_dark else R.color.text_primary))

                btnOpacity100?.setBackgroundResource(if (opacityPercent > 85) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnOpacity100?.setTextColor(ContextCompat.getColor(context, if (opacityPercent > 85) R.color.bg_dark else R.color.text_primary))
            }

            updateDelayUI()
            updateRepeatUI()
            updateOpacityUI()

            btnOptDelayFast?.setOnClickListener {
                delayBetweenMs = 30
                speedMultiplier = 2.0f
                updateDelayUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnOptDelayNormal?.setOnClickListener {
                delayBetweenMs = 60
                speedMultiplier = 1.0f
                updateDelayUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnOptDelaySlow?.setOnClickListener {
                delayBetweenMs = 120
                speedMultiplier = 0.8f
                updateDelayUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }

            btnRepeat1?.setOnClickListener {
                repeatCount = 1
                updateRepeatUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnRepeat2?.setOnClickListener {
                repeatCount = 2
                updateRepeatUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnRepeat3?.setOnClickListener {
                repeatCount = 3
                updateRepeatUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }

            btnOpacity30?.setOnClickListener {
                updateOpacity(30)
                updateOpacityUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnOpacity50?.setOnClickListener {
                updateOpacity(50)
                updateOpacityUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnOpacity80?.setOnClickListener {
                updateOpacity(80)
                updateOpacityUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }
            btnOpacity100?.setOnClickListener {
                updateOpacity(100)
                updateOpacityUI()
                onConfigChanged?.invoke(this@FloatingTriggerView)
            }

            val btnDialogRename = configDialogView!!.findViewById<TextView>(R.id.btnDialogRename)
            btnDialogRename?.setOnClickListener {
                dismissConfigDialog()
                showRenameDialog()
            }

            btnDialogTestRun?.setOnClickListener {
                flashFeedback()
                onTrigger(this@FloatingTriggerView)
                Toast.makeText(context, "▶ Đang bắn combo vào game!", Toast.LENGTH_SHORT).show()
            }

            if (actions.isNotEmpty() && points.isEmpty()) {
                btnDialogRestorePins?.visibility = View.GONE
            } else {
                btnDialogRestorePins?.visibility = View.VISIBLE
                btnDialogRestorePins?.setOnClickListener {
                    dismissConfigDialog()
                    destroy()
                    onRestorePins?.invoke(points)
                    Toast.makeText(context, "🎯 Đã hiện lại các điểm ghim trên game! Bạn hãy kéo chỉnh vị trí chiêu theo ý muốn.", Toast.LENGTH_LONG).show()
                }
            }

            btnDialogDelete?.setOnClickListener {
                dismissConfigDialog()
                destroy()
                onDelete(this)
                Toast.makeText(context, "Đã xóa nút combo [$name]", Toast.LENGTH_SHORT).show()
            }

            btnDialogClose?.setOnClickListener {
                dismissConfigDialog()
            }

            windowManager.addView(configDialogView, dialogParams)
        } catch (e: Throwable) {
            dismissConfigDialog()
        }
    }

    private fun dismissConfigDialog() {
        if (configDialogView != null) {
            try {
                if (configDialogView!!.isAttachedToWindow) {
                    windowManager.removeViewImmediate(configDialogView)
                } else {
                    windowManager.removeView(configDialogView)
                }
            } catch (_: Throwable) {}
            configDialogView = null
        }
    }

    private fun flashFeedback() {
        vibrate(30)
        view.setBackgroundResource(R.drawable.bg_floating_macro_btn_active)
        ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))
        view.postDelayed({
            if (view.isAttachedToWindow) {
                view.setBackgroundResource(R.drawable.bg_floating_macro_btn_idle)
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.cyan_neon))
            }
        }, 200)
    }

    private fun vibrate(ms: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Throwable) {}
    }

    fun destroy() {
        dismissConfigDialog()
        try {
            if (view.isAttachedToWindow) {
                windowManager.removeViewImmediate(view)
            } else {
                windowManager.removeView(view)
            }
        } catch (_: Throwable) {}
    }
}
