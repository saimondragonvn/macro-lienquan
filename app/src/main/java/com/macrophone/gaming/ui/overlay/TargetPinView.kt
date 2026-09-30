package com.macrophone.gaming.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.macrophone.gaming.R
import kotlin.math.abs

/**
 * Điểm Ghim Mục Tiêu Nổi (Target Pin Marker) - Phong cách Game Turbo:
 * - Kích thước nhỏ gọn 44x44dp, không hề chiếm diện tích hay cản trở thao tác chơi game.
 * - Cho phép kéo thả trực tiếp lên các nút chiêu của tướng (Chiêu 1, Chiêu 2, Chiêu 3, Đánh thường).
 * - Cung cấp tọa độ tâm điểm chính xác để bắn lệnh click shell.
 */
class TargetPinView(
    private val context: Context,
    private val windowManager: WindowManager,
    var index: Int,
    initialX: Int,
    initialY: Int,
    private val onPinClicked: ((TargetPinView) -> Unit)? = null,
    private val onPinRemoved: ((TargetPinView) -> Unit)? = null
) {

    val view: View = LayoutInflater.from(
        androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_MacroGaming)
    ).inflate(R.layout.view_target_point, null)
    private val tvIndex: TextView = view.findViewById(R.id.tvPointIndex)

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
        tvIndex.text = index.toString()
        setupTouchListener()

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

                        if (abs(dx) > 6 || abs(dy) > 6) {
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
                        if (!isDragging && System.currentTimeMillis() - downTime < 350) {
                            onPinClicked?.invoke(this@TargetPinView)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    fun updateIndex(newIndex: Int) {
        index = newIndex
        tvIndex.text = index.toString()
    }

    /**
     * Tọa độ tâm điểm chính xác (X, Y) trên màn hình game
     */
    fun getCenterCoordinates(): Pair<Float, Float> {
        val w = if (view.width > 0) view.width else 100
        val h = if (view.height > 0) view.height else 100
        val cx = params.x.toFloat() + (w / 2f)
        val cy = params.y.toFloat() + (h / 2f)
        return Pair(cx, cy)
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
