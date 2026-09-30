package com.macrophone.gaming.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.macrophone.gaming.R
import com.macrophone.gaming.domain.MacroManager
import com.macrophone.gaming.ui.MainActivity
import com.macrophone.gaming.util.PermissionUtils

/**
 * Quản lý Tile trên bảng Cài đặt nhanh (Quick Settings Panel):
 * - Cho phép game thủ bật/tắt nhanh Floating Controller ngay khi đang trong game
 * - Hiển thị trạng thái hoạt động trực quan
 */
class QuickSettingsTileService : TileService() {

    private lateinit var macroManager: MacroManager

    override fun onCreate() {
        super.onCreate()
        macroManager = MacroManager.getInstance(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // 1. Kiểm tra quyền hệ thống trước khi bật
        val hasOverlay = PermissionUtils.hasOverlayPermission(this)

        if (!hasOverlay) {
            Toast.makeText(
                this,
                "Vui lòng mở ứng dụng và cấp quyền Cửa sổ nổi cho Game Turbo!",
                Toast.LENGTH_LONG
            ).show()

            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    appIntent,
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(appIntent)
            }
            return
        }

        // 2. Chuyển đổi trạng thái bật/tắt Floating Service
        val isRunning = macroManager.isFloatingServiceRunning.value
        if (isRunning) {
            FloatingWidgetService.stop(this)
        } else {
            FloatingWidgetService.start(this)
        }

        updateTileState(!isRunning)
    }

    private fun updateTileState(overrideRunning: Boolean? = null) {
        val tile = qsTile ?: return
        val isRunning = overrideRunning ?: macroManager.isFloatingServiceRunning.value

        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_macro_tile)
        tile.label = getString(R.string.qs_tile_label)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isRunning) {
                getString(R.string.qs_tile_state_active)
            } else {
                getString(R.string.qs_tile_state_inactive)
            }
        }

        tile.updateTile()
    }
}
