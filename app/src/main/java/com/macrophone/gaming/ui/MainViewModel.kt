package com.macrophone.gaming.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.macrophone.gaming.data.model.MacroSequence
import com.macrophone.gaming.domain.MacroManager
import com.macrophone.gaming.service.FloatingWidgetService
import com.macrophone.gaming.util.PermissionUtils
import com.macrophone.gaming.util.ShizukuHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PermissionState(
    val hasOverlay: Boolean = false,
    val hasShizukuOrRoot: Boolean = false,
    val hasNotification: Boolean = false,
    val isBatteryOptimized: Boolean = false
) {
    val areCorePermissionsGranted: Boolean
        get() = hasOverlay && hasShizukuOrRoot
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val macroManager = MacroManager.getInstance(application)

    private val _permissions = MutableStateFlow(PermissionState())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    private val _macroList = MutableStateFlow<List<MacroSequence>>(emptyList())
    val macroList: StateFlow<List<MacroSequence>> = _macroList.asStateFlow()

    val activeMacro: StateFlow<MacroSequence?> = macroManager.activeMacro
    val isFloatingServiceRunning: StateFlow<Boolean> = macroManager.isFloatingServiceRunning

    init {
        refreshData()
    }

    fun refreshData() {
        val context = getApplication<Application>()
        val hasShizukuOrRoot = ShizukuHelper.hasShizukuPermission() || ShizukuHelper.isRootAvailable()
        _permissions.value = PermissionState(
            hasOverlay = PermissionUtils.hasOverlayPermission(context),
            hasShizukuOrRoot = hasShizukuOrRoot,
            hasNotification = PermissionUtils.hasNotificationPermission(context),
            isBatteryOptimized = PermissionUtils.isIgnoringBatteryOptimizations(context)
        )
        _macroList.value = macroManager.getAllMacros()
    }

    fun selectMacro(sequence: MacroSequence) {
        macroManager.setActiveMacro(sequence)
    }

    fun deleteMacro(sequence: MacroSequence) {
        macroManager.deleteMacro(sequence.id)
        _macroList.value = macroManager.getAllMacros()
    }

    fun resetDefaults() {
        _macroList.value = macroManager.resetToDefaults()
    }

    fun playMacro(sequence: MacroSequence) {
        macroManager.setActiveMacro(sequence)
        macroManager.startPlayback()
    }

    fun toggleFloatingService(context: Context) {
        if (isFloatingServiceRunning.value) {
            FloatingWidgetService.stop(context)
        } else {
            FloatingWidgetService.start(context)
        }
    }
}
