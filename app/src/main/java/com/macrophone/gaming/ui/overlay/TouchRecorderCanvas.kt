package com.macrophone.gaming.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Custom View hiển thị trực quan toàn bộ các điểm chạm và vệt vuốt khi ghi Macro:
 * - HIỂN THỊ RÕ RÀNG TẤT CẢ VỊ TRÍ ĐÃ NHẤN HOẶC VUỐT TRÊN MÀN HÌNH (kèm số thứ tự 1, 2, 3...)
 * - Tự động phóng to theo mật độ màn hình (density) để hiển thị to, rõ trên mọi loại điện thoại và giả lập PC.
 * - Hỗ trợ hoàn tác (Undo) điểm gần nhất và xóa toàn bộ.
 */
class TouchRecorderCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class RecordedTouchMarker(
        val index: Int,
        val actionType: MacroType,
        val points: List<GesturePoint>,
        val durationMs: Long
    )

    private val density: Float = context.resources.displayMetrics.density

    // Danh sách lưu lại tất cả các điểm/vệt vuốt đã ghi để vẽ cố định trên màn hình
    private val recordedMarkers = mutableListOf<RecordedTouchMarker>()
    private val recordedActions = mutableListOf<MacroAction>()

    // Paint cho điểm đang chạm tay trực tiếp (Live Touch)
    private val activeTouchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FF0055") // Hồng neon trong suốt
        style = Paint.Style.FILL
    }

    private val activeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }

    private val activeTrailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B300E5FF") // Cyan neon
        style = Paint.Style.STROKE
        strokeWidth = 5f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Paint cho các điểm TAP đã ghi (vẽ cố định trên màn hình)
    private val markerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F00F172A") // Nền tối tương phản cao
        style = Paint.Style.FILL
    }

    private val markerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5900E5FF") // Vòng sáng cyan neon
        style = Paint.Style.FILL
    }

    private val markerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // Viền cyan neon
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }

    // Paint cho điểm HOLD đã ghi (Màu tím neon)
    private val holdGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#59A855F7")
        style = Paint.Style.FILL
    }

    private val holdStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A855F7")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
    }

    // Paint cho đường vuốt SWIPE đã ghi (Màu xanh lá / Emerald neon)
    private val swipePathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F210B981")
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = DashPathEffect(floatArrayOf(12f * density, 6f * density), 0f)
    }

    private val swipeArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981")
        style = Paint.Style.FILL
    }

    // Paint cho chữ số thứ tự (1, 2, 3...)
    private val textNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 17f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Paint cho nhãn phụ (CHẠM, GIỮ, VUỐT)
    private val textBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val textHoldBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C084FC")
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
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
    var onCountChanged: ((Int) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val now = System.currentTimeMillis()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val actionIndex = event.actionIndex
                val px = event.getX(actionIndex)
                val py = event.getY(actionIndex)
                isTouching = true
                currentX = px
                currentY = py
                actionDownTime = now
                currentPoints.clear()
                currentTrailPath.reset()
                currentTrailPath.moveTo(px, py)
                currentPoints.add(GesturePoint(px, py, now))

                vibrateFeedback(25)
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

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTouching) {
                    isTouching = false
                    val duration = max(30L, now - actionDownTime)

                    val delayBefore = if (lastActionUpTime > 0L) {
                        max(20L, actionDownTime - lastActionUpTime)
                    } else {
                        50L
                    }
                    lastActionUpTime = now

                    val first = currentPoints.firstOrNull() ?: GesturePoint(x, y, actionDownTime)
                    val totalDist = hypot((x - first.x).toDouble(), (y - first.y).toDouble())

                    val actionType = when {
                        totalDist > (25.0 * density) -> MacroType.SWIPE
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

                    // Lưu vào danh sách hiển thị cố định trên màn hình
                    val nextIndex = recordedMarkers.size + 1
                    recordedMarkers.add(
                        RecordedTouchMarker(
                            index = nextIndex,
                            actionType = actionType,
                            points = pointsToSave,
                            durationMs = duration
                        )
                    )
                    recordedActions.add(macroAction)

                    onActionRecorded?.invoke(macroAction)
                    onCountChanged?.invoke(recordedMarkers.size)

                    vibrateFeedback(35)

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

        // 1. Vẽ tất cả các điểm / vệt vuốt ĐÃ GHI trước đó (luôn hiển thị rõ ràng trên màn hình)
        for (marker in recordedMarkers) {
            when (marker.actionType) {
                MacroType.TAP -> drawRecordedTap(canvas, marker)
                MacroType.HOLD -> drawRecordedHold(canvas, marker)
                MacroType.SWIPE -> drawRecordedSwipe(canvas, marker)
            }
        }

        // 2. Vẽ vệt vuốt đang thực hiện trong thời gian thực (Live touch)
        if (isTouching) {
            canvas.drawPath(currentTrailPath, activeTrailPaint)
            val liveRadius = 24f * density
            canvas.drawCircle(currentX, currentY, liveRadius, activeTouchPaint)
            canvas.drawCircle(currentX, currentY, liveRadius, activeStrokePaint)
        }
    }

    private fun drawRecordedTap(canvas: Canvas, marker: RecordedTouchMarker) {
        val pt = marker.points.firstOrNull() ?: return
        val cx = pt.x
        val cy = pt.y
        val radius = 22f * density

        // Vòng phát sáng bên ngoài
        canvas.drawCircle(cx, cy, radius + (8f * density), markerGlowPaint)

        // Vòng tròn trung tâm
        canvas.drawCircle(cx, cy, radius, markerBgPaint)
        canvas.drawCircle(cx, cy, radius, markerStrokePaint)

        // Số thứ tự ở giữa được căn giữa hoàn hảo theo chiều dọc
        val textY = cy - (textNumberPaint.descent() + textNumberPaint.ascent()) / 2f
        canvas.drawText(marker.index.toString(), cx, textY, textNumberPaint)

        // Nhãn nhỏ "CHẠM" bên dưới
        canvas.drawText("CHẠM", cx, cy + radius + (11f * density), textBadgePaint)
    }

    private fun drawRecordedHold(canvas: Canvas, marker: RecordedTouchMarker) {
        val pt = marker.points.firstOrNull() ?: return
        val cx = pt.x
        val cy = pt.y
        val radius = 25f * density

        canvas.drawCircle(cx, cy, radius + (9f * density), holdGlowPaint)
        canvas.drawCircle(cx, cy, radius, markerBgPaint)
        canvas.drawCircle(cx, cy, radius, holdStrokePaint)

        val textY = cy - (textNumberPaint.descent() + textNumberPaint.ascent()) / 2f
        canvas.drawText(marker.index.toString(), cx, textY, textNumberPaint)
        canvas.drawText("GIỮ (${marker.durationMs}ms)", cx, cy + radius + (11f * density), textHoldBadgePaint)
    }

    private fun drawRecordedSwipe(canvas: Canvas, marker: RecordedTouchMarker) {
        if (marker.points.size < 2) return

        val path = Path()
        val first = marker.points.first()
        path.moveTo(first.x, first.y)

        for (i in 1 until marker.points.size) {
            val p = marker.points[i]
            path.lineTo(p.x, p.y)
        }
        canvas.drawPath(path, swipePathPaint)

        // Vẽ mũi tên ở điểm kết thúc
        val last = marker.points.last()
        val secondLast = marker.points.getOrNull(marker.points.size - 2) ?: first
        drawArrowHead(canvas, secondLast.x, secondLast.y, last.x, last.y)

        // Vẽ vòng tròn số thứ tự tại điểm bắt đầu vuốt
        val startRadius = 20f * density
        canvas.drawCircle(first.x, first.y, startRadius + (7f * density), markerGlowPaint)
        canvas.drawCircle(first.x, first.y, startRadius, markerBgPaint)
        canvas.drawCircle(first.x, first.y, startRadius, markerStrokePaint)

        val textY = first.y - (textNumberPaint.descent() + textNumberPaint.ascent()) / 2f
        canvas.drawText(marker.index.toString(), first.x, textY, textNumberPaint)
        canvas.drawText("VUỐT", first.x, first.y + startRadius + (11f * density), textBadgePaint)
    }

    private fun drawArrowHead(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val arrowLength = 18f * density
        val arrowAngle = Math.toRadians(28.0)

        val dx = (toX - fromX).toDouble()
        val dy = (toY - fromY).toDouble()
        val angle = atan2(dy, dx)

        val x1 = toX - arrowLength * cos(angle - arrowAngle).toFloat()
        val y1 = toY - arrowLength * sin(angle - arrowAngle).toFloat()
        val x2 = toX - arrowLength * cos(angle + arrowAngle).toFloat()
        val y2 = toY - arrowLength * sin(angle + arrowAngle).toFloat()

        val arrowPath = Path().apply {
            moveTo(toX, toY)
            lineTo(x1, y1)
            lineTo(x2, y2)
            close()
        }
        canvas.drawPath(arrowPath, swipeArrowPaint)
    }

    /**
     * Xóa điểm / thao tác cuối cùng đã ghi (Hoàn tác / Undo)
     */
    fun undoLast(): Boolean {
        if (recordedMarkers.isNotEmpty()) {
            recordedMarkers.removeAt(recordedMarkers.size - 1)
            if (recordedActions.isNotEmpty()) {
                recordedActions.removeAt(recordedActions.size - 1)
            }
            onCountChanged?.invoke(recordedMarkers.size)
            invalidate()
            return true
        }
        return false
    }

    /**
     * Xóa toàn bộ điểm đã ghi
     */
    fun clearAll() {
        recordedMarkers.clear()
        recordedActions.clear()
        currentTrailPath.reset()
        onCountChanged?.invoke(0)
        invalidate()
    }

    fun getRecordedCount(): Int = recordedMarkers.size

    fun getRecordedActions(): List<MacroAction> = ArrayList(recordedActions)

    private fun vibrateFeedback(ms: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }
}
