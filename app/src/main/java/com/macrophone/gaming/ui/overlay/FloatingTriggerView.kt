package com.macrophone.gaming.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
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
    val name: String,
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
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = initialX
        y = initialY
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

    private fun setupTouchListener() {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchStartX = 0f
            private var touchStartY = 0f
            private var isDragging = false
            private var downTime = 0L
            private var activePointerId = MotionEvent.INVALID_POINTER_ID

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        val actionIndex = event.actionIndex
                        activePointerId = event.getPointerId(actionIndex)
                        startX = params.x
                        startY = params.y
                        touchStartX = event.getX(actionIndex) + params.x
                        touchStartY = event.getY(actionIndex) + params.y
                        isDragging = false
                        downTime = System.currentTimeMillis()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val pointerIndex = if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                            event.findPointerIndex(activePointerId)
                        } else {
                            0
                        }
                        if (pointerIndex in 0 until event.pointerCount) {
                            val curX = event.getX(pointerIndex) + params.x
                            val curY = event.getY(pointerIndex) + params.y
                            val dx = (curX - touchStartX).toInt()
                            val dy = (curY - touchStartY).toInt()
                            val dragThreshold = (18 * context.resources.displayMetrics.density).toInt()

                            if (abs(dx) > dragThreshold || abs(dy) > dragThreshold) {
                                isDragging = true
                                params.x = startX + dx
                                params.y = startY + dy
                                if (view.isAttachedToWindow) {
                                    try {
                                        windowManager.updateViewLayout(view, params)
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        val actionIndex = event.actionIndex
                        val pointerId = event.getPointerId(actionIndex)
                        if (activePointerId == MotionEvent.INVALID_POINTER_ID || pointerId == activePointerId) {
                            val duration = System.currentTimeMillis() - downTime
                            if (!isDragging && duration < 350) {
                                // 1. Chạm nhanh -> KÍCH HOẠT COMBO VÀO GAME NGAY TỨC THÌ!
                                flashFeedback()
                                onTrigger(this@FloatingTriggerView)
                            } else if (duration >= 450 && !isDragging) {
                                // 2. Nhấn giữ lâu mà không kéo -> Mở Menu Cài Đặt Combo!
                                vibrate(60)
                                showConfigDialog()
                            } else if (isDragging) {
                                // 3. Kéo thả di chuyển vị trí nút
                                onPositionChanged?.invoke(this@FloatingTriggerView)
                            }
                            activePointerId = MotionEvent.INVALID_POINTER_ID
                        }
                        return true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            onPositionChanged?.invoke(this@FloatingTriggerView)
                        }
                        isDragging = false
                        activePointerId = MotionEvent.INVALID_POINTER_ID
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
