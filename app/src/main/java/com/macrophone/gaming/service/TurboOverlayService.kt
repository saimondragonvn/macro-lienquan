package com.macrophone.gaming.service

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.macrophone.gaming.R
import com.macrophone.gaming.core.ShellExecutor
import com.macrophone.gaming.data.model.MacroAction
import com.macrophone.gaming.data.model.MacroState
import com.macrophone.gaming.data.model.MacroType
import com.macrophone.gaming.ui.overlay.FloatingTriggerView
import com.macrophone.gaming.ui.overlay.TargetPinView
import com.macrophone.gaming.ui.overlay.TouchRecorderCanvas
import com.macrophone.gaming.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Service Cửa Sổ Nổi Game Turbo (Kiến trúc Mới - Tối giản, Ổn định 100%):
 * - Tự động nhận diện xoay màn hình (Landscape game Liên Quân Mobile vs Portrait).
 * - Cung cấp Tab Nổi mép màn hình [⚡ TURBO] thu gọn/mở rộng mượt mà.
 * - Hỗ trợ Gán Điểm Ghim Chiêu (Target Pins ①, ②, ③) và tạo Nút Tròn Turbo độc lập.
 * - Hỗ trợ Ghi thao tác trực tiếp và chuyển đổi thành Nút Tròn Turbo.
 * - Thực thi trực tiếp qua ShellExecutor (Shizuku/Root) không cần Trợ năng.
 */
class TurboOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var screenWidth = 0
    private var screenHeight = 0

    // Views của Thanh Điều Khiển Game Turbo
    private var dockView: View? = null
    private lateinit var dockParams: WindowManager.LayoutParams

    // Danh sách các Điểm Ghim Chiêu đang hiển thị
    private val targetPins = mutableListOf<TargetPinView>()

    // Danh sách các Nút Tròn Turbo độc lập trên màn hình
    private val triggerButtons = mutableListOf<FloatingTriggerView>()

    // View Ghi thao tác trực tiếp
    private var recordOverlayView: View? = null

    companion object {
        const val ACTION_START = "com.macrophone.gaming.TURBO_START"
        const val ACTION_STOP = "com.macrophone.gaming.TURBO_STOP"

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
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        updateScreenDimensions()

        startForegroundSafe()
        initDockView()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateScreenDimensions()
        // Giữ dock trong tầm màn hình khi xoay ngang/dọc
        dockParams.x = dockParams.x.coerceIn(0, screenWidth - 100)
        dockParams.y = dockParams.y.coerceIn(0, screenHeight - 100)
        updateViewSafely(dockView, dockParams)
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()

        clearAllTargetPins()
        clearAllTriggerButtons()
        closeRecordOverlay()
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
                this,
                MacroState.IDLE,
                "Game Turbo Pro (120Hz)"
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

    /**
     * Khởi tạo Tab nổi Game Turbo (Pill) ở mép màn hình
     */
    private fun initDockView() {
        try {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_MacroGaming)
            val inflater = LayoutInflater.from(themedContext)
            dockView = inflater.inflate(R.layout.view_floating_dock, null)

            dockParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (screenWidth - 280).coerceAtLeast(16)
                y = (screenHeight / 3).coerceAtLeast(100)
            }

            (dockView as? android.view.ViewGroup)?.isMotionEventSplittingEnabled = true
            setupDockTouchAndDrag(dockView!!)
            setupDockButtons(dockView!!)

            windowManager.addView(dockView, dockParams)
        } catch (e: Throwable) {
            Log.e("TurboOverlayService", "initDockView error: ${e.message}", e)
            Toast.makeText(this, "Chưa cấp đủ quyền 'Cửa sổ thả nổi' hoặc 'Hiển thị trên ứng dụng khác'!", Toast.LENGTH_LONG).show()
        }
    }

    private var selectedSpeedDelay: Long = 30
    private var isOverlayHidden = false

    private fun setupDockTouchAndDrag(root: View) {
        val layoutCollapsed = root.findViewById<View>(R.id.layoutCollapsed)
        val ivExpandedDrag = root.findViewById<View>(R.id.ivExpandedDrag)

        val createDrag = { targetView: View ->
            object : View.OnTouchListener {
                private var startX = 0
                private var startY = 0
                private var touchX = 0f
                private var touchY = 0f
                private var isDragging = false
                private var downTime = 0L
                private var activePointerId = MotionEvent.INVALID_POINTER_ID

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            val actionIndex = event.actionIndex
                            activePointerId = event.getPointerId(actionIndex)
                            startX = dockParams.x
                            startY = dockParams.y
                            touchX = event.getX(actionIndex) + dockParams.x
                            touchY = event.getY(actionIndex) + dockParams.y
                            isDragging = false
                            downTime = System.currentTimeMillis()
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val pIndex = if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                                event.findPointerIndex(activePointerId)
                            } else {
                                0
                            }
                            if (pIndex in 0 until event.pointerCount) {
                                val curX = event.getX(pIndex) + dockParams.x
                                val curY = event.getY(pIndex) + dockParams.y
                                val dx = (curX - touchX).toInt()
                                val dy = (curY - touchY).toInt()

                                if (abs(dx) > 10 || abs(dy) > 10) {
                                    isDragging = true
                                    dockParams.x = startX + dx
                                    dockParams.y = startY + dy
                                    updateViewSafely(dockView, dockParams)
                                }
                            }
                            return true
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                            val actionIndex = event.actionIndex
                            val pointerId = event.getPointerId(actionIndex)
                            if (activePointerId == MotionEvent.INVALID_POINTER_ID || pointerId == activePointerId) {
                                if (isDragging) {
                                    val viewW = if (root.width > 0) root.width else 100
                                    val center = dockParams.x + (viewW / 2)
                                    val targetX = if (center < screenWidth / 2) 16 else (screenWidth - viewW - 16)
                                    snapDock(dockParams.x, targetX)
                                } else if (System.currentTimeMillis() - downTime < 400) {
                                    targetView.performClick()
                                }
                                activePointerId = MotionEvent.INVALID_POINTER_ID
                            }
                            return true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            isDragging = false
                            activePointerId = MotionEvent.INVALID_POINTER_ID
                            return true
                        }
                    }
                    return false
                }
            }
        }

        layoutCollapsed?.setOnTouchListener(createDrag(layoutCollapsed))
        ivExpandedDrag?.setOnTouchListener(createDrag(ivExpandedDrag))
    }

    private fun snapDock(fromX: Int, toX: Int) {
        val anim = ValueAnimator.ofInt(fromX, toX).apply {
            duration = 180
            addUpdateListener {
                dockParams.x = it.animatedValue as Int
                updateViewSafely(dockView, dockParams)
            }
        }
        anim.start()
    }

    private fun collapseDock() {
        val layoutCollapsed = dockView?.findViewById<View>(R.id.layoutCollapsed)
        val layoutExpanded = dockView?.findViewById<View>(R.id.layoutExpanded)
        layoutExpanded?.visibility = View.GONE
        layoutCollapsed?.visibility = View.VISIBLE
        layoutCollapsed?.alpha = 0.85f
    }

    private fun setupDockButtons(root: View) {
        val layoutCollapsed = root.findViewById<View>(R.id.layoutCollapsed)
        val layoutExpanded = root.findViewById<View>(R.id.layoutExpanded)
        val btnCollapseDock = root.findViewById<View>(R.id.btnCollapseDock)
        val btnBottomCollapse = root.findViewById<View>(R.id.btnBottomCollapse)

        val btnAddPoint = root.findViewById<View>(R.id.btnAddPoint)
        val btnRemovePoint = root.findViewById<View>(R.id.btnRemovePoint)
        val btnPlayTest = root.findViewById<View>(R.id.btnPlayTest)
        val btnCreateMacroBtn = root.findViewById<View>(R.id.btnCreateMacroBtn)

        val btnSpeedFast = root.findViewById<TextView>(R.id.btnSpeedFast)
        val btnSpeedNormal = root.findViewById<TextView>(R.id.btnSpeedNormal)
        val btnSpeedSlow = root.findViewById<TextView>(R.id.btnSpeedSlow)

        val btnStartRecord = root.findViewById<View>(R.id.btnStartRecord)
        val btnToggleVisibility = root.findViewById<View>(R.id.btnToggleVisibility)
        val btnClearPoints = root.findViewById<View>(R.id.btnClearPoints)
        val btnCloseService = root.findViewById<View>(R.id.btnCloseService)

        // 1. Mở rộng khi chạm vào tab mép
        layoutCollapsed?.setOnClickListener {
            if (isOverlayHidden) {
                isOverlayHidden = false
                targetPins.forEach { it.view.visibility = View.VISIBLE }
                triggerButtons.forEach { it.view.visibility = View.VISIBLE }
                layoutCollapsed.alpha = 1.0f
            }
            layoutCollapsed.visibility = View.GONE
            layoutExpanded.visibility = View.VISIBLE
        }

        // 2. Thu gọn về mép màn hình
        btnCollapseDock?.setOnClickListener {
            collapseDock()
        }
        btnBottomCollapse?.setOnClickListener {
            collapseDock()
        }

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

        // 4. Thêm Điểm Ghim ①, ②, ③...
        btnAddPoint?.setOnClickListener {
            addNewTargetPin()
        }

        // 5. Bớt Điểm Ghim cuối
        btnRemovePoint?.setOnClickListener {
            removeLastTargetPin()
        }

        // 6. Thử chạy ngay chuỗi điểm ghim
        btnPlayTest?.setOnClickListener {
            executeTargetPinsNow()
        }

        // 7. 🚀 TẠO NÚT BẤM TURBO ĐỘC LẬP TỪ CÁC ĐIỂM GHIM
        btnCreateMacroBtn?.setOnClickListener {
            createTriggerButtonFromPins()
        }

        // 8. 🔴 BẮT ĐẦU GHI THAO TÁC TRỰC TIẾP
        btnStartRecord?.setOnClickListener {
            openRecordOverlay()
        }

        // 9. 👁️ Ẩn/Hiện toàn bộ nút
        btnToggleVisibility?.setOnClickListener {
            toggleOverlayVisibility()
        }

        // 10. Xóa hết điểm ghim
        btnClearPoints?.setOnClickListener {
            clearAllTargetPins()
            Toast.makeText(this, "Đã xóa toàn bộ điểm ghim", Toast.LENGTH_SHORT).show()
        }

        // 11. Tắt Turbo Service
        btnCloseService?.setOnClickListener {
            stopSelf()
        }
    }

    private fun toggleOverlayVisibility() {
        isOverlayHidden = !isOverlayHidden
        val layoutCollapsed = dockView?.findViewById<View>(R.id.layoutCollapsed)
        val layoutExpanded = dockView?.findViewById<View>(R.id.layoutExpanded)

        if (isOverlayHidden) {
            targetPins.forEach { it.view.visibility = View.GONE }
            triggerButtons.forEach { it.view.visibility = View.GONE }
            layoutExpanded?.visibility = View.GONE
            layoutCollapsed?.visibility = View.VISIBLE
            layoutCollapsed?.alpha = 0.35f
            Toast.makeText(this, "Đã ẩn toàn bộ nút! Chạm vào mép màn hình để hiện lại.", Toast.LENGTH_SHORT).show()
        } else {
            targetPins.forEach { it.view.visibility = View.VISIBLE }
            triggerButtons.forEach { it.view.visibility = View.VISIBLE }
            layoutCollapsed?.alpha = 1.0f
            Toast.makeText(this, "Đã hiện lại các nút Game Turbo!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addNewTargetPin(x: Int? = null, y: Int? = null) {
        val index = targetPins.size + 1
        val initialX = x ?: ((screenWidth / 2) - 80 + (targetPins.size * 30))
        val initialY = y ?: ((screenHeight / 2) - 100 + (targetPins.size * 40))

        val pin = TargetPinView(
            context = this,
            windowManager = windowManager,
            index = index,
            initialX = initialX,
            initialY = initialY,
            onPinClicked = { p ->
                val (cx, cy) = p.getCenterCoordinates()
                scope.launch(Dispatchers.IO) {
                    ShellExecutor.tap(cx, cy, 45)
                }
                Toast.makeText(this@TurboOverlayService, "▶ Đã thử kích hoạt Điểm #${p.index} vào game!", Toast.LENGTH_SHORT).show()
            }
        )
        targetPins.add(pin)

        Toast.makeText(this, "Đã thêm Điểm #$index. Kéo đặt lên chiêu và chạm vào điểm để test thử!", Toast.LENGTH_SHORT).show()
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
     * Tạo Nút Tròn Kích Hoạt Combo Độc Lập
     */
    private fun createTriggerButtonFromPins() {
        if (targetPins.isEmpty()) {
            Toast.makeText(this, "Chưa có điểm ghim nào! Bấm [+ Ghim Điểm] để đặt vị trí chiêu trước.", Toast.LENGTH_LONG).show()
            return
        }

        val points = targetPins.map { it.getCenterCoordinates() }
        val name = "C${triggerButtons.size + 1}"
        val btnX = (screenWidth - 200).coerceAtLeast(60)
        val btnY = (screenHeight / 2) - 80

        val triggerBtn = FloatingTriggerView(
            context = this,
            windowManager = windowManager,
            name = name,
            points = points,
            initialX = btnX,
            initialY = btnY,
            delayBetweenMs = selectedSpeedDelay,
            repeatCount = 1,
            onTrigger = { pts, delay, repeat ->
                scope.launch(Dispatchers.IO) {
                    ShellExecutor.executeCombo(pts, delayBetweenMs = delay, repeatCount = repeat)
                }
            },
            onRestorePins = { restoredPoints ->
                restorePinsFromCombo(restoredPoints)
            },
            onDelete = { btn ->
                triggerButtons.remove(btn)
                Toast.makeText(this, "Đã xóa nút Combo", Toast.LENGTH_SHORT).show()
            }
        )
        triggerButtons.add(triggerBtn)

        clearAllTargetPins()
        collapseDock()
        Toast.makeText(this, "✅ Đã tạo nút [$name]! Chạm nút để xả combo, nhấn giữ để chỉnh combo.", Toast.LENGTH_LONG).show()
    }

    private fun restorePinsFromCombo(points: List<Pair<Float, Float>>) {
        clearAllTargetPins()
        points.forEach { (x, y) ->
            addNewTargetPin((x - 22).toInt(), (y - 22).toInt())
        }
        val layoutCollapsed = dockView?.findViewById<View>(R.id.layoutCollapsed)
        val layoutExpanded = dockView?.findViewById<View>(R.id.layoutExpanded)
        layoutCollapsed?.visibility = View.GONE
        layoutExpanded?.visibility = View.VISIBLE
    }

    private fun clearAllTriggerButtons() {
        triggerButtons.forEach { it.destroy() }
        triggerButtons.clear()
    }

    /**
     * Ghi thao tác màn hình trực tiếp
     */
    private fun openRecordOverlay() {
        if (recordOverlayView != null) return

        try {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_MacroGaming)
            val inflater = LayoutInflater.from(themedContext)
            recordOverlayView = inflater.inflate(R.layout.view_touch_recorder, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            val canvas = recordOverlayView!!.findViewById<TouchRecorderCanvas>(R.id.touchCanvas)
            val tvCount = recordOverlayView!!.findViewById<TextView>(R.id.tvPointCount)
            val btnUndo = recordOverlayView!!.findViewById<TextView>(R.id.btnUndoRecord)
            val btnCancel = recordOverlayView!!.findViewById<TextView>(R.id.btnCancelRecord)
            val btnStop = recordOverlayView!!.findViewById<TextView>(R.id.btnStopRecord)

            canvas.onCountChanged = { count ->
                tvCount.text = "Đã ghi: $count thao tác"
            }

            // Chuyển tiếp ngay lập tức thao tác chạm/vuốt vào game trong thời gian thực!
            // Nhờ đó khi bấm vào shop thì shop mở thật, bấm chiêu thì chiêu tung thật!
            canvas.onActionRecorded = { action ->
                scope.launch(Dispatchers.IO) {
                    when (action.type) {
                        com.macrophone.gaming.data.model.MacroType.TAP, com.macrophone.gaming.data.model.MacroType.HOLD -> {
                            val pt = action.points.firstOrNull() ?: return@launch
                            val dur = if (action.type == com.macrophone.gaming.data.model.MacroType.HOLD) {
                                action.durationMs.coerceIn(50L, 1000L)
                            } else {
                                45L
                            }
                            ShellExecutor.tap(pt.x, pt.y, dur)
                        }
                        com.macrophone.gaming.data.model.MacroType.SWIPE -> {
                            val start = action.points.firstOrNull() ?: return@launch
                            val end = action.points.lastOrNull() ?: return@launch
                            val dur = action.durationMs.coerceIn(50L, 500L)
                            ShellExecutor.swipe(start.x, start.y, end.x, end.y, dur)
                        }
                    }
                }
            }

            btnUndo?.setOnClickListener {
                canvas.undoLast()
            }

            btnCancel?.setOnClickListener {
                closeRecordOverlay()
            }

            btnStop?.setOnClickListener {
                val actions = canvas.getRecordedActions()
                if (actions.isEmpty()) {
                    Toast.makeText(this, "Chưa ghi thao tác nào! Hãy chạm trên màn hình.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val recordedActionsList = ArrayList(actions)
                closeRecordOverlay()

                val points = actions.mapNotNull { act ->
                    act.points.firstOrNull()?.let { Pair(it.x, it.y) }
                }

                val name = "R${triggerButtons.size + 1}"
                val btnX = (screenWidth - 200).coerceAtLeast(60)
                val btnY = (screenHeight / 2) - 80

                val triggerBtn = FloatingTriggerView(
                    context = this,
                    windowManager = windowManager,
                    name = name,
                    points = points,
                    initialX = btnX,
                    initialY = btnY,
                    delayBetweenMs = selectedSpeedDelay,
                    repeatCount = 1,
                    onTrigger = { pts, delay, repeat ->
                        scope.launch(Dispatchers.IO) {
                            if (recordedActionsList.isNotEmpty()) {
                                ShellExecutor.executeActions(recordedActionsList, repeatCount = repeat)
                            } else {
                                ShellExecutor.executeCombo(pts, delayBetweenMs = delay, repeatCount = repeat)
                            }
                        }
                    },
                    onRestorePins = { restoredPoints ->
                        restorePinsFromCombo(restoredPoints)
                    },
                    onDelete = { btn ->
                        triggerButtons.remove(btn)
                        Toast.makeText(this, "Đã xóa nút Combo", Toast.LENGTH_SHORT).show()
                    }
                )
                triggerButtons.add(triggerBtn)

                Toast.makeText(this, "✅ Đã tạo nút [$name]! Chạm nút để xả combo, nhấn giữ để chỉnh combo.", Toast.LENGTH_LONG).show()
            }

            dockView?.visibility = View.GONE
            windowManager.addView(recordOverlayView, params)
        } catch (e: Throwable) {
            closeRecordOverlay()
            Toast.makeText(this, "Lỗi mở giao diện ghi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeRecordOverlay() {
        if (recordOverlayView != null) {
            removeViewSafely(recordOverlayView)
            recordOverlayView = null
        }
        dockView?.visibility = View.VISIBLE
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
