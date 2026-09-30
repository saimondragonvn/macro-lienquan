package com.macrophone.gaming.domain

import android.content.Context
import android.widget.Toast
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroSequence
import com.macrophone.gaming.data.model.MacroState
import com.macrophone.gaming.data.model.MacroType
import com.macrophone.gaming.data.model.PlaybackConfig
import com.macrophone.gaming.data.repository.MacroRepository
import com.macrophone.gaming.util.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Trình quản lý trung tâm chịu trách nhiệm:
 * - Điều phối trạng thái hệ thống Macro Game Turbo (IDLE, RECORDING, PLAYING)
 * - Tương tác giữa Game Turbo Floating HUD và UI
 * - Phát lại combo điểm ghim hoặc preset siêu tốc qua Shizuku/Root (HOÀN TOÀN KHÔNG CẦN TRỢ NĂNG)
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
     * Phát trực tiếp một chuỗi thao tác Game Turbo siêu tốc qua Shizuku / Root (Không dùng Trợ năng)
     */
    fun playDirectSequence(sequence: MacroSequence) {
        if (sequence.actions.isEmpty()) return

        stopPlayback()

        _macroState.value = MacroState.PLAYING
        playbackJob = scope.launch(Dispatchers.IO) {
            val executed = playViaShell(sequence)
            scope.launch(Dispatchers.Main) {
                _macroState.value = MacroState.IDLE
                if (!executed) {
                    Toast.makeText(
                        context,
                        "Cần cấp quyền Shizuku (Gỡ lỗi Wi-Fi) hoặc Root để Game Turbo tự động click!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Thực thi combo qua Shizuku shell với cơ chế gom nhóm (Batching):
     * Gom toàn bộ các lệnh tap/swipe vào 1 tiến trình shell duy nhất giúp xả chiêu với độ trễ siêu thấp (Game Turbo style)
     */
    private fun playViaShell(sequence: MacroSequence): Boolean {
        return try {
            val speedFactor = sequence.config.speedMultiplier.coerceAtLeast(0.1f)
            val repeatCount = if (sequence.config.isInfiniteLoop) 1000 else sequence.config.loopCount.coerceAtLeast(1)

            // Xây dựng chuỗi lệnh bash gom nhóm để chạy trong 1 tiến trình shell
            val cmdBuilder = java.lang.StringBuilder()
            for (action in sequence.actions) {
                val delaySec = String.format(Locale.US, "%.3f", (action.delayBeforeMs / speedFactor) / 1000.0)
                if (delaySec.toDouble() > 0.005) {
                    cmdBuilder.append("sleep ").append(delaySec).append("; ")
                }
                when (action.type) {
                    MacroType.TAP -> {
                        val pt = action.points.firstOrNull() ?: continue
                        cmdBuilder.append("input tap ").append(pt.x.toInt()).append(" ").append(pt.y.toInt()).append("; ")
                    }
                    MacroType.HOLD -> {
                        val pt = action.points.firstOrNull() ?: continue
                        val dur = ((action.durationMs / speedFactor).toLong()).coerceAtLeast(50)
                        cmdBuilder.append("input swipe ")
                            .append(pt.x.toInt()).append(" ").append(pt.y.toInt()).append(" ")
                            .append(pt.x.toInt()).append(" ").append(pt.y.toInt()).append(" ")
                            .append(dur).append("; ")
                    }
                    MacroType.SWIPE -> {
                        val p1 = action.points.firstOrNull() ?: continue
                        val p2 = action.points.lastOrNull() ?: continue
                        val dur = ((action.durationMs / speedFactor).toLong()).coerceAtLeast(20)
                        cmdBuilder.append("input swipe ")
                            .append(p1.x.toInt()).append(" ").append(p1.y.toInt()).append(" ")
                            .append(p2.x.toInt()).append(" ").append(p2.y.toInt()).append(" ")
                            .append(dur).append("; ")
                    }
                }
            }

            val batchCommand = cmdBuilder.toString().trim().removeSuffix(";")
            if (batchCommand.isBlank()) return false

            for (r in 0 until repeatCount) {
                if (_macroState.value != MacroState.PLAYING) break

                val success = ShizukuHelper.executePrivilegedCommand(batchCommand)
                if (!success) return false

                if (sequence.config.loopIntervalMs > 0 && r < repeatCount - 1) {
                    Thread.sleep((sequence.config.loopIntervalMs / speedFactor).toLong())
                }
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Dừng phát lại ngay lập tức
     */
    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
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

    fun removeLastRecordedAction(): Boolean {
        if (recordedActions.isNotEmpty()) {
            recordedActions.removeAt(recordedActions.size - 1)
            return true
        }
        return false
    }

    fun saveRecording(
        name: String,
        config: PlaybackConfig = PlaybackConfig(),
        customActions: List<MacroAction>? = null,
        buttonX: Int = 100,
        buttonY: Int = 250
    ): MacroSequence? {
        val actionsToSave = customActions ?: recordedActions
        if (actionsToSave.isEmpty()) {
            _macroState.value = MacroState.IDLE
            return null
        }

        val nextIndex = getAllMacros().size + 1
        val finalName = name.ifBlank { "M$nextIndex" }
        val newSequence = MacroSequence(
            name = finalName,
            description = "Ghi lại trực tiếp từ màn hình",
            actions = ArrayList(actionsToSave),
            config = config,
            hasFloatingButton = true,
            buttonX = buttonX,
            buttonY = buttonY
        )

        repository.saveMacro(newSequence)
        setActiveMacro(newSequence)
        recordedActions.clear()
        _macroState.value = MacroState.IDLE
        return newSequence
    }

    /**
     * Lưu một chuỗi Macro tùy chỉnh và đặt làm active macro
     */
    fun saveDirectSequence(sequence: MacroSequence) {
        repository.saveMacro(sequence)
        setActiveMacro(sequence)
    }

    fun cancelRecording() {
        recordedActions.clear()
        _macroState.value = MacroState.IDLE
    }

    /**
     * Trả về danh sách các thao tác đã ghi (để hiển thị preview trước khi lưu)
     */
    fun getRecordedActions(): List<MacroAction> {
        return ArrayList(recordedActions)
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
