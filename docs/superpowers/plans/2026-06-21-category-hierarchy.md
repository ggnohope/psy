# Category Hierarchy (cha→con) + Time + Detail Record — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chuyển category từ phẳng 1 cấp sang 2 cấp (nhóm cha → mục con), thêm time picker + màn Detail record, parity Android + iOS.

**Architecture:** Phương án B — 2 entity riêng `CategoryGroup` (cha: name/icon/color/type/sortOrder) + `Category` (leaf: groupId/name/icon/sortOrder). Giao dịch gán **leaf**; cha suy ra qua `leaf.groupId`. Stats gom theo cha, xòe ra con. Budget gắn theo group. Snapshot DTO bump v2, byte-compatible Android↔iOS.

**Tech Stack:** Android (Kotlin/Compose/Room/Hilt), iOS (Swift/SwiftUI/SwiftData + PsyCore SwiftPM), Go backend không đổi.

**Spec:** `docs/superpowers/specs/2026-06-21-category-hierarchy-design.md`

**Convention quan trọng (CLAUDE.md):** KHÔNG viết unit test mặc định — verify bằng build + emulator/simulator. Chỉ task PsyCore logic dùng `swift test` (snapshot byte-compat, stats). Mọi feature làm cho **CẢ Android + iOS**.

**Build commands:**
- Android: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd android && ./gradlew :app:assembleDebug`
- iOS app: `cd ios && xcodegen generate && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build`
- PsyCore tests: `cd ios/PsyCore && swift test`

---

## Phase A — Android data layer

### Task A1: Domain models `CategoryGroup` + sửa `Category`

**Files:**
- Create: `android/app/src/main/java/com/psy/domain/model/CategoryGroup.kt`
- Modify: `android/app/src/main/java/com/psy/domain/model/Category.kt`

- [ ] **Step 1: Tạo `CategoryGroup.kt`**
```kotlin
package com.psy.domain.model

data class CategoryGroup(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long, // ARGB packed
    val type: CategoryType,
    val sortOrder: Int,
)
```

- [ ] **Step 2: Sửa `Category.kt`** (bỏ `color`/`type`, thêm `groupId`)
```kotlin
package com.psy.domain.model

data class Category(
    val id: Long = 0,
    val groupId: Long,
    val name: String,
    val icon: String,
    val sortOrder: Int,
)
```

- [ ] **Step 3:** Chưa build (compile sẽ fail cho tới Task A6). Tiếp Task A2.

### Task A2: Entities Room

**Files:**
- Create: `android/app/src/main/java/com/psy/data/db/entity/CategoryGroupEntity.kt`
- Modify: `android/app/src/main/java/com/psy/data/db/entity/CategoryEntity.kt`

- [ ] **Step 1: Tạo `CategoryGroupEntity.kt`**
```kotlin
package com.psy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_groups")
data class CategoryGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,   // CategoryType.name
    val sortOrder: Int,
)
```

- [ ] **Step 2: Sửa `CategoryEntity.kt`**
```kotlin
package com.psy.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index("groupId")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val icon: String,
    val sortOrder: Int,
)
```

### Task A3: DAOs

**Files:**
- Create: `android/app/src/main/java/com/psy/data/db/dao/CategoryGroupDao.kt`
- Modify: `android/app/src/main/java/com/psy/data/db/dao/CategoryDao.kt`

- [ ] **Step 1: Tạo `CategoryGroupDao.kt`**
```kotlin
package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.CategoryGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(group: CategoryGroupEntity): Long
    @Query("SELECT * FROM category_groups WHERE type = :type ORDER BY sortOrder ASC")
    fun observeByType(type: String): Flow<List<CategoryGroupEntity>>
    @Query("SELECT * FROM category_groups ORDER BY sortOrder ASC") fun observeAll(): Flow<List<CategoryGroupEntity>>
    @Query("SELECT COUNT(*) FROM category_groups") suspend fun count(): Int
    @Delete suspend fun delete(group: CategoryGroupEntity)

    // Backup support
    @Query("SELECT * FROM category_groups") suspend fun getAll(): List<CategoryGroupEntity>
    @Query("DELETE FROM category_groups") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<CategoryGroupEntity>)
}
```

- [ ] **Step 2: Sửa `CategoryDao.kt`** — bỏ `observeByType`, thêm `observeByGroup` + `countByGroup`. Kết quả:
```kotlin
package com.psy.data.db.dao

import androidx.room.*
import com.psy.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(category: CategoryEntity): Long
    @Query("SELECT * FROM categories WHERE groupId = :groupId ORDER BY sortOrder ASC")
    fun observeByGroup(groupId: Long): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC") fun observeAll(): Flow<List<CategoryEntity>>
    @Query("SELECT COUNT(*) FROM categories") suspend fun count(): Int
    @Query("SELECT COUNT(*) FROM categories WHERE groupId = :groupId") suspend fun countByGroup(groupId: Long): Int
    @Delete suspend fun delete(category: CategoryEntity)

    // Backup support
    @Query("SELECT * FROM categories") suspend fun getAll(): List<CategoryEntity>
    @Query("DELETE FROM categories") suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<CategoryEntity>)
}
```

### Task A4: Mappers

**Files:**
- Modify: `android/app/src/main/java/com/psy/data/db/mapper/Mappers.kt`

- [ ] **Step 1:** Thay 2 dòng Category mapper, thêm CategoryGroup mapper. Đoạn thay thế:
```kotlin
fun CategoryGroupEntity.toDomain() = CategoryGroup(id, name, icon, color, CategoryType.valueOf(type), sortOrder)
fun CategoryGroup.toEntity() = CategoryGroupEntity(id, name, icon, color, type.name, sortOrder)

fun CategoryEntity.toDomain() = Category(id, groupId, name, icon, sortOrder)
fun Category.toEntity() = CategoryEntity(id, groupId, name, icon, sortOrder)
```
(Xoá 2 dòng `CategoryEntity.toDomain`/`Category.toEntity` cũ.)

### Task A5: Repository interfaces + impls

**Files:**
- Create: `android/app/src/main/java/com/psy/domain/repository/CategoryGroupRepository.kt`
- Create: `android/app/src/main/java/com/psy/data/repo/CategoryGroupRepositoryImpl.kt`
- Modify: `android/app/src/main/java/com/psy/domain/repository/CategoryRepository.kt`
- Modify: `android/app/src/main/java/com/psy/data/repo/CategoryRepositoryImpl.kt`

- [ ] **Step 1: Tạo `CategoryGroupRepository.kt`**
```kotlin
package com.psy.domain.repository

import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryGroupRepository {
    fun observeAll(): Flow<List<CategoryGroup>>
    fun observeByType(type: CategoryType): Flow<List<CategoryGroup>>
    suspend fun count(): Int
    suspend fun upsert(group: CategoryGroup): Long
    suspend fun delete(group: CategoryGroup)
}
```

- [ ] **Step 2: Tạo `CategoryGroupRepositoryImpl.kt`**
```kotlin
package com.psy.data.repo

import com.psy.data.db.dao.CategoryGroupDao
import com.psy.data.db.mapper.toDomain
import com.psy.data.db.mapper.toEntity
import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.CategoryType
import com.psy.domain.repository.CategoryGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryGroupRepositoryImpl @Inject constructor(
    private val dao: CategoryGroupDao
) : CategoryGroupRepository {
    override fun observeAll(): Flow<List<CategoryGroup>> = dao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun observeByType(type: CategoryType): Flow<List<CategoryGroup>> =
        dao.observeByType(type.name).map { l -> l.map { it.toDomain() } }
    override suspend fun count(): Int = dao.count()
    override suspend fun upsert(group: CategoryGroup): Long = dao.upsert(group.toEntity())
    override suspend fun delete(group: CategoryGroup) = dao.delete(group.toEntity())
}
```

- [ ] **Step 3: Sửa `CategoryRepository.kt`** — đổi `observeByType` → `observeByGroup`, thêm `countByGroup`:
```kotlin
package com.psy.domain.repository

import com.psy.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeByGroup(groupId: Long): Flow<List<Category>>
    suspend fun count(): Int
    suspend fun countByGroup(groupId: Long): Int
    suspend fun upsert(category: Category): Long
    suspend fun delete(category: Category)
}
```

- [ ] **Step 4: Sửa `CategoryRepositoryImpl.kt`** tương ứng:
```kotlin
    override fun observeByGroup(groupId: Long): Flow<List<Category>> =
        dao.observeByGroup(groupId).map { l -> l.map { it.toDomain() } }
    override suspend fun countByGroup(groupId: Long): Int = dao.countByGroup(groupId)
```
(Xoá `observeByType`; bỏ import `CategoryType` nếu thừa.)

### Task A6: DI + DB wiring + version bump

**Files:**
- Modify: `android/app/src/main/java/com/psy/data/db/PsyDatabase.kt`
- Modify: `android/app/src/main/java/com/psy/di/DatabaseModule.kt`
- Modify: `android/app/src/main/java/com/psy/di/RepositoryModule.kt`

- [ ] **Step 1: `PsyDatabase.kt`** — thêm `CategoryGroupEntity` vào `entities`, bump `version = 5`, thêm `abstract fun categoryGroupDao(): CategoryGroupDao` + import.

- [ ] **Step 2: `DatabaseModule.kt`** — thêm `@Provides fun provideCategoryGroupDao(db: PsyDatabase): CategoryGroupDao = db.categoryGroupDao()` + import.

- [ ] **Step 3: `RepositoryModule.kt`** — thêm `@Binds @Singleton abstract fun bindCategoryGroupRepo(impl: CategoryGroupRepositoryImpl): CategoryGroupRepository`.

### Task A7: Seed cây mẫu

**Files:**
- Modify: `android/app/src/main/java/com/psy/data/seed/DefaultDataSeeder.kt`

- [ ] **Step 1:** Inject `categoryGroupRepo: CategoryGroupRepository`. Thay block seed category bằng seed group + leaf. Logic: nếu `categoryGroupRepo.count() == 0`, với mỗi group → `upsert` group (lấy id trả về) → upsert các leaf với `groupId = id`. Code:
```kotlin
if (categoryGroupRepo.count() == 0) {
    val palette = listOf(
        0xFFFF8FD6L, 0xFFA18CFFL, 0xFF7FD8FFL, 0xFFFFB86BL, 0xFF6BCB77L,
        0xFFFF6B6BL, 0xFFB088F9L, 0xFF4D96FFL, 0xFFFF5FA2L, 0xFF22C55EL,
    )
    // group icon, group name, type, leaves(name to icon)  — mỗi group có "Khác"
    data class Seed(val name: String, val icon: String, val type: CategoryType, val leaves: List<Pair<String, String>>)
    val seeds = listOf(
        Seed("Ăn uống", "🍜", CategoryType.EXPENSE, listOf("Đi chợ" to "🛒", "Nhà hàng" to "🍽️", "Khác" to "🍴")),
        Seed("Cà phê", "☕", CategoryType.EXPENSE, listOf("Cà phê" to "☕", "Trà sữa" to "🧋", "Khác" to "🥤")),
        Seed("Vận tải", "🚌", CategoryType.EXPENSE, listOf("Grab" to "🛵", "Xăng" to "⛽", "Giữ xe" to "🅿️", "Metro" to "🚇", "Khác" to "🚗")),
        Seed("Mua sắm", "🛍️", CategoryType.EXPENSE, listOf("Quần áo" to "👕", "Đồ dùng" to "🧴", "Khác" to "📦")),
        Seed("Hoá đơn", "🧾", CategoryType.EXPENSE, listOf("Điện nước" to "💡", "Internet" to "🌐", "Khác" to "🧾")),
        Seed("Giải trí", "🎮", CategoryType.EXPENSE, listOf("Khác" to "🎮")),
        Seed("Lương", "💰", CategoryType.INCOME, listOf("Khác" to "💰")),
        Seed("Thưởng", "🎁", CategoryType.INCOME, listOf("Khác" to "🎁")),
    )
    seeds.forEachIndexed { gi, s ->
        val gid = categoryGroupRepo.upsert(
            CategoryGroup(name = s.name, icon = s.icon, color = palette[gi % palette.size], type = s.type, sortOrder = gi)
        )
        s.leaves.forEachIndexed { li, (ln, lic) ->
            categoryRepo.upsert(Category(groupId = gid, name = ln, icon = lic, sortOrder = li))
        }
    }
}
```
(Bỏ block `if (categoryRepo.count() == 0)` cũ.)

- [ ] **Step 2: Build Android** — verify toàn bộ Phase A compile.
Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (các màn UI vẫn dùng API cũ → sẽ fail; xem ghi chú). **Lưu ý:** Stats/AddEdit/ManageCategories VM còn gọi `observeByType` cũ → compile fail tới khi xong Phase B. Nếu muốn build xanh sớm, làm Phase A + B trước khi build.

- [ ] **Step 3: Commit**
```bash
git add android/app/src/main/java/com/psy/{domain,data,di}
git commit -m "feat(android): category 2-level data layer (CategoryGroup + leaf) + seed"
```

---

## Phase B — Android features

### Task B1: ManageCategories (group + leaf CRUD)

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/manage/category/ManageCategoriesViewModel.kt`
- Modify: `android/app/src/main/java/com/psy/ui/manage/category/ManageCategoriesScreen.kt`

- [ ] **Step 1: ViewModel** — quản lý cả group lẫn leaf. State mới:
```kotlin
data class GroupWithLeaves(val group: CategoryGroup, val leaves: List<Category>)

data class ManageCategoriesUiState(
    val type: CategoryType = CategoryType.EXPENSE,
    val groups: List<GroupWithLeaves> = emptyList(),
    // group editor
    val groupEditorOpen: Boolean = false,
    val editingGroupId: Long? = null,
    val groupDraftName: String = "",
    val groupDraftIcon: String = "📦",
    val groupDraftColor: Long = 0xFFA18CFF,
    // leaf editor
    val leafEditorOpen: Boolean = false,
    val editingLeafId: Long? = null,
    val leafParentGroupId: Long? = null,
    val leafDraftName: String = "",
    val leafDraftIcon: String = "📦",
    // delete
    val pendingDeleteGroup: CategoryGroup? = null,
    val pendingDeleteLeaf: Category? = null,
    val message: String? = null,   // vd "Mỗi nhóm phải còn ít nhất 1 mục"
)
```
- Inject `groupRepo: CategoryGroupRepository` + `categoryRepo: CategoryRepository`.
- `groups` build bằng: `_type.flatMapLatest { groupRepo.observeByType(it) }` kết hợp `categoryRepo.observeAll()` → map mỗi group sang `GroupWithLeaves(group, leaves where leaf.groupId == group.id sortedBy sortOrder)`.
- Group editor: `startAddGroup/startEditGroup/saveGroup` (upsert `CategoryGroup` với type = `_type`, sortOrder = max+1 nếu mới).
- Leaf editor: `startAddLeaf(groupId)/startEditLeaf(leaf)/saveLeaf` (upsert `Category` với `groupId = leafParentGroupId`).
- Delete leaf: `confirmDeleteLeaf` — gọi `categoryRepo.countByGroup(leaf.groupId)`; nếu `== 1` → set `message = "Mỗi nhóm phải còn ít nhất 1 mục"`, không xoá; ngược lại `delete`.
- Delete group: `confirmDeleteGroup` — (v1) chặn nếu group còn leaf đang được giao dịch dùng. Vì check giao dịch cần TransactionRepository, inject thêm `txRepo` và thêm DAO query `countByGroup`. **Đơn giản hoá v1:** cho xoá group → xoá kèm mọi leaf của group (cascade thủ công: lặp `categoryRepo.delete` từng leaf rồi `groupRepo.delete`). Cảnh báo trong confirm dialog rằng giao dịch cũ sẽ mất category. (Quyết định: cascade, không chặn — đơn giản & đủ cho v1; ghi rõ trong dialog.)
- `clearMessage()`.

- [ ] **Step 2: Screen** — list group expandable. Mỗi group row: icon + name + color dot + nút edit/delete + nút "＋ thêm mục". Dưới group: list leaf (icon + name + edit/delete). 2 dialog editor (group: dùng `IconColorPicker`; leaf: name + icon picker). Snackbar cho `message`. FAB "＋ Nhóm" gọi `startAddGroup`.

- [ ] **Step 3: Build** (sau khi B2–B4 xong để tránh fail rải rác — hoặc build cuối Phase B).

### Task B2: AddEdit picker (tab group → leaf grid) + Time

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/addedit/AddEditTransactionViewModel.kt`
- Modify: `android/app/src/main/java/com/psy/ui/addedit/AddEditTransactionScreen.kt`

- [ ] **Step 1: ViewModel** — thay danh sách `categories` phẳng bằng group→leaf:
  - State thêm: `groups: List<CategoryGroup>`, `leaves: List<Category>` (leaves của group đang chọn), `selectedGroupId: Long?`. Giữ `selectedCategoryId` = leaf id.
  - Inject `groupRepo`.
  - `init` + `onTypeChange`: load `groups = groupRepo.observeByType(type).first()`; chọn group đầu; load `leaves = categoryRepo.observeByGroup(firstGroupId).first()`; default `selectedCategoryId` = leaf đầu (hoặc null).
  - Thêm `selectGroup(groupId)`: load lại `leaves`, reset `selectedCategoryId` về leaf đầu của group.
  - Khi edit tx: từ `tx.categoryId` (leaf) suy ngược group: load leaf → `groupId` → set `selectedGroupId`.
  - **Time**: đổi `todayEpochMillis()` → `nowEpochMillis()` (`Instant.now().toEpochMilli()` hoặc truyền `now` vào). Thêm `onTimeChange(hour, minute)` cập nhật phần giờ của `date` (giữ ngày, đổi giờ:phút). `onDateChange` giữ phần giờ hiện có khi đổi ngày.

- [ ] **Step 2: Screen** — picker: `ScrollableTabRow`/`LazyRow` các group (tab); dưới là `LazyVerticalGrid` leaf icon+name của group đang chọn; chọn leaf set `selectedCategoryId`. Thêm **TimePicker** (Material3 `TimePickerDialog` hoặc `rememberTimePickerState`) cạnh DatePicker; hiển thị `HH:mm`.

### Task B3: Home + Calendar row (leaf title + group/giờ phụ đề)

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/home/HomeViewModel.kt`
- Modify: `android/app/src/main/java/com/psy/ui/home/HomeScreen.kt`
- Modify: `android/app/src/main/java/com/psy/ui/calendar/CalendarViewModel.kt`
- Modify: `android/app/src/main/java/com/psy/ui/calendar/CalendarScreen.kt`

- [ ] **Step 1: HomeViewModel** — `combine` thêm `groupRepo.observeAll()`. `TxRow` thêm `groupName: String` + `timeLabel: String` (format `HH:mm` từ `tx.date`). Khi build row: `cat = categoryMap[tx.categoryId]` (leaf) → `group = groupMap[cat.groupId]`. `categoryName` = leaf name, `categoryIcon` = leaf icon, `groupName` = group name. (TRANSFER giữ logic cũ.)
- [ ] **Step 2: HomeScreen** — row hiển thị leaf name (title) + phụ đề `"$groupName · $timeLabel"`.
- [ ] **Step 3: CalendarViewModel/Screen** — áp dụng tương tự (kiểm tra cấu trúc row của Calendar; thêm groupName/time nếu hiển thị danh sách giao dịch).

### Task B4: Stats (pie theo group, xòe ra leaf)

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/stats/StatsViewModel.kt`
- Modify: `android/app/src/main/java/com/psy/ui/stats/StatsScreen.kt`

- [ ] **Step 1: ViewModel** — thêm `groupRepo.observeAll()` + `categoryRepo.observeAll()` vào combine. State mới:
```kotlin
data class TopLeaf(val name: String, val icon: String, val amountMinor: Long, val percentInGroup: Float, val count: Int)
data class TopGroup(
    val groupId: Long, val name: String, val icon: String, val color: Long,
    val amountMinor: Long, val percentOfTotal: Float, val count: Int,
    val children: List<TopLeaf>,
)
```
  - `slices` (pie): gom amount theo `leaf.groupId` (chỉ tx có `categoryId != null` và `type == pieMode`), 1 slice / group, màu theo `piePalette[index]`. `PieSlice(groupName, amount, color)`.
  - `top`: với mỗi group có chi tiêu → tính tổng group, % trên tổng pie, count; children = leaf trong group sort desc, mỗi leaf `percentInGroup = leafAmount / groupAmount`. Thay field `top: List<TopGroup>` (bỏ `TopEntry` cũ).
  - Map leaf→group: `categoryMap[leafId].groupId`, `groupMap[groupId]`.
- [ ] **Step 2: Screen** — list group row (đậm: name, % tổng, tổng tiền, count, bar màu group); tap → expand children (leaf: icon, name, % nội bộ, tiền, bar shade màu group). Dùng `remember` set expanded groupIds. Pie không đổi cấu trúc (đã là `List<PieSlice>`).

### Task B5: Budget theo group

**Files:**
- Modify: `android/app/src/main/java/com/psy/domain/model/Budget.kt`
- Modify: `android/app/src/main/java/com/psy/data/db/entity/BudgetEntity.kt`
- Modify: `android/app/src/main/java/com/psy/data/db/dao/BudgetDao.kt` (nếu query theo categoryId)
- Modify: `android/app/src/main/java/com/psy/data/db/mapper/Mappers.kt`
- Modify: `android/app/src/main/java/com/psy/ui/budget/BudgetViewModel.kt`
- Modify: `android/app/src/main/java/com/psy/ui/budget/BudgetScreen.kt`

- [ ] **Step 1:** Đọc `Budget.kt` + `BudgetEntity.kt` + `BudgetViewModel.kt` hiện tại. Đổi field `categoryId: Long?` → `groupId: Long?` ở model + entity + mapper (giữ kiểu nullable). Cập nhật `BudgetDao` nếu có query theo `categoryId`.
- [ ] **Step 2: BudgetViewModel** — picker chọn **group** (dùng `groupRepo.observeByType(EXPENSE)`); tính chi tiêu thực tế của budget = tổng tx tháng có `leaf.groupId == budget.groupId` (join tx→leaf→group). Cập nhật label/icon hiển thị theo group.
- [ ] **Step 3: BudgetScreen** — đổi UI chọn category → chọn group.

- [ ] **Step 4: Build Android (toàn bộ Phase B)**
Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add android/app/src/main/java/com/psy/ui
git commit -m "feat(android): category hierarchy in manage/addedit/home/stats/budget + time picker"
```

### Task B6: Detail record screen (mới)

**Files:**
- Create: `android/app/src/main/java/com/psy/ui/detail/TransactionDetailViewModel.kt`
- Create: `android/app/src/main/java/com/psy/ui/detail/TransactionDetailScreen.kt`
- Modify: `android/app/src/main/java/com/psy/ui/navigation/Routes.kt`
- Modify: `android/app/src/main/java/com/psy/ui/navigation/PsyNavHost.kt`

- [ ] **Step 1: Routes.kt** — thêm:
```kotlin
const val DETAIL_PATTERN = "detail?txId={txId}"
fun detail(txId: Long): String = "detail?txId=$txId"
```
- [ ] **Step 2: ViewModel** — đọc `txId` từ SavedStateHandle; load tx + leaf + group + account(s) + ledger. State read-only: icon/leaf name, ledgerName, dateLabel (`yyyy-MM-dd`), timeLabel (`HH:mm`), accountName (+toAccountName nếu TRANSFER), categoryLabel = `"${group.name}(${type})"`, note (hoặc "no remark"), photoUri. `delete()` xoá tx + emit done.
- [ ] **Step 3: Screen** — Scaffold TopAppBar (back) + body các hàng label/value (giống ảnh Daak) + ảnh nếu có. TopBar actions: Edit (→ `onEdit`), Delete (confirm → `onDeleted`).
- [ ] **Step 4: PsyNavHost** — thêm composable cho `Routes.DETAIL_PATTERN` (navArgument txId LongType). Đổi Home `onTxClick`: `navController.navigate(Routes.detail(id))`. Trong Detail screen: `onEdit = { navController.navigate(Routes.addEdit(id)) }`, `onDeleted = { navController.popBackStack() }`.
- [ ] **Step 5: Build + Commit**
```bash
cd android && ./gradlew :app:assembleDebug
git add android/app/src/main/java/com/psy/ui/detail android/app/src/main/java/com/psy/ui/navigation
git commit -m "feat(android): transaction detail screen + tap-to-detail nav"
```

---

## Phase C — Android snapshot DTO + converter

### Task C1: SnapshotDto v2

**Files:**
- Modify: `android/app/src/main/java/com/psy/data/backup/SnapshotDto.kt`
- Modify: `android/app/src/main/java/com/psy/data/backup/SnapshotManager.kt`

- [ ] **Step 1: SnapshotDto.kt** — bump `version = 2`; thêm `categoryGroups: List<CategoryGroupDto> = emptyList()` vào `SnapshotDto`; thêm `CategoryGroupDto(id, name, icon, color, type, sortOrder)`; đổi `CategoryDto` → `(id, groupId, name, icon, sortOrder)`; đổi `BudgetDto.categoryId` → `groupId`. Cập nhật các hàm `toDto()/toEntity()` tương ứng (Category + Budget + CategoryGroup mới).
- [ ] **Step 2: SnapshotManager.kt** — đọc hiện trạng. Thêm `categoryGroups` vào export (gọi `categoryGroupDao.getAll()`) và import (`deleteAll`/`insertAll`). Inject `CategoryGroupDao`.
- [ ] **Step 3: Converter v1→v2** — trong import, nếu `snapshot.version < 2`: với mỗi `CategoryDto` cũ (v1 có `color`/`type`, không `groupId`) tạo 1 `CategoryGroupEntity` (mượn name/icon/color/type) + 1 leaf "Khác" dưới nó, repoint `transaction.categoryId` cũ → leaf mới, budget cũ (categoryId) → groupId mới. **Lưu ý:** vì `CategoryDto` đã đổi shape, để parse được v1 cần giữ 1 DTO phụ `CategoryDtoV1(id, name, icon, color, type, sortOrder)` + `SnapshotDtoV1`. Quyết định v1: parse linh hoạt bằng kiểm tra `version`; nếu phức tạp, fallback an toàn = bỏ qua categories cũ và để seeder seed lại (chấp nhận reset). Ghi log rõ.

- [ ] **Step 4: Build + Commit**
```bash
cd android && ./gradlew :app:assembleDebug
git add android/app/src/main/java/com/psy/data/backup
git commit -m "feat(android): snapshot v2 with categoryGroups + v1 restore converter"
```

---

## Phase D — iOS PsyCore (models + snapshot + tests)

### Task D1: PsyCore models

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/Models.swift`

- [ ] **Step 1:** Thêm `struct CategoryGroup` (id, name, icon, color: Int64, type: CategoryType, sortOrder) mirror Android. Sửa `struct Category`: bỏ `color`/`type`, thêm `groupId: Int64`; cập nhật `init`.

### Task D2: SnapshotDTO v2 + regression test

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/SnapshotDTO.swift`
- Modify: `ios/PsyCore/Tests/PsyCoreTests/SnapshotDTOTests.swift`

- [ ] **Step 1: Test trước (regression guard byte-compat)** — thêm test: encode `SnapshotDTO` v2 có `categoryGroups` + `categories` (groupId/name/icon/sortOrder) + budget `groupId`, assert JSON keys khớp Android (`categoryGroups`, `groupId`, không có `color`/`type` trong category). Đọc test hiện có để theo style.
Run: `cd ios/PsyCore && swift test`
Expected: FAIL (chưa có field).
- [ ] **Step 2: Sửa `SnapshotDTO.swift`** — `version` default vẫn để encode = 2; thêm `categoryGroups: [CategoryGroupDTO]`; `CategoryGroupDTO(id, name, icon, color, type, sortOrder)`; `CategoryDTO` đổi `(id, groupId, name, icon, sortOrder)`; `BudgetDTO.categoryId` → `groupId`. Giữ field names **khớp Android** (`categoryGroups`, `groupId`).
- [ ] **Step 3:** `swift test` → PASS.
- [ ] **Step 4: Commit**
```bash
git add ios/PsyCore/Sources/PsyCore/{Models,SnapshotDTO}.swift ios/PsyCore/Tests
git commit -m "feat(ios/core): CategoryGroup model + SnapshotDTO v2 (byte-compat android)"
```

### Task D3: StatsEngine + BudgetEngine + HomeEngine + CalendarEngine + AddEditLogic

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/StatsEngine.swift`
- Modify: `ios/PsyCore/Sources/PsyCore/BudgetEngine.swift`
- Modify: `ios/PsyCore/Sources/PsyCore/HomeEngine.swift`
- Modify: `ios/PsyCore/Sources/PsyCore/CalendarEngine.swift`
- Modify: `ios/PsyCore/Sources/PsyCore/AddEditLogic.swift`
- Modify: `ios/PsyCore/Sources/PsyCore/ViewData.swift`
- Modify: `ios/PsyCore/Tests/PsyCoreTests/EngineTests.swift`

- [ ] **Step 1:** Đọc các engine để biết signature hiện tại (đặc biệt cách nhận `categories`). Cập nhật để nhận thêm `groups: [CategoryGroup]` và map leaf→group qua `groupId`.
- [ ] **Step 2: StatsEngine** — pie/top gom theo group (mirror Android B4). Thêm view-data `TopGroup`/`TopLeaf` vào `ViewData.swift` (parity field names với Android state). % nội bộ group = leafAmount/groupAmount.
- [ ] **Step 3: BudgetEngine** — tính theo group: tổng tx có `leaf.groupId == budget.groupId`.
- [ ] **Step 4: HomeEngine/CalendarEngine** — row có leaf name + group name + time label.
- [ ] **Step 5: AddEditLogic** — chọn leaf; suy group; time default = now.
- [ ] **Step 6: Test** — cập nhật `EngineTests.swift` cho stats theo group + % nội bộ group (regression guard spec §10). `swift test` → PASS.
- [ ] **Step 7: Commit**
```bash
cd ios/PsyCore && swift test
git add ios/PsyCore
git commit -m "feat(ios/core): engines group-aware (stats/budget/home/addedit) + tests"
```

---

## Phase E — iOS data + features

### Task E1: SwiftData entities + mappers + repos + seed

**Files:**
- Modify: `ios/Psy/Data/Persistence/Entities.swift`
- Modify: `ios/Psy/Data/Persistence/Mappers.swift`
- Modify: `ios/Psy/Data/Persistence/ModelContainerFactory.swift`
- Create: `ios/Psy/Data/Repositories/CategoryGroupRepository.swift`
- Modify: `ios/Psy/Data/Repositories/CategoryRepository.swift`
- Modify: `ios/Psy/Data/Seed/DefaultDataSeeder.swift`
- Modify: `ios/Psy/Data/Seed/SampleData.swift`
- Modify: `ios/Psy/App/AppContainer.swift`

- [ ] **Step 1: Entities.swift** — thêm `@Model final class CategoryGroupEntity` (id, name, icon, color, type, sortOrder); `CategoryEntity` bỏ color/type, thêm `groupId: Int64`. Cập nhật `ModelContainerFactory` schema list + reset (xoá store khi schema đổi, theo cách iOS port đang làm).
- [ ] **Step 2: Mappers.swift** — map CategoryGroupEntity↔CategoryGroup; Category mapper bỏ color/type + thêm groupId.
- [ ] **Step 3: Repos** — tạo `CategoryGroupRepository` (mirror Android: observeAll/observeByType/count/upsert/delete, phát qua `DataChangeBus`); `CategoryRepository` đổi observeByType→observeByGroup + countByGroup.
- [ ] **Step 4: Seed** — `SampleData`/`DefaultDataSeeder` seed cây mẫu **giống hệt Android Task A7** (cùng tên group/leaf/icon/màu/thứ tự để parity + snapshot khớp).
- [ ] **Step 5: AppContainer** — wire `CategoryGroupRepository` vào DI container.
- [ ] **Step 6: Build iOS**
Run: `cd ios && xcodegen generate && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build`
Expected: build fail ở các View dùng API cũ (xong ở E2). Hoặc gộp build cuối Phase E.

### Task E2: iOS Views (Manage/AddEdit/Home/Calendar/Stats/Budget) + Detail

**Files:**
- Modify: `ios/Psy/Features/Manage/ManageCategoriesView.swift` + `ManageCategoriesViewModel.swift`
- Modify: `ios/Psy/Features/AddEdit/AddEditView.swift` + `AddEditViewModel.swift`
- Modify: `ios/Psy/Features/Home/HomeView.swift` + `HomeViewModel.swift`
- Modify: `ios/Psy/Features/Calendar/CalendarView.swift` + `CalendarViewModel.swift`
- Modify: `ios/Psy/Features/Stats/StatsView.swift` + `StatsViewModel.swift`
- Modify: `ios/Psy/Features/Budget/BudgetView.swift` + `BudgetViewModel.swift`
- Create: `ios/Psy/Features/Detail/TransactionDetailView.swift` + `TransactionDetailViewModel.swift`
- Modify: `ios/Psy/App/RootView.swift` (hoặc nơi định tuyến) cho Detail + tap-to-detail

- [ ] **Step 1: Manage** — UI 2 cấp (group expandable + leaf), CRUD, rule ≥1 leaf (parity Android B1).
- [ ] **Step 2: AddEdit** — picker tab-group → leaf + time picker (default now) (parity B2).
- [ ] **Step 3: Home/Calendar** — row leaf title + group/time subtitle (parity B3).
- [ ] **Step 4: Stats** — pie theo group + xòe leaf, % nội bộ (parity B4).
- [ ] **Step 5: Budget** — chọn group (parity B5).
- [ ] **Step 6: Detail** — màn read-only (Ledger/Date/Time/Account/Category/Remark) + Edit/Delete; tap record ở Home → Detail (parity B6).
- [ ] **Step 7: Build iOS**
Run: `cd ios && xcodegen generate && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build`
Expected: BUILD SUCCEEDED.
- [ ] **Step 8: Commit**
```bash
git add ios/Psy
git commit -m "feat(ios): category hierarchy UI + time + detail screen (parity android)"
```

---

## Phase F — iOS snapshot manager + converter

**Files:**
- Modify: `ios/Psy/Data/Backup/SnapshotManager.swift`
- Modify: `ios/PsyTests/SnapshotManagerTests.swift`

- [ ] **Step 1:** Export/import thêm `categoryGroups`; import budget theo `groupId`. Converter v1→v2 giống Android Task C1 Step 3 (hoặc fallback reset). Đảm bảo restore snapshot tạo từ Android v2 hoạt động.
- [ ] **Step 2: Build app + (nếu có) test app target.**
Run: `cd ios && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build`
- [ ] **Step 3: Commit**
```bash
git add ios/Psy/Data/Backup ios/PsyTests
git commit -m "feat(ios): snapshot v2 + categoryGroups import/export + v1 converter"
```

---

## Phase G — Cross-platform verify

- [ ] **Step 1: Android emulator smoke test** — login → thấy cây mẫu → thêm group "Test" + leaf → log giao dịch vào leaf (chọn giờ) → Home hiện leaf + group + giờ → tap → Detail đúng → Stats pie theo group, xòe ra leaf đúng % → Budget chọn group → xoá leaf cuối bị chặn.
- [ ] **Step 2: iOS simulator smoke test** — lặp lại các bước trên, đối chiếu UI/UX khớp Android.
- [ ] **Step 3: Snapshot cross-platform** — backup ở Android → restore ở iOS (hoặc ngược lại) → cây + giao dịch + budget khớp. Kiểm tra JSON key (`categoryGroups`, `groupId`).
- [ ] **Step 4: PsyCore tests**
Run: `cd ios/PsyCore && swift test`
Expected: ALL PASS.
- [ ] **Step 5:** Mở PR `feature/category-hierarchy` → `main`.

---

## Self-review notes (đã đối chiếu spec)

- Spec §3 (data model) → A1–A6, D1, E1. §4 (rules) → B1, E2-1. §5 (stats) → B4, D3, E2-4. §6 (manage) → B1, E2-1. §7.1 picker → B2, E2-2. §7.2 time → B2, D3-5, E2-2. §7.3 home row → B3, E2-3. §7.4 detail → B6, E2-6. §8.1 reset/seed → A6/A7, E1. §8.2 snapshot → C1, D2, F. §8.3 converter → C1-3, F. §8.4 budget group → B5, D3-3, E2-5. §9 parity → Phase G.
- Type consistency: `groupId` xuyên suốt (entity/model/DTO/DAO), `observeByGroup`/`countByGroup` dùng nhất quán, `TopGroup`/`TopLeaf` dùng ở B4 + D3/E2.
- Known simplification: xoá group = cascade xoá leaf (B1 Step 1) thay vì chặn — đã ghi rõ trong dialog cảnh báo; converter v1 có fallback reset nếu parse phức tạp (C1-3).
