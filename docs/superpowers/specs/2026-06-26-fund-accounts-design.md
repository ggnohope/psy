# Fund accounts (Quỹ) + icon picker expansion — design

Date: 2026-06-26
Status: approved (design)
Platforms: Android + iOS (parity required)

## Problem / feedback

A tester uses an account named **M2** as a *group fund* (quỹ nhóm). Money moving through
M2 is not the user's personal money, yet Psy currently counts M2's transactions in the
Stats **Thu/Chi** (income/expense) totals, pie chart, trend, and "Theo tài khoản"
breakdown — distorting the user's real spending picture.

The tester's ask (verbatim intent): a fund should *"chỉ hiện ra vào dòng tiền thoi, chứ
không tính vào thu chi của t"* — appear only in the cash flow / transaction list, not in
my income/expense. They confirmed a fund is **created exactly like a normal account**; the
only difference is **whether it counts toward income/expense**. Reference apps mark such
categories with a red **"x"** (Thu nhập x / Chi tiêu x / Ngân sách x).

Secondary feedback: the icon picker has too few icons (~36).

## Goals

1. Let any account be flagged as a **fund**. Fund transactions are excluded from
   income/expense statistics but still appear in the transaction list (cash flow).
2. Expand the icon picker from ~36 to ~90 icons.

## Non-goals / scope decisions

- **Account balance unchanged.** A fund account still contributes to its own balance and to
  any total-balance display. The feedback is scoped to thu/chi only.
- Fund is **not** a new account `type` and **not** a category-level flag. It is a boolean on
  the existing `Account`. Creation flow is identical to a normal account plus one toggle.
- No new "fund summary" screen. Funds remain visible via the existing transaction list and
  the by-account breakdown (marked).

## Data model

Add `isFund: Bool = false` (default false → backward compatible) to:

| Platform | Domain model | Persistence entity | Backup DTO |
|----------|--------------|--------------------|-----------|
| Android | `Account` (`domain/model/Account.kt`) | `AccountEntity` (Room, `data/db/entity/AccountEntity.kt`) | `AccountDto` (`data/backup/SnapshotDto.kt`) |
| iOS | `Account` (`PsyCore/Models.swift`) | `AccountEntity` (`Data/Persistence/Entities.swift`) | `AccountDTO` (`PsyCore/SnapshotDTO.swift`) |

### Persistence

- **Room (Android):** add column `isFund INTEGER NOT NULL DEFAULT 0`. Provide a Room
  migration (or follow the existing migration strategy in the DB module). Update
  `AccountEntity ↔ Account` mappers.
- **SwiftData (iOS):** add stored property `isFund: Bool = false` to `AccountEntity`; update
  `Mappers.swift` (`toDomain()`, `apply(_:)`, `init(from:id:)`). SwiftData lightweight
  migration handles the additive property (default false).

### Backup snapshot (cross-platform, byte-compatible)

- Bump `SnapshotDto.version` / `SnapshotDTO.version` **2 → 3** on both platforms.
- `isFund` is the **last** field of the account DTO.
- **Encode:** always emit the field explicitly (Android kotlinx default; iOS custom encoder
  emits the bool — never omitted).
- **Decode (backward compat with v2 blobs missing the field):**
  - Android: kotlinx `@Serializable` default value `= false` handles absence.
  - iOS: custom decoder uses `decodeIfPresent(Bool.self, forKey: .isFund) ?? false`.
- Field name `isFund` must match exactly on both sides so the shared backup blob round-trips.

## Stats exclusion (core logic)

In **both** the Android `StatsViewModel` and iOS `PsyCore/StatsEngine.swift`, build
`fundAccountIds = accounts.filter { it.isFund }.map { it.id }.toSet()` and exclude any
transaction whose `accountId ∈ fundAccountIds` from:

1. **Summary totals** — income & expense sums (so `net`/Chênh lệch and `avgPerDay`/TB ngày
   derive correctly).
2. **Pie chart slices.**
3. **Top groups list.**
4. **6-month trend bars.**

This filter is **additive and independent** of the existing `TRANSFER` exclusion (TRANSFER
is still skipped everywhere it is today). A fund account may be the source/destination of a
transfer; transfers already don't count, so no special handling needed.

### "Theo tài khoản" (by-account breakdown) — keep, but mark

Fund accounts **remain visible** in the by-account breakdown (the user wants to watch fund
cash flow). To support this:

- Add `isFund: Bool` to the by-account stat struct (`AccountStat` iOS / equivalent Android
  per-account model).
- Per-account income/expense for a fund account is still computed and shown in its row.
- The **grand total** shown at the top of Stats (the summary in §1) excludes funds.
- UI renders a **"Quỹ" badge** on fund rows in the breakdown.

## Budget exclusion

The budget "spent" calculation (both platforms' budget engine / ViewModel) also excludes
transactions whose `accountId ∈ fundAccountIds`, consistent with the reference app's
"Ngân sách x". Locate the budget spend aggregation and apply the same `fundAccountIds`
filter. (Implementation plan must identify the exact budget engine files — not yet mapped in
this design.)

## UI changes

### Account editor (add/edit) — both platforms

Add a toggle labeled **"Quỹ — không tính vào thu/chi"** (or concise equivalent) in the
account editor.

- Android: `ManageAccountsScreen.kt` `AccountEditor` + `ManageAccountsViewModel`
  (`draftIsFund` state, populate in `startEdit`, reset in `startAdd`, pass in `saveEditor`).
- iOS: `ManageAccountsView.swift` editor sheet + `ManageAccountsViewModel` (`draftIsFund`,
  load in edit, pass in `save()`).

### Transaction list badge — both platforms

Transactions belonging to a fund account show a small **"Quỹ"** badge/label on the row
(Home + Calendar lists), analogous to the reference app's red "x". Keep it subtle (small
chip), don't clutter the row.

## Icon picker expansion (~36 → ~90)

Add ~50 additional Lucide icons covering common money-tracker categories: food & drink,
transport, shopping, bills/utilities, health, entertainment, work, family/kids, pets,
travel, finance, home.

- **Names:** kebab-case Lucide names, identical strings on both platforms (same Lucide
  source guarantees alignment).
- **Android:** add entries to `LucideIcons.byName` in `ui/icons/LucideIcon.kt` (the Compose
  `com.composables.icons.lucide` library already provides every icon as an `ImageVector`);
  the picker auto-grows from `byName.keys`.
- **iOS:** scaffold one `lucide-<name>.imageset` per new icon under
  `Psy/Resources/Assets.xcassets/`, sourced from **lucide-static v1.21.0** SVGs (same
  version already bundled), with the existing `Contents.json` shape
  (`template-rendering-intent: template`, `preserves-vector-representation: true`). Add the
  new names to both `bundled` and `pickerSet` in `Psy/UI/Icons/LucideIcon.swift`. A small
  build script may scaffold the imagesets.
- **`pickerSet` must be identical (same names, same order) on both platforms.** Android's
  `pickerSet` is derived from `byName.keys` — ensure insertion order matches iOS's explicit
  array, or define an explicit ordered list on Android too.

## Testing / verification

- **PsyCore unit test** (`swift test`): a real regression guard for fund exclusion — assert
  that with a fund account, its transactions are excluded from summary income/expense, pie,
  trend, and budget, but still present per-account in the breakdown. This is a genuine
  business-logic guard (allowed by the project's "test only for real regression guards"
  rule).
- **Build both apps** (Android `assembleDebug`, iOS `xcodebuild`).
- **Manual check** on simulator/emulator: mark M2 as fund → Stats Thu/Chi drop M2; M2 still
  in by-account with badge; transactions still listed with "Quỹ" badge; budget unaffected by
  M2.

## Parity checklist

Both platforms must ship: `isFund` field (model + entity + DTO + v3 bump), stats exclusion
(4 points), by-account badge, budget exclusion, editor toggle, transaction-row badge, and
the identical expanded icon set.
