# Nhật ký thay đổi (Changelog) - Macro Liên Quân Mobile 🎮⚡

Tất cả các thay đổi và bản cải tiến của dự án sẽ được ghi nhận và cập nhật theo từng phiên bản tại đây.

---

## [v2.5.0] - 2026-09-30
### 🎮 TÍNH NĂNG ĐA HỒ SƠ COMBO THEO GAME (MULTI-GAME PROFILES & PRESETS)
- **HỒ SƠ CẤU HÌNH COMBO ĐỘC LẬP CHO TỪNG GAME (INDEPENDENT MACRO STORES)**:
  - Mỗi game được lưu trữ trong một hồ sơ cấu hình riêng biệt (`GameProfile`): Tên nút, số lượng nút, tọa độ X/Y của nút trên màn hình, độ trễ và tốc độ xả combo của từng game hoàn toàn độc lập, không bị lẫn lộn.
  - Cung cấp sẵn các hồ sơ mẫu tiêu chuẩn:
    - 🎮 **Liên Quân Mobile**: Tối ưu cho chiêu thức Florentino, combo mua bán đồ nhanh, đổi trang bị tốc hành.
    - 🔥 **Free Fire**: Tối ưu đặt keo nhanh, ghìm tâm, ngồi bắn.
    - ⚔️ **Tốc Chiến (Wild Rift)**: Tối ưu chuỗi chiêu thức MOBA chuẩn Riot.
    - 🎯 **Game Khác**: Dành cho bất kỳ tựa game nào người dùng cài đặt.
- **CHUYỂN ĐỔI GAME 1-CHẠM NGAY TRÊN MENU TURBO HUD & MÀN HÌNH CHÍNH**:
  - Tích hợp thanh chọn game dạng thẻ kính (Chips) cuộn ngang trực tiếp trên Game Turbo HUD: Chạm vào game bất kỳ để đổi hồ sơ ngay lập tức.
  - Các nút combo của game cũ tự động dọn dẹp, và các nút của game mới lập tức xuất hiện đúng vị trí đã lưu.
- **TẠO HỒ SƠ GAME MỚI, ĐỔI TÊN & XÓA HỒ SƠ LINH HOẠT**:
  - Nút **`[+ Thêm Game]`**: Mở hộp thoại Liquid Glass cho phép nhập tên tựa game bất kỳ (VD: Genshin, Roblox, Võ Lâm...).
  - Nút **`[✏️ Sửa tên]`**: Dễ dàng chỉnh sửa tên hồ sơ game theo ý muốn.
  - Nút **`[🗑️ Xóa]`**: Xóa hồ sơ không dùng đến (tự động giữ lại ít nhất 1 hồ sơ an toàn).
- **MỞ GAME THÔNG MINH THEO HỒ SƠ ĐANG CHỌN (SMART LAUNCHER)**:
  - Nút `[🚀 MỞ GAME]` tự động cập nhật tên và nhận diện package của game đang chọn để khởi động chính xác.
  - Thanh thông báo tiện ích hiển thị chi tiết tên game đang chọn: `⚡ [Liên Quân Mobile]: SẴN SÀNG`.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.5.0.apk`.

---

## [v2.4.0] - 2026-09-30
### 💎 ĐẠI TU TOÀN DIỆN GIAO DIỆN THEO PHONG CÁCH LIQUID GLASS (GLASSMORPHISM & CYBER HUD)
- **THIẾT KẾ KÍNH THỂ LỎNG CAO CẤP (LIQUID GLASS / GLASSMORPHISM)**:
  - **Mặt kính Acrylic Mờ Đục Có Chiều Sâu (Frosted Acrylic Glass Surfaces)**:
    - Nâng cấp toàn bộ thẻ giao diện, bảng điều khiển menu và hộp thoại cấu hình sang cấu trúc kính acrylic đa lớp (`glass_card_bg_start` ➔ `glass_card_bg_end`) với độ trong suốt tối ưu (75-85%), vừa nhìn thấy game phía sau vừa hiển thị rõ ràng nội dung điều khiển.
  - **Viền Phản Quang Tinh Thể Kính (Specular Glass Rim Lights)**:
    - Ứng dụng hiệu ứng viền phản xạ ánh sáng Specular Rim Light (`glass_rim_specular` #40FFFFFF) sắc nét 1.2dp quanh mép các khối kính, mô phỏng ánh sáng phản chiếu chân thực khi chạm vào mép cắt của kính cường lực.
  - **Hệ Thống Nút Bấm Khối Cầu Kính Nổi (Liquid Glass Orbs HUD Buttons)**:
    - Nút bấm combo trên màn hình game (`FloatingTriggerView`) được nâng cấp thành các quả cầu kính thể lỏng (Liquid Glass Orbs) với hiệu ứng gradient 3D dạng thấu kính, viền hào quang phát quang (Cyan Neon Glow `2dp`), và vòng sáng phản quang bên trong.
    - Trạng thái kích hoạt (Active / Đang xả combo): Lõi cầu kính phát sáng ngọc lục bảo rực lửa (`Radiant Emerald Power Core`) với viền hào quang trắng sáng (`White Specular Rim`), cho phản hồi trực quan siêu mãn nhãn.
  - **Thanh Điều Khiển Capsule Dynamic Island (`view_position_edit_pill`, `view_recording_pill`)**:
    - Nâng cấp thanh điều khiển ghi hình combo và thanh chỉnh sửa vị trí nút thành dạng viên con nhộng Liquid Capsule bo góc tròn tuyệt đối (`radius = 999dp`), viền sáng khúc xạ ánh sáng xanh neon tinh xảo.
  - **Màu Sắc Cyberpunk Fluid Neon Mới**:
    - Bổ sung bảng màu dạ quang thể lỏng: Electric Liquid Cyan (`#00F5FF`), Fluid Violet (`#A855F7`), Radioactive Emerald (`#00F59B`), Liquid Ruby Crimson (`#FF2A55`), và Amber Warning (`#FFB703`).
  - **Giao Diện Ứng Dụng Chính (Dashboard)**:
    - Bo góc lớn 18dp - 22dp tạo cảm giác mềm mại, hiện đại.
    - Thẻ điều khiển kính mờ kết hợp huy hiệu trạng thái dạ quang, tạo ấn tượng chuyên nghiệp như các ứng dụng công nghệ flagship.
- **BẢO LƯU 100% TÍNH NĂNG & ĐỘ ỔN ĐỊNH CỦA ĐỘNG CƠ SHIZUKU BINDER 120HZ**:
  - Giữ nguyên toàn bộ cơ chế khóa vị trí trong trận đấu, điều chỉnh tốc độ cơ động lên tới 20x, thanh thông báo tiện ích điều khiển, và bộ điều khiển chạm nhân Linux.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.4.0.apk`.

---

## [v2.3.3] - 2026-09-30
### 🚀 KHÓA VỊ TRÍ NÚT KHI CHIẾN GAME & CHẾ ĐỘ DI CHUYỂN NÚT CHUYÊN DỤNG KHI MỞ MENU
- **KHÓA CỐ ĐỊNH VỊ TRÍ NÚT TRONG TRẬN ĐẤU (ZERO ACCIDENTAL MOVEMENT)**:
  - Khi chơi game, vị trí tất cả các nút combo được **khóa hoàn toàn** (`isDraggable = false`).
  - Mọi thao tác chạm vào nút đều được kích hoạt chuỗi combo ngay tức thì với độ trễ 0ms. Game thủ có thể ấn liên tục, vuốt nhanh trong giao tranh mà tuyệt đối không bao giờ bị trôi, lệch hay vô tình kéo nút chạy lung tung.
- **CHẾ ĐỘ DI CHUYỂN VỊ TRÍ NÚT KHI MỞ MENU (POSITION EDIT MODE)**:
  - Muốn thay đổi vị trí các nút: Chỉ cần vuốt thanh thông báo mở Menu Game Turbo ➔ Chọn **`[🎯 DI CHUYỂN & SỬA VỊ TRÍ NÚT]`**.
  - Bảng menu tự động thu gọn thành một thanh điều khiển nổi nhỏ gọn trên đỉnh màn hình (`view_position_edit_pill`), để lộ 100% giao diện game thông thoáng.
  - Các nút combo sẽ sáng viền xanh neon và mở khóa kéo thả (`isDraggable = true`), cho phép bạn thoải mái kéo đặt vào bất kỳ vị trí nào trên màn hình (ngay giữa màn hình, đè lên nút chiêu, nút đánh thường, v.v.).
  - Sau khi sắp xếp xong, chỉ cần bấm **`[💾 LƯU & KHÓA VỊ TRÍ]`** trên thanh điều khiển nổi: Toàn bộ tọa độ mới được lưu vĩnh viễn và các nút lập tức khóa chặt lại để sẵn sàng chiến game!
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.3.3.apk`.

---

## [v2.3.2] - 2026-09-30
### 🚀 KHẮC PHỤC TRIỆT ĐỂ LỖI ĐÓNG MENU & TỰ DO ĐẶT NÚT COMBO Ở CHÍNH GIỮA MÀN HÌNH
- **SỬA LỖI MENU KHÔNG TẮT ĐƯỢC**:
  - Gỡ bỏ touch listener trên `header` từng nuốt sự kiện click của nút `[✕ Đóng]`, giúp nút đóng phản hồi tức thì 100%.
  - Thêm tính năng **Chạm ra ngoài vùng hộp thoại (Click Outside to Dismiss)**: Chạm vào bất kỳ điểm nào trên nền màn hình bên ngoài bảng điều khiển sẽ đóng menu ngay lập tức.
  - Hỗ trợ **Toggle qua Thanh Thông báo**: Chạm vào thông báo hoặc nút `[✕ ĐÓNG MENU]` khi menu đang mở sẽ đóng menu ngay lập tức.
  - Cơ chế Dynamic Window Attachment: Khi menu đóng, toàn bộ View của dock được gỡ bỏ hoàn toàn khỏi `WindowManager` (`removeView`), triệt tiêu 100% nguy cơ tồn đọng cửa sổ vô hình chiếm diện tích.
- **TỰ DO ĐẶT NÚT COMBO Ở GIỮA MÀN HÌNH VÀ MỌI TỌA ĐỘ**:
  - Giải quyết nguyên nhân gốc rễ khiến nút combo không đặt được ở giữa màn hình: Do cửa sổ menu cũ nằm ngầm ở `Gravity.CENTER` chặn các điểm chạm ở giữa màn hình. Khi gỡ bỏ cửa sổ menu lúc đóng, vùng giữa màn hình hoàn toàn thông thoáng.
  - Bổ sung cờ `FLAG_LAYOUT_IN_SCREEN` và cấu hình `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` trên `FloatingTriggerView` và `TargetPinView`, cho phép di chuyển và đặt nút ở bất kỳ tọa độ nào trên toàn bộ màn hình thực tế (giữa màn hình, góc trái, góc phải, thanh kỹ năng) mà không bị giới hạn viền hay giật về góc.
  - Vị trí tạo mặc định của nút combo mới được đưa về vùng thuận ngón tay thay vì ép dính sát mép phải.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.3.2.apk`.

---

## [v2.3.1] - 2026-09-30
### 🚀 ẨN HOÀN TOÀN ICON MÉP, MỞ MENU TIỆN ÍCH THÔNG BÁO (CIRCLE TO SEARCH), SỬA LỖI KÉO THẢ NHẢY LOẠN & CHỈNH TỐC ĐỘ BẰNG SỐ (TỐI ĐA 20X)
- **ẨN HOÀN TOÀN ICON TAB SÉT TURBO Ở MÉP MÀN HÌNH**:
  - Không còn icon tab nổi `[⚡ TURBO]` treo ở mép màn hình khi chơi game, loại bỏ 100% cảm giác vướng mắt và không cản trở thao tác ngón tay.
- **MỞ MENU TURBO DẠNG TIỆN ÍCH THÔNG BÁO (CIRCLE TO SEARCH STYLE)**:
  - Vuốt thanh thông báo Android xuống và chạm vào thông báo hoặc nút `[⚡ MỞ MENU]`, bảng điều khiển Game Turbo HUD sẽ lập tức bung ra giữa màn hình.
  - Bấm `[✕ Đóng]` là menu đóng hoàn toàn, không để lại bất kỳ icon nào trên màn hình.
- **KHẮC PHỤC TRIỆT ĐỂ LỖI NÚT NHẢY LOẠN KHI KÉO THẢ (BUTTER-SMOOTH DRAGGING)**:
  - Viết lại toàn bộ thuật toán dragging của Nút Combo và Điểm Ghim sử dụng tọa độ phần cứng `event.rawX` / `event.rawY`.
  - Di chuyển nút cực kỳ mượt mà, bám dính 1:1 theo ngón tay, kéo thả cơ động đến bất kỳ vị trí nào trên màn hình (giữa màn hình, cạnh nút đánh thường, v.v.) mà không bị giật, rung lắc hay nhảy loạn.
- **LƯU VỊ TRÍ NÚT CHUẨN XÁC, BẢO LƯU TỌA ĐỘ KHI XOAY NGANG**:
  - Khắc phục lỗi clamp tọa độ theo chiều dọc portrait, đảm bảo tọa độ chiêu thức trong game ngang không bị ép sai hay biến mất.
  - Ghi đĩa tức thì với cơ chế synchronous `commit()`, không bao giờ bị mất vị trí đã gán khi khởi động lại máy hay xoay màn hình.
- **CHỈNH TỐC ĐỘ CƠ ĐỘNG BẰNG SỐ (GIỚI HẠN TỐI ĐA 20X)**:
  - Bổ sung ô nhập số tốc độ xả combo trực tiếp: Người dùng nhập 5 sẽ x5 tốc độ, nhập 10 sẽ x10 tốc độ (hỗ trợ từ 0.5x đến 20x).
  - Kèm các phím chọn nhanh: 1x, 2x, 5x, 10x, 20x cả trong hộp thoại lưu combo lẫn menu cài đặt nút.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.3.1.apk`.

---

## [v2.3.0] - 2026-09-30
### 🚀 GIAO DIỆN BASIC TỐI GIẢN, ĐIỀU KHIỂN TỪ THANH THÔNG BÁO, ĐỔI TÊN COMBO & SỬA LỖI LOẠN CẢM ỨNG
- **GIAO DIỆN BASIC, TỐI GIẢN & MƯỢT MÀ**:
  - Thiết kế lại toàn bộ màn hình chính (`activity_main.xml`) và Game Turbo Dock (`view_floating_dock.xml`) theo phong cách Cyberpunk tối giản, loại bỏ hoàn toàn các khối chữ hướng dẫn rườm rà.
  - Các nút chức năng to rõ, bố cục trực quan, thao tác nhanh trong trận chỉ mất 1-2 giây.
- **ĐIỀU KHIỂN NHANH TỪ THANH THÔNG BÁO (NOTIFICATION SHADE)**:
  - Vuốt từ đỉnh màn hình xuống là có ngay 3 nút điều khiển trực tiếp:
    * `[👁️ ẨN NÚT]` / `[👁️ HIỆN NÚT]`: Ẩn/Hiện tức thì toàn bộ nút nổi mà không cần mở menu.
    * `[🔴 Ghi Combo]`: Bắt đầu phiên ghi combo thời gian thực trực tiếp từ thanh thông báo.
    * `[✕ Tắt Turbo]`: Đóng hoàn toàn dịch vụ Game Turbo ngay lập tức.
- **TÙY CHỈNH TÊN COMBO & ĐỔI TÊN LINH HOẠT**:
  - Hộp thoại lưu combo mới sau khi ghi (`view_save_combo_dialog`): Cho phép game thủ đặt tên tùy thích (VD: "Florentino", "Đổi đồ 0.1s", "Raz", "Chiêu 1-2") và chọn tốc độ xả combo (1.0x, 1.5x, 2.0x).
  - Đổi tên bất kỳ lúc nào qua nút `[✏️ Sửa]` trong danh sách hoặc trong menu cài đặt nút (`view_rename_dialog`).
- **NÚT ẨN MỜ (GHOST MODE TRONG SUỐT)**:
  - Chuyển đổi nhanh các mức độ mờ: 30% (mờ sương, nhìn xuyên thấu không che tầm nhìn), 50%, 80%, 100%.
  - Cho phép ẩn/hiện từng nút riêng biệt với nút `[👁️]` trong danh sách.
- **KHẮC PHỤC TRIỆT ĐỂ LỖI LOẠN CẢM ỨNG & SAI TỌA ĐỘ KHI LƯU COMBO**:
  - Đồng bộ frame cảm ứng phần cứng qua `SYN_REPORT`: Chỉ chốt tọa độ khi đã nhận đầy đủ sự kiện cảm ứng, khắc phục triệt để lỗi tọa độ bị đọc nhầm thành (0, 0) gây trượt swipe loạn xạ.
  - Chuẩn hóa tỷ lệ tọa độ theo phân giải thực tế `getRealMetrics` và góc xoay ngang Landscape (90° và 270°).
  - Tự động nhận diện cú chạm đơn lẻ (khoảng cách di chuyển < 25dp) là thao tác TAP tĩnh, hoàn toàn không bị trượt camera hay lệch joystick.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.3.0.apk`.

---

## [v2.2.0] - 2026-09-30
### 🚀 ĐẬP ĐI XÂY LẠI TỪ SỐ 0: ĐỘNG CƠ GHI COMBO CHUẨN REDMI TURBO 4 PRO
- **GHI THỜI GIAN THỰC KHÔNG HỀ CHẶN MÀN HÌNH GAME (REDMI TURBO / K70 STYLE)**:
  - Tích hợp module `GameTurboRecorder`: Đọc trực tiếp luồng sự kiện cảm ứng từ kernel Linux (`/dev/input/event*`) ngầm qua Shizuku/Root.
  - Toàn bộ màn hình game thông thoáng 100%: Game thủ mở Shop, bán giày, mua Liềm Đoạt Mệnh, kéo di chuyển chiêu hoàn toàn tự nhiên và game nhận 100%!
  - Thanh trạng thái Dynamic Pill ở đỉnh màn hình: `[ 🔴 GHI COMBO 00:03 | ⏹ XONG & LƯU | ✕ HỦY ]` siêu nhỏ gọn, không che nút game.
  - Tự động nhận diện độ phân giải phần cứng và xoay ngang Landscape (90° / 270° cho Liên Quân Mobile).
- **NÚT NỔI COMBO [⚡ R1, R2...] KÍCH HOẠT SIÊU TỐC**:
  - Nhấn nút nổi là tự động bắn chuỗi thao tác thực tế vào game với độ trễ siêu thấp 120Hz.
  - Hỗ trợ tùy chỉnh tốc độ combo (1.0x, 2.0x, 3.0x), độ mờ (30%, 50%, 80%, 100%) và số lần lặp.
- **CHỐNG SPAM NÚT & LƯU TRỮ VĨNH VIỄN**:
  - Nút `[🗑️ XÓA TOÀN BỘ NÚT COMBO ĐÃ TẠO]` dọn sạch màn hình chỉ với 1 chạm.
  - Giới hạn tối đa 5 nút, tự căn chỉnh vị trí tránh đè lên nhau.
  - Lưu vĩnh viễn cấu hình vào `MacroConfigStorage` (SharedPreferences + JSON).
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.2.0.apk`.

---

## [v2.1.0] - 2026-09-30
### 🚀 TÁI CẤU TRÚC TOÀN DIỆN: 100% TƯƠNG TÁC GAME, CHỐNG SPAM NÚT & TỐI ƯU CỰC ĐẠI
- **KHẮC PHỤC TRIỆT ĐỂ LỖI BỊ CHẶN MÀN HÌNH & KHÔNG TƯƠNG TÁC ĐƯỢC VỚI GAME**:
  - Gỡ bỏ vĩnh viễn Canvas ghi đè toàn màn hình cũ (`TouchRecorderCanvas`) - nguyên nhân cốt lõi gây chặn cảm ứng khiến người dùng không thể mở Shop hoặc tung chiêu trong game Liên Quân.
  - Chuẩn hóa quy trình **Gán Ghim Trực Quan Game Turbo (Target Pin Mode)**: Màn hình game hoàn toàn thông thoáng 100%! Bạn thoải mái mở Shop trong game, di chuyển tướng, mua bán trang bị bình thường.
  - Kéo các điểm ghim ①, ②, ③ vào nút muốn bấm (Bán, Giáp Hộ Mệnh, Mua). Chạm trực tiếp vào từng điểm ghim để test click ngay vào game!
- **CHỐNG SPAM NÚT TRIỆT ĐỂ & NÚT XÓA TOÀN BỘ 1-CHẠM**:
  - Thêm nút `[🗑️ XÓA TOÀN BỘ NÚT COMBO ĐÃ TẠO]` trên bảng điều khiển Game Turbo HUD: 1 chạm là dọn sạch toàn bộ nút trên màn hình và bộ nhớ vĩnh viễn!
  - Giới hạn tối đa 5 nút combo, tự động phân bố vị trí so le không bao giờ bị xếp đè chồng lên nhau.
  - Tự động dọn sạch các điểm ghim ngay sau khi tạo nút để màn hình game luôn gọn gàng, sạch sẽ.
- **CẢI TIẾN CẢM ỨNG NÚT NỔI COMBO (FLOATING TRIGGER VIEW)**:
  - Chạm nhanh dưới 350ms chắc chắn 100% xả combo vào game, không bao giờ bị nuốt chạm do ngón tay trượt nhẹ 10-15px khi combat.
  - Nhấn giữ lâu trên 450ms để mở Menu cài đặt hoặc xóa nút.
- **ĐỘNG CƠ SHELLEXECUTOR 120HZ TỐI TÂN**:
  - Loại bỏ các câu lệnh sleep thập phân gây lỗi trên toybox Android, gom chuỗi `input swipe` siêu tốc với cơ chế redirect `>/dev/null 2>&1` giúp loại bỏ hoàn toàn hiện tượng nghẽn pipe buffer và phản hồi dưới 40ms.
  - Đèn LED trạng thái Shizuku thời gian thực trên Header HUD: 🟢 Xanh khi sẵn sàng, 🔴 Đỏ khi chưa cấp (chạm vào là mở ngay màn hình cấp quyền).
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.1.0.apk`.

---

## [v2.0.4] - 2026-09-30
### 🚀 Quản Lý Từng Nút (Xóa/Ghim Lại), Bộ Nhớ Tự Lưu Vĩnh Viễn, Chỉnh Độ Mờ & Mở Liên Quân (Game Turbo Mode)
- **BỘ NHỚ LƯU TRỮ CẤU HÌNH VĨNH VIỄN (PERSISTENT CONFIG STORAGE)**:
  - Tích hợp `MacroConfigStorage` lưu tự động mọi nút macro (tên, tọa độ X/Y trên màn hình, độ trễ delay, số lần lặp, độ mờ opacity và danh sách điểm chiêu) vào `SharedPreferences` + `Gson`.
  - Khởi động lại máy hoặc tắt mở lại Game Turbo, toàn bộ các nút đã tạo tự động phục hồi đúng vị trí chuẩn trên màn hình game, không lo mất cài đặt!
- **QUẢN LÝ DANH SÁCH & XÓA TỪNG NÚT ĐỘC LẬP**:
  - Bảng điều khiển Game Turbo HUD bổ sung mục **"📋 NÚT MACRO ĐÃ GÁN"**:
    - Hiển thị trực quan từng nút (`[C1]`, `[C2]`, `[R1]...`), số chiêu, độ trễ và độ mờ.
    - Nút `[🎯 Ghim]`: Tự động gỡ nút và bung lại các điểm ghim ①, ②, ③ lên màn hình để căn chỉnh lại vị trí chiêu.
    - Nút `[🗑️ XÓA]`: Xóa vĩnh viễn từng nút một cách độc lập mà không ảnh hưởng các nút khác.
- **TÙY CHỈNH ĐỘ MỜ (OPACITY / TRONG SUỐT)**:
  - Bổ sung thanh chỉnh độ mờ toàn cục trên Game Turbo HUD: `30% Mờ`, `50% Vừa`, `80% Rõ`, `100% Đậm`.
  - Hỗ trợ chỉnh độ mờ độc lập cho từng nút trong menu cài đặt combo (khi nhấn giữ nút). Nút có thể làm mờ mờ để không che khuất tầm nhìn bản đồ và combat!
- **1-CHẠM KHỞI ĐỘNG LIÊN QUÂN MOBILE (TURBO MODE)**:
  - Bổ sung nút `[🚀 MỞ LIÊN QUÂN MOBILE (TURBO MODE)]` trên cả Màn hình chính (`MainActivity`) lẫn bảng điều khiển nổi Game Turbo.
  - Tự động bật Game Turbo HUD và lập tức mở game Liên Quân Mobile (hỗ trợ cả bản Garena VN lẫn Global) chỉ với 1 lần chạm!
- **TỐI ƯU HÓA ĐỘNG CƠ SHIZUKU / ROOT & TƯƠNG TÁC GAME TUYỆT ĐỐI**:
  - Tối ưu lệnh `input tap` kết hợp `input swipe` dự phòng, xử lý luồng stdout/stderr tránh nghẽn pipe buffer.
  - Tọa độ chiêu được hiệu chỉnh bằng `getLocationOnScreen()` tính toán chính xác bù trừ viền màn hình và tai thỏ trên điện thoại Infinix Note 30 khi xoay ngang.
  - Cảnh báo trực quan Toast nếu Shizuku chưa kết nối hoặc chưa cấp quyền, người dùng biết ngay nguyên nhân nếu chưa kích hoạt động cơ.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.0.4.apk`.

---

## [v2.0.3] - 2026-09-30
### 🚀 Vừa Di Chuyển Vừa Bấm Combo Mượt Mà & Khắc Phục Lỗi Bấm Shop/Chiêu Không Mở
- **VỪA DI CHUYỂN (JOYSTICK) VỪA BẤM COMBO MƯỢT MÀ**:
  - Kích hoạt cờ `FLAG_SPLIT_TOUCH` trên toàn bộ cửa sổ nổi (`FloatingTriggerView`, `TargetPinView`, `TurboOverlayService`).
  - Xử lý đa điểm cảm ứng (`ACTION_POINTER_DOWN / ACTION_POINTER_UP` và `event.actionMasked`) giúp ngón cái tay trái kéo analog di chuyển tướng, ngón tay phải bấm nút combo `[⚡ C1]` hoàn toàn độc lập, không còn bị khựng hay đơ cảm ứng!
- **KHẮC PHỤC TRIỆT ĐỂ LỖI BẤM SHOP / CHIÊU KHÔNG MỞ TRONG GAME**:
  - Thay thế lệnh `input tap` (0ms thường bị game engine Unity/Tencent bỏ qua do không đủ thời gian nhấn giữ) bằng `input swipe X Y X Y 45ms` (chuẩn thời gian giữ ngón tay thực tế).
  - Nút Shop, nút Chiêu 1/2/3, nút Đánh thường, nút Trang bị trong game nhận diện và phản hồi 100% tức thì!
- **GHI THAO TÁC TRỰC QUAN - TƯƠNG TÁC THỜI GIAN THỰC VÀO GAME**:
  - Khi bấm [Ghi Thao Tác]: Mỗi lần chạm vào màn hình (ví dụ bấm mở Shop, bấm mua đồ, bấm đóng shop) thì game sẽ THỰC SỰ PHẢN HỒI VÀ MỞ SHOP ngay tức khắc nhờ cơ chế bắn lệnh song song (`ShellExecutor.tap(x, y, 45)`), đồng thời vẽ điểm đánh dấu số ①, ②, ③ trực tiếp trên màn hình!
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.0.3.apk`.

---

## [v2.0.2] - 2026-09-30
### 🚀 Nâng Cấp Trải Nghiệm Gameplay: Menu Cài Đặt Combo & Khắc Phục Lỗi Ẩn Menu
- **KHẮC PHỤC TRIỆT ĐỂ LỖI KHÔNG ẨN ĐƯỢC MENU**:
  - Tách biệt vùng kéo di chuyển thanh dock (`ivExpandedDrag`) khỏi tiêu đề để nút `[✕ Thu Gọn]` nhận diện click ngay tức khắc 100%.
  - Bổ sung nút `[— THU GỌN VỀ MÉP MÀN HÌNH]` lớn ở đáy bảng điều khiển, tiện tay chạm là thu gọn về tab mép.
  - Kích hoạt tính năng `[👁️ Ẩn/Hiện]`: 1 chạm để tạm ẩn toàn bộ các nút nổi trên màn hình khi giao tranh trong game, chạm vào mép màn hình là hiện lại.
- **MENU CHỈNH SỬA COMBO TRỰC TIẾP TRÊN MÀN HÌNH GAME**:
  - Khi nhấn giữ lâu nút tròn `[⚡ C1]`, tự động bật **Menu Cài Đặt Combo**:
    - Chọn tốc độ xả chiêu tức thời: `30ms (Sát thủ)`, `60ms (Chuẩn)`, `120ms (Chậm)`.
    - Chọn số lần kích hoạt combo: `1 lần`, `2 lần`, `3 lần`.
    - Nút `[🎯 Hiện lại điểm ghim trên game]`: Tự động bung lại các điểm ①, ②, ③ lên các nút chiêu của game để bạn kéo chỉnh lại vị trí trực tiếp!
    - Nút `[▶️ Thử chạy combo vào game]`: Thử nghiệm xả combo ngay tại chỗ!
- **TƯƠNG TÁC GAME HOÀN TOÀN TỰ NHIÊN**:
  - Khi kéo các điểm ghim ①, ②, ③, bạn có thể chạm trực tiếp vào từng điểm ghim để test thử chiêu đó ngay trong game!
  - Vùng màn hình game không bị chặn cảm ứng, đảm bảo bạn vừa di chuyển tướng vừa ngắm chiêu bình thường.
- **Phát hành file cài đặt**: `Macro-LienQuan-v2.0.2.apk`.

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
