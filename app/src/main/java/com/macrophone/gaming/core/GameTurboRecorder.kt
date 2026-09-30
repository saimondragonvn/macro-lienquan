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
            var minX = 0
            var maxX = 1080
            var minY = 0
            var maxY = 2400

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("add device") && trimmed.contains("/dev/input/")) {
                    currentPath = trimmed.substringAfterLast(" ").trim()
                    hasMtPositionX = false
                    hasMtPositionY = false
                    minX = 0
                    maxX = 0
                    minY = 0
                    maxY = 0
                }
                // Tìm kiếm ABS_MT_POSITION_X (0035) và ABS_MT_POSITION_Y (0036)
                if (trimmed.contains("0035") || trimmed.contains("ABS_MT_POSITION_X")) {
                    hasMtPositionX = true
                    val maxVal = extractMaxVal(trimmed)
                    if (maxVal > 0) {
                        minX = extractMinVal(trimmed)
                        maxX = maxVal
                    }
                }
                if (trimmed.contains("0036") || trimmed.contains("ABS_MT_POSITION_Y")) {
                    hasMtPositionY = true
                    val maxVal = extractMaxVal(trimmed)
                    if (maxVal > 0) {
                        minY = extractMinVal(trimmed)
                        maxY = maxVal
                    }
                }

                if (hasMtPositionX && hasMtPositionY && currentPath.isNotEmpty()) {
                    val finalMinX = minX
                    val finalMaxX = if (maxX > 0) maxX else 1080
                    val finalMinY = minY
                    val finalMaxY = if (maxY > 0) maxY else 2400
                    touchDevice = TouchDevice(currentPath, finalMinX, finalMaxX, finalMinY, finalMaxY)
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

    private fun extractMinVal(line: String): Int {
        val minRegex = Regex("""min\s+(\d+)""")
        val match = minRegex.find(line)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
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
        val realMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(realMetrics)
        val screenW = realMetrics.widthPixels
        val screenH = realMetrics.heightPixels
        val density = realMetrics.density

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
                var hasReceivedCoord = false

                var isPendingDown = false
                var isFingerDown = false
                var isPendingUp = false

                var touchDownTime = 0L
                var lastTouchUpTime = 0L
                var downScreenX = 0f
                var downScreenY = 0f
                var latestScreenX = 0f
                var latestScreenY = 0f

                val devMinX = device?.minX ?: 0
                val devMaxX = device?.maxX ?: max(screenW, screenH)
                val devMinY = device?.minY ?: 0
                val devMaxY = device?.maxY ?: max(screenW, screenH)

                while (isActive && isRecording) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    val now = System.currentTimeMillis()

                    // 1. Nhận diện tọa độ X
                    if (trimmed.contains("ABS_MT_POSITION_X")) {
                        val hexStr = trimmed.split(Regex("\\s+")).last()
                        val raw = hexStr.toLongOrNull(16)?.toFloat()
                        if (raw != null) {
                            curRawX = raw
                            hasReceivedCoord = true
                        }
                    }

                    // 2. Nhận diện tọa độ Y
                    if (trimmed.contains("ABS_MT_POSITION_Y")) {
                        val hexStr = trimmed.split(Regex("\\s+")).last()
                        val raw = hexStr.toLongOrNull(16)?.toFloat()
                        if (raw != null) {
                            curRawY = raw
                            hasReceivedCoord = true
                        }
                    }

                    // 3. Nhận diện Chạm xuống (TOUCH DOWN)
                    val isDownSignal = (trimmed.contains("BTN_TOUCH") && (trimmed.contains("DOWN") || trimmed.endsWith("00000001") || trimmed.endsWith(" 1"))) ||
                            (trimmed.contains("ABS_MT_TRACKING_ID") && !trimmed.contains("ffffffff") && !trimmed.endsWith("-1"))
                    if (isDownSignal && !isFingerDown) {
                        isPendingDown = true
                        touchDownTime = now
                    }

                    // 4. Nhận diện Nhấc tay (TOUCH UP)
                    val isUpSignal = (trimmed.contains("BTN_TOUCH") && (trimmed.contains("UP") || trimmed.endsWith("00000000") || trimmed.endsWith(" 0"))) ||
                            (trimmed.contains("ABS_MT_TRACKING_ID") && (trimmed.contains("ffffffff") || trimmed.endsWith("-1")))
                    if (isUpSignal && (isFingerDown || isPendingDown)) {
                        isPendingUp = true
                    }

                    // 5. CHỐT FRAME CẢM ỨNG (SYN_REPORT) - KHẮC PHỤC TRIỆT ĐỂ LỖI LOẠN CẢM ỨNG
                    if (trimmed.contains("SYN_REPORT")) {
                        val curRotation = wm.defaultDisplay.rotation

                        if (isPendingDown && hasReceivedCoord) {
                            isPendingDown = false
                            isFingerDown = true
                            val (sx, sy) = mapRawToScreen(curRawX, curRawY, devMinX, devMaxX, devMinY, devMaxY, screenW, screenH, curRotation)
                            downScreenX = sx
                            downScreenY = sy
                            latestScreenX = sx
                            latestScreenY = sy
                        } else if (isFingerDown && hasReceivedCoord) {
                            val (sx, sy) = mapRawToScreen(curRawX, curRawY, devMinX, devMaxX, devMinY, devMaxY, screenW, screenH, curRotation)
                            latestScreenX = sx
                            latestScreenY = sy
                        }

                        if (isPendingUp && isFingerDown) {
                            isPendingUp = false
                            isFingerDown = false
                            val duration = max(35L, now - touchDownTime)
                            val delayBefore = if (lastTouchUpTime > 0L) {
                                max(30L, touchDownTime - lastTouchUpTime)
                            } else {
                                50L
                            }
                            lastTouchUpTime = now

                            val distance = hypot((latestScreenX - downScreenX).toDouble(), (latestScreenY - downScreenY).toDouble()).toFloat()

                            val action = if (distance > (25f * density)) {
                                // Thao tác Vuốt thực sự (SWIPE)
                                MacroAction(
                                    type = MacroType.SWIPE,
                                    points = listOf(
                                        GesturePoint(downScreenX, downScreenY, touchDownTime),
                                        GesturePoint(latestScreenX, latestScreenY, now)
                                    ),
                                    durationMs = duration.coerceIn(50L, 500L),
                                    delayBeforeMs = delayBefore
                                )
                            } else if (duration > 350L) {
                                // Thao tác Giữ (HOLD) tại điểm chạm
                                MacroAction(
                                    type = MacroType.HOLD,
                                    points = listOf(GesturePoint(downScreenX, downScreenY, touchDownTime)),
                                    durationMs = duration,
                                    delayBeforeMs = delayBefore
                                )
                            } else {
                                // Thao tác Chạm (TAP) - ĐÚNG 1 TỌA ĐỘ DUY NHẤT, KHÔNG BỊ TRƯỢT LỆCH!
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
     * Chuyển đổi tọa độ cảm ứng thô từ phần cứng sang tọa độ Pixel hiển thị màn hình game
     * Tự động nhận diện cạnh ngắn / cạnh dài và góc xoay ngang (Landscape Liên Quân Mobile)
     */
    private fun mapRawToScreen(
        rawX: Float,
        rawY: Float,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        screenW: Int,
        screenH: Int,
        rotation: Int
    ): Pair<Float, Float> {
        val spanX = max(1, maxX - minX).toFloat()
        val spanY = max(1, maxY - minY).toFloat()

        // Phân biệt cạnh ngắn (Width portrait) và cạnh dài (Height portrait) của cảm ứng phần cứng
        val isXShort = spanX <= spanY
        val rawShort = if (isXShort) (rawX - minX) else (rawY - minY)
        val rawLong = if (isXShort) (rawY - minY) else (rawX - minX)
        val spanShort = if (isXShort) spanX else spanY
        val spanLong = if (isXShort) spanY else spanX

        val normShort = (rawShort / spanShort).coerceIn(0f, 1f)
        val normLong = (rawLong / spanLong).coerceIn(0f, 1f)

        // Đảm bảo displayWidth là cạnh dài và displayHeight là cạnh ngắn khi trong game Liên Quân
        val dispW = max(screenW, screenH).toFloat()
        val dispH = min(screenW, screenH).toFloat()

        return when (rotation) {
            Surface.ROTATION_90 -> {
                // Game thủ xoay ngang (camera/loa bên tay trái - mặc định Liên Quân)
                val sx = normLong * dispW
                val sy = (1f - normShort) * dispH
                Pair(sx, sy)
            }
            Surface.ROTATION_270 -> {
                // Game thủ xoay ngang ngược (camera bên tay phải)
                val sx = (1f - normLong) * dispW
                val sy = normShort * dispH
                Pair(sx, sy)
            }
            Surface.ROTATION_180 -> {
                val sx = (1f - normShort) * dispH
                val sy = (1f - normLong) * dispW
                Pair(sx, sy)
            }
            else -> {
                // Màn hình dọc
                val sx = normShort * dispH
                val sy = normLong * dispW
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
