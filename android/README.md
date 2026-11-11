# Scan NFC — App native (Android / Kotlin)

App đọc thẻ NFC và hiển thị **thông tin chi tiết** mà webapp không lấy được:

- UID, độ dài UID, tần số (13.56 MHz)
- Danh sách công nghệ (NfcA / IsoDep / MifareClassic / NfcV…)
- **ATQA / SAK**
- Loại chip suy ra: MIFARE Classic / Ultralight / NTAG / DESFire / ISO-DEP / FeliCa / ISO 15693
- Dung lượng, số sector, số block (với MIFARE Classic)
- Thông tin NDEF (nếu có)
- Tự ghi nhật ký **Vào / Ra**, đặt tên thẻ, lưu bằng SharedPreferences (không DB)

> Lưu ý: dữ liệu **mã hóa** (MIFARE Classic Crypto1, DESFire) chỉ đọc được nếu có key — app hiển thị metadata, không bẻ khóa.
> App này **đọc** thẻ; không thể biến điện thoại thành thẻ để quẹt vào máy chấm công UID (giới hạn phần cứng/OS).

---

## Yêu cầu
- **Android Studio** (bản mới, ví dụ Koala trở lên) — đã kèm sẵn JDK 17.
- Điện thoại Android có **NFC**, bật **Developer options → USB debugging** (nếu chạy thẳng từ Studio), hoặc cho phép **cài từ nguồn không rõ** (nếu cài file APK).
- minSdk 26 (Android 8.0).

## Mở project
1. Mở **Android Studio** → **Open** → chọn thư mục **`android`** này (không phải thư mục gốc repo).
2. Đợi Gradle **sync** tự tải thư viện (lần đầu vài phút). Nếu Studio hỏi tải Gradle/SDK, bấm đồng ý.

## Cách 1 — Chạy thẳng lên điện thoại (nhanh nhất để thử)
1. Cắm điện thoại qua USB, bật **USB debugging**.
2. Chọn thiết bị ở thanh trên, bấm nút **▶ Run** (Shift+F10).
3. App cài và mở luôn. Áp thẻ vào mặt sau máy → xem thông tin.

## Cách 2 — Export file APK để cài tay

### Debug APK (dùng cá nhân — khuyên dùng, không cần tạo chữ ký)
Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
Khi xong, bấm **locate** trong thông báo, hoặc lấy file tại:
```
android/app/build/outputs/apk/debug/app-debug.apk
```
Copy file này sang điện thoại (USB / Zalo / Drive) rồi mở để cài.
Cần bật **"Cài đặt ứng dụng không rõ nguồn gốc"** cho app mở file (Chrome/Files) khi được hỏi.

### Build bằng dòng lệnh (không cần mở Studio)
Trong thư mục `android`:
```bash
./gradlew assembleDebug        # Linux/macOS
```
```bat
gradlew.bat assembleDebug
```
APK nằm ở `app/build/outputs/apk/debug/app-debug.apk`.
> Nếu báo thiếu `gradle-wrapper.jar`: mở project bằng Android Studio 1 lần (nó tự tạo wrapper), hoặc chạy `gradle wrapper` nếu máy đã cài Gradle.

### Release APK đã ký (nếu muốn bản gọn, tối ưu)
1. **Build → Generate Signed Bundle / APK → APK**.
2. Tạo mới **keystore** (lưu lại mật khẩu), chọn **release**.
3. APK ở `app/build/outputs/apk/release/`.

---

## Cấu trúc code
```
app/src/main/
├── AndroidManifest.xml
├── java/com/scannfc/
│   ├── MainActivity.kt   # bật Reader Mode, nhận tag
│   ├── NfcReader.kt      # trích UID + ATQA/SAK + loại chip + dung lượng + NDEF
│   ├── Storage.kt        # lưu tên thẻ + nhật ký IN/OUT (SharedPreferences)
│   ├── ScanScreen.kt     # giao diện Compose
│   └── ui/Theme.kt
└── res/                  # icon, theme, strings
```
