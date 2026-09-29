# Nhật ký thay đổi (Changelog) - Macro Liên Quân Mobile 🎮⚡

Tất cả các thay đổi và bản cải tiến của dự án sẽ được ghi nhận và cập nhật theo từng phiên bản tại đây.

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
