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
import com.macrophone.gaming.data.model.MacroSequence
import kotlin.math.abs

/**
 * Quản lý một Nút Macro Nổi độc lập trên màn hình game (Floating Macro Trigger Button):
 * - Người chơi kéo thả nút này đến vị trí thuận tay nhất (ví dụ: ngay cạnh nút đánh thường).
 * - Khi CHẠM vào nút: Lập tức phát lại toàn bộ thao tác đã ghi với tốc độ siêu nhanh (2x, 5x, 10x).
 * - Cờ FLAG_NOT_TOUCH_MODAL đảm bảo các ngón tay khác (di chuyển Joystick) hoàn toàn tự do.
 */
class FloatingMacroButton(
    private val context: Context,
    private val windowManager: WindowManager,
    val sequence: MacroSequence,
    private val onTrigger: (MacroSequence) -> Unit,
    private val onStop: () -> Unit,
    private val onDelete: (MacroSequence) -> Unit
) {

    val view: View = LayoutInflater.from(context).inflate(R.layout.view_floating_macro_button, null)
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
        x = sequence.buttonX
        y = sequence.buttonY
    }

    var isPlaying: Boolean = false
        private set

    init {
        // Đặt nhãn ngắn gọn cho nút (tối đa 4-5 ký tự)
        val shortName = if (sequence.name.length > 5) {
            sequence.name.take(4) + ".."
        } else {
            sequence.name
        }
        tvLabel.text = shortName

        setupTouchAndDrag()

        ivDelete.setOnClickListener {
            destroy()
            onDelete(sequence)
        }

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun setupTouchAndDrag() {
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

                        if (abs(dx) > 12 || abs(dy) > 12) {
                            isDragging = true
                            params.x = startX + dx
                            params.y = startY + dy
                            sequence.buttonX = params.x
                            sequence.buttonY = params.y
                            if (view.isAttachedToWindow) {
                                try {
                                    windowManager.updateViewLayout(view, params)
                                } catch (_: Exception) {}
                            }
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - downTime
                        if (!isDragging && duration < 500) {
                            // Người dùng CLICK vào nút Macro để BẬT hoặc DỪNG phát lại!
                            vibrateClick(35)
                            if (isPlaying) {
                                setPlayingState(false)
                                onStop()
                            } else {
                                setPlayingState(true)
                                onTrigger(sequence)
                            }
                        } else if (!isDragging && duration >= 500) {
                            // Nhấn giữ lâu để bật/tắt nút xóa
                            vibrateClick(80)
                            ivDelete.visibility = if (ivDelete.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    /**
     * Cập nhật trạng thái hiển thị của nút khi đang phát hoặc đang chờ
     */
    fun setPlayingState(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            view.setBackgroundResource(R.drawable.bg_floating_macro_btn_active)
            ivIcon.setImageResource(R.drawable.ic_stop)
            ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))
            tvLabel.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            view.setBackgroundResource(R.drawable.bg_floating_macro_btn_idle)
            ivIcon.setImageResource(R.drawable.ic_speed)
            ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.cyan_neon))
            tvLabel.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
    }

    private fun vibrateClick(ms: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    fun setVisible(visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun destroy() {
        if (view.isAttachedToWindow) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
    }
}
