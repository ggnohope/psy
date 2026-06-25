# HostGuardIQ Redesign — Design Spec

**Date:** 2026-06-25
**Source of truth:** `~/Downloads/design_handoff_psy/README.md` + `Psy Enhanced.dc.html` (interactive prototype, all 12 screens, light/dark, accent switching).
**Scope:** Full visual re-skin of every Psy screen from the current "candy" theme to the **HostGuardIQ Design System** (navy/blue/amber, Space Grotesk + IBM Plex, Lucide line icons), **plus** the new states the handoff calls out. Both clients — **Android first, then iOS** — keeping UI/UX *and* business-logic parity.

> Cross-platform rule applies: every token, component, and screen lands on both Android (`android/`) and iOS (`ios/`). Shared calculation logic (stats/budget/calendar/home engines, Money formatting, `SnapshotDTO`) is **not** changed by this work — this is a presentation-layer redesign plus a category-icon data migration.

---

## 1. Goals & Non-Goals

### Goals
- Recreate all 12 screens pixel-accurately to the prototype using each platform's existing component primitives and theme injection (`PsyTheme` / `psyColors`).
- Swap the design foundation: color tokens, typography (3 new font families), shapes/radii, shadows, accent palettes.
- Replace emoji category/account icons with a **large Lucide icon set** rendered identically on both platforms, with a one-time migration of existing data.
- Add the states the handoff specifies: empty states, loading skeletons, amount validation, error toasts.

### Non-Goals
- No changes to business logic, engines, repositories, persistence schema, backend, or auth flow.
- No change to the `SnapshotDTO` field names / null-encoding / byte layout. The `icon` field stays a `String`; only its *vocabulary* changes (emoji → Lucide name).
- No new screens or features beyond the 12 in the handoff and the listed new states.
- No localization refactor (strings stay inline Vietnamese as today).

---

## 2. Key Constraints & Decisions

1. **Icon vocabulary is shared and portable.** `icon: String` is serialized into the cross-platform backup blob (`SnapshotDTO`). Both platforms therefore store and render the **same Lucide name string** (e.g. `"shopping-bag"`, `"coffee"`, `"bus"`, `"landmark"`, `"wallet"`). A backup made on one platform must render identically on the other.

2. **Icon rendering — Approach A (real Lucide on both platforms):**
   - **Android:** `br.com.devsrsouza.compose.icons:lucide` (Compose Icons) — full Lucide set as lazy `ImageVector`s. A `LucideIcon(name: String)` wrapper resolves a stored string → `ImageVector` (name→vector lookup map; fallback `circle-dollar-sign`).
   - **iOS:** a Lucide SwiftPM package (SVG/font-backed), rendered with the current foreground tint. A `LucideIcon(name: String)` view mirrors the Android wrapper (fallback `circle-dollar-sign`).
   - Both wrappers accept the same name strings, guaranteeing identical glyphs and portable snapshots. The exact iOS package is selected during planning (must cover the full set and accept name strings); if no suitable SPM exists, fall back to bundling the required Lucide SVGs as assets keyed by name.
   - **No emoji** anywhere as iconography (HostGuardIQ rule).

3. **Icon picker.** The current `EmojiPicker` (36 fixed emoji) is replaced by a **searchable Lucide icon picker** (scrollable grid + search-by-name field) on both platforms, used in Manage Categories / Manage Accounts editors. This addresses the "current set is too limited" feedback.

4. **Migration of existing icon data.** A one-time, idempotent migration maps known seed emoji → Lucide name (e.g. `💵→wallet`, `🏦→landmark`, `🍜→utensils`, `☕→coffee`, `🚌→bus`, `🛍️→shopping-bag`, …). Any emoji not in the map → fallback `circle-dollar-sign`, still user-editable via the new picker. Migration runs on app start (Android: in the same path as other startup data work; iOS: equivalent). Seed data (`DefaultDataSeeder` on both platforms) is rewritten to emit Lucide names directly.

5. **Accent palette remap.** `AccentPalette` changes from `CANDY_VIOLET / CANDY_PINK / CANDY_MINT` to **`BLUE / AMBER / TEAL`** (maps to primary hex `#0a7cf6 / #f59e0b / #0bb3b0`; rebinds the `--c-blue` primary token app-wide). Persisted legacy values (`"CANDY_VIOLET"` etc.) migrate to `BLUE` on read in `SettingsRepository` (Android) / `SettingsStore` (iOS). `themeMode` (SYSTEM/LIGHT/DARK) is unchanged.

6. **Theme injection mechanism is reused.** Keep `PsyTheme(themeMode, accent)` (Android) and the `psyColors` environment + `PsyTheme` modifier (iOS). Only token *values* and *names* change. Light/dark swap the full token set; accent rebinds the primary.

7. **Category color stays.** `CategoryGroup.color` (ARGB) continues to drive per-category swatches and progress-bar colors. Stats pie continues to use the fixed `piePalette`-by-index (existing decision in CLAUDE.md), unchanged.

---

## 3. Design Foundation

### 3.1 Color tokens
Replace the candy values with the HostGuardIQ token sets (verbatim from handoff). Token names in code map to the `--c-*` set:

**Light:** bg `#f7f9fc`, surface `#ffffff`, surface-2/sunken `#eef2f8`, hair `#dde5ef`, text `#0a2540`, text2 `#33455c`, text3 `#5b6b80`, blue `#0a7cf6`, blue-soft `#e8f2fe`, amber `#f59e0b`, amber-soft `#fef0d4`, teal `#0bb3b0`, teal-soft `#dcf8f7`, green `#1f9d62`, green-soft `#e6f6ed`, red `#e0413a`, red-soft `#fdecec`.

**Dark:** bg `#061a30`, surface `#0d2a48`, surface-2/sunken `#103458`, hair `#1c486f`, text `#eef2f8`, text2 `#aec4da`, text3 `#7e96ae`, blue `#3d97f8`, blue-soft `rgba(61,151,248,.18)`, amber `#fbb43d`, teal `#19e3e0`, green `#3cc987`, red `#f06b65` (soft variants = `.15–.22` alpha of base).

**Brand grounds (both themes):** navy `#0a2540`, deeper `#061a30`; hero gradient `linear-gradient(140deg,#103458,#0a2540,#061a30)`; accent line `linear-gradient(90deg,#0a7cf6,#19e3e0)`; income tint `#7be3b0`, expense tint `#f8a09b` (on navy).

### 3.2 Typography
- **Space Grotesk** (600–700, ~-0.02em tracking): display/headlines/numbers. Sizes: 46 (login), 40 (balance), 32 (detail amount), 28 (titles), 18–20 (section/month), 15 (row amounts).
- **IBM Plex Sans** (400–600): body/UI. 16 (buttons/labels), 13–15 (rows/captions), 11 (nav labels).
- **IBM Plex Mono** (600, .12–.18em, uppercase for eyebrows): eyebrows/time/codes, 10–12px.
- Android: Google Fonts provider (as Quicksand today). iOS: bundle three `.ttf` in `Resources/Fonts` + register in `Info.plist`. Rebuild `CandyTypography` (Android) / `PsyFont` (iOS) to this scale. (Old "Candy"-named symbols may be renamed to neutral names.)

### 3.3 Shapes / radii / spacing
4px base grid; screen horizontal padding ~22px. Radii: chips/inputs 8, buttons 10, cards 14–16, hero 20–24, pills 999. Icon tiles: 36 (list), 42–44 (rows/groups), 48–50 (accounts/detail).

### 3.4 Shadows
`shadow-sm`: `0 1px 2px rgba(10,37,64,.08)` light / `0 2px 10px rgba(0,0,0,.4)` dark. `shadow`: `0 4px 14px rgba(10,37,64,.10), 0 2px 4px rgba(10,37,64,.05)`. `shadow-blue` (CTAs): `0 8px 24px rgba(10,124,246,.30)` light / `.45` dark. No hard black shadows.

---

## 4. Shared Components

Built once per platform, reused across screens. Android: `ui/components/`. iOS: `UI/Components/`.

| Component | Spec |
|---|---|
| **LucideIcon** | `(name: String, size, tint)` → renders Lucide vector by name; fallback `circle-dollar-sign`. |
| **IconTile** | Tinted rounded square (sizes 36/42/44/48/50), holds a LucideIcon; tint + icon color keyed to category color or a passed token. |
| **EyebrowLabel** | IBM Plex Mono, uppercase, tracking .16em, `text3`. |
| **Pill / Badge** | Rounded-999 label; variants: LIVE (teal dot + glow), % badge (color-on-soft), breadcrumb pill (blue-soft). |
| **SegmentedControl** | Sunken container, radius 10, 4px pad; active segment = primary bg + white text. Drives type / stats expense-income / categories chi-thu / account filter. |
| **MonthSwitcher** | Rework existing `MonthSelector`: two 36×36 chevron buttons + Space Grotesk 18/600 month label. |
| **TransactionRow** | Surface card, 1px hair, radius 14, shadow-sm; 44 IconTile + name(15/600) + meta("Cat · time", mono time) + right-aligned signed amount (green/red) + account name. Tap → Detail. |
| **HeroCard** | Navy-gradient card, radius 20, 3px top accent-line bar, decorative radial teal glow, white text. Used by Home balance, Stats summary, Detail hero. |
| **BottomNav** | Surface, top hairline, 4 items; active = icon+label in blue-soft pill + blue; inactive = transparent + text3. Lucide: home, bar-chart, calendar, wallet. |
| **CategoryPicker** | Parent 4-col grid (vertical icon+label tiles, selected = blue-soft + blue inset ring) + hairline + subcategory flex-wrap pills (selected = solid blue + white) + live breadcrumb pill. Selecting a parent auto-selects its first sub. |
| **DonutChart** (rework) | Conic/ring donut with inset hole showing mono label + Space Grotesk amount; legend rows (swatch + name + mono %). |
| **TrendBars** (rework) | 130px grouped bars, green income + red expense (10px, radius-3 top), mono month labels + THU/CHI legend. |
| **IconPicker** (replaces EmojiPicker) | Searchable scrollable Lucide grid + search field; returns a Lucide name string. |
| **ColorPicker** (restyle) | Swatch palette updated to DS colors (blue/amber/teal/green/red + neutrals) for category/account color. |
| **New-state pieces** | `EmptyState` (icon + title + caption), `Skeleton` (shimmer placeholder rows/cards), `Toast` (calm error/info), inline field error text. |

---

## 5. Screens (per handoff §Screens)

Each screen keeps its existing ViewModel/engine and data flow; only the view layer is rebuilt with Section 4 components. Summary of notable per-screen work (full spec = handoff README §1–12 + prototype):

1. **Login** — centered column; navy-gradient shield-check logo badge with **pulsing teal radial glow** (scale 1→1.16, 3.2s); mono eyebrow; Space Grotesk wordmark; Google button (white "G" chip); bottom lock + trust line.
2. **Home** — header (eyebrow + wordmark + 42×42 settings button); **HeroCard** balance (LIVE pill, 40px amount, two income/expense stat tiles); "Hôm nay" section; TransactionRows; 60×60 FAB (bottom 104).
3. **Stats** — MonthSwitcher; account filter SegmentedControl (Tất cả/Tiền mặt/Ngân hàng); navy summary card (2×2 mono/value grid, Thu green / Chi red tint); by-account card (landmark tile + stacked progress bars); expense/income SegmentedControl; DonutChart + legend + top-spend bars; TrendBars (6 months).
4. **Calendar** — title block; MonthSwitcher; white calendar card (T2…CN mono header, 7-col aspect-1 grid, selected day = blue-soft + blue inset ring + red/green dots); day divider; TransactionRows.
5. **Budget** — title + MonthSwitcher; total budget card (left 3px red over-budget border, % pill, red progress, warning row with triangle-alert); per-category cards (IconTile + name + % + progress + caption); dashed "Thêm ngân sách nhóm" button.
6. **Add transaction** ⭐ — type SegmentedControl; centered Space Grotesk amount display + mono caption; amount input; **CategoryPicker** (parent grid + sub pills + live breadcrumb, auto-select-first-sub behavior); account chips (wallet/landmark); read-only date+time mono fields; note input; full-width blue save button.
7. **Transaction detail** — header (back, title, pencil edit, red trash delete); navy HeroCard (50×50 tile + name + mono category label + 32px tinted amount); white detail list card (Sổ/Ngày/Giờ/Tài khoản/Ghi chú rows split by hairline).
8. **Settings** — grouped list card (38×38 tinted IconTile + label + chevron): Giao diện (palette/blue)→Theme, Khoá ứng dụng (lock/teal)→App lock, Quản lý danh mục (list/amber), Quản lý tài khoản (user/green); separate red Đăng xuất card; mono "PSY · v2.0" footer.
9. **Theme (Giao diện)** — mode radio rows (Theo hệ thống / Sáng / Tối, selected = filled blue radio); accent swatches (58×58 Blue/Amber/Teal, selected = check + 3px border; rebinds primary).
10. **App lock** — toggle row (blue IconTile + iOS-style switch, on = blue track) + disabled biometric row at 0.55 opacity with subtitle.
11. **Manage categories** — Chi/Thu SegmentedControl; group cards (42×42 IconTile + name + edit/delete; hairline; child rows; "Thêm mục" text button); 56×56 FAB; editors use the new **IconPicker**.
12. **Manage accounts** — account cards (48×48 IconTile + name + mono CASH/BANK label + chevron); FAB; editor uses IconPicker.

**Bottom nav** shown only on the 4 primary tabs (Home/Stats/Calendar/Budget). Phone chrome (notch/status/home-indicator) is prototype-only — not implemented.

---

## 6. New States (handoff §Interactions)

- **Empty states:** no transactions (Home/Calendar/day), no budgets (Budget), via `EmptyState` (Lucide icon + calm title + caption).
- **Loading skeletons:** shimmer placeholders for list rows and hero/summary cards while data loads.
- **Amount validation (Add):** required, numeric, > 0; inline error + disabled save until valid.
- **Error toasts:** calm, factual, sentence-case (HostGuardIQ voice) for failures (e.g. save/backup errors).
- **Motion:** calm fades + short slides 120–360ms ease-out; only looping animation is the Login teal-glow pulse; respect reduced-motion. No bounce/spring.

---

## 7. Sequencing (Android first, then iOS)

One spec (this doc); separate implementation plans per phase.

- **Phase 1 — Android foundation:** color tokens, typography (3 fonts), shapes/shadows, accent remap + legacy migration, Lucide dep + `LucideIcon` wrapper + icon migration + seed rewrite, IconPicker, and the Section 4 shared components.
- **Phase 2 — Android screens:** in batches — (a) tabs Home/Stats/Calendar/Budget, (b) Add/Detail, (c) Settings/Theme/App-lock, (d) Manage categories/accounts.
- **Phase 3 — Android new states + polish:** empty states, skeletons, validation, toasts, motion; verify build + emulator across light/dark + all 3 accents.
- **Phase 4 — iOS mirror:** repeat Phases 1–3 on iOS keeping pixel + behavior parity; verify `xcodebuild` + simulator; confirm snapshot icon-string round-trips with Android.

---

## 8. Verification

- Per CLAUDE.md: **no unit tests by default** — verify by build + emulator/simulator.
- Android: `./gradlew :app:assembleDebug` + run on emulator; click through all 12 screens in light, dark, and each accent; confirm icon migration on an existing-data install.
- iOS: `xcodegen generate && xcodebuild -scheme Psy ...` + simulator; same screen walk-through.
- Cross-platform: back up on one platform, restore on the other, confirm category/account icons render identically (Lucide-name portability).

---

## 9. Risks

- **iOS Lucide package coverage.** If no SwiftPM covers the full set with name-string lookup, fall back to bundling required Lucide SVGs as named assets. Resolved in Phase 4 planning.
- **Icon migration gaps.** Unmapped emoji fall back to `circle-dollar-sign`; acceptable since editable. Keep the emoji→name map in shared, documented form so both platforms agree.
- **Font availability.** Space Grotesk / IBM Plex on Google Fonts provider (Android) — confirm names; bundle on iOS regardless.
- **Scope size.** 12 screens × 2 platforms + foundation. Phasing + per-phase verification keeps it tractable; Android proves the design before iOS port.
