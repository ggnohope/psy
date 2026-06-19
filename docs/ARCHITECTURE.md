# Psy — Architecture & Knowledge Graph

Bản đồ hệ thống dạng sơ đồ, để session sau (hoặc người mới) nắm nhanh mà không cần đọc lại lịch sử. Tóm tắt + gotcha ở `../CLAUDE.md`; cách chạy ở `RUNNING.md`; CI/CD ở `CICD.md`; chi tiết từng feature ở `superpowers/specs/`.

## 1. System context

```mermaid
flowchart LR
    user([Người dùng]):::ext
    google([Google Identity]):::ext

    subgraph Android["Android app (com.psy)"]
        ui[Compose UI + ViewModels]
        room[(Room DB<br/>offline-first)]
        cm[Credential Manager]
    end

    subgraph AWS["AWS EC2 (ap-southeast-1)"]
        proxy[Reverse proxy<br/>443 → 8080]
        server[Go server :8080]
        pg[(Postgres)]
    end

    user --> ui
    ui <--> room
    ui --> cm --> google
    ui -- "Retrofit / HTTPS<br/>(login + backup)" --> proxy --> server --> pg
    server -- "verify Google ID token" --> google

    classDef ext fill:#eee,stroke:#999,color:#333;
```

App **dùng được hoàn toàn offline** (Room). Backend chỉ phục vụ **đăng nhập Google** (bắt buộc khi mở app) và **sao lưu/đồng bộ snapshot**. TLS do reverse proxy trên EC2; server chỉ nghe HTTP :8080.

## 2. Android — layers (MVVM + Hilt)

```mermaid
flowchart TD
    subgraph UI["ui/ — Compose screens + ViewModels"]
        home[home] ; stats[stats] ; cal[calendar] ; budget[budget]
        addedit[addedit] ; manage[manage/account · manage/category]
        settings[settings] ; auth[auth/LoginScreen] ; lock[lock] ; app[app/AppRoot+AppViewModel]
    end
    subgraph DOMAIN["domain/"]
        models[model: Ledger·Account·Category·Transaction·Budget·Currency·Enums]
        repos[repository interfaces]
        util[util: Money]
    end
    subgraph DATA["data/"]
        roomdao[db: dao · entity · mapper]
        remote[remote: Retrofit API + dto]
        backup[backup]
        authd[auth: token store + Google]
        settingsd[settings: DataStore]
        seed[seed]
        repoimpl[repo: repository impls]
    end
    di[di/ — Hilt modules<br/>NetworkModule reads R.string.base_url]

    UI --> repos
    repoimpl -. implements .-> repos
    repoimpl --> roomdao
    repoimpl --> remote
    repoimpl --> backup
    UI --> authd --> remote
    di -.provides.-> repoimpl & remote & roomdao
```

ViewModel chỉ phụ thuộc **repository interfaces** (`domain/repository`); implementation ở `data/repo`. `NetworkModule` đọc base URL từ `R.string.base_url` (resValue), gắn Bearer token từ token store.

## 3. Domain data model (ER)

```mermaid
erDiagram
    LEDGER ||--o{ ACCOUNT : "có"
    LEDGER ||--o{ CATEGORY : "có"
    LEDGER ||--o{ TRANSACTION : "có"
    LEDGER ||--o{ BUDGET : "có"
    ACCOUNT ||--o{ TRANSACTION : "accountId"
    ACCOUNT ||--o{ TRANSACTION : "toAccountId (TRANSFER)"
    CATEGORY ||--o{ TRANSACTION : "categoryId"
    CATEGORY ||--o{ BUDGET : "giới hạn"

    TRANSACTION {
        long id
        enum type "INCOME|EXPENSE|TRANSFER"
        long amountMinor "số nguyên (minor units)"
        long accountId
        long toAccountId "chỉ TRANSFER"
        long categoryId "null cho TRANSFER"
        long date "epoch millis"
    }
    ACCOUNT {
        string name
        enum type "CASH|BANK|CREDIT|ASSET"
        string icon
        long color "ARGB"
    }
```

Tiền lưu bằng **minor units (Long)**, format bằng `Money.formatMinor` (số nguyên). Stats: **TRANSFER không tính** vào thu/chi.

## 4. Backend (Go)

```mermaid
flowchart TD
    main[cmd/server] --> cfg[internal/config]
    main --> dbpkg[internal/db<br/>Connect + Migrate]
    main --> router[internal/api<br/>router + handlers]
    router --> mw[internal/auth<br/>JWT middleware]
    router --> authh[auth: google.go<br/>verify ID token → IssueJWT]
    router --> backuph[backup handlers]
    authh --> userpkg[internal/user<br/>UpsertBySub]
    backuph --> snap[internal/snapshotstore<br/>Save/Get versioned]
    dbpkg --> pgx[(Postgres via pgxpool)]
    dbpkg -. "//go:embed migrations/*.sql<br/>+ pg_advisory_lock" .-> pgx
```

`/auth/google` xác thực Google ID token → upsert user → cấp JWT (HS256). `/backup` (sau middleware JWT) lưu/đọc **snapshot** dữ liệu user (versioned). Migration nhúng, chạy lúc start, serialize bằng advisory lock.

## 5. CI/CD pipeline

```mermaid
flowchart LR
    pr[PR / push] --> aci[android-ci<br/>assembleDebug+lint]
    pr --> bci[backend-ci<br/>vet/build/test +Postgres]
    tag[git tag v*] --> arel[android-release<br/>signed APK → Release]
    main[push main] --> dep[backend-deploy]
    dep --> ghcr[(GHCR image)]
    dep --> oidc[OIDC assume<br/>psy-github-deploy]
    oidc --> sg[open→close SG :22<br/>for runner IP]
    dep --> ec2[SSH EC2<br/>compose pull + up]
    ghcr --> ec2
```

Secrets: `ANDROID_KEYSTORE_*` (release signing), `AWS_DEPLOY_ROLE_ARN`, `EC2_HOST/USER/SSH_KEY`, `GHCR_TOKEN`. Xem `CICD.md`.

## 6. Knowledge graph — quyết định ↔ nơi áp dụng

```mermaid
flowchart LR
    d1{{Google-only auth<br/>+ login gate}} --> app[ui/app/AppRoot] & authd[data/auth] & backend_auth[backend auth]
    d2{{BASE_URL = resValue<br/>không dùng BuildConfig}} --> gradle[app/build.gradle.kts] & net[di/NetworkModule]
    d3{{TRANSFER loại khỏi stats}} --> statsvm[ui/stats/StatsViewModel]
    d4{{Pie màu theo palette<br/>không theo category.color}} --> statsvm
    d5{{Migrate advisory lock}} --> migrate[backend internal/db/migrate.go]
    d6{{Deploy: OIDC + dynamic SG}} --> deploy[.github/workflows/backend-deploy.yml] & iam[IAM role psy-github-deploy]
    d7{{No unit tests mặc định}} --> conv[convention toàn repo]
    d8{{Release signing<br/>keystore.properties}} --> gradle & relwf[android-release.yml]
```

## Mục cần đọc khi đụng vào…
| Việc | Đọc |
|---|---|
| Sửa 1 feature | `superpowers/specs/*-<feature>-design.md` |
| Chạy local | `RUNNING.md` |
| CI/CD, deploy, secrets | `CICD.md` |
| Gotcha nhanh + lệnh build | `../CLAUDE.md` |
