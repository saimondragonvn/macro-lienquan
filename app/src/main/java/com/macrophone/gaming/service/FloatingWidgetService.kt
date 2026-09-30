package com.macrophone.gaming.service

import android.animation.ValueAnimator
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import androidx.appcompat.view.ContextThemeWrapper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.macrophone.gaming.R
import com.macrophone.gaming.data.model.GesturePoint
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroSequence
import com.macrophone.gaming.data.model.MacroState
import com.macrophone.gaming.data.model.MacroType
import com.macrophone.gaming.data.model.PlaybackConfig
import com.macrophone.gaming.domain.MacroManager
import com.macrophone.gaming.ui.overlay.FloatingMacroButton
import com.macrophone.gaming.ui.overlay.TargetPointMarker
import com.macrophone.gaming.ui.overlay.TouchRecorderCanvas
import com.macrophone.gaming.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Service quản lý Cửa sổ nổi (Floating Overlay Controller):
 * - Quản lý dock điều khiển có thể kéo thả và tự động ghim viền (Edge Snapping).
 * - Quản lý các NÚT MACRO NỔI (Floating Macro Buttons): Lưu chuỗi thao tác thành từng nút bấm riêng
 *   trên màn hình game. Khi bấm nút, macro sẽ tự động thực hiện lại combo siêu tốc!
 * - Hỗ trợ ghi thao tác thực tế và chỉnh tốc độ tăng tốc (1x, 2x, 3x, 5x, 10x).
 * - Không hề khóa hoặc cản trở cảm ứng đa điểm (tay trái vẫn chạy joystick bình thường).
 */
class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var macroManager: MacroManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObserverJob: Job? = null

    // Views và LayoutParams của Dock nổi
    private var dockView: View? = null
    private lateinit var dockParams: WindowManager.LayoutParams

    // Danh sách các Nút Macro Nổi độc lập trên màn hình
    private val floatingMacroButtons = mutableListOf<FloatingMacroButton>()

    // Danh sách các Điểm Ghim Mục Tiêu (Target Points)
    private val targetPoints = mutableListOf<TargetPointMarker>()
    private var areTargetPointsVisible = true

    // Views và LayoutParams của Cửa sổ Cài đặt
    private var settingsDialogView: View? = null
    private var settingsParams: WindowManager.LayoutParams? = null

    // Views và LayoutParams của Cửa sổ Ghi thao tác (Record Overlay)
    private var recordOverlayView: View? = null
    private var recordParams: WindowManager.LayoutParams? = null

    // Views và LayoutParams của Dialog Lưu & Chỉnh tốc độ
    private var saveDialogView: View? = null
    private var saveParams: WindowManager.LayoutParams? = null

    private var screenWidth = 0
    private var screenHeight = 0

    // Receiver tự động đóng màn hình ghi khi người dùng bấm Home, Recent Apps hoặc tắt màn hình
    private var isSystemDialogsReceiverRegistered = false
    private val systemDialogsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (recordOverlayView != null) {
                closeRecordOverlay()
            }
        }
    }

    private fun registerSystemDialogsReceiver() {
        if (!isSystemDialogsReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(systemDialogsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    registerReceiver(systemDialogsReceiver, filter)
                }
                isSystemDialogsReceiverRegistered = true
            } catch (_: Throwable) {}
        }
    }

    private fun unregisterSystemDialogsReceiver() {
        if (isSystemDialogsReceiverRegistered) {
            try {
                unregisterReceiver(systemDialogsReceiver)
            } catch (_: Throwable) {}
            isSystemDialogsReceiverRegistered = false
        }
    }

    companion object {
        const val ACTION_START_SERVICE = "com.macrophone.gaming.ACTION_START_FLOATING"
        const val ACTION_STOP_SERVICE = "com.macrophone.gaming.ACTION_STOP_FLOATING"

        fun start(context: Context) {
            val intent = Intent(context, FloatingWidgetService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingWidgetService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        macroManager = MacroManager.getInstance(this)
        measureScreenDimensions()

        startForegroundNotification()
        initFloatingDock()
        loadExistingFloatingMacroButtons()
        observeMacroState()

        macroManager.setFloatingServiceRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterSystemDialogsReceiver()
        macroManager.setFloatingServiceRunning(false)
        stateObserverJob?.cancel()
        serviceScope.cancel()

        clearAllTargetPoints()
        clearAllFloatingMacroButtons()

        removeViewSafely(dockView)
        removeViewSafely(settingsDialogView)
        closeRecordOverlay()
        removeViewSafely(saveDialogView)

        super.onDestroy()
    }

    private fun measureScreenDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            screenWidth = windowMetrics.bounds.width()
            screenHeight = windowMetrics.bounds.height()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }
    }

    private fun startForegroundNotification() {
        try {
            NotificationHelper.createNotificationChannel(this)
            val notification = NotificationHelper.buildNotification(
                this,
                macroManager.macroState.value,
                macroManager.activeMacro.value?.name
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    androidx.core.app.ServiceCompat.startForeground(
                        this,
                        NotificationHelper.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (_: Throwable) {
                    androidx.core.app.ServiceCompat.startForeground(
                        this,
                        NotificationHelper.NOTIFICATION_ID,
                        notification,
                        0
                    )
                }
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e("FloatingWidgetService", "startForegroundNotification error: ${e.message}", e)
        }
    }

    /**
     * Khởi tạo thanh dock nổi (Floating Dock)
     */
    private fun initFloatingDock() {
        val inflater = LayoutInflater.from(this)
        dockView = inflater.inflate(R.layout.view_floating_dock, null)

        dockParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - 320).coerceAtLeast(20)
            y = 350
        }

        setupDockTouchDrag(dockView!!)
        setupDockButtons(dockView!!)

        try {
            windowManager.addView(dockView, dockParams)
        } catch (e: Exception) {
            Log.e("FloatingWidgetService", "Cannot add dockView: ${e.message}", e)
            Toast.makeText(
                this,
                "Chưa cấp đủ quyền 'Cửa sổ thả nổi' hoặc 'Hiển thị trên ứng dụng khác'! Vui lòng kiểm tra cài đặt Infinix.",
                Toast.LENGTH_LONG
            ).show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                stopSelf()
            }, 800)
        }
    }

    /**
     * Tự động tải lại các Nút Macro Nổi từ danh sách Macro đã lưu
     */
    private fun loadExistingFloatingMacroButtons() {
        val macros = macroManager.getAllMacros()
        macros.filter { it.hasFloatingButton }.forEach { sequence ->
            createFloatingMacroButton(sequence)
        }
    }

    /**
     * Tạo một Nút Macro Nổi riêng biệt trên màn hình game
     */
    private fun createFloatingMacroButton(sequence: MacroSequence) {
        val macroBtn = FloatingMacroButton(
            context = this,
            windowManager = windowManager,
            sequence = sequence,
            onTrigger = { seq ->
                // Phát lại thao tác (hỗ trợ cả Trợ năng lẫn Shizuku/Root)
                macroManager.playDirectSequence(seq)
            },
            onStop = {
                macroManager.stopPlayback()
            },
            onDelete = { seq ->
                floatingMacroButtons.removeAll { it.sequence.id == seq.id }
                macroManager.deleteMacro(seq.id)
                Toast.makeText(this, "Đã xóa nút: ${seq.name}", Toast.LENGTH_SHORT).show()
            }
        )
        floatingMacroButtons.add(macroBtn)
    }

    private fun clearAllFloatingMacroButtons() {
        floatingMacroButtons.forEach { it.destroy() }
        floatingMacroButtons.clear()
    }

    private fun setupDockTouchDrag(root: View) {
        val collapsed = root.findViewById<View>(R.id.layoutCollapsed)
        val expandedHeader = root.findViewById<View>(R.id.layoutExpandedHeader)

        val createDragListener = { targetView: View ->
            object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isDragging = false
                private var downTime = 0L

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = dockParams.x
                            initialY = dockParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            downTime = System.currentTimeMillis()
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()

                            if (abs(dx) > 10 || abs(dy) > 10) {
                                isDragging = true
                                dockParams.x = initialX + dx
                                dockParams.y = initialY + dy
                                updateViewLayoutSafely(dockView, dockParams)
                            }
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (isDragging) {
                                val viewWidth = if (root.width > 0) root.width else 100
                                val middle = screenWidth / 2
                                val currentCenterX = dockParams.x + (viewWidth / 2)
                                val targetX = if (currentCenterX < middle) 16 else (screenWidth - viewWidth - 16)
                                animateDockSnap(dockParams.x, targetX)
                                return true
                            }
                            val duration = System.currentTimeMillis() - downTime
                            if (duration < 400) {
                                targetView.performClick()
                            }
                            return true
                        }
                    }
                    return false
                }
            }
        }

        collapsed?.setOnTouchListener(createDragListener(collapsed))
        expandedHeader?.setOnTouchListener(createDragListener(expandedHeader))
    }

    private fun animateDockSnap(fromX: Int, toX: Int) {
        val animator = ValueAnimator.ofInt(fromX, toX).apply {
            duration = 200
            addUpdateListener { animation ->
                dockParams.x = animation.animatedValue as Int
                updateViewLayoutSafely(dockView, dockParams)
            }
        }
        animator.start()
    }

    private fun setupDockButtons(root: View) {
        val layoutCollapsed = root.findViewById<View>(R.id.layoutCollapsed)
        val layoutExpanded = root.findViewById<View>(R.id.layoutExpanded)
        val btnCollapseDock = root.findViewById<View>(R.id.btnCollapseDock)

        val btnAddPoint = root.findViewById<View>(R.id.btnAddPoint)
        val btnRemovePoint = root.findViewById<View>(R.id.btnRemovePoint)
        val btnPlayTest = root.findViewById<View>(R.id.btnPlayTest)
        val btnCreateMacroBtn = root.findViewById<View>(R.id.btnCreateMacroBtn)

        val btnStartRecord = root.findViewById<View>(R.id.btnStartRecord)

        val btnToggleVisibility = root.findViewById<View>(R.id.btnToggleVisibility)
        val btnClearPoints = root.findViewById<View>(R.id.btnClearPoints)
        val btnCloseService = root.findViewById<View>(R.id.btnCloseService)

        // Bấm tab thu gọn -> Mở rộng Game Turbo HUD
        layoutCollapsed?.setOnClickListener {
            layoutCollapsed.visibility = View.GONE
            layoutExpanded.visibility = View.VISIBLE
        }

        // Bấm nút đóng panel -> Thu gọn về tab mép màn hình
        btnCollapseDock?.setOnClickListener {
            layoutExpanded.visibility = View.GONE
            layoutCollapsed.visibility = View.VISIBLE
        }

        btnAddPoint?.setOnClickListener {
            addNewTargetPoint()
        }

        btnRemovePoint?.setOnClickListener {
            removeLastTargetPoint()
        }

        btnPlayTest?.setOnClickListener {
            if (macroManager.macroState.value == MacroState.PLAYING) {
                macroManager.stopPlayback()
            } else {
                if (targetPoints.isNotEmpty()) {
                    playTargetPointsSequence()
                } else {
                    macroManager.startPlayback()
                }
            }
        }

        btnCreateMacroBtn?.setOnClickListener {
            createMacroButtonFromTargetPoints()
        }

        // NÚT BẮT ĐẦU GHI (START) CỰC KỲ RÕ RÀNG
        btnStartRecord?.setOnClickListener {
            openRecordOverlay()
        }

        btnToggleVisibility?.setOnClickListener {
            toggleTargetPointsVisibility()
        }

        btnClearPoints?.setOnClickListener {
            clearAllTargetPoints()
            Toast.makeText(this, "Đã xóa toàn bộ điểm ghim", Toast.LENGTH_SHORT).show()
        }

        btnCloseService?.setOnClickListener {
            macroManager.stopPlayback()
            stopSelf()
        }
    }

    private fun addNewTargetPoint() {
        val index = targetPoints.size + 1
        val initialX = (screenWidth / 2) - 60 + (targetPoints.size * 20)
        val initialY = (screenHeight / 2) - 100 + (targetPoints.size * 50)

        val marker = TargetPointMarker(this, windowManager, index, initialX, initialY)
        targetPoints.add(marker)

        Toast.makeText(
            this,
            "Đã thêm Điểm #$index. Kéo thả vào nút chiêu mong muốn!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun removeLastTargetPoint() {
        if (targetPoints.isNotEmpty()) {
            val last = targetPoints.removeAt(targetPoints.size - 1)
            last.destroy()
            Toast.makeText(this, "Đã xóa điểm #${last.index}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleTargetPointsVisibility() {
        areTargetPointsVisible = !areTargetPointsVisible
        targetPoints.forEach { it.setVisible(areTargetPointsVisible) }
        floatingMacroButtons.forEach { it.setVisible(areTargetPointsVisible) }
        val msg = if (areTargetPointsVisible) "Đã hiện nút & điểm ghim" else "Đã ẩn nút & điểm ghim"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun clearAllTargetPoints() {
        targetPoints.forEach { it.destroy() }
        targetPoints.clear()
    }

    private fun playTargetPointsSequence() {
        val actions = targetPoints.map { marker ->
            val (cx, cy) = marker.getCenterCoordinates()
            MacroAction(
                type = MacroType.TAP,
                points = listOf(GesturePoint(cx, cy)),
                durationMs = 25L,
                delayBeforeMs = 40L
            )
        }

        val activeConfig = macroManager.activeMacro.value?.config ?: PlaybackConfig()
        val dynamicSequence = MacroSequence(
            id = "target_points_combo",
            name = "Combo ${targetPoints.size} Điểm Ghim",
            actions = actions,
            config = activeConfig
        )

        macroManager.playDirectSequence(dynamicSequence)
    }

    /**
     * Tạo trực tiếp Nút Macro Nổi từ các Điểm Ghim (Target Points #1, #2, #3...)
     * Không hề chặn cảm ứng màn hình, người chơi có thể đặt pin chính xác lên từng nút chiêu.
     */
    private fun createMacroButtonFromTargetPoints() {
        if (targetPoints.isEmpty()) {
            Toast.makeText(
                this,
                "Chưa có điểm ghim nào! Hãy bấm nút [+ Ghim Điểm] để đặt các vị trí chiêu (1, 2, 3...) trước.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val actions = targetPoints.map { marker ->
            val (cx, cy) = marker.getCenterCoordinates()
            MacroAction(
                type = MacroType.TAP,
                points = listOf(GesturePoint(cx, cy)),
                durationMs = 25L,
                delayBeforeMs = 40L
            )
        }

        val macroIndex = macroManager.getAllMacros().size + 1
        val macroName = "M$macroIndex"
        val config = PlaybackConfig(
            speedMultiplier = 5.0f,
            loopCount = 1,
            loopIntervalMs = 50L
        )

        val btnX = (screenWidth - 240).coerceAtLeast(60)
        val btnY = (screenHeight / 2) - 100

        val newSequence = MacroSequence(
            name = macroName,
            description = "Combo ${targetPoints.size} điểm ghim",
            actions = actions,
            config = config,
            hasFloatingButton = true,
            buttonX = btnX,
            buttonY = btnY
        )

        macroManager.saveDirectSequence(newSequence)
        clearAllTargetPoints()
        createFloatingMacroButton(newSequence)

        Toast.makeText(
            this,
            "✅ Đã tạo nút tròn Macro [$macroName] từ ${actions.size} điểm ghim! Chạm nút để xả combo, đè nút để xóa.",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Mở chế độ ghi thao tác
     */
    private fun openRecordOverlay() {
        if (recordOverlayView != null) return

        macroManager.startRecording()

        val inflater = LayoutInflater.from(this)
        recordOverlayView = inflater.inflate(R.layout.view_touch_recorder, null)

        recordParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val canvas = recordOverlayView!!.findViewById<TouchRecorderCanvas>(R.id.touchCanvas)
        val tvCount = recordOverlayView!!.findViewById<TextView>(R.id.tvPointCount)
        val btnUndo = recordOverlayView!!.findViewById<TextView>(R.id.btnUndoRecord)
        val btnCancel = recordOverlayView!!.findViewById<TextView>(R.id.btnCancelRecord)
        val btnStop = recordOverlayView!!.findViewById<TextView>(R.id.btnStopRecord)

        canvas.onActionRecorded = { action: MacroAction ->
            macroManager.addRecordedAction(action)
        }

        canvas.onCountChanged = { count ->
            tvCount.text = "Đã ghi: $count thao tác"
        }

        btnUndo?.setOnClickListener {
            val undone = canvas.undoLast()
            if (undone) {
                macroManager.removeLastRecordedAction()
                Toast.makeText(this, "↩ Đã xóa thao tác vừa ghi", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel?.setOnClickListener {
            closeRecordOverlay()
        }

        btnStop?.setOnClickListener {
            val actions = canvas.getRecordedActions()
            if (actions.isEmpty()) {
                Toast.makeText(this, "Chưa ghi thao tác nào! Hãy chạm hoặc vuốt trên màn hình.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            closeRecordOverlay()

            val macroIndex = macroManager.getAllMacros().size + 1
            val macroName = "M$macroIndex"
            val config = PlaybackConfig(
                speedMultiplier = 5.0f, // 5x speed cho game xả combo siêu tốc
                loopCount = 1,
                loopIntervalMs = 50L
            )

            val btnX = (screenWidth - 220).coerceAtLeast(60)
            val btnY = (screenHeight / 2) - 100

            val savedSequence = macroManager.saveRecording(
                name = macroName,
                config = config,
                customActions = actions,
                buttonX = btnX,
                buttonY = btnY
            )

            if (savedSequence != null) {
                createFloatingMacroButton(savedSequence)

                Toast.makeText(
                    this,
                    "✅ Đã tạo nút tròn Turbo [$macroName]! Chạm nút để xả combo, đè nút để xóa.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        dockView?.visibility = View.GONE
        registerSystemDialogsReceiver()

        try {
            windowManager.addView(recordOverlayView, recordParams)
        } catch (e: Exception) {
            closeRecordOverlay()
            Toast.makeText(this, "Lỗi hiển thị màn hình ghi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeRecordOverlay() {
        unregisterSystemDialogsReceiver()
        if (recordOverlayView != null) {
            try {
                if (recordOverlayView!!.isAttachedToWindow) {
                    windowManager.removeViewImmediate(recordOverlayView)
                }
            } catch (_: Exception) {
                try {
                    windowManager.removeView(recordOverlayView)
                } catch (_: Exception) {}
            }
            recordOverlayView = null
        }
        dockView?.visibility = View.VISIBLE
        macroManager.cancelRecording()
    }

    /**
     * Mở Dialog Lưu thao tác, chỉnh tốc độ (Speed Multiplier 1x - 10x) và tạo Nút Macro Nổi
     */
    private fun openSaveAndSpeedDialog() {
        if (saveDialogView != null) {
            removeViewSafely(saveDialogView)
            saveDialogView = null
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_MacroGaming)
        val inflater = LayoutInflater.from(themedContext)
        saveDialogView = inflater.inflate(R.layout.view_save_macro_dialog, null)

        saveParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val etName = saveDialogView!!.findViewById<EditText>(R.id.etMacroName)
        val gesturePreview = saveDialogView!!.findViewById<com.macrophone.gaming.ui.overlay.GesturePreviewCanvas>(R.id.gesturePreview)
        val tvPreviewEmpty = saveDialogView!!.findViewById<TextView>(R.id.tvPreviewEmpty)
        val tvOrig = saveDialogView!!.findViewById<TextView>(R.id.tvOriginalDuration)
        val tvAccel = saveDialogView!!.findViewById<TextView>(R.id.tvAcceleratedDuration)
        val cbFloating = saveDialogView!!.findViewById<CheckBox>(R.id.cbCreateFloatingButton)
        val btnCancel = saveDialogView!!.findViewById<TextView>(R.id.btnCancelSaveDialog)
        val btnConfirm = saveDialogView!!.findViewById<TextView>(R.id.btnConfirmSaveDialog)

        // Hiển thị preview thao tác đã ghi
        val recordedActions = macroManager.getRecordedActions()
        if (recordedActions.isNotEmpty()) {
            gesturePreview.setActions(recordedActions)
            tvPreviewEmpty.visibility = View.GONE
        } else {
            tvPreviewEmpty.visibility = View.VISIBLE
        }

        // Tính toán thời gian ghi gốc từ các action vừa ghi nhận
        val totalOriginalMs = recordedActions.sumOf { it.durationMs + it.delayBeforeMs }
        val origSec = String.format("%.2f", totalOriginalMs / 1000.0)
        val defaultName = "Combo #${(System.currentTimeMillis() % 1000)}"
        etName.setText(defaultName)

        var selectedSpeed = 5.0f
        var selectedLoop = 1

        val speedChips = listOf(
            saveDialogView!!.findViewById<TextView>(R.id.chipSpeed1x) to 1.0f,
            saveDialogView!!.findViewById<TextView>(R.id.chipSpeed2x) to 2.0f,
            saveDialogView!!.findViewById<TextView>(R.id.chipSpeed3x) to 3.0f,
            saveDialogView!!.findViewById<TextView>(R.id.chipSpeed5x) to 5.0f,
            saveDialogView!!.findViewById<TextView>(R.id.chipSpeed10x) to 10.0f
        )

        fun updateSpeedDisplay() {
            speedChips.forEach { (view, speed) ->
                if (speed == selectedSpeed) {
                    view.setBackgroundResource(R.drawable.bg_chip_selected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
                } else {
                    view.setBackgroundResource(R.drawable.bg_chip_unselected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            }
            tvOrig.text = "Thời gian ghi gốc: ~${origSec}s (${recordedActions.size} thao tác)"
            val accelerated = String.format("%.2f", totalOriginalMs / 1000.0 / selectedSpeed)
            tvAccel.text = "Thời gian sau khi tăng tốc: ${accelerated}s (Nhanh hơn ${selectedSpeed}x! ⚡)"
        }
        updateSpeedDisplay()

        speedChips.forEach { (view, speed) ->
            view.setOnClickListener {
                selectedSpeed = speed
                updateSpeedDisplay()
            }
        }

        val loopChips = listOf(
            saveDialogView!!.findViewById<TextView>(R.id.chipSaveLoop1) to 1,
            saveDialogView!!.findViewById<TextView>(R.id.chipSaveLoop5) to 5,
            saveDialogView!!.findViewById<TextView>(R.id.chipSaveLoopInf) to -1
        )

        fun updateLoopDisplay() {
            loopChips.forEach { (view, loop) ->
                if (loop == selectedLoop) {
                    view.setBackgroundResource(R.drawable.bg_chip_selected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
                } else {
                    view.setBackgroundResource(R.drawable.bg_chip_unselected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            }
        }
        updateLoopDisplay()

        loopChips.forEach { (view, loop) ->
            view.setOnClickListener {
                selectedLoop = loop
                updateLoopDisplay()
            }
        }

        btnCancel.setOnClickListener {
            macroManager.cancelRecording()
            removeViewSafely(saveDialogView)
            saveDialogView = null
        }

        btnConfirm.setOnClickListener {
            val macroName = etName.text.toString().trim().ifBlank { defaultName }
            val config = PlaybackConfig(
                speedMultiplier = selectedSpeed,
                loopCount = selectedLoop,
                loopIntervalMs = 50L
            )

            // Lưu MacroSequence
            val savedSequence = macroManager.saveRecording(macroName, config)
            if (savedSequence != null) {
                if (cbFloating.isChecked) {
                    // Tạo Nút Macro Nổi trên màn hình game!
                    createFloatingMacroButton(savedSequence)
                    Toast.makeText(
                        this,
                        "Đã tạo Nút Macro [${savedSequence.name}]! Chạm vào nút để kích hoạt.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, "Đã lưu combo: ${savedSequence.name}", Toast.LENGTH_SHORT).show()
                }
            }

            removeViewSafely(saveDialogView)
            saveDialogView = null
        }

        try {
            windowManager.addView(saveDialogView, saveParams)
        } catch (_: Exception) {}
    }

    private fun toggleSettingsDialog() {
        if (settingsDialogView != null) {
            removeViewSafely(settingsDialogView)
            settingsDialogView = null
            return
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_MacroGaming)
        val inflater = LayoutInflater.from(themedContext)
        settingsDialogView = inflater.inflate(R.layout.view_macro_settings_dialog, null)

        settingsParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        setupSettingsDialog(settingsDialogView!!)
        try {
            windowManager.addView(settingsDialogView, settingsParams)
        } catch (_: Exception) {}
    }

    private fun setupSettingsDialog(root: View) {
        val currentMacro = macroManager.activeMacro.value
        val config = currentMacro?.config ?: PlaybackConfig()

        val btnClose = root.findViewById<ImageButton>(R.id.btnDialogClose)
        val spinnerPresets = root.findViewById<Spinner>(R.id.spinnerPresets)
        val seekBarInterval = root.findViewById<SeekBar>(R.id.seekBarInterval)
        val tvIntervalLabel = root.findViewById<TextView>(R.id.tvIntervalLabel)
        val btnApply = root.findViewById<TextView>(R.id.btnApplySettings)

        val allMacros = macroManager.getAllMacros()
        val names = allMacros.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinnerPresets.adapter = adapter

        val selectedIndex = allMacros.indexOfFirst { it.id == currentMacro?.id }
        if (selectedIndex >= 0) {
            spinnerPresets.setSelection(selectedIndex)
        }

        spinnerPresets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = allMacros.getOrNull(position)
                if (selected != null && selected.id != macroManager.activeMacro.value?.id) {
                    macroManager.setActiveMacro(selected)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        var selectedSpeed = config.speedMultiplier
        val speedChips = listOf(
            root.findViewById<TextView>(R.id.chipSpeed05) to 0.5f,
            root.findViewById<TextView>(R.id.chipSpeed1) to 1.0f,
            root.findViewById<TextView>(R.id.chipSpeed2) to 2.0f,
            root.findViewById<TextView>(R.id.chipSpeed5) to 5.0f,
            root.findViewById<TextView>(R.id.chipSpeed10) to 10.0f
        )

        fun updateSpeedChips() {
            speedChips.forEach { (view, speed) ->
                if (speed == selectedSpeed) {
                    view.setBackgroundResource(R.drawable.bg_chip_selected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
                } else {
                    view.setBackgroundResource(R.drawable.bg_chip_unselected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            }
        }
        updateSpeedChips()

        speedChips.forEach { (view, speed) ->
            view.setOnClickListener {
                selectedSpeed = speed
                updateSpeedChips()
            }
        }

        var selectedLoop = config.loopCount
        val loopChips = listOf(
            root.findViewById<TextView>(R.id.chipLoop1) to 1,
            root.findViewById<TextView>(R.id.chipLoop5) to 5,
            root.findViewById<TextView>(R.id.chipLoop10) to 10,
            root.findViewById<TextView>(R.id.chipLoopInfinite) to -1
        )

        fun updateLoopChips() {
            loopChips.forEach { (view, loop) ->
                if (loop == selectedLoop) {
                    view.setBackgroundResource(R.drawable.bg_chip_selected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
                } else {
                    view.setBackgroundResource(R.drawable.bg_chip_unselected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            }
        }
        updateLoopChips()

        loopChips.forEach { (view, loop) ->
            view.setOnClickListener {
                selectedLoop = loop
                updateLoopChips()
            }
        }

        seekBarInterval.progress = config.loopIntervalMs.toInt().coerceIn(0, 2000)
        tvIntervalLabel.text = "Độ trễ giữa các lần lặp: ${seekBarInterval.progress}ms"

        seekBarInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvIntervalLabel.text = "Độ trễ giữa các lần lặp: ${progress}ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnApply.setOnClickListener {
            val newConfig = PlaybackConfig(
                speedMultiplier = selectedSpeed,
                loopCount = selectedLoop,
                loopIntervalMs = seekBarInterval.progress.toLong()
            )
            macroManager.updateActiveConfig(newConfig)
            Toast.makeText(this, "Đã cập nhật cấu hình: ${selectedSpeed}x", Toast.LENGTH_SHORT).show()
            removeViewSafely(settingsDialogView)
            settingsDialogView = null
        }

        btnClose.setOnClickListener {
            removeViewSafely(settingsDialogView)
            settingsDialogView = null
        }
    }

    private fun observeMacroState() {
        stateObserverJob = serviceScope.launch {
            macroManager.macroState.collectLatest { state ->
                val btnPlayTest = dockView?.findViewById<TextView>(R.id.btnPlayTest)
                val ivCollapsedHandle = dockView?.findViewById<ImageView>(R.id.ivCollapsedHandle)
                val ivExpandedDrag = dockView?.findViewById<ImageView>(R.id.ivExpandedDrag)

                val isPlaying = state == MacroState.PLAYING

                // Đồng bộ trạng thái đèn LED của tất cả các Nút Macro Nổi
                floatingMacroButtons.forEach { it.setPlayingState(isPlaying) }

                when (state) {
                    MacroState.PLAYING -> {
                        btnPlayTest?.text = "⏹ Dừng"
                        btnPlayTest?.setTextColor(ContextCompat.getColor(this@FloatingWidgetService, R.color.crimson_stop))
                        ivCollapsedHandle?.setColorFilter(ContextCompat.getColor(this@FloatingWidgetService, R.color.emerald_play))
                        ivExpandedDrag?.setColorFilter(ContextCompat.getColor(this@FloatingWidgetService, R.color.emerald_play))
                    }
                    MacroState.RECORDING -> {
                        btnPlayTest?.text = "▶ Thử"
                        btnPlayTest?.setTextColor(ContextCompat.getColor(this@FloatingWidgetService, R.color.emerald_play))
                        ivCollapsedHandle?.setColorFilter(ContextCompat.getColor(this@FloatingWidgetService, R.color.crimson_stop))
                        ivExpandedDrag?.setColorFilter(ContextCompat.getColor(this@FloatingWidgetService, R.color.crimson_stop))
                    }
                    else -> {
                        btnPlayTest?.text = "▶ Thử"
                        btnPlayTest?.setTextColor(ContextCompat.getColor(this@FloatingWidgetService, R.color.emerald_play))
                        ivCollapsedHandle?.setColorFilter(ContextCompat.getColor(this@FloatingWidgetService, R.color.cyan_neon))
                        ivExpandedDrag?.setColorFilter(ContextCompat.getColor(this@FloatingWidgetService, R.color.cyan_neon))
                    }
                }

                val notif = NotificationHelper.buildNotification(
                    this@FloatingWidgetService,
                    state,
                    macroManager.activeMacro.value?.name
                )
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notifManager.notify(NotificationHelper.NOTIFICATION_ID, notif)
            }
        }
    }

    private fun removeViewSafely(view: View?) {
        if (view != null) {
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeViewImmediate(view)
                } else {
                    windowManager.removeView(view)
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateViewLayoutSafely(view: View?, params: WindowManager.LayoutParams) {
        if (view != null && view.isAttachedToWindow) {
            try {
                windowManager.updateViewLayout(view, params)
            } catch (_: Exception) {}
        }
    }
}
