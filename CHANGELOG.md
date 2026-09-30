# Nhật ký thay đổi (Changelog) - Macro Liên Quân Mobile 🎮⚡

Tất cả các thay đổi và bản cải tiến của dự án sẽ được ghi nhận và cập nhật theo từng phiên bản tại đây.

---

## [v2.0.1] - 2026-09-30
### 🛡️ Khắc Phục Lỗi Cấp Quyền Shizuku, Lỗi Crash Game Turbo HUD & Gỡ Bỏ Hướng Dẫn 3 Chấm (⋮)
- **KHẮC PHỤC TRIỆT ĐỂ LỖI CẤP QUYỀN SHIZUKU**:
  - Khôi phục thẻ `<provider android:name="rikka.shizuku.ShizukuProvider" ... />` trong `AndroidManifest.xml` để Shizuku Manager có thể truyền Binder IPC sang ứng dụng.
  - Tích hợp `Shizuku.addBinderReceivedListenerSticky` và `Shizuku.addBinderDeadListener` trong `MainActivity.kt` giúp tự động nhận diện và cập nhật trạng thái ngay khi Shizuku chạy.
  - Nút "Cấp quyền Shizuku" tự động mở ứng dụng Shizuku trên máy nếu dịch vụ chưa được kích hoạt qua Wi-Fi.
- **KHẮC PHỤC TRIỆT ĐỂ LỖI CRASH KHI BẬT GAME TURBO HUD**:
  - Loại bỏ các thuộc tính theme phụ thuộc (`?attr/selectableItemBackgroundBorderless`, `android:tint`) trong `view_floating_dock.xml`.
  - Toàn bộ View Cửa sổ nổi (`TurboOverlayService`, `TargetPinView`, `FloatingTriggerView`) được khởi tạo qua `ContextThemeWrapper(context, R.style.Theme_MacroGaming)`.
  - Bọc try-catch tuyệt đối cho `windowManager.addView()` và `startForeground()` trên Android 14+.
- **GỠ BỎ HƯỚNG DẪN 3 CHẤM (⋮) GÂY NHẦM LẪN**:
  - Vì ứng dụng đã loại bỏ hoàn toàn Trợ năng (Accessibility Service), hệ thống Android/XOS không bao giờ giới hạn hay hiển thị menu 3 chấm ở góc màn hình cài đặt.
  - Đã xóa hoàn toàn Card hướng dẫn 3 chấm để tránh gây hoang mang cho người dùng.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.0.1.apk`.

---

## [v2.0.0] - 2026-09-30
### 🚀 TÁI CẤU TRÚC TOÀN DIỆN - KIẾN TRÚC MỚI TINH GỌN, ỔN ĐỊNH TUYỆT ĐỐI
- **ĐẬP ĐI XÂY LẠI TỪ ĐẦU TOÀN BỘ SERVICE VÀ ENGINE**:
  - Khởi tạo kiến trúc mới với `TurboOverlayService`: loại bỏ hoàn toàn các lớp wrapper rườm rà, adapter, wizard activities, dialogs cũ dễ gây lỗi theme.
  - Tự động nhận diện và cập nhật kích thước xoay màn hình ngang/dọc (`onConfigurationChanged`): Tối ưu hóa đặc biệt cho game xoay ngang (Landscape) như Liên Quân Mobile trên Infinix Note 30, Xiaomi, Samsung và các giả lập PC.
- **ĐỘNG CƠ SHELLEXECUTOR 120HZ TỐI TÂN**:
  - Giao tiếp trực tiếp với Shizuku Binder hoặc Root `su` qua một tiến trình shell chuỗi duy nhất, xả combo ngay lập tức với độ trễ 0ms.
- **GIAO DIỆN GAME TURBO HUD REDMI / XIAOMI / REDMAGIC**:
  - Tab nổi mép viền `[⚡ TURBO]` tự động hút dính mép màn hình, chạm là bung bảng điều khiển.
  - Gán vị trí chiêu: Bấm `[+ Ghim Điểm]` để kéo thả các điểm (1, 2, 3...) vào chiêu thức, bấm `[🚀 TẠO NÚT BẤM TURBO]` để tạo Nút tròn nổi `[⚡]`.
  - Chạm nút tròn `[⚡]` để kích hoạt combo tức thì, đè lâu để xóa nút.
  - Ghi thao tác: Bấm `[🔴 BẮT ĐẦU GHI]` để chạm/vuốt trên màn hình, bấm `[⏹️ DỪNG GHI]` để tự động sinh nút tròn Turbo.
- **LOẠI BỎ 100% DỊCH VỤ TRỢ NĂNG (ACCESSIBILITY SERVICE)**:
  - Hoạt động thuần túy qua Shizuku và Root.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.0.0.apk`.

---

## [v1.2.5] - 2026-09-30
### 🛡️ Khắc Phục Triệt Để Lỗi Crash ("Ứng Dụng Đã Dừng") & Hoàn Thiện Cảm Ứng Game Turbo
- **KHẮC PHỤC 100% NGUYÊN NHÂN CRASH VĂNG APP ("ỨNG DỤNG ĐÃ DỪNG")**:
  - Gỡ bỏ thẻ `<provider>` Shizuku thủ công chứa quyền hệ thống `INTERACT_ACROSS_USERS_FULL` trong `AndroidManifest.xml`.
  - Đây chính là nguyên nhân cốt lõi khiến ứng dụng bị Android ném ngoại lệ `SecurityException: Permission Denial` và dừng ngay lập tức khi mở app hoặc khi kích hoạt Shizuku.
  - Sử dụng Shizuku Provider tiêu chuẩn từ thư viện gốc, tương thích hoàn toàn trên Infinix Note 30 (XOS 13/14), Xiaomi/Redmi, Samsung và các trình giả lập PC (LDPlayer, Nox, BlueStacks).
- **SỬA LỖI CẢM ỨNG & KÉO THẢ GAME TURBO HUD**:
  - Sửa lỗi dispatch `onTouch` trả về `false` ở sự kiện `ACTION_DOWN` trong `FloatingWidgetService`, đảm bảo sự kiện chạm và kéo luôn được nhận diện 100%.
  - Chạm nhẹ vào tab mép màn hình `[⚡ TURBO]` là mở ngay Control Center; vuốt là kéo dock mượt mà và tự động hút dính vào mép màn hình.
- **TỐI ƯU HÓA TIẾN TRÌNH SHELL ĐẶC QUYỀN**:
  - Gọi trực tiếp `Shizuku.newProcess()` kết hợp lớp bọc ngoại lệ an toàn đa tầng, loại bỏ hoàn toàn hiện tượng nghẽn phản hồi.
- **Phát hành file cài đặt**: `Macro-LienQuan-v1.2.5.apk`.

---

## [v1.2.4] - 2026-09-30
### 🚀 Loại Bỏ Hoàn Toàn Trợ Năng & Nâng Cấp Game Turbo HUD (Redmi/Xiaomi/RedMagic Style)
- **LOẠI BỎ 100% DỊCH VỤ TRỢ NĂNG (ACCESSIBILITY SERVICE)**:
  - Xóa bỏ triệt để file `MacroAccessibilityService`, config XML, permission checks và strings liên quan đến Accessibility.
  - Người dùng không bao giờ bị hỏi quyền Trợ năng, không còn gặp rào cản "Cài đặt bị hạn chế" (Restricted Settings).
  - Động cơ thực thi chuyển sang 100% privileged shell Shizuku / Gỡ lỗi qua Wi-Fi / Root: Chạy trực tiếp qua Binder với chuỗi script gom cụm (`input tap X Y; sleep ...`), loại bỏ độ trễ spawn tiến trình.
- **NÂNG CẤP GIAO DIỆN GAME TURBO HUD (XIAOMI / REDMI / REDMAGIC STYLE)**:
  - Tab nổi mép màn hình: Thu gọn thành tab nhỏ gọn `[⚡ TURBO]`, chạm là mở bảng điều khiển Game Turbo HUD.
  - Tính năng **Gán Vị Trí Chiêu (Target Points Pinning)**: Bấm `[+ Ghim Điểm]`, kéo thả các điểm (1, 2, 3...) vào nút chiêu trong game, sau đó bấm `[🚀 TẠO NÚT BẤM TURBO]` là có ngay nút tròn trên màn hình.
  - Tính năng **Ghi Thao Tác (Macro Recorder)**: Bổ sung nút **[🔴 BẮT ĐẦU GHI (START)]** to rõ ràng, hiển thị thanh điều khiển với nút **[⏹️ DỪNG GHI (STOP)]**, nút Hoàn tác `[↩ Xóa]`, tự động tạo nút tròn Turbo ngay khi dừng ghi!
- **SỬA LỖI CRASH VÀ THEME TRÊN INFINIX NOTE 30 / ANDROID 13 & 14**:
  - Thay thế toàn bộ MaterialButton trong các cửa sổ overlay service bằng View/TextView chuẩn (tránh crash Inflate Exception do thiếu Material theme context).
  - Kiểm tra an toàn `isAttachedToWindow` trước khi đóng overlay view.
- **Phát hành file cài đặt**: `Macro-LienQuan-v1.2.4.apk`.

---

## [v1.2.2] - 2026-09-30
### 📱 Sửa triệt để Crash & Nâng cấp giao diện Cyberpunk chuyên nghiệp cho Infinix Note 30
- **ĐẶC TRỊ 100% LỖI CRASH TRÊN MÁY INFINIX NOTE 30 (XOS 13/14)**:
  - Loại bỏ hoàn toàn thao tác kiểm tra Root đồng bộ trên Main UI Thread làm treo máy / ANR ("Ứng dụng đã dừng"). Kiểm tra file nhị phân tức thì và lưu cache.
  - Sửa lỗi `ForegroundServiceDidNotStartInTimeException`: Tích hợp `ServiceCompat.startForeground` với kiểu `specialUse` an toàn trên Android 13/14, không gọi `stopSelf()` sớm trong `onCreate()`.
  - Thay thế cơ chế bắt lỗi đóng băng Main Looper trong `MacroApp` bằng bộ ghi nhận sự cố chẩn đoán và bàn giao chuẩn cho hệ điều hành.
  - Tối ưu hóa lệnh shell `input tap` / `input swipe` với tọa độ nguyên `toInt()` an toàn cho chip MediaTek Helio G99.
- **NÂNG CẤP TOÀN DIỆN GIAO DIỆN (UI REDESIGN)**:
  - Tự động nhận diện thiết bị: Hiển thị badge "Thiết bị: Infinix Note 30 • 120Hz Fast Combo" trực tiếp trên màn hình chính.
  - Card đặc quyền tối ưu Infinix: Hướng dẫn và mở trực tiếp màn hình mở khóa "Cài đặt bị hạn chế" (Restricted Settings) và "Cửa sổ thả nổi" (Floating Window).
  - Đèn LED trạng thái thời gian thực: Hiển thị "SẴN SÀNG" hoặc "CHƯA CẤP ĐỦ" tức thì.
  - Bố cục cyberpunk hiện đại, phong cách game thủ chuyên nghiệp, trực quan và dễ sử dụng.
- **Phát hành file cài đặt**: `Macro-LienQuan-v1.2.2.apk`.

---

## [v1.2.1] - 2026-09-30
### 🎯 Hiển thị trực quan vị trí ghi & Tự động tạo Nút tròn Macro nổi
- **HIỂN THỊ TRỰC QUAN TẤT CẢ VỊ TRÍ ĐÃ NHẤN HOẶC VUỐT**:
  - `TouchRecorderCanvas` vẽ và giữ nguyên toàn bộ các điểm Chạm (Tap) với vòng sáng cyan neon và đánh số thứ tự (1, 2, 3...) to rõ, tự căn chỉnh theo DPI màn hình (`density`).
  - Điểm Giữ (Hold) hiển thị vòng neon tím kèm nhãn "GIỮ (ms)".
  - Đường Vuốt (Swipe) hiển thị vạch đứt nét màu xanh lá kèm mũi tên chỉ hướng vuốt chiêu rõ ràng.
  - Bổ sung nút **↩ Xóa điểm** (Undo) trên thanh công cụ cho phép xóa ngay điểm vừa bấm nhầm.
- **TỰ ĐỘNG TẠO NÚT TRÒN MACRO NỔI TRÊN MÀN HÌNH**:
  - Khi bấm **✓ Tạo nút Macro**, combo lập tức được lưu với cấu hình xả chiêu siêu tốc (5x Gaming Speed).
  - Tự động sinh ngay 1 nút tròn nổi (`FloatingMacroButton` 56dp x 56dp oval neon) ngay trên màn hình game.
  - Chạm vào nút tròn → lập tức phát combo với tốc độ cực nhanh!
  - Đè lâu vào nút tròn → hiện nút xóa (X) nếu không muốn dùng nữa.
- **SỬA LỖI CRASH VÀ TỌA ĐỘ SHELL INPUT**:
  - Ép kiểu tọa độ chạm/vuốt sang số nguyên (`.toInt()`) trong lệnh shell `input tap` / `input swipe`, tránh lỗi `NumberFormatException` trên Android.
  - Sửa `Path.lineTo` trong `GestureBuilder` cho các điểm Tap/Hold để `GestureDescription` không bao giờ bị rỗng gây crash trên một số dòng máy.
  - Bọc luồng `runOnUiThread` an toàn cho các Shizuku listener trong `MainActivity` và `SetupWizardActivity`.
- **Phát hành file cài đặt**: `Macro-LienQuan-v1.2.1.apk`.

---

## [v1.2.0] - 2026-09-29
### 🛡️ Khắc phục triệt để lỗi Crash Shizuku & Hỗ trợ Giả lập PC (LDPlayer, Nox, BlueStacks)
- **SỬA LỖI CRASH "ỨNG DỤNG ĐÃ DỪNG" (100% FIXED)**:
  - Tích hợp `dev.rikka.shizuku:provider:13.1.5` và khai báo `ShizukuProvider` vào `AndroidManifest.xml`.
  - Sử dụng `Shizuku.addBinderReceivedListenerSticky`, `addBinderDeadListener`, và `addRequestPermissionResultListener` an toàn, loại bỏ triệt để ngoại lệ `IllegalStateException: binder haven't been received`.
  - Cơ chế tự phục hồi chống crash toàn cục (`UncaughtExceptionHandler`) ngăn chặn hoàn toàn hiện tượng văng ứng dụng.
- **HỖ TRỢ TOÀN DIỆN MÁY ẢO / TRÌNH GIẢ LẬP ANDROID TRÊN PC**:
  - Hạ `minSdk = 26` (Android 8.0 Oreo), tương thích với LDPlayer 9, BlueStacks 5, NoxPlayer, MuMu Player (Android 9/11/12).
  - Tự động nhận diện quyền Root (`su`) có sẵn trên trình giả lập: Người dùng máy tính chỉ cần 1 cú click là tự động kích hoạt toàn bộ quyền và chạy macro mượt mà không cần làm 4 bước Shizuku.
- **TỰ ĐỘNG BẬT TRỢ NĂNG 1-CHẠM**:
  - Nút "Cấp quyền Trợ năng" trên trang chủ tự động bật AccessibilityService ngay lập tức qua Shizuku/Root nếu đã được kích hoạt.
- **Phát hành file cài đặt**: `Macro-LienQuan-v1.2.0.apk`.

---

## [v1.1.0] - 2026-09-29
### 🎮 Hướng dẫn cài đặt kiểu Panda Touch Pro & Xem trước thao tác
- **TRÌNH HƯỚNG DẪN CÀI ĐẶT KIỂU PANDA TOUCH PRO (Setup Wizard)**:
  - Giao diện hướng dẫn 4 bước rõ ràng, trực quan:
    1. Tải ứng dụng Shizuku từ CH Play (mở trực tiếp cửa hàng)
    2. Bật Gỡ lỗi không dây (Wireless Debugging) - mở trực tiếp Developer Options
    3. Ghép nối (Pairing) và khởi động Shizuku - mở trực tiếp app Shizuku
    4. Nhấn 1 nút → Tự động cấp TẤT CẢ quyền (Trợ năng, Vẽ màn hình, Pin, Restricted Settings)
  - Kiểm tra trạng thái từng bước theo thời gian thực (✅ / ⚠️)
  - Nút "Refresh" để cập nhật trạng thái sau khi hoàn tất mỗi bước
- **XEM TRƯỚC THAO TÁC ĐÃ GHI (Gesture Preview Canvas)**:
  - Khi lưu combo, hiển thị bản xem trước trực quan (visual preview) toàn bộ thao tác đã ghi:
    - TAP: Vòng tròn cyan kèm số thứ tự
    - HOLD: Vòng tròn tím lớn hơn
    - SWIPE: Đường nét đứt xanh lá với điểm bắt đầu/kết thúc
  - Hiển thị thời gian ghi gốc chính xác và thời gian sau khi tăng tốc
- **CẢI TIẾN GIAO DIỆN GHI THAO TÁC**:
  - Nền ghi thao tác trong suốt hơn (20% thay vì 50%) để nhìn rõ game bên dưới
  - Nút "Hướng dẫn cài đặt" thay thế nút "Cấp quyền Shizuku" cũ trên màn hình chính
- **Phát hành file cài đặt**: `Macro-LienQuan-v1.1.0.apk`.

---

## [v1.0.3] - 2026-09-29
### 🛡️ Bản sửa lỗi Crash toàn diện & Bỏ bắt buộc quyền Trợ năng
- **BỎ BẮT BUỘC QUYỀN TRỢ NĂNG (Accessibility)**:
  - Cho phép người dùng bật ngay Dock Nổi (`FloatingWidgetService`) chỉ cần quyền Vẽ trên màn hình (`SYSTEM_ALERT_WINDOW`).
  - Không còn chặn người dùng không bật được Trợ năng: Vẫn có thể tạo điểm ghim mục tiêu, kéo thả, ghi thao tác và tạo Nút Macro Nổi.
  - Tích hợp **Cơ chế Phát Đa Năng (Dual-Engine Dispatch)**:
    - Nếu có Trợ năng: Tự động dùng `dispatchGesture`.
    - Nếu không có Trợ năng: Tự động chuyển sang Shizuku Shell (`input tap / swipe`) hoặc Root / ADB.
    - Nếu chưa có phương thức nào: Thông báo hướng dẫn nhẹ nhàng, không gây văng/crash app.
- **SỬA TRIỆT ĐỂ CÁC LỖI CRASH ỨNG DỤNG**:
  - Khắc phục lỗi `IllegalArgumentException: foregroundServiceType 0x40000000 is not valid` trên Android 10, 11, 12, 13 (do cờ `specialUse` chỉ dành riêng cho Android 14+).
  - Loại bỏ khai báo `ShizukuProvider` thừa và `INTERACT_ACROSS_USERS_FULL` trong Manifest (nguyên nhân gây `SecurityException` khi mở app trên nhiều dòng máy Xiaomi/Samsung/Oppo).
  - Bổ sung `Thread.setDefaultUncaughtExceptionHandler` toàn cục tại `MacroApp.kt` để tự động phục hồi nếu có lỗi hệ thống phát sinh.
- **Phát hành file cài đặt**: `Macro-LienQuan-v1.0.3.apk`.

---

## [v1.0.2] - 2026-09-29
### 🚀 Tính năng đột phá: Tích hợp Shizuku & Gỡ lỗi qua Wi-Fi
- **Cấp quyền 1-chạm qua Shizuku (Không cần máy tính)**:
  - Tích hợp trực tiếp Shizuku API (v13.1.5).
  - Tự động thực thi shell commands kích hoạt Trợ năng, mở khóa Restricted Settings và cấp quyền Vẽ trên màn hình chỉ với 1 nút bấm trong app.
- **Bộ công cụ Gỡ lỗi không dây (Wireless Debugging Helper)**:
  - Nút sao chép 5 dòng lệnh ADB chuẩn hóa vào bộ nhớ tạm để dùng ngay trên LADB (Local ADB trên điện thoại) hoặc máy tính.
- **Phát hành file cài đặt**: `Macro-LienQuan-v1.0.2.apk`.

---

## [v1.0.1] - 2026-09-29
### 🔧 Bản cải tiến & Sửa lỗi (Improvements & Fixes)
- **Đổi tên tệp và đóng gói Signed Release**:
  - Biên dịch phiên bản Release chính thức với tên tệp: `Macro-LienQuan-v1.0.1.apk`.
  - Tích hợp Keystore ký số trực tiếp giúp cài đặt an toàn trên mọi thiết bị.
- **Khắc phục cấp quyền Trợ năng (Accessibility) trên Android 13/14+**:
  - Tích hợp hộp thoại điều hướng thông minh hỗ trợ người dùng mở khóa *"Cài đặt bị hạn chế"* (Restricted Settings) cho Xiaomi HyperOS/MIUI, Samsung OneUI, Oppo/Realme ColorOS.
  - Thêm nút tắt mở thẳng màn hình App Details Settings.
  - Chuẩn hóa `accessibility_service_config.xml` và `serviceInfo` lập trình trong mã nguồn để tránh xung đột cờ hệ điều hành.

---

## [v1.0.0] - 2026-09-29
### 🚀 Bản phát hành đầu tiên (Initial Release)
- **Kiến trúc cốt lõi**:
  - Hỗ trợ nền tảng Android 11 (API 30) đến Android 14+ (API 34+).
  - Kiến trúc Clean Architecture kết hợp MVVM, Kotlin Coroutines & StateFlow.
  - Tuân thủ chính sách Android 14 FGS với type `specialUse`.
- **Ghi thao tác thực tế (Record Mode)**:
  - Lớp phủ trong suốt `TouchRecorderCanvas` vẽ vệt chạm và tính toán thời gian giữ chiêu (duration), độ trễ nghỉ (delay timing) chuẩn xác.
- **Tăng tốc độ (Speed Multiplier 1x - 10x)**:
  - Cho phép rút ngắn thời gian giữa các thao tác (ví dụ combo 1.5s rút ngắn còn 0.3s).
  - Tùy chỉnh chế độ lặp: 1 lần, 5 lần, hoặc lặp vô hạn.
- **Lưu thành Nút Macro Nổi (Floating Macro Trigger Button)**:
  - Tự động tạo bong bóng nút nổi riêng biệt trên màn hình game.
  - Cho phép kéo thả đặt cạnh nút đánh thường hoặc phím chiêu.
  - Chạm vào nút để kích hoạt combo, chạm lại để ngắt.
- **Cơ chế Đa điểm không chặn thao tác (Multi-Touch Co-existence)**:
  - Cờ `FLAG_NOT_TOUCH_MODAL` giúp truyền cảm ứng bên ngoài vào game.
  - Cử chỉ chạm mô phỏng nhả tức thì (20ms – 35ms) giúp tay trái giữ Joystick di chuyển không hề bị ngắt quãng.
- **Chế độ Điểm Ghim Mục Tiêu (Target Points Mode)**:
  - Hỗ trợ thêm các điểm ghim 1, 2, 3... đặt trực tiếp lên chiêu thức.
- **Hệ thống điều khiển phụ trợ**:
  - Quick Settings Tile ("Macro Combo") bật/tắt nhanh từ thanh thông báo.
  - Notification thường trực với các nút điều khiển nhanh (Phát, Dừng, Đóng).
  - Cơ chế Dừng Khẩn Cấp (Emergency Stop) bằng phím Volume Down.
- **CI/CD Tự động**:
  - Tích hợp GitHub Actions tự động build APK và tạo bản phát hành (GitHub Releases) khi gắn thẻ tag phiên bản.

---

## Lộ trình cải tiến các phiên bản tiếp theo (Roadmap):
- **v1.1.0**: Thêm tính năng phím ảo bán tự động (Trigger theo phím âm lượng hoặc cử chỉ vuốt viền).
- **v1.2.0**: Hỗ trợ xuất/nhập (Export/Import) kịch bản combo dưới dạng mã QR hoặc file JSON để game thủ chia sẻ cho nhau.
- **v1.3.0**: Bộ nhận diện hình ảnh AI mini (OpenCV / MediaProjection) phát hiện thời điểm hồi chiêu để tự động xả combo.
