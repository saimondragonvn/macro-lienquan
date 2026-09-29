package com.macrophone.gaming.data.model

import com.google.gson.annotations.SerializedName

/**
 * Một thao tác độc lập trong chuỗi macro
 * @param type Loại thao tác (TAP, HOLD, SWIPE)
 * @param points Danh sách các điểm tọa độ (TAP/HOLD có 1 điểm, SWIPE có từ 2 điểm trở lên)
 * @param durationMs Thời lượng diễn ra thao tác (mili-giây)
 * @param delayBeforeMs Độ trễ chờ trước khi thực hiện thao tác này (mili-giây)
 */
data class MacroAction(
    @SerializedName("type") val type: MacroType,
    @SerializedName("points") val points: List<GesturePoint>,
    @SerializedName("durationMs") val durationMs: Long,
    @SerializedName("delayBeforeMs") val delayBeforeMs: Long = 50L
)
