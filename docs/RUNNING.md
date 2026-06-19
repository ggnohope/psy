# Psy — Hướng dẫn chạy / Running guide

Monorepo: `android/` (Kotlin/Compose app) + `backend/` (Go + Postgres).

## Yêu cầu
- **Android Studio** (đã có JBR sẵn) — mở thư mục `android/`.
- **Go 1.26+** (`go version`).
- **Docker** (để chạy Postgres nhanh) — hoặc một Postgres cài sẵn.
- (Tuỳ chọn) tài khoản Google Cloud để bật Google Sign-In thật.

JBR cho gradle CLI (nếu chạy ngoài Android Studio):
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

---

## A. Chạy backend (dev-login — KHÔNG cần Google)

Đủ để test backup/restore ngay.

1) Khởi động Postgres bằng Docker:
```bash
docker run --rm -d --name psy-pg \
  -e POSTGRES_USER=psy -e POSTGRES_PASSWORD=psy -e POSTGRES_DB=psy \
  -p 5432:5432 postgres:16
```

2) Chạy Go server (tự chạy migration tạo bảng `users`/`snapshots`):
```bash
cd backend
DEV_LOGIN_ENABLED=true JWT_SECRET=doi-chuoi-bi-mat-nay PORT=8080 \
  DATABASE_URL="postgres://psy:psy@localhost:5432/psy?sslmode=disable" \
  go run ./cmd/server
# Log: "listening on :8080"
```

3) Kiểm tra: `curl localhost:8080/health` → `{"status":"ok"}`.

> Env: `PORT` (8080), `DATABASE_URL`, `JWT_SECRET` (đổi cho prod), `GOOGLE_CLIENT_ID` (cho Google),
> `DEV_LOGIN_ENABLED` (`true`/`false` — TẮT ở prod để chặn dev-login).

Dừng: `Ctrl+C` server; `docker stop psy-pg`.

---

## B. Mở & chạy app Android

1) Android Studio → **Open** → chọn thư mục `android/`. Chờ Gradle sync.
2) Tạo/khởi động một **emulator** (AVD), hoặc cắm máy thật (xem mục E).
3) Run ▶. App là **offline-first** — mở là dùng được ngay (ghi chép, thống kê, ngân sách… không cần backend).
4) Test **backup**: backend đang chạy (mục A) → trong app: **Cài đặt (⚧/⚙️) → Sao lưu & đồng bộ → nhập email vào "Email (dev)" → Đăng nhập (dev) → Sao lưu ngay**. (Emulator gọi host qua `http://10.0.2.2:8080`.)

CLI build (tuỳ chọn): `cd android && ./gradlew :app:assembleDebug`.

---

## C. Bật Google Sign-In thật (tuỳ chọn)

Cần **OAuth Client ID kiểu Web** (dùng làm `serverClientId` ở Android và `audience` ở backend).

1) Vào **Google Cloud Console** → tạo Project (hoặc chọn project có sẵn).
2) **APIs & Services → OAuth consent screen**: chọn **External**, điền tên app + email, lưu (ở chế độ Testing thêm email của bạn vào "Test users").
3) **APIs & Services → Credentials → Create Credentials → OAuth client ID**:
   - Application type: **Web application** → tạo → copy **Client ID** (dạng `xxxx.apps.googleusercontent.com`). Đây là cái dùng cho cả app lẫn backend.
   - (Khuyến nghị thêm) Tạo thêm một OAuth client ID **Android**: cần **package name** = `com.psy` và **SHA-1** của keystore debug:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
       -storepass android -keypass android | grep SHA1
     ```
4) **Cắm Web Client ID vào 2 nơi:**
   - Android: `android/app/src/main/res/values/strings.xml` → đổi
     `<string name="google_web_client_id">REPLACE_WITH_OAUTH_WEB_CLIENT_ID</string>`
     thành Web Client ID vừa copy.
   - Backend: chạy server kèm env `GOOGLE_CLIENT_ID=<Web Client ID>` (cùng giá trị).
5) Chạy lại app → **Sao lưu & đồng bộ → Đăng nhập với Google**. (Nếu chưa cấu hình, nút Google báo "Chưa cấu hình Google" — dev-login vẫn dùng được.)

> Lưu ý prod: đặt `JWT_SECRET` mạnh & bí mật; `DEV_LOGIN_ENABLED=false`; backend nên chạy sau HTTPS
> (khi đó bỏ `network_security_config` cleartext hoặc chỉ giữ cho dev host).

---

## D. Đẩy code lên Git remote (origin)

Hiện đang commit local trên nhánh `main`, chưa push.
```bash
cd /Users/hoalam/Codes/psy
git remote -v                 # kiểm tra origin đã set chưa
# nếu chưa có origin:
# git remote add origin <URL-repo-cua-ban>
git push -u origin main
```

---

## E. Đổi BASE_URL (emulator / máy thật)

BASE_URL giờ là **string resource `base_url`** khai trong `android/app/build.gradle.kts` (không hardcode trong code nữa):
- **debug** (defaultConfig): `http://10.0.2.2:8080/` (emulator → host).
- **release**: `https://your-backend.example.com/` (đổi thành backend đã host).

Chạy trên **máy thật** (chung Wi-Fi với máy chạy backend dev):
1) Lấy IP LAN: `ipconfig getifaddr en0` (vd `192.168.1.20`).
2) Đổi `resValue("string", "base_url", ...)` ở **defaultConfig** thành `http://192.168.1.20:8080/`.
3) HTTP cleartext: thêm IP đó vào `res/xml/network_security_config.xml` (hoặc dùng HTTPS).

## F. Deploy backend (production)

Backend đã có `backend/Dockerfile` (multi-stage, ảnh distroless nhỏ; migration nhúng sẵn nên tự chạy lúc khởi động).

**Chạy full stack bằng Docker (local):**
```bash
cd backend && make stack        # docker compose --profile full up -d --build (postgres + server)
```

**Host lên cloud (Render / Railway / Fly.io / VPS):**
1) Trỏ service vào `backend/Dockerfile` (các nền tảng này tự build từ Dockerfile).
2) Đặt env: `DATABASE_URL` (Postgres managed của nền tảng), `JWT_SECRET` (chuỗi mạnh, bí mật), `GOOGLE_CLIENT_ID` (Web client ID), `PORT` (thường nền tảng tự set).
3) Sau khi có domain HTTPS → sửa `base_url` của **release** trong `app/build.gradle.kts` thành domain đó, build **release APK** gửi người thân.

> Lưu ý prod: `JWT_SECRET` mạnh & bí mật; chạy sau **HTTPS** (khi đó có thể bỏ cleartext trong `network_security_config.xml`, chỉ giữ cho dev host).
