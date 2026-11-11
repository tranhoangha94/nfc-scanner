# Scan NFC — Webapp cá nhân

Webapp 1 file đọc thẻ NFC (lấy UID), tự ghi nhật ký **Vào / Ra**, lưu ngay trên máy
(localStorage), xuất CSV. Không cần cài app, không server, không tài khoản.

## Yêu cầu

- **Điện thoại Android** + trình duyệt **Chrome** (hoặc Edge) bản 89 trở lên.
- Trang phải chạy qua **HTTPS** (hoặc `localhost`). Mở file `.html` trực tiếp bằng `file://` **sẽ không quét được NFC**.
- Bật **NFC** trong Cài đặt điện thoại.

> ❌ iOS / Safari và máy tính **không hỗ trợ** Web NFC → sẽ hiện cảnh báo.
> ❌ Không thể dùng điện thoại làm "thẻ ảo" quẹt vào máy chấm công UID (giới hạn phần cứng). App này **đọc** thẻ để ghi nhật ký cá nhân.

---

## Cách chạy (chọn 1 trong 3)

### Cách 1 — GitHub Pages (miễn phí, khuyên dùng) ~3 phút
1. Tạo tài khoản github.com (nếu chưa có) và tạo 1 repository mới, ví dụ `scannfc`.
2. Upload file `index.html` vào repo (nút **Add file → Upload files**).
3. Vào **Settings → Pages**, mục *Branch* chọn `main` / thư mục `/root`, bấm **Save**.
4. Đợi ~1 phút, GitHub cho link dạng `https://<tên-bạn>.github.io/scannfc/`.
5. Mở link đó bằng **Chrome trên Android**.

### Cách 2 — Netlify Drop (không cần tài khoản) ~1 phút
1. Mở https://app.netlify.com/drop
2. Kéo thả file `index.html` vào trang.
3. Nhận ngay link `https://...netlify.app` → mở bằng Chrome Android.

### Cách 3 — Chạy thử trên máy tính qua localhost
Trong thư mục chứa `index.html`, mở terminal và chạy:

```bash
python -m http.server 8000
```

Rồi mở `http://localhost:8000` **trên chính máy đó bằng Chrome**.
(Lưu ý: NFC cần đầu đọc — cách này chủ yếu để xem giao diện, quét thật nên dùng Cách 1 hoặc 2 trên điện thoại.)

---

## Cách dùng

1. Mở link bằng Chrome Android.
2. Bấm **▶ Bắt đầu quét** → cho phép quyền NFC khi được hỏi.
3. Áp thẻ vào **mặt sau điện thoại** (gần camera) → app hiện **UID**, **VÀO/RA**, giờ.
4. Lần đầu quẹt 1 thẻ: gõ tên vào ô **"Đặt tên cho thẻ này"** → **Lưu**.
5. Mỗi ngày: lần quẹt **chẵn = VÀO**, **lẻ = RA** (tự xen kẽ theo từng thẻ).
6. **⤓ CSV**: xuất toàn bộ lịch sử ra file Excel/CSV. **Xóa hết**: xóa lịch sử.

### Mẹo: tạo icon như app
Trong Chrome Android: menu **⋮ → Thêm vào màn hình chính (Add to Home screen)**.
Từ đó mở như một app bình thường.

---

## Lưu ý về dữ liệu
- Dữ liệu (tên thẻ + lịch sử) lưu trong **localStorage của trình duyệt** trên đúng điện thoại đó.
- Xóa dữ liệu duyệt web / gỡ Chrome có thể mất dữ liệu → nên **xuất CSV** định kỳ để sao lưu.
- Dữ liệu không đồng bộ sang máy khác (đúng tinh thần dùng cá nhân).
