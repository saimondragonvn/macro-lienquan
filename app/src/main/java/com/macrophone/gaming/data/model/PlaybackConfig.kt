package com.macrophone.gaming.data.model

import com.google.gson.annotations.SerializedName

/**
 * Cấu hình phát lại thao tác:
 * - speedMultiplier: Hệ số tốc độ (0.5x, 1x, 2x, 5x, 10x)
 * - loopCount: Số lần lặp lại (-1 đại diện cho lặp vô hạn)
 * - loopIntervalMs: Độ trễ nghỉ giữa mỗi lần lặp (mili-giây)
 */
data class PlaybackConfig(
    @SerializedName("speedMultiplier") val speedMultiplier: Float = 1.0f,
    @SerializedName("loopCount") val loopCount: Int = 1,
    @SerializedName("loopIntervalMs") val loopIntervalMs: Long = 100L
) {
    val isInfiniteLoop: Boolean
        get() = loopCount <= -1
}
