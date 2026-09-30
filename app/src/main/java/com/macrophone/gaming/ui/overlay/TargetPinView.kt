package com.macrophone.gaming.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
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
        tvIndex.text = index.toString()
        (view as? android.view.ViewGroup)?.isMotionEventSplittingEnabled = true
        setupTouchListener()

        try {
            windowManager.addView(view, params)
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
                        val dragThreshold = 6 * context.resources.displayMetrics.density

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
                        if (!isDragging && System.currentTimeMillis() - downTime < 350) {
                            onPinClicked?.invoke(this@TargetPinView)
                        }
                        isDragging = false
                        return true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
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
     * Tọa độ tâm điểm chính xác (X, Y) trên màn hình game (chuẩn tuyệt đối cả khi xoay ngang/có tai thỏ)
     */
    fun getCenterCoordinates(): Pair<Float, Float> {
        val density = context.resources.displayMetrics.density
        val defaultSize = (44 * density).toInt()
        val w = if (view.width > 0) view.width else defaultSize
        val h = if (view.height > 0) view.height else defaultSize
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val cx = if (view.isAttachedToWindow && (loc[0] > 0 || loc[1] > 0)) {
            loc[0].toFloat() + (w / 2f)
        } else {
            params.x.toFloat() + (w / 2f)
        }
        val cy = if (view.isAttachedToWindow && (loc[0] > 0 || loc[1] > 0)) {
            loc[1].toFloat() + (h / 2f)
        } else {
            params.y.toFloat() + (h / 2f)
        }
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
