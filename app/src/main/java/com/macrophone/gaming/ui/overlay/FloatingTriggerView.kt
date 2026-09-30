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
import androidx.core.content.ContextCompat
import com.macrophone.gaming.R
import kotlin.math.abs

/**
 * Nút Tròn Kích Hoạt Combo Nổi Trên Màn Hình Game (Floating Trigger Button):
 * - Kéo thả tự do đến mọi vị trí thuận ngón tay (gần nút đánh thường hoặc góc phải).
 * - CHẠM VÀO NÚT: Bắn ngay chuỗi combo chiêu đã gán với độ trễ siêu thấp 0ms!
 * - NHẤN GIỮ LÂU: Bật/tắt nút xóa (✕) để dọn dẹp khi không muốn dùng nữa.
 */
class FloatingTriggerView(
    private val context: Context,
    private val windowManager: WindowManager,
    val name: String,
    val points: List<Pair<Float, Float>>,
    initialX: Int,
    initialY: Int,
    private val onTrigger: (List<Pair<Float, Float>>) -> Unit,
    private val onDelete: (FloatingTriggerView) -> Unit
) {

    val view: View = LayoutInflater.from(
        androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_MacroGaming)
    ).inflate(R.layout.view_floating_macro_button, null)
    private val tvLabel: TextView = view.findViewById(R.id.tvMacroBtnLabel)
    private val ivIcon: ImageView = view.findViewById(R.id.ivMacroBtnIcon)
    private val ivDelete: ImageView = view.findViewById(R.id.ivMacroBtnDelete)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = initialX
        y = initialY
    }

    init {
        tvLabel.text = name.take(4)
        setupTouchListener()

        ivDelete.setOnClickListener {
            destroy()
            onDelete(this)
        }

        try {
            windowManager.addView(view, params)
        } catch (_: Throwable) {}
    }

    private fun setupTouchListener() {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchStartX = 0f
            private var touchStartY = 0f
            private var isDragging = false
            private var downTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                        isDragging = false
                        downTime = System.currentTimeMillis()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchStartX).toInt()
                        val dy = (event.rawY - touchStartY).toInt()

                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDragging = true
                            params.x = startX + dx
                            params.y = startY + dy
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
                        if (!isDragging && duration < 400) {
                            // Chạm nhanh -> KÍCH HOẠT COMBO!
                            flashFeedback()
                            onTrigger(points)
                        } else if (!isDragging && duration >= 500) {
                            // Nhấn giữ lâu -> Hiển thị nút xóa
                            vibrate(80)
                            ivDelete.visibility = if (ivDelete.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        }
                        return true
                    }
                }
                return false
            }
        })
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
        try {
            if (view.isAttachedToWindow) {
                windowManager.removeViewImmediate(view)
            } else {
                windowManager.removeView(view)
            }
        } catch (_: Throwable) {}
    }
}
