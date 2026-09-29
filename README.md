# Macro Gaming Phone (Fast Combo Controller) 🎮⚡

Dự án Android hoàn chỉnh bằng **Kotlin** hỗ trợ từ **Android 11 (API 30) đến Android 14+ (API 34+)**, cung cấp giải pháp ghi lại thao tác cảm ứng của game thủ, tăng tốc độ chuỗi thao tác (Speed Multiplier 1x – 10x), và **lưu thành NÚT MACRO NỔI độc lập trên màn hình** để kích hoạt combo tức thì.

---

### 📥 Tải file APK cài đặt ngay:
👉 **[Tải file APK chính thức (Macro-LienQuan-v1.0.2.apk)](https://github.com/saimondragonvn/macro-lienquan/releases/download/v1.0.2/Macro-LienQuan-v1.0.2.apk)**  
👉 **[Xem toàn bộ các bản phát hành (Releases)](https://github.com/saimondragonvn/macro-lienquan/releases)**

---

### ⚡ CÁCH CẤP QUYỀN TRỢ NĂNG 1-CHẠM BẰNG SHIZUKU (GỠ LỖI QUA WI-FI):
Nếu máy bạn bị mờ công tắc Trợ năng hoặc không bật được thủ công:
1. Cài ứng dụng **Shizuku** từ Google Play (hoặc GitHub).
2. Vào **Cài đặt điện thoại > Tùy chọn nhà phát triển > Bật "Gỡ lỗi không dây" (Wireless Debugging)**.
3. Mở Shizuku > Chọn **Ghép nối (Pairing)** qua mã 6 số Wi-Fi > Nhấn **Khởi động (Start)**.
4. Mở app **Macro Gaming Combo**, bấm nút tím: **`[⚡ Cấp quyền Shizuku]`** $\rightarrow$ App sẽ tự động cấp toàn bộ quyền và kích hoạt Trợ năng ngay lập tức mà không cần bấm thêm bất cứ thứ gì!

---

### 📋 Hoặc dùng LADB (Chạy ADB ngay trên điện thoại không cần PC):
Trong app có nút **`[📋 Lệnh Wi-Fi]`**, bấm để copy 5 dòng lệnh sau dán vào LADB hoặc máy tính:
```bash
appops set com.macrophone.gaming ACCESS_RESTRICTED_SETTINGS allow
appops set com.macrophone.gaming SYSTEM_ALERT_WINDOW allow
settings put secure enabled_accessibility_services com.macrophone.gaming/com.macrophone.gaming.service.MacroAccessibilityService
settings put secure accessibility_enabled 1
dumpsys deviceidle whitelist +com.macrophone.gaming
```

---

> [!IMPORTANT]
> **Đặc điểm cốt lõi**:
> 1. **Ghi thao tác**: Ghi lại toàn bộ chuỗi bấm chiêu, vuốt, giữ và độ trễ trong trận đấu.
> 2. **Chỉnh tốc độ**: Rút ngắn thời gian giữa các thao tác (ví dụ combo 1.5s tăng tốc 5x chỉ còn 0.3s).
> 3. **Lưu thành NÚT MACRO NỔI**: Tạo một nút bấm tròn nhỏ gọn trên màn hình game. Bật/chạm nút là combo tự động kích hoạt.
> 4. **Hoàn toàn không chặn cảm ứng khác (Multi-touch Co-existence)**: Trong lúc nút macro đang phát combo, **tay trái của bạn vẫn ghì giữ joystick chạy vòng quanh, tay phải vẫn vuốt góc nhìn hoặc bấm nút khác hoàn toàn bình thường**!

---

## 🏛️ 1. Kiến trúc & Công nghệ cốt lõi

- **Ngôn ngữ & Mô hình**: Kotlin, Clean Architecture kết hợp MVVM, Reactive StateFlow & Coroutines.
- **Nút Macro Nổi độc lập (Floating Macro Trigger Buttons)**:
  - Mỗi kịch bản combo có thể tạo một nút bấm nổi riêng biệt trên màn hình game (`FloatingMacroButton`: M1, M2, Fast Combo...).
  - Kéo thả tự do đến góc tiện tay bấm nhất (cạnh nút đánh thường, cạnh phím chiêu).
  - Cờ `FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL` giúp 100% diện tích màn hình còn lại không bị chặn cảm ứng.
- **Mô phỏng cảm ứng**: Tận dụng `AccessibilityService` với `dispatchGesture()` và `GestureDescription.Builder`:
  - `TAP`: Chạm siêu nhanh với thời lượng giải phóng Input Pointer tức thì (**20ms – 35ms**).
  - `HOLD`: Nhấn giữ tại vị trí cụ thể theo thời gian tùy chỉnh.
  - `SWIPE`: Vuốt theo quỹ đạo liên tục qua danh sách các điểm tọa độ $(X, Y)$.
- **Hệ thống điều khiển**:
  - Thanh tiện ích Dock nổi ghim viền (Smooth Edge Snapping).
  - `TileService` (Quick Settings Tile): Bật/tắt thanh dock trực tiếp từ bảng thông báo hệ thống.
  - **Emergency Stop**: Bấm phím **Volume Down** để ngắt tức thì phiên phát macro khi có biến.

---

## 🚀 2. Quy trình sử dụng: Ghi thao tác ➔ Tăng tốc ➔ Tạo Nút Macro

### Bước 1: Ghi thao tác cử chỉ (Record Session)
- Bấm nút **Record** (🔴) trên thanh Dock.
- Thực hiện chuỗi bấm chiêu trên màn hình (ví dụ: Chiêu 2 $\rightarrow$ Chiêu 1 $\rightarrow$ Đánh thường $\rightarrow$ Tốc biến).
- Hệ thống ghi lại chính xác:
  - Tọa độ $(X, Y)$ của từng điểm chạm (Down, Move, Up).
  - Khoảng cách thời gian nghỉ giữa các lần bấm (Delay timing).
- Bấm **Lưu Combo**.

### Bước 2: Chỉnh tốc độ siêu tốc (Speed Multiplier)
- Cửa sổ cài đặt hiện ra:
  - Xem thống kê: *Thời gian ghi gốc: 1.50 giây (4 thao tác)*.
  - Chọn hệ số tăng tốc: **1x, 2x, 3x, 5x, 10x**.
  - Xem thời gian sau khi tăng tốc: *0.30 giây (Nhanh hơn 5 lần! ⚡)*.
  - Chọn số lần lặp: 1 lần, 5 lần, hoặc Vô hạn.
  - Đặt tên nút: Ví dụ "Combo 1".

### Bước 3: Tạo Nút Macro Nổi trên màn hình game
- Tích chọn *"Tạo nút Macro nổi trên màn hình game"* $\rightarrow$ Bấm **LƯU & TẠO NÚT**.
- Ngay lập tức xuất hiện một **Nút Macro Nổi** (kích thước $56 \times 56\text{dp}$, viền LED Neon).
- Người chơi kéo nút này đặt ngay cạnh ngón tay cái bên phải.

### Bước 4: Chơi game & Kích hoạt Macro
- Khi vào pha giao tranh: **Chạm 1 cái vào Nút Macro Nổi**!
- Nút Macro chuyển sang màu xanh ngọc phát sáng và tự động xả combo với tốc độ thần tốc.
- Trong lúc macro đang bấm chiêu, **tay trái của bạn vẫn dùng cần gạt Joystick di chuyển nhân vật bình thường** không hề bị ngắt quãng!
- Chạm lại vào Nút Macro để dừng nếu muốn hủy ngang.

---

## 📂 3. Cấu trúc thư mục dự án

```
macro-phone/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/macrophone/gaming/
│       │   ├── MacroApp.kt
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   ├── GesturePoint.kt
│       │   │   │   ├── MacroAction.kt
│       │   │   │   ├── MacroSequence.kt
│       │   │   │   ├── MacroState.kt
│       │   │   │   ├── MacroType.kt
│       │   │   │   └── PlaybackConfig.kt
│       │   │   └── repository/
│       │   │       └── MacroRepository.kt
│       │   ├── domain/
│       │   │   └── MacroManager.kt
│       │   ├── service/
│       │   │   ├── FloatingWidgetService.kt
│       │   │   ├── MacroAccessibilityService.kt
│       │   │   ├── NotificationActionReceiver.kt
│       │   │   └── QuickSettingsTileService.kt
│       │   ├── ui/
│       │   │   ├── MainActivity.kt
│       │   │   ├── MainViewModel.kt
│       │   │   ├── adapter/
│       │   │   │   └── MacroPresetAdapter.kt
│       │   │   └── overlay/
│       │   │       ├── FloatingMacroButton.kt
│       │   │       ├── TargetPointMarker.kt
│       │   │       └── TouchRecorderCanvas.kt
│       │   └── util/
│       │       ├── GestureBuilder.kt
│       │       ├── NotificationHelper.kt
│       │       └── PermissionUtils.kt
│       └── res/
│           ├── drawable/
│           │   ├── bg_floating_macro_btn_idle.xml
│           │   ├── bg_floating_macro_btn_active.xml
│           │   ├── bg_floating_pill.xml
│           │   ├── bg_target_point.xml
│           │   ├── bg_dialog_card.xml
│           │   └── ic_*.xml
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── view_floating_dock.xml
│           │   ├── view_floating_macro_button.xml
│           │   ├── view_save_macro_dialog.xml
│           │   ├── view_macro_settings_dialog.xml
│           │   ├── view_target_point.xml
│           │   └── view_touch_recorder.xml
│           ├── values/
│           │   ├── colors.xml (Cyberpunk Dark Theme)
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               └── accessibility_service_config.xml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## ⚙️ 4. Hướng dẫn cấp quyền trên Android 11+ đến Android 14+

Để ứng dụng hoạt động chính xác, cần cấp 3 quyền hệ thống cốt lõi:

### 4.1 Cấp quyền qua giao diện ứng dụng:
1. **Dịch vụ Trợ năng (Accessibility Service)**:
   - Nhấn nút **Cấp quyền** tại mục Accessibility Service $\rightarrow$ Bật công tắc **Macro Gaming Combo Service**.
2. **Quyền vẽ trên ứng dụng khác (Display over other apps)**:
   - Nhấn nút **Cấp quyền** $\rightarrow$ Cho phép hiển thị trên ứng dụng khác.
3. **Bỏ qua tối ưu hóa pin (Battery Optimization Exemption)**:
   - Chọn "Không tối ưu hóa" để tránh bị hệ thống ngắt tiến trình ngầm.

### 4.2 Cấp quyền nhanh qua lệnh ADB (Dành cho Developer / Tester):
```bash
# 1. Cấp quyền vẽ trên ứng dụng khác (SYSTEM_ALERT_WINDOW)
adb shell appops set com.macrophone.gaming SYSTEM_ALERT_WINDOW allow

# 2. Bật dịch vụ Trợ năng tự động
adb shell settings put secure enabled_accessibility_services com.macrophone.gaming/com.macrophone.gaming.service.MacroAccessibilityService
adb shell settings put secure accessibility_enabled 1

# 3. Bỏ qua tối ưu pin
adb shell dumpsys deviceidle whitelist +com.macrophone.gaming
```

---

## 🛠️ 5. Hướng dẫn Build & Chạy qua Gradle CLI

### Lệnh biên dịch:
```bash
# Trên Windows PowerShell / Command Prompt:
gradlew.bat assembleDebug

# Trên Linux / macOS:
./gradlew assembleDebug
```

File APK sau khi build thành công sẽ nằm tại:
```
app/build/outputs/apk/debug/app-debug.apk
```
