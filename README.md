# Macro Liên Quân Mobile - Game Turbo Pro (120Hz) 🎮⚡

Ứng dụng **Game Turbo Combo Controller** độc quyền cho game thủ **Liên Quân Mobile (Arena of Valor)**, mô phỏng chuẩn xác tính năng **Combos trên dòng điện thoại Gaming cao cấp Redmi Turbo 4 Pro / K70 / Black Shark / ROG Phone**.

---

### 📥 Tải file APK cài đặt ngay:
👉 **[Tải file APK mới nhất (Macro-LienQuan-v2.4.0.apk)](https://github.com/saimondragonvn/macro-lienquan/releases/download/v2.4.0/Macro-LienQuan-v2.4.0.apk)**  
👉 **[Xem toàn bộ các bản phát hành (Releases)](https://github.com/saimondragonvn/macro-lienquan/releases)**

---

## 🚀 ĐẶC TÍNH NỔI BẬT CHUẨN REDMI TURBO 4 PRO

1. **ĐẬP ĐI XÂY LẠI TỪ SỐ 0 - GHI THỜI GIAN THỰC KHÔNG HỀ CHẶN MÀN HÌNH**:
   - Ứng dụng đọc luồng cảm ứng trực tiếp từ Linux kernel (`/dev/input/event*`) ngầm qua **Shizuku (Gỡ lỗi Wi-Fi ADB)** hoặc **Root (`su`)**.
   - **HOÀN TOÀN KHÔNG CHẶN CẢM ỨNG MÀN HÌNH**: Khi bấm `[🔴 Bắt đầu ghi combo]`, màn hình game thông thoáng 100%! Game thủ tự do di chuyển analog, mở Shop, mua đồ, bán đồ, tung chiêu trong trận đấu thực tế và game nhận 100%!
   - Bấm `[⏹ Xong & Lưu]` trên thanh Capsule ở mép trên màn hình là có ngay nút Combo.

2. **NÚT NỔI KÍCH HOẠT COMBO [⚡ R1, R2...] SIÊU TỐC 120HZ**:
   - Kéo thả tự do đến mọi vị trí thuận ngón tay (ngay cạnh nút đánh thường hoặc phím chiêu).
   - **Chạm nhanh (<350ms)**: Bắn combo thao tác vào game tức thì với độ trễ cực thấp.
   - **Nhấn giữ lâu (>450ms)**: Mở menu tùy chỉnh tốc độ phát lại (1x, 2x, 3x), độ mờ trong suốt (30% - 100%), số lần lặp, hoặc xóa nút.

3. **GÁN GHIM TRỰC QUAN (TARGET PINS)**:
   - Dễ dàng đặt các điểm ghim ①, ②, ③ trực tiếp lên nút game (ví dụ nút Shop, nút Giáp Hộ Mệnh, nút Mua).
   - Chạm vào từng điểm ghim để test click ngay vào game!

4. **CHỐNG SPAM NÚT TRIỆT ĐỂ & BỘ NHỚ LƯU TRỮ VĨNH VIỄN**:
   - Nút `[🗑️ XÓA TOÀN BỘ NÚT COMBO ĐÃ TẠO]` dọn sạch màn hình chỉ với 1 chạm.
   - Tối đa 5 nút combo, tự căn chỉnh so le để không bao giờ bị đè chồng lên nhau.
   - Toàn bộ cấu hình và vị trí nút được lưu vĩnh viễn: tắt app hoặc khởi động lại điện thoại vẫn giữ nguyên.

5. **100% KHÔNG DÙNG ACCESSIBILITY SERVICE (DỊCH VỤ TRỢ NĂNG)**:
   - Hoàn toàn loại bỏ Accessibility Service, triệt tiêu nguy cơ bị hệ thống cảnh báo hoặc xung đột đa điểm cảm ứng khi kéo Joystick.

---

## ⚡ CÁCH KÍCH HOẠT SHIZUKU (GỠ LỖI WI-FI KHÔNG CẦN PC)

App sử dụng động cơ đặc quyền Shizuku để mô phỏng ngón tay game thủ ở cấp độ phần cứng:

1. Cài ứng dụng **Shizuku** từ Google Play Store.
2. Vào **Cài đặt điện thoại > Tùy chọn nhà phát triển > Bật "Gỡ lỗi không dây" (Wireless debugging)**.
3. Mở Shizuku > Chọn **Ghép nối (Pairing)** qua mã 6 số Wi-Fi > Nhấn **Khởi động (Start)**.
4. Mở ứng dụng **Macro Liên Quân** > Bấm **Cấp quyền Shizuku** (hoặc đèn LED xanh trên thanh Game Turbo) > Chọn **Luôn cho phép**.
5. Động cơ 120Hz sẵn sàng!

---

## 📂 Cấu trúc thư mục dự án

```
macro-phone/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/macrophone/gaming/
│       │   ├── MacroApp.kt
│       │   ├── core/
│       │   │   ├── GameTurboRecorder.kt       # Đọc luồng cảm ứng /dev/input kernel
│       │   │   └── ShellExecutor.kt           # Động cơ Shizuku & Root siêu tốc 120Hz
│       │   ├── data/
│       │   │   ├── MacroConfigStorage.kt      # Lưu trữ vĩnh viễn danh sách combo
│       │   │   └── model/
│       │   ├── service/
│       │   │   ├── TurboOverlayService.kt     # Quản lý Dock Turbo & Capsule Ghi
│       │   │   ├── QuickSettingsTileService.kt
│       │   │   └── NotificationActionReceiver.kt
│       │   ├── ui/
│       │   │   ├── MainActivity.kt            # Giao diện cấp quyền & khởi chạy
│       │   │   └── overlay/
│       │   │       ├── FloatingTriggerView.kt # Nút nổi kích hoạt combo
│       │   │       └── TargetPinView.kt       # Điểm ghim thủ công
│       │   └── util/
│       │       ├── ShizukuHelper.kt
│       │       ├── PermissionUtils.kt
│       │       └── NotificationHelper.kt
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── view_floating_dock.xml      # Thanh Game Turbo HUD
│           │   ├── view_recording_pill.xml     # Capsule Ghi thao tác thời gian thực
│           │   ├── view_floating_macro_button.xml
│           │   └── view_trigger_config_dialog.xml
│           └── values/
```

---

## 🛠️ Build & Phát hành

Dự án được tự động biên dịch và tạo bản phát hành (Signed Release APK) qua **GitHub Actions**:
- Mỗi khi đẩy code lên nhánh `main`, file APK sẽ tự động được ký chứng chỉ số và tải lên mục **Releases** với định danh phiên bản tương ứng.
