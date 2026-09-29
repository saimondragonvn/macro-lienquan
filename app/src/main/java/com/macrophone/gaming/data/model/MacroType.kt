package com.macrophone.gaming.data.model

/**
 * Kiểu cử chỉ thao tác cảm ứng
 */
enum class MacroType {
    TAP,    // Nhấn chạm điểm đơn lẻ
    HOLD,   // Nhấn giữ tại chỗ theo thời gian
    SWIPE   // Vuốt/Kéo từ điểm A đến điểm B (hoặc nhiều điểm)
}
