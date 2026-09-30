package com.macrophone.gaming.service

import android.animation.ValueAnimator
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.macrophone.gaming.R
import com.macrophone.gaming.core.GameTurboRecorder
import com.macrophone.gaming.core.ShellExecutor
import com.macrophone.gaming.data.MacroConfigStorage
import com.macrophone.gaming.data.SavedMacroTrigger
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroState
import com.macrophone.gaming.ui.MainActivity
import com.macrophone.gaming.ui.overlay.FloatingTriggerView
import com.macrophone.gaming.ui.overlay.TargetPinView
import com.macrophone.gaming.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Service Cửa Sổ Nổi Game Turbo (Kiến trúc v2.1.0 - Chuẩn Xiaomi / Redmi Turbo 4 Pro):
 * - TÍNH NĂNG GHI COMBO THỰC TẾ (REDMI TURBO STYLE):
 *   Đọc trực tiếp luồng cảm ứng Linux kernel (/dev/input/event*) ngầm qua Shizuku/Root.
 *   Game thủ mở Shop, mua bán đồ, tung chiêu trong Liên Quân hoàn toàn bình thường 100%!
 *   Bấm [Xong & Lưu] là có ngay nút Combo nổi trên màn hình.
 * - TÍNH NĂNG GHIM ĐIỂM THỦ CÔNG (TARGET PINS):
 *   Cho phép đặt các điểm ghim ①, ②, ③ trực tiếp lên nút game.
 * - QUẢN LÝ COMBO & CHỐNG SPAM NÚT:
 *   Nút [XÓA TOÀN BỘ NÚT] 1-chạm, quản lý từng nút, xóa vĩnh viễn hoặc sửa lại.
 * - NÚT NỔI COMBO [⚡ C1], [⚡ R1]:
 *   Chạm nhanh là xả combo siêu tốc 120Hz ngay lập tức.
 */
class TurboOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var configStorage: MacroConfigStorage
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var screenWidth = 0
    private var screenHeight = 0

    // Views của Thanh Điều Khiển Game Turbo
    private var dockView: View? = null
    private lateinit var dockParams: WindowManager.LayoutParams

    // View Thanh trạng thái ghi combo (Dynamic Island / Top Pill)
    private var recordingPillView: View? = null
    private var recordingTimerJob: Job? = null
    private var recordingSeconds = 0

    // Danh sách các Điểm Ghim Chiêu đang hiển thị trên màn hình
    private val targetPins = mutableListOf<TargetPinView>()

    // Danh sách các Nút Tròn Turbo độc lập trên màn hình
    private val triggerButtons = mutableListOf<FloatingTriggerView>()

    private var selectedSpeedDelay: Long = 30
    private var isOverlayHidden = false
    private var isMenuOpen = false

    private var tvEngineStatusBadge: TextView? = null
    private var btnGlobalOpacity30: TextView? = null
    private var btnGlobalOpacity50: TextView? = null
    private var btnGlobalOpacity80: TextView? = null
    private var btnGlobalOpacity100: TextView? = null

    companion object {
        const val ACTION_START = "com.macrophone.gaming.TURBO_START"
        const val ACTION_STOP = "com.macrophone.gaming.TURBO_STOP"
        const val ACTION_OPEN_MENU = "com.macrophone.gaming.ACTION_OPEN_MENU"
        const val ACTION_TOGGLE_VISIBILITY = "com.macrophone.gaming.ACTION_TOGGLE_VISIBILITY"
        const val ACTION_START_RECORD = "com.macrophone.gaming.ACTION_START_RECORD"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, TurboOverlayService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TurboOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun openMenu(context: Context) {
            val intent = Intent(context, TurboOverlayService::class.java).apply {
                action = ACTION_OPEN_MENU
            }
            context.startService(intent)
        }

        fun toggleVisibility(context: Context) {
            val intent = Intent(context, TurboOverlayService::class.java).apply {
                action = ACTION_TOGGLE_VISIBILITY
            }
            context.startService(intent)
        }

        fun startRecord(context: Context) {
            val intent = Intent(context, TurboOverlayService::class.java).apply {
                action = ACTION_START_RECORD
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        configStorage = MacroConfigStorage(this)
        updateScreenDimensions()

        startForegroundSafe()
        initDockView()
        loadSavedTriggers()
        GameTurboRecorder.prewarm(scope)
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_OPEN_MENU -> {
                toggleTurboMenu()
                return START_STICKY
            }
            ACTION_TOGGLE_VISIBILITY -> {
                toggleOverlayVisibility()
                return START_STICKY
            }
            ACTION_START_RECORD -> {
                startRealtimeComboRecording()
                return START_STICKY
            }
        }
        updateEngineBadge()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateScreenDimensions()
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()

        cancelRealtimeComboRecording()
        clearAllTargetPins()
        clearAllTriggerButtons()
        removeViewSafely(dockView)

        super.onDestroy()
    }

    private fun updateScreenDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            screenWidth = metrics.bounds.width()
            screenHeight = metrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }
    }

    private fun startForegroundSafe() {
        try {
            NotificationHelper.createNotificationChannel(this)
            val notification = NotificationHelper.buildNotification(
                context = this,
                isButtonsHidden = isOverlayHidden,
                isRecording = false
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    androidx.core.app.ServiceCompat.startForeground(
                        this,
                        NotificationHelper.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: Throwable) {
                    Log.w("TurboOverlayService", "startForeground with type failed: ${e.message}")
                    try {
                        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
                    } catch (e2: Throwable) {
                        Log.e("TurboOverlayService", "startForeground fallback failed: ${e2.message}")
                    }
                }
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e("TurboOverlayService", "startForegroundSafe: ${e.message}")
        }
    }

    private fun updateNotification(activeComboName: String? = null) {
        try {
            val notification = NotificationHelper.buildNotification(
                context = this,
                isButtonsHidden = isOverlayHidden,
                isRecording = GameTurboRecorder.isRecording,
                activeComboName = activeComboName,
                isMenuOpen = isMenuOpen
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            Log.w("TurboOverlayService", "updateNotification failed: ${e.message}")
        }
    }

    /**
     * Khởi tạo Giao diện Game Turbo Control Panel (Mặc định ẨN HOÀN TOÀN, không để lại icon mép)
     */
    private fun initDockView() {
        try {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_MacroGaming)
            val inflater = LayoutInflater.from(themedContext)
            dockView = inflater.inflate(R.layout.view_floating_dock, null)

            dockParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            (dockView as? android.view.ViewGroup)?.isMotionEventSplittingEnabled = true
            setupDockTouchAndDrag(dockView!!)
            setupDockButtons(dockView!!)
            // Không addView vào windowManager ngay lúc khởi động, chỉ add khi mở menu!
        } catch (e: Throwable) {
            Log.e("TurboOverlayService", "initDockView error: ${e.message}", e)
            Toast.makeText(this, "Chưa cấp quyền 'Hiển thị trên ứng dụng khác'!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupDockTouchAndDrag(root: View) {
        val ivExpandedDrag = root.findViewById<View>(R.id.ivExpandedDrag)

        val createDrag = { targetView: View ->
            object : View.OnTouchListener {
                private var startX = 0
                private var startY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isDragging = false
                private var downTime = 0L

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = dockParams.x
                            startY = dockParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            downTime = System.currentTimeMillis()
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()

                            if (!isDragging && (abs(dx) > 10 || abs(dy) > 10)) {
                                isDragging = true
                            }
                            if (isDragging) {
                                dockParams.x = startX + dx
                                dockParams.y = startY + dy
                                updateViewSafely(dockView, dockParams)
                            }
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (!isDragging && (System.currentTimeMillis() - downTime < 350)) {
                                targetView.performClick()
                            }
                            isDragging = false
                            return true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            isDragging = false
                            return true
                        }
                    }
                    return false
                }
            }
        }

        // CHỈ gán kéo panel cho icon kéo thả, KHÔNG gán lên toàn bộ header để tránh nuốt click nút Đóng
        ivExpandedDrag?.setOnTouchListener(createDrag(ivExpandedDrag))
    }

    fun openTurboMenu() {
        if (isMenuOpen && dockView?.isAttachedToWindow == true) return
        isMenuOpen = true

        if (isOverlayHidden) {
            isOverlayHidden = false
            triggerButtons.forEach { it.view.visibility = View.VISIBLE }
            targetPins.forEach { it.view.visibility = View.VISIBLE }
        }

        dockView?.findViewById<View>(R.id.layoutExpanded)?.visibility = View.VISIBLE
        dockView?.findViewById<View>(R.id.layoutCollapsed)?.visibility = View.GONE

        updateEngineBadge()
        renderAssignedTriggersList()

        try {
            if (dockView != null) {
                if (dockView?.isAttachedToWindow != true) {
                    windowManager.addView(dockView, dockParams)
                } else {
                    windowManager.updateViewLayout(dockView, dockParams)
                }
            }
        } catch (e: Throwable) {
            Log.e("TurboOverlayService", "openTurboMenu error: ${e.message}", e)
        }

        updateNotification()

        try {
            @Suppress("DEPRECATION")
            val closeDialogs = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialogs)
        } catch (_: Throwable) {}
    }

    fun closeTurboMenu() {
        isMenuOpen = false
        try {
            if (dockView?.isAttachedToWindow == true) {
                windowManager.removeViewImmediate(dockView)
            }
        } catch (_: Throwable) {
            try {
                windowManager.removeView(dockView)
            } catch (_: Throwable) {}
        }
        updateNotification()
    }

    fun toggleTurboMenu() {
        if (isMenuOpen && dockView?.isAttachedToWindow == true) {
            closeTurboMenu()
        } else {
            openTurboMenu()
        }
    }

    private fun updateEngineBadge() {
        tvEngineStatusBadge = dockView?.findViewById(R.id.tvEngineStatusBadge)
        if (ShellExecutor.isEngineReady()) {
            tvEngineStatusBadge?.text = "🟢 Shizuku 120Hz: SẴN SÀNG"
            tvEngineStatusBadge?.setTextColor(ContextCompat.getColor(this, R.color.emerald_play))
        } else {
            tvEngineStatusBadge?.text = "🔴 Shizuku: CHƯA CẤP (BẤM ĐÂY)"
            tvEngineStatusBadge?.setTextColor(ContextCompat.getColor(this, R.color.amber_warning))
        }
    }

    private fun setupDockButtons(root: View) {
        val layoutCollapsed = root.findViewById<View>(R.id.layoutCollapsed)
        val layoutExpanded = root.findViewById<View>(R.id.layoutExpanded)
        val btnCollapseDock = root.findViewById<View>(R.id.btnCollapseDock)
        val btnBottomCollapse = root.findViewById<View>(R.id.btnBottomCollapse)

        val btnLaunchGameFromDock = root.findViewById<View>(R.id.btnLaunchGameFromDock)
        tvEngineStatusBadge = root.findViewById(R.id.tvEngineStatusBadge)

        val btnStartKernelRecord = root.findViewById<View>(R.id.btnStartKernelRecord)

        val btnAddPoint = root.findViewById<View>(R.id.btnAddPoint)
        val btnRemovePoint = root.findViewById<View>(R.id.btnRemovePoint)
        val btnPlayTest = root.findViewById<View>(R.id.btnPlayTest)
        val btnCreateMacroBtn = root.findViewById<View>(R.id.btnCreateMacroBtn)

        val btnSpeedFast = root.findViewById<TextView>(R.id.btnSpeedFast)
        val btnSpeedNormal = root.findViewById<TextView>(R.id.btnSpeedNormal)
        val btnSpeedSlow = root.findViewById<TextView>(R.id.btnSpeedSlow)

        btnGlobalOpacity30 = root.findViewById(R.id.btnGlobalOpacity30)
        btnGlobalOpacity50 = root.findViewById(R.id.btnGlobalOpacity50)
        btnGlobalOpacity80 = root.findViewById(R.id.btnGlobalOpacity80)
        btnGlobalOpacity100 = root.findViewById(R.id.btnGlobalOpacity100)

        val btnRefreshTriggers = root.findViewById<View>(R.id.btnRefreshTriggers)
        val btnClearAllTriggers = root.findViewById<View>(R.id.btnClearAllTriggers)

        val btnToggleVisibility = root.findViewById<View>(R.id.btnToggleVisibility)
        val btnClearPoints = root.findViewById<View>(R.id.btnClearPoints)
        val btnCloseService = root.findViewById<View>(R.id.btnCloseService)

        val dockRoot = root.findViewById<View>(R.id.dockRoot)

        // Bấm ra ngoài vùng hộp thoại (vào nền mờ) để đóng menu tức thì
        dockRoot?.setOnClickListener {
            closeTurboMenu()
        }

        // Chặn sự kiện click xuyên qua bảng menu để không kích hoạt đóng ngoài ý muốn khi thao tác bên trong menu
        layoutExpanded?.setOnClickListener {
            // Chặn click
        }

        // 0. Mở Liên Quân Mobile nhanh từ Game Turbo Dock
        btnLaunchGameFromDock?.setOnClickListener {
            launchLienQuanGame()
            closeTurboMenu()
        }

        // Bấm vào Đèn LED trạng thái Shizuku để mở màn hình cấp quyền
        tvEngineStatusBadge?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            closeTurboMenu()
        }

        // 🔴 BẮT ĐẦU GHI COMBO THỜI GIAN THỰC (CHUẨN REDMI GAME TURBO)
        btnStartKernelRecord?.setOnClickListener {
            startRealtimeComboRecording()
        }

        // 1. Mở rộng khi chạm vào tab mép (nếu có kích hoạt)
        layoutCollapsed?.setOnClickListener {
            openTurboMenu()
        }

        // 2. Đóng hoàn toàn menu Turbo
        btnCollapseDock?.setOnClickListener { closeTurboMenu() }
        btnBottomCollapse?.setOnClickListener { closeTurboMenu() }

        // 3. Tùy chọn tốc độ xả combo
        val updateSpeedUI = {
            btnSpeedFast?.setBackgroundResource(if (selectedSpeedDelay <= 35) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
            btnSpeedFast?.setTextColor(ContextCompat.getColor(this, if (selectedSpeedDelay <= 35) R.color.bg_dark else R.color.text_primary))

            btnSpeedNormal?.setBackgroundResource(if (selectedSpeedDelay in 36..80) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
            btnSpeedNormal?.setTextColor(ContextCompat.getColor(this, if (selectedSpeedDelay in 36..80) R.color.bg_dark else R.color.text_primary))

            btnSpeedSlow?.setBackgroundResource(if (selectedSpeedDelay > 80) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
            btnSpeedSlow?.setTextColor(ContextCompat.getColor(this, if (selectedSpeedDelay > 80) R.color.bg_dark else R.color.text_primary))
        }

        updateSpeedUI()

        btnSpeedFast?.setOnClickListener {
            selectedSpeedDelay = 30
            updateSpeedUI()
        }
        btnSpeedNormal?.setOnClickListener {
            selectedSpeedDelay = 60
            updateSpeedUI()
        }
        btnSpeedSlow?.setOnClickListener {
            selectedSpeedDelay = 120
            updateSpeedUI()
        }

        // 4. Chỉnh Độ mờ toàn cục (Opacity)
        updateGlobalOpacityUI()
        btnGlobalOpacity30?.setOnClickListener { setGlobalOpacity(30) }
        btnGlobalOpacity50?.setOnClickListener { setGlobalOpacity(50) }
        btnGlobalOpacity80?.setOnClickListener { setGlobalOpacity(80) }
        btnGlobalOpacity100?.setOnClickListener { setGlobalOpacity(100) }

        // 5. Thêm Điểm Ghim ①, ②, ③...
        btnAddPoint?.setOnClickListener {
            addNewTargetPin()
        }

        // 6. Bớt Điểm Ghim cuối
        btnRemovePoint?.setOnClickListener {
            removeLastTargetPin()
        }

        // 7. Thử chạy ngay chuỗi điểm ghim
        btnPlayTest?.setOnClickListener {
            executeTargetPinsNow()
        }

        // 8. 🚀 TẠO NÚT BẤM TURBO ĐỘC LẬP TỪ CÁC ĐIỂM GHIM
        btnCreateMacroBtn?.setOnClickListener {
            createTriggerButtonFromPins()
        }

        // 9. Nạp lại danh sách nút macro
        btnRefreshTriggers?.setOnClickListener {
            renderAssignedTriggersList()
            Toast.makeText(this, "Đã làm mới danh sách nút macro", Toast.LENGTH_SHORT).show()
        }

        // Xóa toàn bộ nút đã tạo (chống spam nút)
        btnClearAllTriggers?.setOnClickListener {
            clearAllMacroTriggers()
        }

        // 11. 👁️ Ẩn/Hiện toàn bộ nút
        btnToggleVisibility?.setOnClickListener {
            toggleOverlayVisibility()
        }

        // 12. Xóa hết điểm ghim
        btnClearPoints?.setOnClickListener {
            clearAllTargetPins()
            Toast.makeText(this, "Đã xóa toàn bộ điểm ghim", Toast.LENGTH_SHORT).show()
        }

        // 13. Tắt Turbo Service
        btnCloseService?.setOnClickListener {
            stopSelf()
        }
    }

    /**
     * Bắt đầu phiên ghi combo thời gian thực (Chuẩn Redmi Turbo 4 Pro)
     * Toàn bộ màn hình game thông thoáng 100%, thao tác trực tiếp vào Liên Quân!
     */
    private fun startRealtimeComboRecording() {
        if (!ShellExecutor.isEngineReady()) {
            Toast.makeText(this, "⚠️ Động cơ Shizuku chưa kích hoạt! Hãy mở app cấp quyền Shizuku trước.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            closeTurboMenu()
            return
        }

        closeTurboMenu()

        try {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_MacroGaming)
            recordingPillView = LayoutInflater.from(themedContext).inflate(R.layout.view_recording_pill, null)

            val pillParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 12
            }

            val tvTimer = recordingPillView!!.findViewById<TextView>(R.id.tvRecordingTimer)
            val tvCount = recordingPillView!!.findViewById<TextView>(R.id.tvRecordingActionCount)
            val btnStop = recordingPillView!!.findViewById<View>(R.id.btnStopRecordingPill)
            val btnCancel = recordingPillView!!.findViewById<View>(R.id.btnCancelRecordingPill)

            btnStop.setOnClickListener {
                stopRealtimeComboRecording()
            }

            btnCancel.setOnClickListener {
                cancelRealtimeComboRecording()
            }

            recordingSeconds = 0
            recordingTimerJob?.cancel()
            recordingTimerJob = scope.launch(Dispatchers.Main) {
                while (GameTurboRecorder.isRecording) {
                    delay(1000)
                    recordingSeconds++
                    val mins = recordingSeconds / 60
                    val secs = recordingSeconds % 60
                    tvTimer.text = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
                }
            }

            windowManager.addView(recordingPillView, pillParams)

            GameTurboRecorder.startRecording(
                context = this,
                scope = scope,
                onActionRecorded = { _, totalCount ->
                    tvCount.text = "Đã ghi nhận: $totalCount thao tác vào game!"
                },
                onStarted = {
                    updateNotification()
                    Toast.makeText(this, "🔴 Đang ghi! Bạn hãy vào game bấm Shop/Chiêu bình thường.", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(this, "Lỗi: $err", Toast.LENGTH_LONG).show()
                    cancelRealtimeComboRecording()
                }
            )
        } catch (e: Throwable) {
            Log.e("TurboOverlayService", "startRealtimeComboRecording error: ${e.message}")
            cancelRealtimeComboRecording()
        }
    }

    /**
     * Dừng phiên ghi và mở hộp thoại cho phép nhập tên Combo và chọn tốc độ
     */
    private fun stopRealtimeComboRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        removeViewSafely(recordingPillView)
        recordingPillView = null

        val actions = GameTurboRecorder.stopRecording()
        updateNotification()

        if (actions.isEmpty()) {
            Toast.makeText(this, "Chưa ghi nhận thao tác nào! Hãy chạm vào game trong lúc ghi.", Toast.LENGTH_SHORT).show()
            return
        }

        if (triggerButtons.size >= 5) {
            Toast.makeText(this, "Đã đạt tối đa 5 nút combo! Hãy xóa bớt nút cũ trước.", Toast.LENGTH_LONG).show()
            return
        }

        showSaveComboDialog(actions)
    }

    private fun showSaveComboDialog(actions: List<MacroAction>) {
        try {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_MacroGaming)
            val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.view_save_combo_dialog, null)

            val dialogParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val tvSummary = dialogView.findViewById<TextView>(R.id.tvSaveComboSummary)
            val etName = dialogView.findViewById<android.widget.EditText>(R.id.etSaveComboName)
            val tvSpeedLiveBadge = dialogView.findViewById<TextView>(R.id.tvSpeedLiveBadge)
            val etSaveSpeedMultiplier = dialogView.findViewById<android.widget.EditText>(R.id.etSaveSpeedMultiplier)
            val btnSaveSpeed1 = dialogView.findViewById<TextView>(R.id.btnSaveSpeed1)
            val btnSaveSpeed2 = dialogView.findViewById<TextView>(R.id.btnSaveSpeed2)
            val btnSaveSpeed5 = dialogView.findViewById<TextView>(R.id.btnSaveSpeed5)
            val btnSaveSpeed10 = dialogView.findViewById<TextView>(R.id.btnSaveSpeed10)
            val btnSaveSpeed20 = dialogView.findViewById<TextView>(R.id.btnSaveSpeed20)
            val btnConfirm = dialogView.findViewById<TextView>(R.id.btnSaveComboConfirm)
            val btnCancel = dialogView.findViewById<TextView>(R.id.btnSaveComboCancel)

            val defaultName = "C${triggerButtons.size + 1}"
            etName.setText(defaultName)
            etName.selectAll()
            tvSummary.text = "Đã ghi nhận ${actions.size} thao tác chuẩn xác"

            var chosenSpeed = 1.0f
            val updateSpeedBtns = {
                val formatted = if (chosenSpeed % 1f == 0f) "${chosenSpeed.toInt()}" else "$chosenSpeed"
                tvSpeedLiveBadge?.text = "x$formatted"

                val is1 = chosenSpeed == 1.0f
                val is2 = chosenSpeed == 2.0f
                val is5 = chosenSpeed == 5.0f
                val is10 = chosenSpeed == 10.0f
                val is20 = chosenSpeed == 20.0f

                btnSaveSpeed1?.setBackgroundResource(if (is1) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnSaveSpeed1?.setTextColor(ContextCompat.getColor(this, if (is1) R.color.bg_dark else R.color.text_primary))

                btnSaveSpeed2?.setBackgroundResource(if (is2) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnSaveSpeed2?.setTextColor(ContextCompat.getColor(this, if (is2) R.color.bg_dark else R.color.text_primary))

                btnSaveSpeed5?.setBackgroundResource(if (is5) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnSaveSpeed5?.setTextColor(ContextCompat.getColor(this, if (is5) R.color.bg_dark else R.color.text_primary))

                btnSaveSpeed10?.setBackgroundResource(if (is10) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnSaveSpeed10?.setTextColor(ContextCompat.getColor(this, if (is10) R.color.bg_dark else R.color.text_primary))

                btnSaveSpeed20?.setBackgroundResource(if (is20) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
                btnSaveSpeed20?.setTextColor(ContextCompat.getColor(this, if (is20) R.color.bg_dark else R.color.text_primary))
            }

            etSaveSpeedMultiplier?.setText("1")
            updateSpeedBtns()

            etSaveSpeedMultiplier?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val num = s?.toString()?.toFloatOrNull()
                    if (num != null && num > 0) {
                        chosenSpeed = num.coerceIn(0.5f, 20.0f)
                        val formatted = if (chosenSpeed % 1f == 0f) "${chosenSpeed.toInt()}" else "$chosenSpeed"
                        tvSpeedLiveBadge?.text = "x$formatted"
                    }
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            btnSaveSpeed1?.setOnClickListener { chosenSpeed = 1.0f; etSaveSpeedMultiplier?.setText("1"); updateSpeedBtns() }
            btnSaveSpeed2?.setOnClickListener { chosenSpeed = 2.0f; etSaveSpeedMultiplier?.setText("2"); updateSpeedBtns() }
            btnSaveSpeed5?.setOnClickListener { chosenSpeed = 5.0f; etSaveSpeedMultiplier?.setText("5"); updateSpeedBtns() }
            btnSaveSpeed10?.setOnClickListener { chosenSpeed = 10.0f; etSaveSpeedMultiplier?.setText("10"); updateSpeedBtns() }
            btnSaveSpeed20?.setOnClickListener { chosenSpeed = 20.0f; etSaveSpeedMultiplier?.setText("20"); updateSpeedBtns() }

            val closeDialog = {
                try {
                    if (dialogView.isAttachedToWindow) {
                        windowManager.removeViewImmediate(dialogView)
                    } else {
                        windowManager.removeView(dialogView)
                    }
                } catch (_: Throwable) {}
            }

            btnConfirm.setOnClickListener {
                val enteredName = etName.text?.toString()?.trim() ?: ""
                val comboName = if (enteredName.isNotEmpty()) enteredName else defaultName

                val enteredSpeed = etSaveSpeedMultiplier?.text?.toString()?.toFloatOrNull() ?: chosenSpeed
                val finalSpeed = enteredSpeed.coerceIn(0.5f, 20.0f)

                val offset = (triggerButtons.size * 65)
                // Đặt mặc định ở vùng ngón tay thuận tiện, người dùng có thể tự do kéo tới mọi vị trí kể cả giữa màn hình
                val btnX = (screenWidth * 0.70f).toInt().coerceAtLeast(60)
                val btnY = ((screenHeight / 3) + offset).coerceIn(100, (screenHeight - 160).coerceAtLeast(100))

                val triggerBtn = createFloatingTriggerInstance(
                    id = java.util.UUID.randomUUID().toString(),
                    name = comboName,
                    actions = actions,
                    posX = btnX,
                    posY = btnY,
                    speedMultiplier = finalSpeed,
                    opacity = configStorage.globalOpacity
                )
                triggerButtons.add(triggerBtn)

                configStorage.upsertTrigger(
                    SavedMacroTrigger(
                        id = triggerBtn.id,
                        name = comboName,
                        x = btnX,
                        y = btnY,
                        delayBetweenMs = 40,
                        repeatCount = 1,
                        opacityPercent = configStorage.globalOpacity,
                        actions = actions,
                        speedMultiplier = finalSpeed
                    )
                )

                renderAssignedTriggersList()
                closeDialog()
                Toast.makeText(this, "✅ Đã lưu Combo [$comboName] ($finalSpeed x tốc độ)! Chạm nút nổi trên màn hình để xả combo!", Toast.LENGTH_LONG).show()
            }

            btnCancel.setOnClickListener {
                closeDialog()
                Toast.makeText(this, "Đã hủy lưu combo", Toast.LENGTH_SHORT).show()
            }

            windowManager.addView(dialogView, dialogParams)
        } catch (e: Throwable) {
            Log.e("TurboOverlayService", "showSaveComboDialog error: ${e.message}")
        }
    }

    private fun cancelRealtimeComboRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        removeViewSafely(recordingPillView)
        recordingPillView = null
        GameTurboRecorder.cancelRecording()
        updateNotification()
        Toast.makeText(this, "Đã hủy ghi combo", Toast.LENGTH_SHORT).show()
    }

    /**
     * Mở game Liên Quân Mobile tự động
     */
    private fun launchLienQuanGame() {
        val packages = listOf(
            "com.garena.game.kgvn",         // Liên Quân Mobile Garena VN
            "com.levelinfinite.sgameGlobal", // Honor of Kings / AoV Global
            "com.garena.game.kgtw",         // AoV Đài Loan
            "com.garena.game.kgth"          // AoV Thái Lan
        )
        val pm = packageManager
        for (pkg in packages) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                configStorage.lastGamePackage = pkg
                Toast.makeText(this, "🚀 Đang mở Liên Quân Mobile...", Toast.LENGTH_SHORT).show()
                return
            }
        }

        Toast.makeText(this, "Đang mở Google Play...", Toast.LENGTH_LONG).show()
        try {
            val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.garena.game.kgvn")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(playIntent)
        } catch (_: Throwable) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.garena.game.kgvn")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(webIntent)
        }
    }

    /**
     * Cập nhật độ mờ toàn cục cho toàn bộ nút trên màn hình và lưu cấu hình
     */
    private fun setGlobalOpacity(percent: Int) {
        configStorage.globalOpacity = percent
        triggerButtons.forEach { btn ->
            btn.updateOpacity(percent)
            configStorage.upsertTrigger(
                SavedMacroTrigger(
                    id = btn.id,
                    name = btn.name,
                    x = btn.params.x,
                    y = btn.params.y,
                    delayBetweenMs = btn.delayBetweenMs,
                    repeatCount = btn.repeatCount,
                    opacityPercent = percent,
                    points = btn.points,
                    actions = btn.actions,
                    speedMultiplier = btn.speedMultiplier
                )
            )
        }
        updateGlobalOpacityUI()
        renderAssignedTriggersList()
        Toast.makeText(this, "👻 Đã chỉnh độ mờ: $percent%", Toast.LENGTH_SHORT).show()
    }

    private fun updateGlobalOpacityUI() {
        val current = configStorage.globalOpacity
        val is30 = current <= 35
        val is50 = current in 36..65
        val is80 = current in 66..85
        val is100 = current > 85

        btnGlobalOpacity30?.setBackgroundResource(if (is30) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
        btnGlobalOpacity30?.setTextColor(ContextCompat.getColor(this, if (is30) R.color.bg_dark else R.color.text_primary))

        btnGlobalOpacity50?.setBackgroundResource(if (is50) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
        btnGlobalOpacity50?.setTextColor(ContextCompat.getColor(this, if (is50) R.color.bg_dark else R.color.text_primary))

        btnGlobalOpacity80?.setBackgroundResource(if (is80) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
        btnGlobalOpacity80?.setTextColor(ContextCompat.getColor(this, if (is80) R.color.bg_dark else R.color.text_primary))

        btnGlobalOpacity100?.setBackgroundResource(if (is100) R.drawable.bg_turbo_btn_primary else R.drawable.bg_turbo_btn_secondary)
        btnGlobalOpacity100?.setTextColor(ContextCompat.getColor(this, if (is100) R.color.bg_dark else R.color.text_primary))
    }

    /**
     * Khôi phục toàn bộ các nút Macro đã lưu từ bộ nhớ vĩnh viễn khi khởi động service
     */
    private fun loadSavedTriggers() {
        val savedList = configStorage.loadAllTriggers()
        if (savedList.isEmpty()) return

        val maxDim = Math.max(screenWidth, screenHeight).coerceAtLeast(1920)

        savedList.take(5).forEach { saved ->
            if (saved.points.isNotEmpty() || saved.actions.isNotEmpty()) {
                val posX = saved.x.coerceIn(0, maxDim)
                val posY = saved.y.coerceIn(0, maxDim)
                val btn = createFloatingTriggerInstance(
                    id = saved.id,
                    name = saved.name,
                    points = saved.points,
                    actions = saved.actions,
                    posX = posX,
                    posY = posY,
                    delay = saved.delayBetweenMs,
                    repeat = saved.repeatCount,
                    opacity = saved.opacityPercent,
                    speedMultiplier = saved.speedMultiplier.coerceIn(0.5f, 20.0f)
                )
                triggerButtons.add(btn)
            }
        }
        renderAssignedTriggersList()
    }

    /**
     * Tạo một thực thể FloatingTriggerView với đầy đủ callback đồng bộ bộ nhớ và xóa
     */
    private fun createFloatingTriggerInstance(
        id: String,
        name: String,
        points: List<Pair<Float, Float>> = emptyList(),
        actions: List<MacroAction> = emptyList(),
        posX: Int,
        posY: Int,
        delay: Long = 40,
        repeat: Int = 1,
        opacity: Int = 85,
        speedMultiplier: Float = 1.0f
    ): FloatingTriggerView {
        lateinit var triggerBtn: FloatingTriggerView

        triggerBtn = FloatingTriggerView(
            context = this,
            windowManager = windowManager,
            id = id,
            name = name,
            points = points,
            actions = actions,
            initialX = posX,
            initialY = posY,
            delayBetweenMs = delay,
            repeatCount = repeat,
            opacityPercent = opacity,
            speedMultiplier = speedMultiplier,
            onTrigger = { btn ->
                if (!ShellExecutor.isEngineReady()) {
                    Toast.makeText(this@TurboOverlayService, "⚠️ Động cơ Shizuku chưa bật! Hãy mở app cấp quyền Shizuku.", Toast.LENGTH_LONG).show()
                } else {
                    scope.launch(Dispatchers.IO) {
                        val ok = if (btn.actions.isNotEmpty()) {
                            ShellExecutor.executeActions(btn.actions, speedMultiplier = btn.speedMultiplier, repeatCount = btn.repeatCount)
                        } else {
                            ShellExecutor.executeCombo(btn.points, delayBetweenMs = btn.delayBetweenMs, repeatCount = btn.repeatCount)
                        }
                        if (!ok) {
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(this@TurboOverlayService, "⚠️ Không thể click tự động! Kiểm tra lại quyền Shizuku.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onRestorePins = { restoredPoints ->
                val pts = ArrayList(restoredPoints)
                triggerBtn.destroy()
                triggerButtons.remove(triggerBtn)
                configStorage.deleteTrigger(triggerBtn.id)
                restorePinsFromCombo(pts)
                renderAssignedTriggersList()
                Toast.makeText(this@TurboOverlayService, "🎯 Đã bung ${pts.size} điểm ghim lên game! Bạn hãy kéo đặt vào vị trí chiêu.", Toast.LENGTH_SHORT).show()
            },
            onPositionChanged = { btn ->
                configStorage.upsertTrigger(
                    SavedMacroTrigger(
                        id = btn.id,
                        name = btn.name,
                        x = btn.params.x,
                        y = btn.params.y,
                        delayBetweenMs = btn.delayBetweenMs,
                        repeatCount = btn.repeatCount,
                        opacityPercent = btn.opacityPercent,
                        points = btn.points,
                        actions = btn.actions,
                        speedMultiplier = btn.speedMultiplier
                    )
                )
            },
            onConfigChanged = { btn ->
                configStorage.upsertTrigger(
                    SavedMacroTrigger(
                        id = btn.id,
                        name = btn.name,
                        x = btn.params.x,
                        y = btn.params.y,
                        delayBetweenMs = btn.delayBetweenMs,
                        repeatCount = btn.repeatCount,
                        opacityPercent = btn.opacityPercent,
                        points = btn.points,
                        actions = btn.actions,
                        speedMultiplier = btn.speedMultiplier
                    )
                )
                renderAssignedTriggersList()
            },
            onDelete = { btn ->
                triggerButtons.remove(btn)
                configStorage.deleteTrigger(btn.id)
                renderAssignedTriggersList()
                Toast.makeText(this@TurboOverlayService, "🗑️ Đã xóa nút [${btn.name}]", Toast.LENGTH_SHORT).show()
            }
        )

        return triggerBtn
    }

    /**
     * Hiển thị danh sách các nút Macro đã gán và cung cấp nút xóa từng nút
     */
    private fun renderAssignedTriggersList() {
        val container = dockView?.findViewById<LinearLayout>(R.id.layoutAssignedTriggers) ?: return
        val tvEmpty = dockView?.findViewById<TextView>(R.id.tvNoTriggers)

        container.removeAllViews()

        if (triggerButtons.isEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            return
        }

        tvEmpty?.visibility = View.GONE
        val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_MacroGaming)
        val inflater = LayoutInflater.from(themedContext)

        triggerButtons.forEach { btn ->
            val itemView = inflater.inflate(R.layout.item_assigned_trigger, container, false)
            val tvBadge = itemView.findViewById<TextView>(R.id.tvTriggerBadge)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvTriggerTitle)
            val tvSub = itemView.findViewById<TextView>(R.id.tvTriggerSub)
            val btnRename = itemView.findViewById<TextView>(R.id.btnTriggerRename)
            val btnToggle = itemView.findViewById<TextView>(R.id.btnTriggerToggle)
            val btnRestore = itemView.findViewById<TextView>(R.id.btnTriggerRestorePins)
            val btnDelete = itemView.findViewById<TextView>(R.id.btnTriggerDelete)

            tvBadge.text = btn.name.take(4)
            tvTitle.text = "Nút Combo [${btn.name}]"
            val desc = if (btn.actions.isNotEmpty()) {
                "${btn.actions.size} thao tác • ${btn.speedMultiplier}x tốc độ • ${btn.opacityPercent}% mờ"
            } else {
                "${btn.points.size} chiêu • ${btn.delayBetweenMs}ms • ${btn.opacityPercent}% mờ"
            }
            tvSub.text = desc

            btnRename?.setOnClickListener {
                btn.showRenameDialog()
            }

            val isVisible = btn.view.visibility == View.VISIBLE
            btnToggle?.text = if (isVisible) "👁️" else "🙈"
            btnToggle?.setOnClickListener {
                val nowVis = btn.view.visibility == View.VISIBLE
                btn.setVisible(!nowVis)
                btnToggle.text = if (!nowVis) "👁️" else "🙈"
                Toast.makeText(this, if (!nowVis) "Đã hiện nút [${btn.name}]" else "Đã ẩn nút [${btn.name}]", Toast.LENGTH_SHORT).show()
            }

            if (btn.actions.isNotEmpty() && btn.points.isEmpty()) {
                btnRestore.visibility = View.GONE
            } else {
                btnRestore.visibility = View.VISIBLE
                btnRestore.setOnClickListener {
                    val pts = ArrayList(btn.points)
                    btn.destroy()
                    triggerButtons.remove(btn)
                    configStorage.deleteTrigger(btn.id)
                    restorePinsFromCombo(pts)
                    renderAssignedTriggersList()
                    Toast.makeText(this, "🎯 Đã hiện lại ${pts.size} điểm ghim trên game! Bạn hãy kéo đặt lên chiêu.", Toast.LENGTH_LONG).show()
                }
            }

            btnDelete.setOnClickListener {
                btn.destroy()
                triggerButtons.remove(btn)
                configStorage.deleteTrigger(btn.id)
                renderAssignedTriggersList()
                Toast.makeText(this, "🗑️ Đã xóa vĩnh viễn nút [${btn.name}]!", Toast.LENGTH_SHORT).show()
            }

            container.addView(itemView)
        }
    }

    /**
     * Xóa toàn bộ nút combo đã tạo khỏi màn hình và bộ nhớ (chống spam nút)
     */
    private fun clearAllMacroTriggers() {
        clearAllTriggerButtons()
        configStorage.clearAllTriggers()
        renderAssignedTriggersList()
        Toast.makeText(this, "🗑️ Đã xóa toàn bộ nút Macro đã tạo!", Toast.LENGTH_SHORT).show()
    }

    private fun toggleOverlayVisibility() {
        isOverlayHidden = !isOverlayHidden
        val layoutCollapsed = dockView?.findViewById<View>(R.id.layoutCollapsed)
        val layoutExpanded = dockView?.findViewById<View>(R.id.layoutExpanded)

        layoutCollapsed?.visibility = View.GONE

        if (isOverlayHidden) {
            targetPins.forEach { it.view.visibility = View.GONE }
            triggerButtons.forEach { it.view.visibility = View.GONE }
            layoutExpanded?.visibility = View.GONE
            Toast.makeText(this, "👁️ Đã ẩn toàn bộ nút trên màn hình!", Toast.LENGTH_SHORT).show()
        } else {
            targetPins.forEach { it.view.visibility = View.VISIBLE }
            triggerButtons.forEach { it.view.visibility = View.VISIBLE }
            Toast.makeText(this, "👁️ Đã hiện lại các nút Combo!", Toast.LENGTH_SHORT).show()
        }
        updateNotification()
    }

    private fun addNewTargetPin(x: Int? = null, y: Int? = null) {
        if (targetPins.size >= 10) {
            Toast.makeText(this, "Tối đa 10 điểm ghim!", Toast.LENGTH_SHORT).show()
            return
        }

        val index = targetPins.size + 1
        val initialX = x ?: ((screenWidth / 2) - 80 + (targetPins.size * 35))
        val initialY = y ?: ((screenHeight / 2) - 80 + (targetPins.size * 35))

        val pin = TargetPinView(
            context = this,
            windowManager = windowManager,
            index = index,
            initialX = initialX,
            initialY = initialY,
            onPinClicked = { p ->
                val (cx, cy) = p.getCenterCoordinates()
                if (!ShellExecutor.isEngineReady()) {
                    Toast.makeText(this@TurboOverlayService, "⚠️ Động cơ Shizuku chưa bật! Hãy mở app cấp quyền Shizuku.", Toast.LENGTH_LONG).show()
                } else {
                    scope.launch(Dispatchers.IO) {
                        ShellExecutor.tap(cx, cy, 35)
                    }
                    Toast.makeText(this@TurboOverlayService, "▶ Đã click Điểm #${p.index} vào game!", Toast.LENGTH_SHORT).show()
                }
            }
        )
        targetPins.add(pin)

        Toast.makeText(this, "Đã thêm Điểm #$index. Kéo đặt lên nút Shop/Chiêu trong game!", Toast.LENGTH_SHORT).show()
    }

    private fun removeLastTargetPin() {
        if (targetPins.isNotEmpty()) {
            val last = targetPins.removeAt(targetPins.size - 1)
            last.destroy()
            Toast.makeText(this, "Đã xóa điểm #${last.index}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAllTargetPins() {
        targetPins.forEach { it.destroy() }
        targetPins.clear()
    }

    private fun executeTargetPinsNow() {
        if (targetPins.isEmpty()) {
            Toast.makeText(this, "Chưa có điểm ghim nào! Bấm [+ Ghim Điểm] trước.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!ShellExecutor.isEngineReady()) {
            Toast.makeText(this, "⚠️ Động cơ Shizuku chưa bật! Vui lòng mở app cấp quyền Shizuku.", Toast.LENGTH_LONG).show()
            return
        }

        val points = targetPins.map { it.getCenterCoordinates() }
        scope.launch(Dispatchers.IO) {
            val ok = ShellExecutor.executeCombo(points, delayBetweenMs = selectedSpeedDelay, repeatCount = 1)
            if (!ok) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(this@TurboOverlayService, "Cần cấp quyền Shizuku hoặc Root để tự động click!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Tạo Nút Tròn Kích Hoạt Combo Độc Lập từ các điểm ghim
     */
    private fun createTriggerButtonFromPins() {
        if (targetPins.isEmpty()) {
            Toast.makeText(this, "Chưa có điểm ghim nào! Bấm [+ Ghim Điểm] để đặt vị trí chiêu trước.", Toast.LENGTH_LONG).show()
            return
        }

        if (triggerButtons.size >= 5) {
            Toast.makeText(this, "Đã đạt tối đa 5 nút combo! Hãy xóa bớt nút cũ trước.", Toast.LENGTH_LONG).show()
            return
        }

        val points = targetPins.map { it.getCenterCoordinates() }
        val name = "C${triggerButtons.size + 1}"
        val offset = (triggerButtons.size * 65)
        val btnX = (screenWidth * 0.70f).toInt().coerceAtLeast(60)
        val btnY = ((screenHeight / 3) + offset).coerceIn(100, (screenHeight - 160).coerceAtLeast(100))

        val triggerBtn = createFloatingTriggerInstance(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            points = points,
            posX = btnX,
            posY = btnY,
            delay = selectedSpeedDelay,
            repeat = 1,
            opacity = configStorage.globalOpacity
        )
        triggerButtons.add(triggerBtn)

        configStorage.upsertTrigger(
            SavedMacroTrigger(
                id = triggerBtn.id,
                name = name,
                x = btnX,
                y = btnY,
                delayBetweenMs = selectedSpeedDelay,
                repeatCount = 1,
                opacityPercent = configStorage.globalOpacity,
                points = points
            )
        )

        clearAllTargetPins()
        closeTurboMenu()
        renderAssignedTriggersList()
        Toast.makeText(this, "✅ Đã tạo nút [$name]! Chạm nút để xả combo, nhấn giữ để chỉnh sửa.", Toast.LENGTH_LONG).show()
    }

    private fun restorePinsFromCombo(points: List<Pair<Float, Float>>) {
        clearAllTargetPins()
        val density = resources.displayMetrics.density
        val offset = (22 * density).toInt()
        points.forEach { (x, y) ->
            addNewTargetPin((x - offset).toInt(), (y - offset).toInt())
        }
        openTurboMenu()
    }

    private fun clearAllTriggerButtons() {
        triggerButtons.forEach { it.destroy() }
        triggerButtons.clear()
    }

    private fun removeViewSafely(view: View?) {
        if (view != null) {
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeViewImmediate(view)
                } else {
                    windowManager.removeView(view)
                }
            } catch (_: Throwable) {}
        }
    }

    private fun updateViewSafely(view: View?, params: WindowManager.LayoutParams) {
        if (view != null && view.isAttachedToWindow) {
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Throwable) {}
        }
    }
}
