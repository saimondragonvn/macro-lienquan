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

/**
 * Quản lý một Điểm Ghim Mục Tiêu nổi (Floating Target Marker) độc lập trên màn hình.
 * - Chỉ chiếm diện tích 44x44dp, 100% diện tích còn lại của màn hình hoàn toàn KHÔNG BỊ CHẶN cảm ứng!
 * - Cho phép kéo thả tự do đặt lên các nút chiêu, nút bắn trong game.
 * - FLAG_NOT_TOUCH_MODAL giúp các thao tác chạm khác (joystick, xoay góc nhìn) hoạt động bình thường.
 */
class TargetPointMarker(
    private val context: Context,
    private val windowManager: WindowManager,
    var index: Int,
    initialX: Int,
    initialY: Int
) {

    val view: View = LayoutInflater.from(context).inflate(R.layout.view_target_point, null)
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

    var isVisible: Boolean = true
        private set

    init {
        tvIndex.text = index.toString()
        setupDragListener()
        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun setupDragListener() {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchStartX = 0f
            private var touchStartY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchStartX).toInt()
                        val dy = (event.rawY - touchStartY).toInt()
                        params.x = startX + dx
                        params.y = startY + dy
                        if (view.isAttachedToWindow) {
                            try {
                                windowManager.updateViewLayout(view, params)
                            } catch (_: Exception) {}
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
     * Tọa độ tâm điểm $(X, Y)$ để click chính xác qua shell đặc quyền
     */
    fun getCenterCoordinates(): Pair<Float, Float> {
        val width = if (view.width > 0) view.width else 120
        val height = if (view.height > 0) view.height else 120
        val cx = params.x.toFloat() + (width / 2f)
        val cy = params.y.toFloat() + (height / 2f)
        return Pair(cx, cy)
    }

    fun setVisible(visible: Boolean) {
        isVisible = visible
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun destroy() {
        try {
            if (view.isAttachedToWindow) {
                windowManager.removeViewImmediate(view)
            } else {
                windowManager.removeView(view)
            }
        } catch (_: Exception) {}
    }
}
