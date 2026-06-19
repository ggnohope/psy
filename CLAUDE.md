# Psy — project context for Claude

Cute money-tracker, **offline-first**. Monorepo: `android/` (Kotlin/Compose app) + `backend/` (Go + Postgres, đăng nhập Google + sao lưu/đồng bộ). Đọc file này đầu mỗi session; chi tiết sâu xem `docs/ARCHITECTURE.md`.

> Quy ước trả lời: **song ngữ Việt–Anh**, giữ thuật ngữ tiếng Anh. **KHÔNG viết unit test mặc định** (tốn token) — verify bằng build + emulator; chỉ thêm test khi là regression guard cho bug thật.

## Layout
```
android/   Kotlin · Jetpack Compose (Material3) · MVVM · Hilt · Room · Retrofit
backend/   Go · chi · pgxpool · Postgres · JWT
design/    icon source artwork
docs/      ARCHITECTURE.md, RUNNING.md, CICD.md, superpowers/specs (1 spec / feature)
```
Package Android: `com.psy`. Module Go: `github.com/hoalam/psy/backend`. Repo: `github.com/ggnohope/psy`.

## Build / run nhanh
- **Android CLI** cần JBR: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` rồi `cd android && ./gradlew :app:assembleDebug`. (Mở Android Studio thì khỏi.)
- **Backend dev**: `cd backend && make dev` (Postgres Docker :5433 + server :8080). `curl localhost:8080/health`.
- **Release APK**: tag `v*` → CI build signed APK vào GitHub Release. Hoặc `./gradlew :app:assembleRelease`.

## Gotchas / quyết định không hiển nhiên (đọc kỹ trước khi sửa)
- **Toolchain bleeding-edge**: Gradle 9.4.1, AGP 9.2.1, Kotlin 2.2.10, compileSdk/targetSdk 36, minSdk 26, Java 11 source. CI dùng Temurin **JDK 21**.
- **BASE_URL** là **string resource** `base_url` khai bằng `resValue(...)` trong `android/app/build.gradle.kts` (KHÔNG dùng `BuildConfig` — KSP/Hilt không resolve được). Cần `buildFeatures { resValues = true }`. debug=`http://10.0.2.2:8080/`, release=`https://psy-backend.duckdns.org/`.
- **Auth = Google-only + login gate**: phải đăng nhập trước khi vào app (`AppRoot` dựa `isSignedIn` tri-state). Auto-backup khi `onStop`. Settings chỉ cho **logout** để đổi account. Web OAuth client ID nằm ở `strings.xml` (`google_web_client_id`, public/an toàn commit); Credential Manager dùng nó làm `serverClientId`. Android OAuth client đăng ký package `com.psy` + SHA-1 **của cả debug và release keystore**.
- **Stats theo account**: TRANSFER bị **loại** khỏi tính thu/chi (đồng nhất pie). Pie tô màu theo `piePalette` cố định theo index, KHÔNG theo `category.color`.
- **Money.formatMinor** dùng số nguyên (không Double — tránh mất chính xác).
- **Backend migrations**: runner chạy lại TẤT cả `.sql` mỗi lần gọi (không tracking), dựa vào `IF NOT EXISTS`. Đã bọc `pg_advisory_lock` để serialize (concurrent migrate race → duplicate catalog). Migration nhúng `//go:embed`, tự chạy lúc server start.
- **Signing**: `keystore.properties` + `psy-release.jks` ở `android/` (gitignored). CI release đọc keystore từ secret base64.

## CI/CD & deploy (chi tiết: docs/CICD.md)
- 4 GitHub Actions: `android-ci`, `android-release` (tag `v*`), `backend-ci` (có Postgres service), `backend-deploy` (push main).
- Deploy: build image → **GHCR** `ghcr.io/ggnohope/psy-backend` → assume IAM role qua **GitHub OIDC** (`psy-github-deploy`) → **mở SG port 22 cho IP runner rồi đóng** → SSH vào **EC2** (`ec2-user`, region `ap-southeast-1`, SG `sg-0901eba322f854af8`) → `docker compose -f docker-compose.prod.yml pull && up -d`.
- EC2 có `~/psy/backend/{docker-compose.prod.yml,.env}`; TLS do reverse proxy 443→8080. Secret `.env`/`*.pem`/`*.key` gitignored.

## Quy trình làm việc
Feature mới: brainstorm → spec (`docs/superpowers/specs/YYYY-MM-DD-*.md`) → plan → implement → verify (build/emulator) → PR vào `main`. Mỗi feature đã có 1 spec trong thư mục đó — đọc spec liên quan trước khi sửa feature.
