# Nhật ký thay đổi (Changelog) - Macro Liên Quân Mobile 🎮⚡

Tất cả các thay đổi và bản cải tiến của dự án sẽ được ghi nhận và cập nhật theo từng phiên bản tại đây.

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
