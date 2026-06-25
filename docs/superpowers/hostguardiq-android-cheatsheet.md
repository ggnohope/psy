# HostGuardIQ Android — component & token cheatsheet (for screen re-skins)

Foundation is built and on branch `feature/hostguardiq-redesign`. Use these APIs; do NOT hard-code hexes — read tokens from `LocalPsyColors.current`. NO emoji as iconography.

## Tokens — `com.psy.ui.theme.LocalPsyColors`
```kotlin
val colors = LocalPsyColors.current
```
Fields (all `Color` unless noted): `bg, surface, sunken, hair, text, text2, text3, blue, blueSoft, amber, amberSoft, teal, tealSoft, green, greenSoft, red, redSoft, isDark: Boolean`.
Brand extras (props): `navy, navyDeep, incomeTint (#7BE3B0), expenseTint (#F8A09B), heroGradient: Brush, accentLine: Brush`.
- `blue` = the active accent primary (rebinds with accent choice). Use it for primary/active states.
- Income amounts → `colors.green`; expense → `colors.red`. On the navy hero, income → `colors.incomeTint`, expense → `colors.expenseTint`.

## Typography — `com.psy.ui.theme.*`
Families: `SpaceGrotesk` (display/numbers), `PlexSans` (body/UI), `PlexMono` (eyebrows/time/codes).
Material styles preset in `PsyTypography`: `headlineMedium` (SpaceGrotesk 28 Bold = screen titles), `titleLarge` (SG 20 SemiBold), `titleMedium` (SG 18 SemiBold = month/section), `bodyLarge` (PlexSans 16), `bodyMedium` (PlexSans 14), `labelLarge` (PlexSans 16 SemiBold = buttons), `labelSmall` (PlexMono 11 SemiBold = eyebrows).
For ad-hoc display numbers use `Text(..., fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 40.sp)`.

## Shapes — `PsyShapes`
`extraSmall`=8 (chips/inputs), `small`=10 (buttons), `medium`=14 (cards), `large`=20 (hero). Pills = `RoundedCornerShape(999.dp)`.

## Icons — Lucide
- **Category/account data icons** (stored string): `com.psy.ui.icons.LucideIcon(name = group.icon, tint = ..., size = 24.dp)`.
- **UI/chrome icons** (back, plus, settings, pencil, etc.): import directly, e.g. `import com.composables.icons.lucide.{Lucide, ArrowLeft, Plus, Settings, Pencil, Trash2, TriangleAlert, ShieldCheck, Lock, Palette, List, User, LogOut, ChevronLeft, ChevronRight, Check, ArrowUpRight, ArrowDownRight}` then `Icon(Lucide.ArrowLeft, null, tint = colors.text)`. (Lucide PascalCase; kebab "trash-2" → `Trash2`, "triangle-alert" → `TriangleAlert`.)
- Icon tint uses `currentColor` semantics — always pass `tint`.

## Shared components — `com.psy.ui.components.*`
- `IconTile(iconName: String, tint: Color, bg: Color, size: Dp = 44.dp)` — tinted rounded-square holding a Lucide data icon. For category tiles: `tint = Color(group.color)`, `bg = Color(group.color).copy(alpha = 0.14f)`.
- `EyebrowLabel(text: String, color: Color? = null)` — mono uppercase eyebrow.
- `SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit)` — type/filter toggles.
- `HeroCard { /* ColumnScope content */ }` — navy gradient + 3px accent bar; white text inside.
- `TransactionRow(iconName, iconTint, iconBg, name, meta, amount, isIncome: Boolean, account, onClick)`.
- `EmptyState(iconName, title, caption)`.
- `Skeleton(modifier)` — shimmer placeholder; give it a size via modifier.
- `IconPicker(selected: String, onPick: (String)->Unit)` — searchable Lucide picker (Manage editors).
- `ColorPicker(selected: Long, onPick: (Long)->Unit)` — DS color swatches.
- `MonthSelector(...)` (existing in `ui/components/MonthSelector.kt`) — keep using; restyle only if needed to chevron buttons + SpaceGrotesk label.
- Charts: `ui/components/charts/DonutChart.kt`, `TrendBars.kt` — reuse; pass DS colors.

## Rules
- Replace any `MaterialTheme.colorScheme.*` reads with `LocalPsyColors.current.*` where it changes appearance, but M3 widgets (Button/TextField) still inherit the mapped scheme.
- Remove imports of deprecated `CandyViolet/CandySky/CandyPink/CandyPinkDeep/CandyGreen` and `accentColorsFor` from `ui.theme` — replace with `LocalPsyColors` tokens. (These live in `ui/theme/LegacyTheme.kt` and will be deleted once no screen imports them.)
- Screen horizontal padding ~22.dp. Cards: surface bg, 1px hair border, radius 14, soft shadow.
- Build to verify: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd android && ./gradlew :app:assembleDebug`.
- Vietnamese copy stays; calm sentence-case tone; no emoji.
