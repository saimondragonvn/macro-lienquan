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
 * Hồ sơ Cấu hình Macro riêng biệt cho từng Game (Game Profile):
 * - Mỗi game có một danh sách nút macro riêng, vị trí nút riêng, độ mờ riêng.
 * - Ví dụ: Liên Quân Mobile có nút Florentino, Mua Đồ; Free Fire có nút Keo Nhanh; Tốc Chiến có combo riêng.
 */
data class GameProfile(
    val id: String,
    var name: String,
    var iconEmoji: String = "🎮",
    var packageName: String? = null,
    val triggers: MutableList<SavedMacroTrigger> = mutableListOf(),
    var globalOpacity: Int = 85
)

/**
 * Bộ Nhớ Lưu Trữ Cấu Hình Đa Hồ Sơ (Multi-Game Profile & Preset Storage):
 * - Tự động ghi nhớ các nút combo theo từng Game riêng biệt.
 * - Cho phép chuyển đổi nhanh giữa các game (Liên Quân, Free Fire, Tốc Chiến, Game khác...) ngay trong trận đấu!
 */
class MacroConfigStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "macro_gaming_config_v2"
        private const val KEY_GAME_PROFILES = "saved_game_profiles_v1"
        private const val KEY_ACTIVE_PROFILE_ID = "active_game_profile_id"
        private const val KEY_LEGACY_SAVED_TRIGGERS = "saved_macro_triggers"
        private const val KEY_GLOBAL_OPACITY = "global_opacity_percent"
        private const val KEY_AUTO_HIDE_OUTSIDE = "auto_hide_outside_game"
        private const val KEY_LAST_GAME_PACKAGE = "last_game_package"

        const val DEFAULT_PROFILE_LIENQUAN_ID = "profile_lienquan"
        const val DEFAULT_PROFILE_FREEFIRE_ID = "profile_freefire"
        const val DEFAULT_PROFILE_WILDRIFT_ID = "profile_wildrift"
        const val DEFAULT_PROFILE_CUSTOM_ID = "profile_custom"
    }

    /**
     * Lấy toàn bộ danh sách Hồ Sơ Game
     */
    @Synchronized
    fun getAllProfiles(): MutableList<GameProfile> {
        val json = prefs.getString(KEY_GAME_PROFILES, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<MutableList<GameProfile>>() {}.type
                val list: MutableList<GameProfile>? = gson.fromJson(json, type)
                if (!list.isNullOrEmpty()) {
                    return list
                }
            } catch (_: Throwable) {}
        }

        // Khởi tạo các cấu hình mặc định ban đầu & Di chuyển dữ liệu cũ (Migration)
        val initialProfiles = mutableListOf<GameProfile>()

        // 1. Kiểm tra xem có dữ liệu nút macro cũ không để gán vào Liên Quân
        val legacyTriggers = loadLegacyTriggers()

        initialProfiles.add(
            GameProfile(
                id = DEFAULT_PROFILE_LIENQUAN_ID,
                name = "Liên Quân Mobile",
                iconEmoji = "🎮",
                packageName = "com.garena.game.kgvn",
                triggers = legacyTriggers,
                globalOpacity = 85
            )
        )
        initialProfiles.add(
            GameProfile(
                id = DEFAULT_PROFILE_FREEFIRE_ID,
                name = "Free Fire",
                iconEmoji = "🔥",
                packageName = "com.dts.freefireth",
                triggers = mutableListOf(),
                globalOpacity = 85
            )
        )
        initialProfiles.add(
            GameProfile(
                id = DEFAULT_PROFILE_WILDRIFT_ID,
                name = "Tốc Chiến",
                iconEmoji = "⚔️",
                packageName = "com.riotgames.league.wildriftvn",
                triggers = mutableListOf(),
                globalOpacity = 85
            )
        )
        initialProfiles.add(
            GameProfile(
                id = DEFAULT_PROFILE_CUSTOM_ID,
                name = "Game Khác",
                iconEmoji = "🎯",
                triggers = mutableListOf(),
                globalOpacity = 85
            )
        )

        saveAllProfiles(initialProfiles)
        if (prefs.getString(KEY_ACTIVE_PROFILE_ID, null) == null) {
            setActiveProfileId(DEFAULT_PROFILE_LIENQUAN_ID)
        }
        return initialProfiles
    }

    /**
     * Lưu danh sách hồ sơ game xuống đĩa
     */
    @Synchronized
    fun saveAllProfiles(profiles: List<GameProfile>) {
        try {
            val json = gson.toJson(profiles)
            prefs.edit().putString(KEY_GAME_PROFILES, json).commit()
        } catch (_: Throwable) {}
    }

    /**
     * Lấy ID hồ sơ game đang được chọn
     */
    fun getActiveProfileId(): String {
        return prefs.getString(KEY_ACTIVE_PROFILE_ID, DEFAULT_PROFILE_LIENQUAN_ID) ?: DEFAULT_PROFILE_LIENQUAN_ID
    }

    /**
     * Đổi hồ sơ game đang hoạt động
     */
    fun setActiveProfileId(profileId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).commit()
    }

    /**
     * Lấy đối tượng GameProfile đang hoạt động
     */
    @Synchronized
    fun getActiveProfile(): GameProfile {
        val profiles = getAllProfiles()
        val activeId = getActiveProfileId()
        return profiles.find { it.id == activeId } ?: profiles.firstOrNull() ?: GameProfile(
            DEFAULT_PROFILE_LIENQUAN_ID,
            "Liên Quân Mobile",
            "🎮"
        )
    }

    /**
     * Tạo một hồ sơ Game mới
     */
    @Synchronized
    fun createProfile(name: String, emoji: String = "🎮", packageName: String? = null): GameProfile {
        val profiles = getAllProfiles()
        val cleanName = name.trim().ifEmpty { "Game Mới ${profiles.size + 1}" }
        val newProfile = GameProfile(
            id = "profile_${System.currentTimeMillis()}",
            name = cleanName,
            iconEmoji = emoji,
            packageName = packageName,
            triggers = mutableListOf(),
            globalOpacity = globalOpacity
        )
        profiles.add(newProfile)
        saveAllProfiles(profiles)
        setActiveProfileId(newProfile.id)
        return newProfile
    }

    /**
     * Đổi tên hồ sơ game
     */
    @Synchronized
    fun renameProfile(profileId: String, newName: String): Boolean {
        val profiles = getAllProfiles()
        val profile = profiles.find { it.id == profileId } ?: return false
        profile.name = newName.trim().ifEmpty { profile.name }
        saveAllProfiles(profiles)
        return true
    }

    /**
     * Xóa một hồ sơ game (chỉ xóa nếu còn ít nhất 2 hồ sơ)
     */
    @Synchronized
    fun deleteProfile(profileId: String): Boolean {
        val profiles = getAllProfiles()
        if (profiles.size <= 1) return false

        val removed = profiles.removeAll { it.id == profileId }
        if (removed) {
            if (getActiveProfileId() == profileId) {
                setActiveProfileId(profiles.first().id)
            }
            saveAllProfiles(profiles)
            return true
        }
        return false
    }

    /**
     * Lấy toàn bộ danh sách nút Macro của game đang hoạt động
     */
    @Synchronized
    fun loadAllTriggers(): MutableList<SavedMacroTrigger> {
        val active = getActiveProfile()
        return active.triggers.toMutableList()
    }

    /**
     * Lưu toàn bộ danh sách nút Macro vào game đang hoạt động
     */
    @Synchronized
    fun saveAllTriggers(triggers: List<SavedMacroTrigger>) {
        val profiles = getAllProfiles()
        val activeId = getActiveProfileId()
        val active = profiles.find { it.id == activeId } ?: profiles.firstOrNull() ?: return

        active.triggers.clear()
        active.triggers.addAll(triggers)
        saveAllProfiles(profiles)
    }

    /**
     * Thêm hoặc cập nhật một nút macro chính xác theo ID trong game đang chọn
     */
    @Synchronized
    fun upsertTrigger(trigger: SavedMacroTrigger) {
        val profiles = getAllProfiles()
        val activeId = getActiveProfileId()
        val active = profiles.find { it.id == activeId } ?: profiles.firstOrNull() ?: return

        val index = active.triggers.indexOfFirst { it.id == trigger.id }
        if (index >= 0) {
            active.triggers[index] = trigger
        } else {
            active.triggers.add(trigger)
        }
        saveAllProfiles(profiles)
    }

    /**
     * Xóa một nút macro theo ID hoặc tên trong game đang chọn
     */
    @Synchronized
    fun deleteTrigger(identifier: String) {
        val profiles = getAllProfiles()
        val activeId = getActiveProfileId()
        val active = profiles.find { it.id == activeId } ?: profiles.firstOrNull() ?: return

        val changed = active.triggers.removeAll { it.id == identifier || it.name == identifier }
        if (changed) {
            saveAllProfiles(profiles)
        }
    }

    /**
     * Xóa toàn bộ nút macro của game đang chọn
     */
    @Synchronized
    fun clearAllTriggers() {
        val profiles = getAllProfiles()
        val activeId = getActiveProfileId()
        val active = profiles.find { it.id == activeId } ?: profiles.firstOrNull() ?: return

        active.triggers.clear()
        saveAllProfiles(profiles)
    }

    /**
     * Di chuyển dữ liệu nút cũ của bản tiền nhiệm nếu có
     */
    private fun loadLegacyTriggers(): MutableList<SavedMacroTrigger> {
        val json = prefs.getString(KEY_LEGACY_SAVED_TRIGGERS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<SavedMacroTrigger>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (_: Throwable) {
            mutableListOf()
        }
    }

    /**
     * Độ mờ toàn cục (20% - 100%, mặc định 85%)
     */
    var globalOpacity: Int
        get() = prefs.getInt(KEY_GLOBAL_OPACITY, 85)
        set(value) = prefs.edit().putInt(KEY_GLOBAL_OPACITY, value.coerceIn(20, 100)).apply()

    /**
     * Chế độ tự động ẩn khi thoát khỏi game
     */
    var isAutoHideOutsideGame: Boolean
        get() = prefs.getBoolean(KEY_AUTO_HIDE_OUTSIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_HIDE_OUTSIDE, value).apply()

    var lastGamePackage: String
        get() = prefs.getString(KEY_LAST_GAME_PACKAGE, "com.garena.game.kgvn") ?: "com.garena.game.kgvn"
        set(value) = prefs.edit().putString(KEY_LAST_GAME_PACKAGE, value).apply()
}
