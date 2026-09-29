package com.macrophone.gaming.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.macrophone.gaming.data.model.GesturePoint
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroSequence
import com.macrophone.gaming.data.model.MacroType
import com.macrophone.gaming.data.model.PlaybackConfig

/**
 * Quản lý lưu trữ và nạp các mẫu combo Macro từ SharedPreferences (sử dụng Gson).
 */
class MacroRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "macro_gaming_prefs"
        private const val KEY_MACRO_LIST = "saved_macro_list"
        private const val KEY_ACTIVE_MACRO_ID = "active_macro_id"
    }

    /**
     * Lấy toàn bộ danh sách Macro đã lưu, nếu rỗng thì tạo các preset mặc định.
     */
    fun getAllMacros(): List<MacroSequence> {
        val json = prefs.getString(KEY_MACRO_LIST, null)
        return if (json.isNullOrEmpty()) {
            val defaults = createDefaultPresets()
            saveAllMacros(defaults)
            defaults
        } else {
            try {
                val type = object : TypeToken<List<MacroSequence>>() {}.type
                gson.fromJson<List<MacroSequence>>(json, type) ?: createDefaultPresets()
            } catch (e: Exception) {
                createDefaultPresets()
            }
        }
    }

    /**
     * Lưu danh sách Macro
     */
    fun saveAllMacros(list: List<MacroSequence>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_MACRO_LIST, json).apply()
    }

    /**
     * Thêm mới hoặc cập nhật 1 Macro
     */
    fun saveMacro(sequence: MacroSequence) {
        val currentList = getAllMacros().toMutableList()
        val index = currentList.indexOfFirst { it.id == sequence.id }
        if (index >= 0) {
            currentList[index] = sequence
        } else {
            currentList.add(sequence)
        }
        saveAllMacros(currentList)
    }

    /**
     * Xóa 1 Macro theo ID
     */
    fun deleteMacro(id: String) {
        val currentList = getAllMacros().toMutableList()
        currentList.removeAll { it.id == id }
        saveAllMacros(currentList)
    }

    /**
     * Lưu ID Macro đang được kích hoạt sử dụng
     */
    fun setActiveMacroId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_MACRO_ID, id).apply()
    }

    /**
     * Lấy Macro đang được kích hoạt
     */
    fun getActiveMacro(): MacroSequence? {
        val list = getAllMacros()
        val activeId = prefs.getString(KEY_ACTIVE_MACRO_ID, null)
        return list.firstOrNull { it.id == activeId } ?: list.firstOrNull()
    }

    /**
     * Khôi phục danh sách combo mẫu ban đầu
     */
    fun resetToDefaults(): List<MacroSequence> {
        val defaults = createDefaultPresets()
        saveAllMacros(defaults)
        if (defaults.isNotEmpty()) {
            setActiveMacroId(defaults.first().id)
        }
        return defaults
    }

    /**
     * Tạo các Preset combo thông dụng cho game MOBA / FPS / RPG
     */
    private fun createDefaultPresets(): List<MacroSequence> {
        // Preset 1: Fast Skill Combo (MOBA: Chiêu 2 -> Chiêu 1 -> Đánh thường)
        val mobaCombo = MacroSequence(
            id = "preset_moba_burst",
            name = "MOBA Fast Burst (Skill 2-1-Attack)",
            description = "Combo dồn sát thương cực nhanh 3 nút liên tiếp",
            actions = listOf(
                MacroAction(
                    type = MacroType.TAP,
                    points = listOf(GesturePoint(850f, 750f)),
                    durationMs = 50L,
                    delayBeforeMs = 30L
                ),
                MacroAction(
                    type = MacroType.TAP,
                    points = listOf(GesturePoint(750f, 850f)),
                    durationMs = 50L,
                    delayBeforeMs = 80L
                ),
                MacroAction(
                    type = MacroType.TAP,
                    points = listOf(GesturePoint(950f, 900f)),
                    durationMs = 50L,
                    delayBeforeMs = 60L
                )
            ),
            config = PlaybackConfig(speedMultiplier = 1.0f, loopCount = 1, loopIntervalMs = 100L),
            hasFloatingButton = false
        )

        // Preset 2: FPS Rapid Fire Auto-Click (Bắn nhấp 5 lần siêu tốc)
        val rapidFire = MacroSequence(
            id = "preset_fps_rapid_fire",
            name = "FPS Rapid Fire (5x Tap)",
            description = "Nhấp nhả liên thanh nhanh tốc biến",
            actions = (1..5).map {
                MacroAction(
                    type = MacroType.TAP,
                    points = listOf(GesturePoint(920f, 880f)),
                    durationMs = 40L,
                    delayBeforeMs = 40L
                )
            },
            config = PlaybackConfig(speedMultiplier = 2.0f, loopCount = 1, loopIntervalMs = 50L),
            hasFloatingButton = false
        )

        // Preset 3: Skill Swipe & Cast (Định hướng kéo vuốt chiêu)
        val swipeSkill = MacroSequence(
            id = "preset_swipe_directional",
            name = "Directional Swipe & Release",
            description = "Vuốt định hướng từ tâm chiêu ra phía trước",
            actions = listOf(
                MacroAction(
                    type = MacroType.SWIPE,
                    points = listOf(
                        GesturePoint(800f, 800f),
                        GesturePoint(700f, 650f),
                        GesturePoint(600f, 500f)
                    ),
                    durationMs = 120L,
                    delayBeforeMs = 50L
                )
            ),
            config = PlaybackConfig(speedMultiplier = 1.0f, loopCount = 1, loopIntervalMs = 100L),
            hasFloatingButton = false
        )

        return listOf(mobaCombo, rapidFire, swipeSkill)
    }
}
