package com.macrophone.gaming.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.macrophone.gaming.data.model.GesturePoint
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroType
import kotlin.math.hypot
import kotlin.math.max

/**
 * Custom View vẽ phản hồi cảm ứng và thu thập các điểm cử chỉ (down, move, up)
 * để chuyển đổi thành các MacroAction chính xác.
 */
class TouchRecorderCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val touchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCEF4444")
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8006B6D4")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val currentTrailPath = Path()
    private val currentPoints = mutableListOf<GesturePoint>()
    private var actionDownTime = 0L
    private var lastActionUpTime = 0L

    private var currentX = 0f
    private var currentY = 0f
    private var isTouching = false

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    var onActionRecorded: ((MacroAction) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val now = System.currentTimeMillis()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                currentX = x
                currentY = y
                actionDownTime = now
                currentPoints.clear()
                currentTrailPath.reset()
                currentTrailPath.moveTo(x, y)
                currentPoints.add(GesturePoint(x, y, now))

                // Rung nhẹ phản hồi khi bắt đầu chạm
                vibrateFeedback(30)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTouching) {
                    currentX = x
                    currentY = y
                    currentTrailPath.lineTo(x, y)
                    currentPoints.add(GesturePoint(x, y, now))
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTouching) {
                    isTouching = false
                    val duration = max(30L, now - actionDownTime)

                    // Tính khoảng cách thời gian chờ từ lần thả tay trước
                    val delayBefore = if (lastActionUpTime > 0L) {
                        max(20L, actionDownTime - lastActionUpTime)
                    } else {
                        50L
                    }
                    lastActionUpTime = now

                    // Xác định khoảng cách di chuyển tổng thể
                    val first = currentPoints.firstOrNull() ?: GesturePoint(x, y, actionDownTime)
                    val totalDist = hypot((x - first.x).toDouble(), (y - first.y).toDouble())

                    val actionType = when {
                        totalDist > 25.0 -> MacroType.SWIPE
                        duration > 350L -> MacroType.HOLD
                        else -> MacroType.TAP
                    }

                    val pointsToSave = if (actionType == MacroType.SWIPE) {
                        ArrayList(currentPoints)
                    } else {
                        listOf(first)
                    }

                    val macroAction = MacroAction(
                        type = actionType,
                        points = pointsToSave,
                        durationMs = duration,
                        delayBeforeMs = delayBefore
                    )

                    onActionRecorded?.invoke(macroAction)

                    // Rung nhẹ khi thả tay
                    vibrateFeedback(40)

                    currentTrailPath.reset()
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Vẽ vệt vuốt nếu đang kéo
        canvas.drawPath(currentTrailPath, trailPaint)

        // Vẽ vòng tròn vị trí điểm đang chạm
        if (isTouching) {
            canvas.drawCircle(currentX, currentY, 32f, touchPaint)
            canvas.drawCircle(currentX, currentY, 32f, strokePaint)
        }
    }

    private fun vibrateFeedback(ms: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }
}
