package com.macrophone.gaming.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroType

/**
 * Trình xem trước trực quan (Visual Preview) cho chuỗi thao tác đã ghi:
 * - TAP: Hiển thị vòng tròn tại điểm chạm kèm số thứ tự
 * - HOLD: Hiển thị vòng tròn lớn hơn với viền đậm
 * - SWIPE: Hiển thị đường cong nét đứt với mũi tên hướng
 *
 * Giúp người dùng kiểm tra lại thao tác trước khi lưu thành macro.
 */
class GesturePreviewCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var actions: List<MacroAction> = emptyList()

    private val tapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC06B6D4") // Cyan neon
        style = Paint.Style.FILL
    }

    private val tapStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val holdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC8B5CF6") // Purple
        style = Paint.Style.FILL
    }

    private val holdStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val swipePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC10B981") // Emerald
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = DashPathEffect(floatArrayOf(15f, 8f), 0f)
    }

    private val swipeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC10B981")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val indexBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA000000")
        style = Paint.Style.FILL
    }

    fun setActions(newActions: List<MacroAction>) {
        actions = newActions
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (actions.isEmpty()) return

        // Tính tỷ lệ scale nếu cần (khi preview nhỏ hơn màn hình gốc)
        val maxX = actions.flatMap { it.points }.maxOfOrNull { it.x } ?: 1f
        val maxY = actions.flatMap { it.points }.maxOfOrNull { it.y } ?: 1f
        val scaleX = if (maxX > width && width > 0) width.toFloat() / (maxX + 50f) else 1f
        val scaleY = if (maxY > height && height > 0) height.toFloat() / (maxY + 50f) else 1f
        val scale = minOf(scaleX, scaleY, 1f)

        actions.forEachIndexed { index, action ->
            when (action.type) {
                MacroType.TAP -> drawTap(canvas, action, index, scale)
                MacroType.HOLD -> drawHold(canvas, action, index, scale)
                MacroType.SWIPE -> drawSwipe(canvas, action, index, scale)
            }
        }
    }

    private fun drawTap(canvas: Canvas, action: MacroAction, index: Int, scale: Float) {
        val pt = action.points.firstOrNull() ?: return
        val cx = pt.x * scale
        val cy = pt.y * scale
        val radius = 24f

        canvas.drawCircle(cx, cy, radius, tapPaint)
        canvas.drawCircle(cx, cy, radius, tapStrokePaint)
        drawIndex(canvas, cx, cy, index + 1)
    }

    private fun drawHold(canvas: Canvas, action: MacroAction, index: Int, scale: Float) {
        val pt = action.points.firstOrNull() ?: return
        val cx = pt.x * scale
        val cy = pt.y * scale
        val radius = 34f

        canvas.drawCircle(cx, cy, radius, holdPaint)
        canvas.drawCircle(cx, cy, radius, holdStrokePaint)
        drawIndex(canvas, cx, cy, index + 1)
    }

    private fun drawSwipe(canvas: Canvas, action: MacroAction, index: Int, scale: Float) {
        if (action.points.size < 2) return

        val path = Path()
        val first = action.points.first()
        path.moveTo(first.x * scale, first.y * scale)

        // Vẽ điểm bắt đầu
        canvas.drawCircle(first.x * scale, first.y * scale, 14f, swipeDotPaint)

        for (i in 1 until action.points.size) {
            val pt = action.points[i]
            path.lineTo(pt.x * scale, pt.y * scale)
        }
        canvas.drawPath(path, swipePaint)

        // Vẽ điểm kết thúc (mũi tên)
        val last = action.points.last()
        canvas.drawCircle(last.x * scale, last.y * scale, 10f, swipeDotPaint)

        // Vẽ index tại điểm đầu
        drawIndex(canvas, first.x * scale, first.y * scale - 20f, index + 1)
    }

    private fun drawIndex(canvas: Canvas, cx: Float, cy: Float, number: Int) {
        val label = number.toString()
        canvas.drawCircle(cx, cy - 32f, 14f, indexBgPaint)
        canvas.drawText(label, cx, cy - 24f, textPaint)
    }
}
