package com.macrophone.gaming.util

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroType
import kotlin.math.max

/**
 * Xây dựng GestureDescription từ MacroAction với hệ số tốc độ (Speed Multiplier).
 * Tối ưu hóa thời lượng chạm siêu nhanh (Ultra-snappy 20-30ms) để giải phóng Input Pointer ngay lập tức,
 * cho phép người dùng đồng thời sử dụng các ngón tay khác (như giữ joystick di chuyển, xoay camera)
 * mà KHÔNG HỀ BỊ CHẶN hay xung đột cảm ứng đa điểm (Multi-touch Co-existence).
 */
object GestureBuilder {

    /**
     * Tạo GestureDescription cho một hành động Macro đơn lẻ
     * @param action Hành động (chứa danh sách điểm, duration gốc, delay gốc)
     * @param speedMultiplier Hệ số tăng tốc (ví dụ: 0.5x, 1x, 2x, 5x, 10x)
     */
    fun buildGesture(action: MacroAction, speedMultiplier: Float = 1.0f): GestureDescription? {
        if (action.points.isEmpty()) return null

        val path = Path()
        val firstPoint = action.points.first()
        path.moveTo(firstPoint.x, firstPoint.y)

        when (action.type) {
            MacroType.TAP, MacroType.HOLD -> {
                path.lineTo(firstPoint.x, firstPoint.y)
            }
            MacroType.SWIPE -> {
                for (i in 1 until action.points.size) {
                    val p = action.points[i]
                    path.lineTo(p.x, p.y)
                }
            }
        }

        val safeMultiplier = if (speedMultiplier <= 0f) 1.0f else speedMultiplier

        val effectiveDuration = when (action.type) {
            MacroType.TAP -> {
                // Nhấn nhả siêu nhanh (20ms - 35ms) để nhả ngay Input Pointer,
                // bảo đảm các ngón tay khác của game thủ vẫn thao tác bình thường
                val scaled = (action.durationMs / safeMultiplier).toLong()
                max(15L, scaled).coerceAtMost(35L)
            }
            MacroType.HOLD -> {
                max(50L, (action.durationMs / safeMultiplier).toLong())
            }
            MacroType.SWIPE -> {
                max(30L, (action.durationMs / safeMultiplier).toLong())
            }
        }

        val stroke = GestureDescription.StrokeDescription(path, 0L, effectiveDuration)
        return GestureDescription.Builder()
            .addStroke(stroke)
            .build()
    }

    /**
     * Tính độ trễ chờ hiệu dụng giữa các thao tác
     */
    fun calculateEffectiveDelay(delayMs: Long, speedMultiplier: Float): Long {
        val safeMultiplier = if (speedMultiplier <= 0f) 1.0f else speedMultiplier
        return max(5L, (delayMs / safeMultiplier).toLong())
    }
}
