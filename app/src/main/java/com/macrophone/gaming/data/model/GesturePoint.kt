package com.macrophone.gaming.data.model

import com.google.gson.annotations.SerializedName

/**
 * Điểm tọa độ và thời điểm chạm
 */
data class GesturePoint(
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float,
    @SerializedName("timestampMs") val timestampMs: Long = 0L
)
