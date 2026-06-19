# CI/CD — Psy

GitHub Actions. 4 workflow trong `.github/workflows/`.

| Workflow | Trigger | Việc |
|---|---|---|
| `android-ci.yml` | push/PR đụng `android/**` | `assembleDebug` + `lintDebug` |
| `android-release.yml` | tag `v*` | build **signed release APK** → đính vào GitHub Release |
| `backend-ci.yml` | push/PR đụng `backend/**` | `go vet` + `build` + `test` (Postgres service) |
| `backend-deploy.yml` | push `main` đụng `backend/**` | build image → GHCR → SSH vào EC2 `pull && up -d` |

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

**Backend (deploy EC2):**
| Secret | Giá trị |
|---|---|
| `EC2_HOST` | IP / DNS của EC2 |
| `EC2_USER` | user SSH (vd `ubuntu` hoặc `ec2-user`) |
| `EC2_SSH_KEY` | **toàn bộ nội dung** `psy-backend-ssh.pem` (gồm cả dòng BEGIN/END) |
| `GHCR_TOKEN` | GitHub PAT scope `read:packages` (để EC2 pull image private) |

> Push image lên GHCR dùng `GITHUB_TOKEN` mặc định (không cần tạo). `GHCR_TOKEN` chỉ để EC2 *pull*.

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

> TLS (`https://psy-backend.duckdns.org`) do reverse proxy phía trước (nginx/caddy) đảm nhiệm — proxy `:443 → 127.0.0.1:8080`. Stack này chỉ expose `:8080`.

Lần đầu kéo image (sau khi CI đã push): `docker compose -f docker-compose.prod.yml up -d`.

---

## 3. Cách dùng hằng ngày

- **CI**: tự chạy khi mở PR / push. Xem tab Actions.
- **Deploy backend**: merge PR (đụng `backend/**`) vào `main` → `backend-deploy.yml` tự build + deploy.
- **Release Android**:
  ```bash
  # bump versionCode/versionName trong android/app/build.gradle.kts trước
  git tag v1.0 && git push origin v1.0
  ```
  → `android-release.yml` build APK đã ký, đính vào Release `v1.0`. Tải về gửi người thân.

---

## Lưu ý
- Image GHCR: `ghcr.io/ggnohope/psy-backend` (`:latest` + `:<sha>`). Mặc định package private — nhớ `GHCR_TOKEN` cho EC2, hoặc đặt package thành public ở GitHub Packages.
- Migration nhúng trong binary → chạy tự động lúc server khởi động, không cần bước riêng.
- Đổi domain backend release: sửa `base_url` (block `release`) trong `android/app/build.gradle.kts`.
