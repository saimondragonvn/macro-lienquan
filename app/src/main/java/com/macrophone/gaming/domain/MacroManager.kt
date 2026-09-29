package com.macrophone.gaming.domain

import android.content.Context
import android.widget.Toast
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroSequence
import com.macrophone.gaming.data.model.MacroState
import com.macrophone.gaming.data.model.MacroType
import com.macrophone.gaming.data.model.PlaybackConfig
import com.macrophone.gaming.data.repository.MacroRepository
import com.macrophone.gaming.service.MacroAccessibilityService
import com.macrophone.gaming.util.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private var playbackJob: Job? = null
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
     * Phát trực tiếp một chuỗi thao tác (hỗ trợ cả Trợ năng lẫn Shizuku/Root/ADB)
     */
    fun playDirectSequence(sequence: MacroSequence) {
        if (sequence.actions.isEmpty()) return

        stopPlayback()

        val accService = MacroAccessibilityService.instance
        if (accService != null && MacroAccessibilityService.isRunning) {
            _macroState.value = MacroState.PLAYING
            accService.playMacro(
                sequence = sequence,
                onComplete = {
                    scope.launch { _macroState.value = MacroState.IDLE }
                },
                onError = {
                    scope.launch { _macroState.value = MacroState.IDLE }
                }
            )
            return
        }

        // Nếu Dịch vụ Trợ năng chưa bật: Chạy mô phỏng click qua Shizuku Shell hoặc ADB / Root
        _macroState.value = MacroState.PLAYING
        playbackJob = scope.launch(Dispatchers.IO) {
            val executed = playViaShell(sequence)
            scope.launch(Dispatchers.Main) {
                _macroState.value = MacroState.IDLE
                if (!executed) {
                    Toast.makeText(
                        context,
                        "Cần bật Trợ năng HOẶC chạy Shizuku/ADB để điện thoại tự động click!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun playViaShell(sequence: MacroSequence): Boolean {
        return try {
            val speedFactor = sequence.config.speedMultiplier.coerceAtLeast(0.1f)
            val repeatCount = if (sequence.config.isInfiniteLoop) 1000 else sequence.config.loopCount.coerceAtLeast(1)

            for (r in 0 until repeatCount) {
                if (_macroState.value != MacroState.PLAYING) break

                for (action in sequence.actions) {
                    if (_macroState.value != MacroState.PLAYING) break

                    val delay = (action.delayBeforeMs / speedFactor).toLong()
                    if (delay > 0) {
                        Thread.sleep(delay)
                    }

                    when (action.type) {
                        MacroType.TAP -> {
                            val pt = action.points.firstOrNull() ?: continue
                            executeShellInput("input tap ${pt.x} ${pt.y}")
                        }
                        MacroType.HOLD -> {
                            val pt = action.points.firstOrNull() ?: continue
                            val dur = ((action.durationMs / speedFactor).toLong()).coerceAtLeast(50)
                            executeShellInput("input swipe ${pt.x} ${pt.y} ${pt.x} ${pt.y} $dur")
                        }
                        MacroType.SWIPE -> {
                            val p1 = action.points.firstOrNull() ?: continue
                            val p2 = action.points.lastOrNull() ?: continue
                            val dur = ((action.durationMs / speedFactor).toLong()).coerceAtLeast(20)
                            executeShellInput("input swipe ${p1.x} ${p1.y} ${p2.x} ${p2.y} $dur")
                        }
                    }
                }

                if (sequence.config.loopIntervalMs > 0 && r < repeatCount - 1) {
                    Thread.sleep((sequence.config.loopIntervalMs / speedFactor).toLong())
                }
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun executeShellInput(cmd: String): Boolean {
        // 1. Thử qua Shizuku nếu đang chạy
        if (ShizukuHelper.isShizukuRunning() && ShizukuHelper.hasShizukuPermission()) {
            try {
                val clazz = Class.forName("rikka.shizuku.Shizuku")
                val method = clazz.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                method.isAccessible = true
                val proc = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? java.lang.Process
                proc?.waitFor()
                proc?.destroy()
                return true
            } catch (_: Throwable) {}
        }

        // 2. Thử qua Root su
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            proc.waitFor()
            if (proc.exitValue() == 0) return true
        } catch (_: Throwable) {}

        // 3. Thử qua sh thông thường
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            proc.waitFor()
            if (proc.exitValue() == 0) return true
        } catch (_: Throwable) {}

        return false
    }

    /**
     * Dừng phát lại ngay lập tức
     */
    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
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
