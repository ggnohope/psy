# HostGuardIQ iOS (SwiftUI) — component & token cheatsheet (for view re-skins)

Foundation + shared components are built and committed on branch `feature/hostguardiq-redesign`. The iOS app BUILDS. Re-skin views to match the Android result + the design handoff. NO emoji as iconography. Do NOT change ViewModels/engines/data flow — only the view layer.

## Tokens — `@Environment(\.psyColors) private var psyColors`
Fields: `bg, surface, sunken, hair, text, text2, text3, blue, blueSoft, amber, amberSoft, teal, tealSoft, green, greenSoft, red, redSoft, isDark`.
Brand: `navy, navyDeep, incomeTint, expenseTint, heroGradient (LinearGradient), accentLine (LinearGradient)`.
- `blue` = active accent primary. Income → `green` (on navy → `incomeTint`); expense → `red` (on navy → `expenseTint`).
- Legacy aliases still exist (`primary`=blue, `onSurface`=text, `background`=bg, `secondary`=teal, `tertiary`=amber) — prefer the real token names; the legacy `CandyColor.*` enum and these aliases get deleted once all views are migrated.

## Typography — `PsyFont`
`headlineMedium` (Space Grotesk 28 bold = screen titles), `titleLarge` (SG 20), `titleMedium` (SG 18), `bodyLarge` (Plex Sans 16), `bodyMedium` (Plex 14), `labelLarge` (Plex 16 semibold), `labelSmall` (Plex Mono 11). Helpers: `PsyFont.display(_ size)` (Space Grotesk bold, for big numbers), `PsyFont.mono(_ size)` (IBM Plex Mono semibold).

## Shapes
`PsyRadius.chip` 8, `.button` 10, `.card` 14, `.hero` 20, `.pill` 999. (`CandyShape.small/medium/large` = 10/14/20 legacy.)

## Icons — Lucide
- Data icons (stored string): `LucideIcon(name: group.icon, size: 24, tint: Color(argb: group.color))`.
- Chrome icons: `LucideIcon(name: "arrow-left", size: 20, tint: psyColors.text)`. Bundled chrome names: arrow-left, plus, settings, pencil, trash-2, triangle-alert, shield-check, lock, palette, list, user, log-out, chevron-right, chevron-left, check, arrow-up-right, arrow-down-right, calendar, fingerprint, delete, x, chart-column, wallet, landmark, house. (Unknown names fall back to circle-dollar-sign.)

## Shared components — `UI/Components/HGComponents.swift`
- `IconTile(iconName:, tint:, bg:, size:)` — tinted rounded tile. Category: `tint: Color(argb: color)`, `bg: Color(argb: color).opacity(0.14)`.
- `EyebrowLabel(text:, color:)` — mono uppercase.
- `SegmentedControl(options:[String], selectedIndex:Int, onSelect:)`.
- `HeroCard { content }` — navy gradient + accent bar; put white text inside.
- `TransactionRowView(iconName:, iconTint:, iconBg:, name:, meta:, amount:, isIncome:, account:)`.
- `EmptyStateView(iconName:, title:, caption:)`.
- `MonthSwitcher(label:, onPrev:, onNext:)` — or keep existing `MonthSelector`.
- Pickers: `IconPicker(selected:, onPick:)` (searchable Lucide), `ColorPicker(selected:, onPick:)` (DS palette).

## Rules
- Replace `CandyColor.*` and `psyColors.primary/onSurface/background` with real tokens (income→green, expense→red, accents→blue/teal, surfaces→surface/bg, text→text/text2/text3).
- Replace any emoji `Text(...)` icon rendering with `LucideIcon`.
- Screen horizontal padding ~22. Cards: `psyColors.surface`, 1px `psyColors.hair` border, radius 14.
- Build: `cd ios && xcodebuild -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 17' build`. Do NOT run xcodegen (parent handles it for new files). Do NOT run builds in parallel.
- Vietnamese copy stays; calm sentence-case; no emoji.
