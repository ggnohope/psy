# Psy — iOS port (Swift / SwiftUI) design

> Spec cho package mới `ios/`: port toàn bộ Psy Android app sang Swift, **full feature parity**, UI/UX khớp Android, dùng chung backend Go hiện có.
> Ngày: 2026-06-20. Liên quan: `../../ARCHITECTURE.md`, các spec feature Android trong cùng thư mục, `backend/`.

## 0. Mục tiêu & phạm vi

**Mục tiêu:** Một ứng dụng iOS native (Swift 6 / SwiftUI) tái hiện đầy đủ Psy: offline-first money tracker, đăng nhập Google bắt buộc, auto backup/sync snapshot lên backend hiện có, theme "Candy Pop", app lock PIN/biometric — feature parity 1:1 với Android.

**Trong scope:**
- Package mới `ios/` (XcodeGen → `Psy.xcodeproj`).
- Toàn bộ feature: Home, Stats, Calendar, Budget, Add/Edit transaction, Manage accounts/categories, Settings (Appearance + Lock), Auth gate (Google Sign-In), App lock (PIN + biometric), auto backup/restore.
- Thay đổi backend **nhỏ, backward-compatible**: chấp nhận nhiều Google audience (`GOOGLE_CLIENT_IDS`) để iOS ID token validate được.

**Ngoài scope:** thay đổi Android app; thay đổi schema/endpoint backend (ngoài multi-audience); multi-ledger UI; đa tiền tệ ngoài VND/USD; đồng bộ ảnh đính kèm qua cloud (giữ nguyên như Android — chỉ path nằm trong blob).

**Quyết định đã chốt (qua brainstorming):**
- Persistence: **SwiftData** + min deploy target **iOS 17**. Charts: **Swift Charts**.
- Project gen: **XcodeGen** (`project.yml`).
- Verify: user cài full Xcode → headless `xcodebuild` + `simctl`; domain logic test qua `swift test`.
- Backend multi-audience: **có**, trong spec này.
- Font: **Quicksand** (bundle TTF) để khớp tuyệt đối Android.

## 1. Technology mapping (Android → iOS)

| Android | iOS |
|---|---|
| Kotlin + Jetpack Compose (Material3) | Swift 6 + SwiftUI |
| Hilt (DI) | Composition root thủ công `AppContainer`, inject qua `.environment` |
| Room (SQLite) | SwiftData (`@Model`, `ModelContext`) |
| Coroutines `Flow` + `combine`/`flatMapLatest` | Combine publishers + `combineLatest`/`map` (port 1:1) |
| DataStore preferences (settings) | `UserDefaults` |
| DataStore (JWT token) | **Keychain** |
| Retrofit + kotlinx.serialization | `URLSession` + `Codable` |
| Credential Manager (Google) | GoogleSignIn iOS SDK (SPM) |
| BiometricPrompt | LocalAuthentication (Face ID / Touch ID) |
| Navigation Compose | `TabView` + `NavigationStack` |
| Custom DonutChart / TrendBars | Swift Charts |
| `R.string.base_url` (resValue) | xcconfig → Info.plist key, đọc lúc runtime |

**Reactivity (then chốt):** giữ business logic trong **ViewModel** (`@Observable`), KHÔNG rải `@Query` vào View. Mỗi repository expose Combine publisher cho từng bảng (`CurrentValueSubject`); mọi mutation gọi `send()` sau khi ghi SwiftData. ViewModel dùng `combineLatest` để mirror chính xác `combine(...)` của Android. Lý do: để port StatsViewModel/BudgetViewModel/CalendarViewModel… nguyên văn, tránh lệch logic.

## 2. Cấu trúc thư mục

```
ios/
  project.yml                       # XcodeGen spec
  Config/
    Debug.xcconfig                  # BASE_URL = http://localhost:8080/
    Release.xcconfig                # BASE_URL = https://psy-backend.duckdns.org/
  Psy/
    PsyApp.swift                    # @main App; scenePhase → onStart/onStop; ModelContainer; AppContainer
    Info.plist                      # BASE_URL key, UIAppFonts, GIDClientID, CFBundleURLTypes (reversed client id)
    Domain/
      Model/{Account,Category,Transaction,Budget,Ledger,Currency,Enums}.swift
      Money.swift
    Data/
      Persistence/
        Entities.swift              # @Model: LedgerEntity, AccountEntity, CategoryEntity, TransactionEntity, BudgetEntity
        ModelContainer+Psy.swift
        IdAllocator.swift           # max(id)+1 per table
        DataChangeBus.swift         # per-table CurrentValueSubject (Flow analog)
      Repositories/
        {Ledger,Account,Category,Transaction,Budget}Repository.swift
        BackupRepository.swift  AuthRepository.swift  SettingsRepository.swift
      Remote/
        APIClient.swift             # URLSession + Bearer + base URL
        AuthAPI.swift  BackupAPI.swift  DTOs.swift
      Backup/
        SnapshotDTO.swift           # Codable, field-name compatible với Android blob
        SnapshotManager.swift       # export/import/wipe/isLocalEmpty
      Auth/TokenStore.swift         # Keychain (token, email, lastSyncAt)
      Seed/DefaultDataSeeder.swift
    Features/
      App/{AppRoot,AppViewModel}.swift
      Auth/{LoginView,GoogleSignInClient}.swift
      Lock/{LockView,BiometricAuthenticator}.swift
      Home/{HomeView,HomeViewModel}.swift
      Stats/{StatsView,StatsViewModel}.swift
      Calendar/{CalendarView,CalendarViewModel}.swift
      Budget/{BudgetView,BudgetViewModel}.swift
      AddEdit/{AddEditTransactionView,AddEditTransactionViewModel}.swift
      Manage/{ManageAccountsView,ManageAccountsViewModel,ManageCategoriesView,ManageCategoriesViewModel}.swift
      Settings/{SettingsView,AppearanceView,AppearanceViewModel,LockSettingsView,LockSettingsViewModel}.swift
    UI/
      Theme/{Color,Theme,Typography,Shape}.swift
      Components/{MonthSelector,BudgetProgress,IconColorPicker,DonutChart,TrendBars}.swift
    Resources/
      Assets.xcassets               # AppIcon, AccentColor
      Fonts/Quicksand-*.ttf
  PsyTests/
    MoneyTests.swift  StatsMathTests.swift   # logic thuần, swift test từ CLI
```

DI: `AppContainer` (lớp khởi tạo ModelContainer + tất cả repository + APIClient + TokenStore). Inject vào SwiftUI environment; ViewModel nhận dependency qua initializer (factory trên container) — mirror Hilt graph.

## 3. Domain model & ID strategy

Backup blob **dùng chung backend với Android** ⇒ JSON phải tương thích trường-tên. `Codable` property names khớp y hệt: `id, ledgerId, type, amountMinor, categoryId, accountId, toAccountId, note, date, createdAt, updatedAt, photoUri` (Transaction); tương tự Ledger/Account/Category/Budget. `type` lưu dạng String (`"INCOME"|"EXPENSE"|"TRANSFER"`, `"CASH"|"BANK"|"CREDIT"|"ASSET"`…) khớp `enum.name` của Kotlin.

- **ID:** mỗi `@Model` mang `id: Int64` tường minh (KHÔNG dùng `PersistentIdentifier` cho reference). Cấp phát kiểu autoincrement: `IdAllocator` lấy `max(existing id)+1` per table (mirror Room `autoGenerate`). Cần thiết để reference chéo (`categoryId`, `accountId`, `toAccountId`) và round-trip snapshot ổn định.
- **Tiền:** `Int64` minor units. `Money.formatMinor(amountMinor:fractionDigits:suffix:)` port nguyên văn: integer arithmetic, grouping `#,##0` kiểu US (dấu phẩy), suffix sau dấu cách (`đ`/`$`), giữ dấu âm.
- **Currency:** `VND(đ,0)`, `USD($,2)`, `of(code)` default VND.
- **Color:** `color: Int64` ARGB packed (0xFFAARRGGBB) như Android. Helper `Color(argb: Int64)`.
- **photoUri:** path file trong sandbox app (Documents/`photos/`). Lưu ảnh bằng `PhotosPicker` → copy vào Documents. Restore từ backup Android → path không tồn tại → View hiển thị placeholder, không crash (giống hành vi cross-device sẵn có).

Enums: `TxType {income, expense, transfer}`, `AccountType {cash, bank, credit, asset}`, `CategoryType {income, expense}` — raw string khớp Kotlin `.name` để serialize.

## 4. Features (full parity)

Bottom `TabView` 4 tab đúng thứ tự Android: **Trang chủ · Thống kê · Lịch · Ngân sách**. Nút **Cài đặt** ở toolbar góc trên (Home). FAB-style "+" mở Add/Edit (sheet hoặc push). Toàn bộ label tiếng Việt khớp Android.

### 4.1 Home (`HomeViewModel`)
- Range tháng hiện tại nửa-mở `[firstOfMonth, firstOfNextMonth)` theo `ZoneId.systemDefault()` (dùng `Calendar.current`).
- Tổng income/expense/net; **TRANSFER loại khỏi** thu/chi.
- List group theo ngày, sort giảm dần; label `"Hôm nay"`/`"Hôm qua"`/`dd/MM/yyyy`.
- Row TRANSFER: icon `🔄`, hiển thị account → toAccount. Row thường: icon category. Có ảnh đính kèm thì hiển thị thumbnail.
- monthLabel `MM/yyyy`.

### 4.2 Stats (`StatsViewModel`) — port nguyên văn
- Tháng chọn được (`prev`/`next`), pie mode EXPENSE/INCOME, account filter (`nil` = "Tất cả").
- Window 6 tháng `[month-5, monthEnd)` query 1 lần, derive subset tháng hiện tại.
- **Account breakdown** tính từ TẤT CẢ account (trước filter), sort desc theo `income+expense`.
- Summary: income/expense/net + `avgPerDayMinor = expense / daysToCount` (tháng hiện tại = ngày hôm nay; tháng khác = số ngày trong tháng).
- **Pie slices**: nhóm theo category (chỉ tx đúng pieMode & có categoryId), màu theo `piePalette` **cố định theo index** (KHÔNG theo `category.color`), sort desc.
- Top 10 theo amount với percent.
- Trend 6 tháng (income/expense theo từng tháng, label `M/yy`).
- Filter account áp lên cả window → summary/pie/trend/top đều theo account đã lọc; breakdown thì không.
- UI: donut (Swift Charts `SectorMark`), trend bars (`BarMark`), card so sánh account bấm lọc.

### 4.3 Calendar (`CalendarViewModel`)
- Lưới **Thứ Hai đầu tuần**: leading nulls = `dayOfWeek(MON=0..SUN=6)`, trailing nulls cho đủ tuần, `chunked(7)`.
- Mỗi `DayCell`: income/expense (TRANSFER loại), `isToday`. Chấm màu thu/chi.
- Chọn ngày → list giao dịch ngày đó (cùng format row như Home).

### 4.4 Budget (`BudgetViewModel`)
- Tổng (uncategorised, `categoryId == null`) + per-category. **Spent = chỉ EXPENSE** (INCOME/TRANSFER loại).
- Per-category sort desc theo % sử dụng. Picker chỉ EXPENSE category chưa có budget.
- Editor: add total / add category / edit / remove; amount chỉ digit; `canSave` = amount>0 và (total mode hoặc đang edit hoặc đã chọn category). VND: typed integer = amountMinor.
- `BudgetProgress` component (thanh % + cảnh báo vượt).

### 4.5 Add/Edit transaction (`AddEditTransactionViewModel`)
- Type segmented: Chi (EXPENSE) / Thu (INCOME) / Chuyển khoản (TRANSFER).
- amountText strip non-digit; `amountMinor = typed * 10^fractionDigits` (VND ×1, USD ×100).
- INCOME/EXPENSE: category grid theo type, account picker. TRANSFER: from+to account (khác nhau), không category, icon 🔄.
- date picker; note; **ảnh đính kèm** (`PhotosPicker` → `PhotoStorage` copy vào Documents; remove xoá file; lỗi → message transient).
- Edit: prefill từ tx (reverse amountMinor / 10^digits), giữ `createdAt` gốc. Delete (chỉ khi edit).
- `canSave`: amount>0; INCOME/EXPENSE cần category+account; TRANSFER cần account≠toAccount.
- Done event (one-shot) → pop về màn trước.

### 4.6 Manage (accounts / categories)
- CRUD đầy đủ; `IconColorPicker` (emoji icon + color từ palette). Category có type (income/expense) + sortOrder.

### 4.7 Settings
- **Appearance**: theme mode (System/Light/Dark) + 3 accent palette (Candy Violet/Pink/Mint), preview tức thì.
- **App lock** (`LockSettingsViewModel`): bật/tắt lock, đặt/đổi PIN (sha256 `"psy_salt:" + pin`), bật biometric (chỉ khi đã có PIN). PIN hash & cờ lock **không** nằm trong snapshot (local-only, mirror Android — lưu UserDefaults).
- **Logout**: best-effort backup → wipe local → clear token.

### 4.8 Auth gate & lifecycle (`AppViewModel` / `AppRoot`)
- `isSignedIn` tri-state: `nil` (loading, hiện spinner — tránh nháy Login) / `false` (Login) / `true`. Nguồn = token tồn tại trong Keychain.
- `AppRoot` switch: loading → Login → Lock (nếu lockEnabled) → App.
- **scenePhase**: `.active` (ON_START) → lock nếu `lockEnabled` và (lần đầu hoặc background > 2s); `.background` (ON_STOP) → ghi `lastBackgroundedAt` + auto-backup best-effort nếu signed-in và đã quá throttle **5 phút** từ lần sync cuối. Chạy trên Task tách rời (mirror appScope).
- Sign-in: GoogleSignIn → idToken → `POST /auth/google` → lưu JWT+email Keychain → `prepareLocalDataAfterLogin` (nếu local rỗng: download backup → import, else seed default data).
- Lock: PIN verify qua `SettingsRepository`; biometric qua LocalAuthentication; unlock → vào app.

## 5. Theme "Candy Pop"
Port palette y hệt: `CandyViolet 0xFFA18CFF`, `CandySky 0xFF7FD8FF`, `CandyPink 0xFFFF8FD6`, `CandyPinkDeep 0xFFFF5FA2`, `CandyGreen 0xFF22C55E`; surface light `0xFFF4F0FF`/onSurface `0xFF2B2640`; surface dark `0xFF1C1830`/onSurface `0xFFEDE9FF`. 3 accent sets (primary/secondary/tertiary) như `accentColorsFor`. Light/Dark/System theo settings.

**Font Quicksand:** bundle `Quicksand-{Medium,SemiBold,Bold}.ttf` (OFL) vào `Resources/Fonts/`, khai `UIAppFonts` trong Info.plist, đăng ký SwiftUI `Font.custom`. Typography mirror: headlineMedium 24 ExtraBold, titleMedium 16 Bold, bodyMedium 14 Medium, labelSmall 11 SemiBold. Shapes bo tròn lớn (Candy).

## 6. Config & Google Sign-In
- **BASE_URL**: simulator gọi host trực tiếp ⇒ Debug `http://localhost:8080/` (khác `10.0.2.2` của Android), Release `https://psy-backend.duckdns.org/`. Inject xcconfig → Info.plist `BASE_URL`; `BuildConfig.baseURL` đọc runtime. ATS: cho phép `localhost` cleartext (NSAllowsLocalNetworking).
- **GoogleSignIn**: cần tạo **iOS OAuth client ID** mới ở Google Cloud (bundle id `com.psy`). Cấu hình: `GIDClientID` (iOS client id) + `CFBundleURLTypes` reversed client id trong Info.plist; `serverClientID` = web client id hiện có (`885786271406-...apps.googleusercontent.com`). Lấy `user.idToken.tokenString` gửi backend.
- **Placeholder**: iOS client id & reversed id để placeholder + README hướng dẫn; build vẫn chạy, login cần điền giá trị thật.

## 7. Backend change (multi-audience) — backward compatible
- `internal/config`: thêm `GoogleClientIDs []string` đọc từ env `GOOGLE_CLIENT_IDS` (CSV). Backward-compat: nếu trống, fallback `[GOOGLE_CLIENT_ID]`.
- `VerifyGoogleIDToken`: nhận `clientIDs []string`, thử `idtoken.Validate` lần lượt từng audience; khớp 1 cái → OK; tất cả fail → error.
- Handler `handleGoogleLogin` truyền `cfg.GoogleClientIDs`.
- Test: cập nhật/ giữ test hiện có; chỉ thêm guard cho multi-aud nếu cần (theo convention "no unit test mặc định" — chỉ thêm khi là regression guard thật).
- Deploy: `.env` trên EC2 thêm `GOOGLE_CLIENT_IDS=<web>,<ios>` (tài liệu trong CICD/README; không commit secret).

## 8. Verification plan
- Domain logic (`Money`, stats math, calendar grid, budget calc) tách được test thuần → `swift test` (chạy được với Command Line Tools, không cần simulator).
- App build/UI: user cài full Xcode → tôi `cd ios && xcodegen generate && xcodebuild -scheme Psy -destination 'generic/platform=iOS Simulator' build`, boot simulator qua `simctl`, smoke test các tab.
- Backend: `cd backend && make dev` + `go test ./...` cho multi-audience.

## 9. Rủi ro & quyết định không hiển nhiên
- **Snapshot compatibility**: JSON field-name phải khớp Android tuyệt đối — đây là ràng buộc cứng (test round-trip bằng blob mẫu Android).
- **ID allocation**: SwiftData không có autoincrement Long; tự quản `IdAllocator`. Single-user UI nên không lo concurrency, vẫn bọc trong ModelContext thao tác tuần tự.
- **Reactivity**: chọn Combine thay `@Query` để port logic chính xác — đánh đổi: phải tự `send()` sau mỗi mutation (kỷ luật trong repository).
- **Google audience**: token iOS có `aud` = iOS client id ⇒ bắt buộc backend multi-audience (đã đưa vào scope).
- **Font binary**: Quicksand TTF cần thêm vào repo (OFL license — commit được).
- **localhost vs 10.0.2.2**: khác biệt nền tảng, dễ quên khi so với Android.

## 10. Out of scope (YAGNI)
Multi-ledger UI, đa tiền tệ mở rộng, nhập cents USD ở UI (giữ giả định cents=0 như Android), đồng bộ file ảnh qua cloud, push notification, widget, iPad-specific layout (chạy được nhưng không tối ưu riêng).
