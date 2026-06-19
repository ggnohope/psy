# Spec: CI/CD cho Psy (GitHub Actions)

Date: 2026-06-19
Status: Approved

## Mục tiêu
Tự động build/test/lint Android + backend trên mỗi push/PR, và CD có kiểm soát:
backend tự deploy lên EC2 khi merge `main`, Android build signed release APK khi tạo git tag.

## Bối cảnh (đã khảo sát)
- Repo: GitHub `ggnohope/psy` → dùng **GitHub Actions**.
- Backend: Go 1.26.3, có `Dockerfile` + `docker-compose.yml`, host trên **AWS EC2** (truy cập SSH bằng `psy-backend-ssh.pem` = EC2 key pair). Domain `psy-backend.duckdns.org`.
- Android: Gradle 9.4.1, AGP 9.2, release signing đã cấu hình qua `keystore.properties` (gitignored) + `psy-release.jks`.
- Chưa có workflow nào.

## Quyết định đã chốt
- **Mô hình:** CI tự động mọi push/PR + CD có kiểm soát.
- **#1 Android signing trong CI:** ĐỒNG Ý đưa release keystore vào GitHub Secrets.
- **#2 Registry backend:** dùng **GHCR** (build trên runner, EC2 chỉ pull).

## Workflows (`.github/workflows/`)

Mỗi workflow lọc bằng `paths:` để chỉ chạy khi phần liên quan thay đổi.

### 1. `android-ci.yml` — push/PR đụng `android/**`
- `ubuntu-latest`, `actions/setup-java@v4` Temurin **JDK 21** (AGP 9.2/Gradle 9.4 cần JDK ≥17).
- `gradle/actions/setup-gradle@v4` (cache).
- `./gradlew :app:assembleDebug :app:lintDebug` (working-directory `android/`).
- Không ép unit test (theo preference; verify bằng build + lint).

### 2. `android-release.yml` — tag `v*`
- Decode `ANDROID_KEYSTORE_BASE64` → `android/psy-release.jks`.
- Sinh `android/keystore.properties` từ secrets (`storeFile=psy-release.jks`, `storePassword`, `keyAlias`, `keyPassword`).
- `./gradlew :app:assembleRelease`.
- `softprops/action-gh-release@v2`: đính `app-release.apk` vào GitHub Release của tag.
- Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

### 3. `backend-ci.yml` — push/PR đụng `backend/**`
- `actions/setup-go@v5` Go 1.26.
- **Postgres service container** (postgres:16) + env `TEST_DATABASE_URL` để chạy cả DB-gated tests.
- `go vet ./...` + `go build ./...` + `go test ./...` (working-directory `backend/`). Migration nhúng sẵn nên tự chạy.

### 4. `backend-deploy.yml` — push `main` đụng `backend/**`
- `docker/login-action` → GHCR (dùng `GITHUB_TOKEN` để push).
- `docker/build-push-action` build `backend/Dockerfile` → push `ghcr.io/ggnohope/psy-backend:latest` + `:${{ github.sha }}`.
- `appleboy/ssh-action` SSH vào EC2:
  - `echo $GHCR_TOKEN | docker login ghcr.io -u ggnohope --password-stdin` (token read:packages để pull image private).
  - `cd ~/psy && docker compose -f docker-compose.prod.yml pull server && docker compose -f docker-compose.prod.yml up -d server`.
- Secrets: `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` (nội dung `.pem`), `GHCR_TOKEN` (PAT read:packages — dùng trên EC2 để pull).

## File mới cần thêm
- `.github/workflows/{android-ci,android-release,backend-ci,backend-deploy}.yml`
- `backend/docker-compose.prod.yml`: như compose hiện tại nhưng service `server` dùng `image: ghcr.io/ggnohope/psy-backend:latest` (thay `build: .`), đọc `JWT_SECRET`/`GOOGLE_CLIENT_ID`/`DATABASE_URL` từ env trên EC2 (file `.env` đặt sẵn trên server, KHÔNG trong repo). Postgres giữ nguyên service như dev.

## Secrets tổng hợp (tạo ở GitHub repo settings)
Android: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
Backend: `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `GHCR_TOKEN`.
(JWT_SECRET / GOOGLE_CLIENT_ID nằm trong `.env` trên EC2, không cần GitHub Secret trừ khi muốn inject lúc deploy.)

## Out of scope
- Auto-bump versionCode (làm tay khi tag).
- Play Store publishing (AAB) — hiện sideload APK.
- Multi-environment (staging/prod) — chỉ một môi trường prod.
- Infra-as-code cho EC2 (server đã dựng sẵn tay).

## Việc cần làm thủ công (ngoài code, ghi để nhớ)
1. Tạo các GitHub Secrets ở trên.
2. Trên EC2: đặt `docker-compose.prod.yml` (hoặc clone repo) + file `.env`; cài Docker + compose plugin.
3. Tạo GH PAT `read:packages` cho `GHCR_TOKEN`.
4. Base64 keystore: `base64 -i android/psy-release.jks | pbcopy` → dán vào secret.

## Verify
- Workflow YAML hợp lệ (actionlint nếu có / GitHub parse).
- android-ci & backend-ci chạy xanh trên một PR thử.
- Release: tạo tag thử `v0.0.1-test` → có APK trong Release.
- Deploy: merge thử → image lên GHCR, EC2 chạy bản mới (`/health`).
