package com.macrophone.gaming.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.macrophone.gaming.R
import com.macrophone.gaming.ui.MainActivity

/**
 * Quản lý Tile trên bảng Cài đặt nhanh (Quick Settings Panel):
 * - Cho phép game thủ bật/tắt nhanh Game Turbo HUD ngay khi đang trong game
 * - Hiển thị trạng thái hoạt động trực quan
 */
class QuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // 1. Kiểm tra quyền hệ thống trước khi bật
        val hasOverlay = Settings.canDrawOverlays(this)

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

        // 2. Chuyển đổi trạng thái bật/tắt TurboOverlayService
        val isRunning = TurboOverlayService.isRunning
        if (isRunning) {
            TurboOverlayService.stop(this)
        } else {
            TurboOverlayService.start(this)
        }

        updateTileState(!isRunning)
    }

    private fun updateTileState(overrideRunning: Boolean? = null) {
        val tile = qsTile ?: return
        val isRunning = overrideRunning ?: TurboOverlayService.isRunning

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
