package com.macrophone.gaming.domain

import android.content.Context
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroSequence
import com.macrophone.gaming.data.model.MacroState
import com.macrophone.gaming.data.model.PlaybackConfig
import com.macrophone.gaming.data.repository.MacroRepository
import com.macrophone.gaming.service.MacroAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Trình quản lý trung tâm chịu trách nhiệm:
 * - Điều phối trạng thái hệ thống Macro (IDLE, RECORDING, PLAYING)
 * - Tương tác giữa Floating Service, Accessibility Service và UI
 * - Phát lại combo điểm ghim hoặc preset với cơ chế đa điểm không chặn thao tác
 */
class MacroManager private constructor(private val context: Context) {

    private val repository = MacroRepository(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _macroState = MutableStateFlow(MacroState.IDLE)
    val macroState: StateFlow<MacroState> = _macroState.asStateFlow()

    private val _activeMacro = MutableStateFlow<MacroSequence?>(null)
    val activeMacro: StateFlow<MacroSequence?> = _activeMacro.asStateFlow()

    private val _isFloatingServiceRunning = MutableStateFlow(false)
    val isFloatingServiceRunning: StateFlow<Boolean> = _isFloatingServiceRunning.asStateFlow()

    private val recordedActions = mutableListOf<MacroAction>()

    init {
        _activeMacro.value = repository.getActiveMacro()
    }

    companion object {
        @Volatile
        private var instance: MacroManager? = null

        fun getInstance(context: Context): MacroManager {
            return instance ?: synchronized(this) {
                instance ?: MacroManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun setFloatingServiceRunning(running: Boolean) {
        _isFloatingServiceRunning.value = running
    }

    fun setActiveMacro(sequence: MacroSequence) {
        _activeMacro.value = sequence
        repository.setActiveMacroId(sequence.id)
    }

    fun updateActiveConfig(config: PlaybackConfig) {
        val current = _activeMacro.value ?: return
        val updated = current.copy(config = config)
        _activeMacro.value = updated
        repository.saveMacro(updated)
    }

    /**
     * Bắt đầu phát lại macro đã chọn sẵn
     */
    fun startPlayback() {
        val macro = _activeMacro.value ?: return
        playDirectSequence(macro)
    }

    /**
     * Phát trực tiếp một chuỗi thao tác (ví dụ từ các điểm ghim Target Points trên màn hình)
     */
    fun playDirectSequence(sequence: MacroSequence) {
        if (sequence.actions.isEmpty()) return

        val accService = MacroAccessibilityService.instance ?: return

        _macroState.value = MacroState.PLAYING
        accService.playMacro(
            sequence = sequence,
            onComplete = {
                scope.launch {
                    _macroState.value = MacroState.IDLE
                }
            },
            onError = {
                scope.launch {
                    _macroState.value = MacroState.IDLE
                }
            }
        )
    }

    /**
     * Dừng phát lại ngay lập tức
     */
    fun stopPlayback() {
        MacroAccessibilityService.instance?.stopMacro()
        _macroState.value = MacroState.IDLE
    }

    /**
     * Bắt đầu chế độ ghi thao tác (Record Mode)
     */
    fun startRecording() {
        stopPlayback()
        recordedActions.clear()
        _macroState.value = MacroState.RECORDING
    }

    fun addRecordedAction(action: MacroAction) {
        if (_macroState.value == MacroState.RECORDING) {
            recordedActions.add(action)
        }
    }

    fun saveRecording(name: String, config: PlaybackConfig = PlaybackConfig()): MacroSequence? {
        if (recordedActions.isEmpty()) {
            _macroState.value = MacroState.IDLE
            return null
        }

        val newSequence = MacroSequence(
            name = name.ifBlank { "Custom Combo #${System.currentTimeMillis() % 1000}" },
            description = "Ghi lại trực tiếp từ màn hình",
            actions = ArrayList(recordedActions),
            config = config
        )

        repository.saveMacro(newSequence)
        setActiveMacro(newSequence)
        recordedActions.clear()
        _macroState.value = MacroState.IDLE
        return newSequence
    }

    fun cancelRecording() {
        recordedActions.clear()
        _macroState.value = MacroState.IDLE
    }

    fun getAllMacros(): List<MacroSequence> {
        return repository.getAllMacros()
    }

    fun deleteMacro(id: String) {
        repository.deleteMacro(id)
        if (_activeMacro.value?.id == id) {
            _activeMacro.value = repository.getActiveMacro()
        }
    }

    fun resetToDefaults(): List<MacroSequence> {
        val list = repository.resetToDefaults()
        _activeMacro.value = repository.getActiveMacro()
        return list
    }
}
