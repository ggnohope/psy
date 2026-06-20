# CI/CD — Psy

GitHub Actions. 5 workflow trong `.github/workflows/`.

| Workflow | Trigger | Việc |
|---|---|---|
| `android-ci.yml` | push/PR đụng `android/**` | `assembleDebug` + `lintDebug` |
| `ios-ci.yml` | push/PR đụng `ios/**` | `swift test` (PsyCore) + `xcodebuild test` (simulator) — runner **macOS** |
| `release.yml` | tag `v*` | build **signed APK (Android)** + **ad-hoc IPA (iOS)**, cùng version từ tag → đính cả hai vào 1 GitHub Release |
| `backend-ci.yml` | push/PR đụng `backend/**` | `go vet` + `build` + `test` (Postgres service) |
| `backend-deploy.yml` | push `main` đụng `backend/**` | build image → GHCR → SSH vào EC2 `pull && up -d` |

> `release.yml` thay thế `android-release.yml` cũ — giờ release cả Android + iOS trong **một** run, cùng version + build number (xem mục Release bên dưới).

---

## 1. GitHub Secrets cần tạo

`Settings → Secrets and variables → Actions → New repository secret`:

**Android (signing):**
| Secret | Giá trị |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -i android/psy-release.jks \| pbcopy` rồi dán |
| `ANDROID_KEYSTORE_PASSWORD` | storePassword trong `keystore.properties` |
| `ANDROID_KEY_ALIAS` | `psy` |
| `ANDROID_KEY_PASSWORD` | keyPassword |

**iOS (ad-hoc signing — release.yml job `ios`):**
| Secret | Giá trị |
|---|---|
| `IOS_DIST_CERT_P12_BASE64` | `base64 -i AppleDistribution.p12 \| pbcopy` (cert **Apple Distribution** export ra `.p12`) |
| `IOS_DIST_CERT_PASSWORD` | mật khẩu của `.p12` |
| `IOS_PROVISIONING_PROFILE_BASE64` | `base64 -i psy_adhoc.mobileprovision \| pbcopy` (profile **Ad Hoc** cho `com.hoalam.psy`) |
| `IOS_TEAM_ID` | Apple Team ID (10 ký tự) |
| `IOS_KEYCHAIN_PASSWORD` | chuỗi tạm bất kỳ (cho keychain tạm trên runner) |

> **Chuẩn bị (làm 1 lần trong Apple Developer portal):** ① tạo/export cert **Apple Distribution** → `.p12`; ② tạo profile **Ad Hoc** cho App ID `com.hoalam.psy`, đăng ký **UDID** của các thiết bị test; ③ thêm 5 secret trên.
> Giới hạn ad-hoc: thêm thiết bị mới ⇒ phải regenerate profile + cập nhật lại `IOS_PROVISIONING_PROFILE_BASE64`. (Muốn hết phiền → chuyển sang TestFlight sau.)
> `ExportOptions.plist` dùng `method: release-testing` (Xcode 15.3+). Nếu runner xài Xcode cũ hơn, đổi thành `ad-hoc` trong `release.yml`.

**Backend (deploy EC2):**
| Secret | Giá trị |
|---|---|
| `AWS_DEPLOY_ROLE_ARN` | `arn:aws:iam::529088301474:role/psy-github-deploy` (OIDC role, đã tạo) |
| `EC2_HOST` | IP / DNS của EC2 |
| `EC2_USER` | user SSH (vd `ubuntu` hoặc `ec2-user`) |
| `EC2_SSH_KEY` | **toàn bộ nội dung** `psy-backend-ssh.pem` (gồm cả dòng BEGIN/END) |
| `GHCR_TOKEN` | GitHub PAT scope `read:packages` (để EC2 pull image private) |

> Push image lên GHCR dùng `GITHUB_TOKEN` mặc định (không cần tạo). `GHCR_TOKEN` chỉ để EC2 *pull*.

### Deploy không mở SSH ra Internet (OIDC + dynamic SG)
SG giữ SSH (22) khoá về IP của bạn. Khi deploy, workflow assume IAM role qua **GitHub OIDC** (không AWS key dài hạn), tự **mở 22 cho đúng IP runner** rồi **đóng lại** (`if: always()`). AWS đã setup sẵn (qua CLI):
- OIDC provider `token.actions.githubusercontent.com`.
- IAM role `psy-github-deploy` — trust `repo:ggnohope/psy:ref:refs/heads/main`; policy chỉ `ec2:Authorize/RevokeSecurityGroupIngress` trên `sg-0901eba322f854af8` + `DescribeSecurityGroups`.

---

## 2. Setup EC2 (một lần)

```bash
# trên EC2
# a) cài Docker + compose plugin (nếu chưa)
# b) đặt compose.prod + .env tại ~/psy/backend/
mkdir -p ~/psy/backend && cd ~/psy/backend
# copy docker-compose.prod.yml từ repo lên (scp hoặc git clone repo vào ~/psy)
# tạo .env (KHÔNG commit):
cat > .env <<EOF
JWT_SECRET=<chuoi-manh-bi-mat>
GOOGLE_CLIENT_ID=885786271406-u79onredebj2udsoaen122vphe3pk8hd.apps.googleusercontent.com
POSTGRES_PASSWORD=<mat-khau-db>
EOF
chmod 600 .env
# c) đăng nhập GHCR để pull được image private
echo <GHCR_TOKEN> | docker login ghcr.io -u ggnohope --password-stdin
```

- `GOOGLE_CLIENT_IDS` (tùy chọn): danh sách audience Google ID token được chấp nhận, phân tách bằng dấu phẩy — ví dụ `WEB_CLIENT_ID,IOS_CLIENT_ID`. Cho phép cùng backend phục vụ cả Android (web client) và iOS (iOS client). Nếu để trống, fallback về `GOOGLE_CLIENT_ID` (tương thích ngược).

> TLS (`https://psy-backend.duckdns.org`) do reverse proxy phía trước (nginx/caddy) đảm nhiệm — proxy `:443 → 127.0.0.1:8080`. Stack này chỉ expose `:8080`.

Lần đầu kéo image (sau khi CI đã push): `docker compose -f docker-compose.prod.yml up -d`.

---

## 3. Cách dùng hằng ngày

- **CI**: tự chạy khi mở PR / push. Xem tab Actions.
- **Deploy backend**: merge PR (đụng `backend/**`) vào `main` → `backend-deploy.yml` tự build + deploy.
- **Release cả Android + iOS cùng lúc** — chỉ cần 1 tag, KHÔNG sửa version thủ công:
  ```bash
  git tag v1.2.0 && git push origin v1.2.0
  ```
  → `release.yml` chạy 1 run:
  - Version lấy từ tag: `VERSION_NAME = 1.2.0` (bỏ chữ `v`); build number = `github.run_number` (tăng dần).
  - Job `android`: APK đã ký `1.2.0 (build N)`.
  - Job `ios`: ad-hoc IPA `1.2.0 (build N)`.
  - Cả hai đính vào **một** GitHub Release `v1.2.0`. Hai bên **luôn cùng version + build**.
  - Cài iOS IPA: thiết bị phải nằm trong UDID của provisioning profile (ad-hoc).

  > Version trong code (`build.gradle.kts` = `1.0`, `project.yml` MARKETING_VERSION = `1.0.0`) chỉ là fallback cho build local; release luôn override từ tag.

---

## Lưu ý
- Image GHCR: `ghcr.io/ggnohope/psy-backend` (`:latest` + `:<sha>`). Mặc định package private — nhớ `GHCR_TOKEN` cho EC2, hoặc đặt package thành public ở GitHub Packages.
- Migration nhúng trong binary → chạy tự động lúc server khởi động, không cần bước riêng.
- Đổi domain backend release: sửa `base_url` (block `release`) trong `android/app/build.gradle.kts`.
