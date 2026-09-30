package com.macrophone.gaming.core

import android.content.Context
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.macrophone.gaming.data.model.GesturePoint
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.hypot
import kotlin.math.max

/**
 * Bộ Ghi Thao Tác Game Turbo Pro (Chuẩn Xiaomi / Redmi Turbo 4 Pro):
 * - Đọc trực tiếp luồng sự kiện cảm ứng từ kernel Linux (/dev/input/event*) qua Shizuku/Root.
 * - HOÀN TOÀN KHÔNG CHẶN CẢM ỨNG MÀN HÌNH: Game thủ chơi game, mở Shop, bấm chiêu 100% tự nhiên!
 * - Tự động nhận diện thiết bị cảm ứng (Touchscreen) và độ phân giải màn hình.
 * - Tự động chuyển đổi tọa độ theo hướng xoay ngang (Landscape Liên Quân Mobile).
 * - Ghi lại chính xác tọa độ chạm, giữ, vuốt và khoảng trễ giữa các chiêu thức.
 */
object GameTurboRecorder {

    private const val TAG = "GameTurboRecorder"

    data class TouchDevice(
        val path: String,
        val minX: Int = 0,
        val maxX: Int = 1080,
        val minY: Int = 0,
        val maxY: Int = 2400
    )

    @Volatile
    var isRecording = false
        private set

    private var activeProcess: Process? = null
    private var recordingJob: Job? = null
    private val recordedActions = mutableListOf<MacroAction>()

    private var touchDevice: TouchDevice? = null

    fun prewarm(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            detectTouchDevice()
        }
    }

    /**
     * Dò tìm thiết bị cảm ứng màn hình từ getevent -p
     */
    fun detectTouchDevice(): TouchDevice? {
        touchDevice?.let { return it }

        try {
            val proc = executeShellCommand("getevent -p") ?: return null
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            var currentPath = ""
            var hasMtPositionX = false
            var hasMtPositionY = false
            var maxX = 1080
            var maxY = 2400

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("add device") && trimmed.contains("/dev/input/")) {
                    currentPath = trimmed.substringAfterLast(" ").trim()
                    hasMtPositionX = false
                    hasMtPositionY = false
                }
                // Tìm kiếm ABS_MT_POSITION_X (0035) và ABS_MT_POSITION_Y (0036)
                if (trimmed.contains("0035") || trimmed.contains("ABS_MT_POSITION_X")) {
                    hasMtPositionX = true
                    val maxVal = extractMaxVal(trimmed)
                    if (maxVal > 0) maxX = maxVal
                }
                if (trimmed.contains("0036") || trimmed.contains("ABS_MT_POSITION_Y")) {
                    hasMtPositionY = true
                    val maxVal = extractMaxVal(trimmed)
                    if (maxVal > 0) maxY = maxVal
                }

                if (hasMtPositionX && hasMtPositionY && currentPath.isNotEmpty()) {
                    touchDevice = TouchDevice(currentPath, 0, maxX, 0, maxY)
                    break
                }
                line = reader.readLine()
            }
            proc.destroy()
        } catch (e: Throwable) {
            Log.w(TAG, "detectTouchDevice error: ${e.message}")
        }

        return touchDevice
    }

    private fun extractMaxVal(line: String): Int {
        val maxRegex = Regex("""max\s+(\d+)""")
        val match = maxRegex.find(line)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /**
     * Bắt đầu phiên ghi thao tác thời gian thực chuẩn Game Turbo (Redmi Turbo style)
     */
    fun startRecording(
        context: Context,
        scope: CoroutineScope,
        onActionRecorded: ((MacroAction, Int) -> Unit)? = null,
        onStarted: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (isRecording) return

        if (!ShellExecutor.isEngineReady()) {
            onError?.invoke("Động cơ Shizuku chưa kích hoạt! Hãy mở app cấp quyền Shizuku.")
            return
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val rotation = display.rotation
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels
        val screenH = metrics.heightPixels

        // Tự động quét thiết bị cảm ứng
        val device = detectTouchDevice()
        val cmd = if (device != null && device.path.isNotEmpty()) {
            "getevent -lt ${device.path}"
        } else {
            "getevent -lt"
        }

        val proc = executeShellCommand(cmd)
        if (proc == null) {
            onError?.invoke("Không thể khởi động tiến trình đọc cảm ứng kernel!")
            return
        }

        activeProcess = proc
        isRecording = true
        recordedActions.clear()
        onStarted?.invoke()

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var curRawX = 0f
                var curRawY = 0f
                var isFingerDown = false
                var touchDownTime = 0L
                var lastTouchUpTime = 0L
                var downScreenX = 0f
                var downScreenY = 0f

                val devMaxX = device?.maxX ?: max(screenW, screenH)
                val devMaxY = device?.maxY ?: max(screenW, screenH)

                while (isActive && isRecording) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    val now = System.currentTimeMillis()

                    // 1. Nhận diện tọa độ X
                    if (trimmed.contains("ABS_MT_POSITION_X")) {
                        val hexStr = trimmed.split(Regex("\\s+")).last()
                        val raw = hexStr.toLongOrNull(16)?.toFloat() ?: curRawX
                        curRawX = raw
                    }

                    // 2. Nhận diện tọa độ Y
                    if (trimmed.contains("ABS_MT_POSITION_Y")) {
                        val hexStr = trimmed.split(Regex("\\s+")).last()
                        val raw = hexStr.toLongOrNull(16)?.toFloat() ?: curRawY
                        curRawY = raw
                    }

                    // 3. Nhận diện Chạm xuống (TOUCH DOWN)
                    val isDownEvent = (trimmed.contains("BTN_TOUCH") && (trimmed.contains("DOWN") || trimmed.endsWith("00000001") || trimmed.endsWith(" 1"))) ||
                            (trimmed.contains("ABS_MT_TRACKING_ID") && !trimmed.contains("ffffffff") && !trimmed.endsWith("-1"))
                    if (isDownEvent) {
                        if (!isFingerDown) {
                            isFingerDown = true
                            touchDownTime = now

                            // Chuyển đổi tọa độ phần cứng sang tọa độ hiển thị màn hình game (chuẩn xoay ngang)
                            val (sx, sy) = mapRawToScreen(curRawX, curRawY, devMaxX, devMaxY, screenW, screenH, rotation)
                            downScreenX = sx
                            downScreenY = sy
                        }
                    }

                    // 4. Nhận diện Nhấc tay (TOUCH UP)
                    val isUpEvent = (trimmed.contains("BTN_TOUCH") && (trimmed.contains("UP") || trimmed.endsWith("00000000") || trimmed.endsWith(" 0"))) ||
                            (trimmed.contains("ABS_MT_TRACKING_ID") && (trimmed.contains("ffffffff") || trimmed.endsWith("-1")))
                    if (isUpEvent) {
                        if (isFingerDown) {
                            isFingerDown = false
                            val duration = max(35L, now - touchDownTime)
                            val delayBefore = if (lastTouchUpTime > 0L) {
                                max(30L, touchDownTime - lastTouchUpTime)
                            } else {
                                50L
                            }
                            lastTouchUpTime = now

                            val (upScreenX, upScreenY) = mapRawToScreen(curRawX, curRawY, devMaxX, devMaxY, screenW, screenH, rotation)
                            val distance = hypot((upScreenX - downScreenX).toDouble(), (upScreenY - downScreenY).toDouble()).toFloat()

                            val action = if (distance > (30f * metrics.density)) {
                                // Thao tác Vuốt (SWIPE)
                                MacroAction(
                                    type = MacroType.SWIPE,
                                    points = listOf(
                                        GesturePoint(downScreenX, downScreenY, touchDownTime),
                                        GesturePoint(upScreenX, upScreenY, now)
                                    ),
                                    durationMs = duration.coerceIn(50L, 500L),
                                    delayBeforeMs = delayBefore
                                )
                            } else if (duration > 350L) {
                                // Thao tác Giữ (HOLD)
                                MacroAction(
                                    type = MacroType.HOLD,
                                    points = listOf(GesturePoint(downScreenX, downScreenY, touchDownTime)),
                                    durationMs = duration,
                                    delayBeforeMs = delayBefore
                                )
                            } else {
                                // Thao tác Chạm (TAP) chuẩn xác cho game
                                MacroAction(
                                    type = MacroType.TAP,
                                    points = listOf(GesturePoint(downScreenX, downScreenY, touchDownTime)),
                                    durationMs = 35L,
                                    delayBeforeMs = delayBefore
                                )
                            }

                            synchronized(recordedActions) {
                                recordedActions.add(action)
                            }
                            scope.launch(Dispatchers.Main) {
                                onActionRecorded?.invoke(action, recordedActions.size)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Recording stream interrupted: ${e.message}")
            } finally {
                stopRecording()
            }
        }
    }

    /**
     * Chuyển đổi tọa độ cảm ứng thô từ phần cứng sang tọa độ Pixel hiển thị màn hình
     * Tự động bù trừ khi xoay ngang (Landscape trong Liên Quân Mobile)
     */
    private fun mapRawToScreen(
        rawX: Float,
        rawY: Float,
        maxX: Int,
        maxY: Int,
        screenW: Int,
        screenH: Int,
        rotation: Int
    ): Pair<Float, Float> {
        val normX = (rawX / maxX.toFloat()).coerceIn(0f, 1f)
        val normY = (rawY / maxY.toFloat()).coerceIn(0f, 1f)

        return when (rotation) {
            Surface.ROTATION_90 -> {
                // Xoay ngang 90 độ (chuẩn game thủ cầm máy Liên Quân)
                val sx = normY * screenW
                val sy = (1f - normX) * screenH
                Pair(sx, sy)
            }
            Surface.ROTATION_270 -> {
                // Xoay ngang ngược 270 độ
                val sx = (1f - normY) * screenW
                val sy = normX * screenH
                Pair(sx, sy)
            }
            else -> {
                // Màn hình dọc
                val sx = normX * screenW
                val sy = normY * screenH
                Pair(sx, sy)
            }
        }
    }

    /**
     * Dừng phiên ghi và trả về chuỗi thao tác đã ghi nhận
     */
    fun stopRecording(): List<MacroAction> {
        isRecording = false
        try {
            activeProcess?.destroy()
            activeProcess = null
            recordingJob?.cancel()
            recordingJob = null
        } catch (_: Throwable) {}

        return synchronized(recordedActions) {
            ArrayList(recordedActions)
        }
    }

    /**
     * Hủy phiên ghi hiện tại
     */
    fun cancelRecording() {
        stopRecording()
        synchronized(recordedActions) {
            recordedActions.clear()
        }
    }

    private fun executeShellCommand(cmd: String): Process? {
        if (ShellExecutor.isShizukuRunning() && ShellExecutor.hasShizukuPermission()) {
            try {
                val method = try {
                    Shizuku::class.java.getMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                } catch (_: Throwable) {
                    Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                }
                method.isAccessible = true
                return method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? Process
            } catch (e: Throwable) {
                Log.w(TAG, "Shizuku process error: ${e.message}")
            }
        }

        if (ShellExecutor.isRootAvailable()) {
            try {
                return Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            } catch (e: Throwable) {
                Log.w(TAG, "Root process error: ${e.message}")
            }
        }

        return null
    }
}
