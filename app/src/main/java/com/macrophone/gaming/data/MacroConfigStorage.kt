package com.macrophone.gaming.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.macrophone.gaming.data.model.MacroAction

/**
 * Dữ liệu lưu trữ cấu hình Nút Macro nổi trên màn hình game
 */
data class SavedMacroTrigger(
    val id: String,
    val name: String,
    var x: Int,
    var y: Int,
    var delayBetweenMs: Long = 40,
    var repeatCount: Int = 1,
    var opacityPercent: Int = 85,
    val points: List<Pair<Float, Float>> = emptyList(),
    val actions: List<MacroAction> = emptyList(),
    var speedMultiplier: Float = 1.0f
)

/**
 * Bộ Nhớ Lưu Trữ Cấu Hình Macro (Persistent Storage):
 * - Tự động ghi nhớ mọi nút combo đã tạo, vị trí x/y trên màn hình, độ trễ và độ mờ.
 * - Khi mở lại Game Turbo hoặc mở lại game, tự động khôi phục đúng vị trí cũ mà không cần cài lại từ đầu!
 */
class MacroConfigStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "macro_gaming_config_v2"
        private const val KEY_SAVED_TRIGGERS = "saved_macro_triggers"
        private const val KEY_GLOBAL_OPACITY = "global_opacity_percent"
        private const val KEY_AUTO_HIDE_OUTSIDE = "auto_hide_outside_game"
        private const val KEY_LAST_GAME_PACKAGE = "last_game_package"
    }

    /**
     * Lấy toàn bộ danh sách nút Macro đã lưu
     */
    fun loadAllTriggers(): MutableList<SavedMacroTrigger> {
        val json = prefs.getString(KEY_SAVED_TRIGGERS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<SavedMacroTrigger>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (_: Throwable) {
            mutableListOf()
        }
    }

    /**
     * Lưu toàn bộ danh sách nút Macro
     */
    fun saveAllTriggers(triggers: List<SavedMacroTrigger>) {
        try {
            val json = gson.toJson(triggers)
            prefs.edit().putString(KEY_SAVED_TRIGGERS, json).apply()
        } catch (_: Throwable) {}
    }

    /**
     * Thêm hoặc cập nhật một nút macro
     */
    fun upsertTrigger(trigger: SavedMacroTrigger) {
        val list = loadAllTriggers()
        val index = list.indexOfFirst { it.id == trigger.id || it.name == trigger.name }
        if (index >= 0) {
            list[index] = trigger
        } else {
            list.add(trigger)
        }
        saveAllTriggers(list)
    }

    /**
     * Xóa một nút macro theo tên hoặc ID
     */
    fun deleteTrigger(identifier: String) {
        val list = loadAllTriggers()
        val changed = list.removeAll { it.id == identifier || it.name == identifier }
        if (changed) {
            saveAllTriggers(list)
        }
    }

    /**
     * Xóa toàn bộ nút macro đã lưu
     */
    fun clearAllTriggers() {
        prefs.edit().remove(KEY_SAVED_TRIGGERS).apply()
    }

    /**
     * Độ mờ toàn cục (20% - 100%, mặc định 85%)
     */
    var globalOpacity: Int
        get() = prefs.getInt(KEY_GLOBAL_OPACITY, 85)
        set(value) = prefs.edit().putInt(KEY_GLOBAL_OPACITY, value.coerceIn(20, 100)).apply()

    /**
     * Chế độ tự động ẩn khi thoát khỏi Liên Quân Mobile
     */
    var isAutoHideOutsideGame: Boolean
        get() = prefs.getBoolean(KEY_AUTO_HIDE_OUTSIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_HIDE_OUTSIDE, value).apply()

    var lastGamePackage: String
        get() = prefs.getString(KEY_LAST_GAME_PACKAGE, "com.garena.game.kgvn") ?: "com.garena.game.kgvn"
        set(value) = prefs.edit().putString(KEY_LAST_GAME_PACKAGE, value).apply()
}
