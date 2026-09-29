package com.macrophone.gaming.data.model

/**
 * Trạng thái hoạt động của hệ thống Macro
 */
enum class MacroState {
    IDLE,       // Chờ, không chạy gì
    RECORDING,  // Đang trong chế độ ghi thao tác
    PLAYING,    // Đang phát lại macro
    PAUSED      // Đang tạm dừng
}
