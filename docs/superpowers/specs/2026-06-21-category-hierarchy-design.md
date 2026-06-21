# Category hierarchy (cha → con) + Time + Detail record — Design

> Spec ngày 2026-06-21. Bắt nguồn từ feedback BVT user: muốn 1 mục chi tiêu lớn chia ra nhiều mục nhỏ (vd "Vận tải" → Grab, Xăng, Giữ xe, Metro…), tự thêm/custom được, và stats chia chi tiết. Tham chiếu UX app "Daak" (Money-tracker tương tự).
>
> **Cross-platform bắt buộc:** mọi thay đổi làm cho **cả Android (`android/`) và iOS (`ios/`)**, giữ parity UI/UX + business logic. Logic chung (stats/seed/snapshot) phải khớp giữa ViewModels (Android) và `PsyCore` engines (iOS).

## 1. Mục tiêu & scope

Category chuyển từ **phẳng 1 cấp** sang **2 cấp**: nhóm cha (group) → mục con (leaf).

**Trong scope:**
1. Data model 2 cấp (Phương án B: 2 entity riêng `CategoryGroup` + `Category`/leaf).
2. Giao dịch chỉ gán **leaf**; cha để gom nhóm + stats.
3. Mỗi group luôn có **≥1 leaf**; chặn xoá leaf cuối.
4. Stats gom theo cha, bấm xòe ra con (con hiện % nội bộ cha).
5. Manage Categories quản lý cả group lẫn leaf (custom name/icon).
6. AddEdit picker theo tab-group → lưới leaf.
7. **Time**: cho chọn giờ khi thêm giao dịch, default = giờ hiện tại; hiển thị giờ ở list/detail.
8. **Màn Detail record** (read-only) mới.
9. Reset + seed cây mẫu mới; snapshot DTO bump version, giữ byte-compatible Android↔iOS.

**Ngoài scope (để spec sau):**
- **Tag** (gắn nhãn giao dịch) — user không chọn lần này.

## 2. Quyết định đã chốt (qua brainstorm)

| # | Quyết định |
|---|------------|
| 1 | Phương án **B**: 2 entity riêng (`CategoryGroup` cha + `Category` leaf). |
| 2 | Giao dịch **bắt buộc gán leaf**; cha không log trực tiếp. |
| 3 | Mỗi group **luôn ≥1 leaf**; không cho xoá leaf cuối cùng. |
| 4 | Stats **mặc định theo cha**, bấm để xòe ra con (Option 1 của Daak). |
| 5 | **Reset** data cũ + seed cây mẫu mới (không viết data-migration phức tạp). |
| 6 | Scope thêm: **Time** (default = now nếu không chọn) + **màn Detail record**. KHÔNG có Tag. |
| 7 | Leaf **bỏ `color`/`type`** — suy ra từ group (leaf chỉ có icon + tên). |
| 8 | Tap record ở Home → mở **Detail** (không mở edit ngay). |
| 9 | **Budget** chuyển sang gắn theo **group** (`groupId`), không per-leaf. |

## 3. Data model

### 3.1 Android (Room)

**MỚI — `CategoryGroupEntity`** (`data/db/entity/CategoryGroupEntity.kt`):
```kotlin
@Entity(tableName = "category_groups")
data class CategoryGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,      // ARGB packed
    val type: String,     // CategoryType.name: INCOME | EXPENSE
    val sortOrder: Int,
)
```

**SỬA — `CategoryEntity`** (giờ = leaf/con):
```kotlin
@Entity(tableName = "categories", indices = [Index("groupId")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,    // FK -> category_groups.id  (MỚI, required)
    val name: String,
    val icon: String,
    val sortOrder: Int,
)
```
- Bỏ `color` và `type` khỏi leaf — màu lấy theo group (shade trong stats), type lấy theo group.

**`TransactionEntity`**: giữ nguyên. `categoryId: Long?` giờ trỏ **leaf id** (vẫn null cho TRANSFER). Suy ra cha qua `leaf.groupId`.

**Domain models** (`domain/model/`):
- MỚI `CategoryGroup(id, name, icon, color, type: CategoryType, sortOrder)`.
- `Category(id, groupId, name, icon, sortOrder)` — bỏ `color`, `type`.

**DAO**:
- MỚI `CategoryGroupDao`: `observeByType(type)`, `observeAll()`, `upsert`, `delete`, `count`, `getAll`/`deleteAll`/`insertAll` (backup).
- `CategoryDao`: thêm `observeByGroup(groupId): Flow<List<CategoryEntity>>`, `countByGroup(groupId): Int`; bỏ `observeByType` (type giờ thuộc group). Giữ backup helpers.

**Repository** (`domain/repository` + `data/repo`):
- MỚI `CategoryGroupRepository` interface + impl.
- `CategoryRepository`: đổi `observeByType` → `observeByGroup`; thêm `countByGroup`.

**DI**: thêm provide cho `CategoryGroupDao` + `CategoryGroupRepository` (DatabaseModule, RepositoryModule). PsyDatabase: thêm `CategoryGroupEntity` vào `entities`, bump `version` (xem §8).

### 3.2 iOS (PsyCore + SwiftData) — parity

- **PsyCore Models**: MỚI `struct CategoryGroup(id: Int64, name, icon, color: Int64, type: CategoryType, sortOrder: Int)`; `struct Category` bỏ `color`/`type`, thêm `groupId: Int64`.
- **Entities.swift**: MỚI `@Model CategoryGroupEntity`; `CategoryEntity` bỏ color/type, thêm `groupId: Int64`.
- **Repositories**: MỚI `CategoryGroupRepository`; `CategoryRepository` đổi observe theo `groupId`.
- Giữ explicit `Int64` ids như convention iOS hiện tại.

## 4. Rule nghiệp vụ

Đặt ở ViewModel (Android) / engine + repo (iOS), khớp logic:

1. **Log leaf-only**: AddEdit picker chỉ cho chọn leaf.
2. **≥1 leaf / group**: `confirmDelete` leaf kiểm tra `categoryRepo.countByGroup(groupId)`; nếu == 1 → chặn, hiện thông báo "Mỗi nhóm phải còn ít nhất 1 mục".
3. **Xoá group**: confirm; xoá kèm toàn bộ leaf con. Nếu còn giao dịch tham chiếu bất kỳ leaf nào của group → chặn xoá (yêu cầu chuyển/ xoá giao dịch trước). (Quyết định đơn giản v1: chặn; không auto-reassign.)
4. **Đổi type**: group có `type` (INCOME|EXPENSE) cố định khi tạo; leaf kế thừa. (Không cho đổi type group có leaf đang dùng — giữ đơn giản.)

## 5. Stats (gom theo cha, xòe ra con)

`StatsViewModel` (Android) / stats engine (iOS):

- **Pie** = theo **group**: gom amount mọi leaf thuộc group, group theo `leaf.groupId`. Mỗi group 1 màu theo `piePalette` cố định **theo index** (giữ convention hiện tại — KHÔNG theo color thật, nhưng group có `color` riêng cho list/manage).
- **Top/List**: 
  - Hàng **group**: tên, % trên tổng (theo pieMode), tổng tiền, count (số giao dịch trong group), màu = palette index.
  - Bấm group → **xòe** danh sách **leaf**: tên + icon, **% nội bộ group** (amount leaf / tổng group), tiền, count. Bar leaf dùng **shade** màu group.
- **TRANSFER** vẫn bị loại khỏi thu/chi (giữ nguyên).
- Cấu trúc state: thêm cấp lồng, vd `TopGroup(group, amountMinor, percent, count, children: List<TopLeaf>)`.

## 6. Manage Categories (cây 2 cấp)

`ManageCategoriesScreen/ViewModel` (Android) + `ManageCategoriesView` (iOS):

- Tab theo `CategoryType` (Chi/Thu) như hiện tại.
- List **group** (mỗi group expandable) → hiện các **leaf**.
- Group editor: name + icon + color (+ type khi tạo mới).
- Leaf editor: name + icon, gắn `groupId`.
- Thêm leaf trong context 1 group. Reorder bằng `sortOrder`.
- Áp rule §4 (chặn xoá leaf cuối / xoá group có giao dịch).

## 7. AddEdit picker + Time + Home row + Detail

### 7.1 AddEdit category picker
- Hàng **tab = group** (cuộn ngang, giống ảnh Daak) → lưới **leaf** của group đang chọn → chọn 1 leaf.
- `AddEditUiState`: thay `categories` bằng cấu trúc `groups: List<CategoryGroup>` + `leavesByGroup` (hoặc load leaf theo group đang chọn). `selectedCategoryId` = leaf id.
- `onTypeChange` reload **groups theo type** (thay vì categories theo type).

### 7.2 Time
- `prefillDate`: đổi từ `todayEpochMillis()` (00:00) → **`now`** (epoch millis kèm giờ hiện tại) cho giao dịch mới.
- Thêm **time picker** cạnh date picker; lưu `date` = epoch millis đầy đủ (ngày + giờ).
- Nếu user không chỉnh giờ → giữ giờ hiện tại (mới) hoặc giờ gốc (edit).

### 7.3 Home / Calendar row
- Title = **leaf** name + icon. Phụ đề = **group** name + giờ (format `HH:mm`). Số tiền giữ nguyên.
- VM cần join leaf → group để lấy group name.

### 7.4 Màn Detail record (MỚI)
- Route MỚI `detail?txId={txId}` trong `Routes` + `PsyNavHost`. Đổi `onTxClick` ở Home: `navigate(Routes.detail(id))` thay vì addEdit.
- Nội dung (read-only): icon+tên leaf · Ledger · Date · Time · Account (+ toAccount nếu TRANSFER) · Category dạng `group(Type)` · Remark (note, "no remark" nếu rỗng) · ảnh (nếu có).
- Action: **Edit** → `navigate(Routes.addEdit(id))`; **Delete** (confirm) → xoá + popBack.
- iOS: thêm `TransactionDetailView` tương ứng + điều hướng.

## 8. Reset, seed, snapshot, budget

### 8.1 Reset + seed
- **Android**: bump `PsyDatabase.version` (hiện 4 → 5). `fallbackToDestructiveMigration(dropAllTables=true)` đang bật → tự wipe & rebuild. Giữ TODO hiện có (migration thật trước GA).
- **iOS**: xoá SwiftData store cũ khi schema đổi (theo cách iOS port đang xử lý reset).
- **Seed cây mẫu** (`DefaultDataSeeder` + iOS seeder): tạo group + leaf. Đề xuất bộ mẫu (EXPENSE):
  - Ăn uống 🍜 → Đi chợ, Nhà hàng, Khác
  - Cà phê ☕ → Cà phê, Trà sữa, Khác
  - Vận tải 🚌 → Grab, Xăng, Giữ xe, Metro, Khác
  - Mua sắm 🛍️ → Quần áo, Đồ dùng, Khác
  - Hoá đơn 🧾 → Điện nước, Internet, Khác
  - Giải trí 🎮 → Khác
  - (INCOME) Lương 💰 → Khác; Thưởng 🎁 → Khác
  - Mỗi group seed sẵn leaf **"Khác"** để luôn có ≥1 leaf. (Danh sách cụ thể chốt khi implement, miễn mỗi group ≥1 leaf.)

### 8.2 Snapshot DTO (cross-platform, byte-compatible)
- `SnapshotDto`/`SnapshotDTO`: thêm mảng **`categoryGroups: List<CategoryGroupDto>`**.
- MỚI `CategoryGroupDto(id, name, icon, color, type, sortOrder)`.
- `CategoryDto` đổi thành `(id, groupId, name, icon, sortOrder)` — bỏ `color`, `type`.
- `BudgetDto`: `categoryId` → đổi tên ngữ nghĩa thành **`groupId`** (xem §8.3). Giữ field name khớp 2 nền.
- Bump `SnapshotDto.version = 2`. Field names + null-encoding phải khớp Android↔iOS (giữ gotcha snapshot).

### 8.3 Backup cũ (v1) — convert phòng thủ
- Vì chấp nhận reset, không cần migration data local. Nhưng server có thể còn snapshot **v1** (flat categories) → restore phải không crash.
- Khi restore gặp `version < 2`: **convert** — mỗi category v1 → 1 `CategoryGroup` (mượn name/icon/color/type) + 1 leaf "Khác" dưới nó; repoint `transaction.categoryId` cũ → leaf mới; budget cũ (per category) → budget theo group mới.
- Áp dụng giống nhau ở cả Android & iOS để snapshot vẫn dùng chung.

### 8.4 Budget theo group
- `Budget.categoryId` → **`groupId`** (budget cấp nhóm cha). Budget engine cộng mọi giao dịch có `leaf.groupId == budget.groupId` trong tháng.
- Cập nhật `BudgetScreen/ViewModel` (Android) + `BudgetEngine` (iOS) để pick group thay vì category, và tính tổng theo group.

## 9. Parity checklist (Android + iOS)

- [ ] Data model: `CategoryGroup` + `Category(leaf)` + repos/DAO/DI
- [ ] Seed cây mẫu khớp giữa 2 nền
- [ ] Stats: pie theo group + list xòe leaf (state lồng nhau khớp)
- [ ] Manage Categories: CRUD group + leaf, rule ≥1 leaf
- [ ] AddEdit: picker tab-group → leaf + time picker (default now)
- [ ] Home/Calendar row: leaf title + group/giờ phụ đề
- [ ] Màn Detail record + điều hướng
- [ ] Budget theo group
- [ ] SnapshotDTO v2 byte-compatible + converter v1
- [ ] Build verify: Android `:app:assembleDebug` + iOS `xcodebuild` + `swift test` (PsyCore)

## 10. Verify (không viết unit test mặc định)

- Android: build + emulator — thêm group/leaf, log giao dịch vào leaf, xem stats xòe, xem Detail, đổi giờ.
- iOS: `xcodegen generate` + `xcodebuild` simulator; `swift test` cho PsyCore engines (stats/budget/snapshot byte-compat).
- Snapshot: backup ở 1 nền → restore nền kia khớp.
- Chỉ thêm test khi cần regression guard (vd snapshot v1→v2 converter, stats % nội bộ group).
