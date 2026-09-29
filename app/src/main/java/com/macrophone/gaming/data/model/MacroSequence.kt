package com.macrophone.gaming.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Chuỗi kịch bản Macro hoàn chỉnh bao gồm danh sách các hành động, cấu hình tốc độ và nút nổi
 */
data class MacroSequence(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("actions") val actions: List<MacroAction>,
    @SerializedName("config") val config: PlaybackConfig = PlaybackConfig(),
    @SerializedName("hasFloatingButton") val hasFloatingButton: Boolean = true,
    @SerializedName("buttonX") var buttonX: Int = 120,
    @SerializedName("buttonY") var buttonY: Int = 600,
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis()
) {
    val totalActionCount: Int
        get() = actions.size

    val totalOriginalDurationMs: Long
        get() = actions.sumOf { it.durationMs + it.delayBeforeMs }

    val effectiveAcceleratedDurationMs: Long
        get() {
            val speed = if (config.speedMultiplier <= 0f) 1.0f else config.speedMultiplier
            return (totalOriginalDurationMs / speed).toLong()
        }
}
