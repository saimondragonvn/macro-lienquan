package com.macrophone.gaming.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.macrophone.gaming.data.model.MacroSequence
import com.macrophone.gaming.util.GestureBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Service hỗ trợ thao tác tự động (AccessibilityService) chịu trách nhiệm:
 * - Tiếp nhận GestureDescription và điều phối dispatchGesture()
 * - Áp dụng hệ số tốc độ (Speed Multiplier: 0.5x, 1x, 2x, 5x, 10x)
 * - Điều khiển vòng lặp (Loop Mode) hoặc bấm giữ combo liên tục
 * - Cơ chế Dừng Khẩn Cấp (Emergency Stop) bằng phím Volume Down
 */
class MacroAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playbackJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        @Volatile
        var instance: MacroAccessibilityService? = null
            private set

        val isRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Dịch vụ tập trung vào dispatch cử chỉ, không cần xử lý lắng nghe sự kiện nội dung
    }

    override fun onInterrupt() {
        stopMacro()
    }

    override fun onDestroy() {
        stopMacro()
        serviceScope.coroutineContext.cancelChildren()
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    /**
     * Bắt đầu phát lại chuỗi MacroSequence theo đúng cấu hình tốc độ và vòng lặp
     */
    fun playMacro(
        sequence: MacroSequence,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Dừng bất kỳ phiên phát nào đang chạy trước đó
        stopMacro()

        val config = sequence.config
        val speedMultiplier = config.speedMultiplier
        val loopCount = config.loopCount
        val loopIntervalMs = config.loopIntervalMs

        playbackJob = serviceScope.launch {
            try {
                var currentIteration = 0
                val isInfinite = config.isInfiniteLoop

                while (isActive && (isInfinite || currentIteration < loopCount)) {
                    currentIteration++

                    // Duyệt từng hành động trong chuỗi combo
                    for (action in sequence.actions) {
                        if (!isActive) break

                        // 1. Chờ độ trễ trước hành động (đã chia tỉ lệ theo tốc độ)
                        val effectiveDelay = GestureBuilder.calculateEffectiveDelay(
                            action.delayBeforeMs,
                            speedMultiplier
                        )
                        if (effectiveDelay > 0) {
                            delay(effectiveDelay)
                        }

                        // 2. Tạo GestureDescription
                        val gesture = GestureBuilder.buildGesture(action, speedMultiplier)
                        if (gesture != null) {
                            val success = dispatchGestureAsync(gesture)
                            if (!success) {
                                // Nếu gesture dispatch thất bại (ví dụ màn hình bị khóa hoặc xung đột)
                                mainHandler.post { onError("Không thể thực hiện cử chỉ chạm màn hình") }
                                return@launch
                            }
                        }
                    }

                    // 3. Nếu còn vòng lặp tiếp theo, nghỉ một khoảng loopIntervalMs
                    if (isActive && (isInfinite || currentIteration < loopCount)) {
                        val effectiveInterval = GestureBuilder.calculateEffectiveDelay(
                            loopIntervalMs,
                            speedMultiplier
                        )
                        delay(effectiveInterval)
                    }
                }

                mainHandler.post { onComplete() }
            } catch (e: Exception) {
                mainHandler.post { onError(e.message ?: "Lỗi phát macro") }
            }
        }
    }

    /**
     * Dừng phát macro
     */
    fun stopMacro() {
        playbackJob?.cancel()
        playbackJob = null
    }

    /**
     * Chuyển đổi dispatchGesture() từ callback thành suspend function không làm nghẽn thread
     */
    private suspend fun dispatchGestureAsync(gesture: GestureDescription): Boolean =
        suspendCancellableCoroutine { continuation ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            val dispatched = dispatchGesture(gesture, callback, null)
            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        }

    /**
     * Xử lý phím cứng khẩn cấp: Nhấn Volume Down để ngắt ngay combo đang phát
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (playbackJob?.isActive == true) {
                stopMacro()
                return true
            }
        }
        return super.onKeyEvent(event)
    }
}
