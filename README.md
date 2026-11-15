# Khoá An Toàn (AppLock)

App Android khoá ứng dụng + khoá tin nhắn đến, nhiều lớp xác thực (PIN, Hình, Vân tay, Gương mặt), lịch sử truy cập, chụp ảnh người mở khoá sai, chống gỡ cài đặt. Giao diện kiểu kính (glass).

## Cách build ra file .apk

### Cách 1 — GitHub Actions (khuyên dùng, build trên server GitHub, không cần máy mạnh)
1. Dùng Termux: `git init && git add . && git commit -m "init" && git remote add origin <repo của bạn> && git push -u origin main`
2. Vào tab **Actions** trên GitHub → workflow "Build APK" sẽ tự chạy.
3. Khi xong (màu xanh), mở job → mục **Artifacts** → tải `AppLock-debug-apk.zip` → giải nén lấy file `.apk`.
4. Cũng có thể bấm nút **Run workflow** (workflow_dispatch) để chạy lại thủ công.

### Cách 2 — Android Studio
Mở thư mục này bằng Android Studio → để Gradle sync xong → **Build > Build APK(s)**.

## Thiết lập lần đầu sau khi cài APK

1. Mở app → đặt **mã số chủ 6 số** (dùng để mở các app đã khoá kiểu PIN).
2. Ở màn chính, nếu thấy cảnh báo thiếu quyền, bấm **Cấp quyền** để bật:
   - **Trợ năng (Accessibility)** — bắt buộc, để app phát hiện khi bạn mở 1 ứng dụng bị khoá.
   - **Truy cập thông báo (Notification access)** — bắt buộc cho tính năng khoá tin nhắn đến.
3. Chọn 1 ứng dụng trong danh sách → chọn loại khoá (Không khoá / Mã số / Hình / Vân tay / Gương mặt) → **Lưu**.
   - Vân tay / Gương mặt dùng thẳng dữ liệu sinh trắc học đã đăng ký sẵn trong **Cài đặt hệ thống** của máy — app không tự lưu mẫu vân tay/khuôn mặt nào.
4. (Tuỳ chọn) Vào **Cài đặt > Ứng dụng quản trị thiết bị** trên máy để kích hoạt Device Admin cho app — giúp cản trở việc gỡ cài đặt thông thường.

## Lưu ý quan trọng về giới hạn thực tế

- **Chống gỡ cài đặt**: khi bật Device Admin, Android sẽ chặn nút "Gỡ cài đặt" thông thường và bắt tắt quyền Device Admin trước. Tuy nhiên người dùng vẫn có thể vào thẳng **Cài đặt hệ thống > Ứng dụng quản trị thiết bị** để tắt quyền đó — đây là giới hạn của Android dành cho app thường (không phải app quản lý thiết bị cấp doanh nghiệp), không có cách nào chặn hoàn toàn bằng mã nguồn app.
- **Khoá tin nhắn đến**: cơ chế hiện tại ẩn **nội dung thông báo** của app bị khoá (thay bằng dòng "đã khoá"), không chặn được việc mở trực tiếp app nhắn tin nếu bạn đã mở khoá nó trong danh sách khoá app.
- **Vân tay / Gương mặt**: máy phải có sẵn vân tay/khuôn mặt đăng ký trong Cài đặt hệ thống thì mới chọn được loại khoá này.
- App chưa đăng ký lên Google Play nên các quyền nhạy cảm (Accessibility, Notification access, Device Admin) không bị giới hạn như app public — chỉ dùng cho nhu cầu cá nhân/sideload.

## Cập nhật bảo mật & tính năng mới (phiên bản này)

- Chống dò mật khẩu: nhập sai PIN/Hình nhiều lần bị khoá tạm tăng dần (30s → 1 phút → 5 phút → 15 phút).
- Mật khẩu chủ băm bằng PBKDF2-HMAC-SHA256 (120.000 vòng) thay vì SHA-256 thường, so khớp theo kiểu hằng thời gian.
  ⚠️ Do đổi thuật toán băm, sau khi cập nhật bản cũ phải **đặt lại mã số/hình chủ** (mã cũ sẽ không còn đúng).
- Màn khoá bật `FLAG_SECURE`: chặn chụp/quay màn hình và xem trước trong Recents lúc nhập mật khẩu.
- Mỗi lần có người mở 1 app bị khoá (đúng lẫn sai mật khẩu) đều tự chụp 1 ảnh camera trước, ảnh được **mã hoá AES-256-GCM** (Android Keystore) ngay khi lưu xuống máy.
- Ảnh + lịch sử truy cập tự xoá sau **30 ngày**; màn Lịch sử gộp hiển thị theo từng ngày ("Hôm nay", "Hôm qua", dd/MM/yyyy).
- Có màn **Vị trí thiết bị** (định vị chính máy đang cầm, mở trong Google Maps) — cần cấp quyền vị trí.
- Tự kiểm tra bản mới trên GitHub Releases của repo khi mở app (tối đa 1 lần/24h), báo ngay trong app nếu có bản mới hơn.

**Không có trong bản này (cố ý không làm):** app chủ điều khiển/giám sát từ xa nhiều thiết bị khác, nghe lén micro từ xa, bật/tắt chạy ngầm từ xa giữa các máy. Các tính năng này có cùng cơ chế với phần mềm theo dõi lén (stalkerware) nên không được hỗ trợ xây dựng, bất kể mục đích sử dụng.

## Cấu trúc mã nguồn chính

| File | Vai trò |
|---|---|
| `MainActivity.kt` | Danh sách app, kiểm tra quyền |
| `LockSettingActivity.kt` | Chọn loại khoá cho từng app |
| `LockScreenActivity.kt` | Màn hình khoá + xác thực |
| `auth/PrefsManager.kt` | Lưu mật khẩu chủ đã băm, danh sách app bị khoá |
| `auth/BiometricAuthHelper.kt` | Gọi BiometricPrompt hệ thống |
| `ui/PatternLockView.kt` | Vẽ khoá hình |
| `service/AppLockAccessibilityService.kt` | Phát hiện app foreground |
| `service/NotificationGuardService.kt` | Ẩn nội dung thông báo |
| `camera/IntruderCameraHelper.kt` | Chụp ảnh người mở khoá sai |
| `admin/MyDeviceAdminReceiver.kt` | Chống gỡ cài đặt |
| `data/` | Room — lịch sử truy cập |
| `history/HistoryActivity.kt` | Màn hình lịch sử truy cập |
