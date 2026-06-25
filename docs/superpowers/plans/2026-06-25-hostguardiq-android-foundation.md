# HostGuardIQ Redesign — Phase 1: Android Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the HostGuardIQ design foundation on Android — color tokens, typography, shapes/shadows, accent remap + migration, the Lucide icon system + data migration, and the shared UI components — so screen re-skins (Phase 2) can be built on top.

**Architecture:** Introduce a rich `PsyColors` token object exposed via a `CompositionLocal` (mirroring iOS's `psyColors` environment), since Material3's `ColorScheme` lacks slots for the full `--c-*` token set. `PsyTheme` keeps wiring `MaterialTheme` (for built-in M3 widgets) AND provides `LocalPsyColors`. Custom composables read tokens from `LocalPsyColors.current`. Icons become Lucide-name strings rendered by a `LucideIcon(name)` wrapper backed by the Compose Icons library.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Compose Icons (`br.com.devsrsouza.compose.icons:lucide`), Google Fonts provider, DataStore.

**Verification:** Per project CLAUDE.md, NO unit tests — verify each task by `./gradlew :app:assembleDebug` and emulator inspection. Set `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` first.

---

## File Structure

- `android/gradle/libs.versions.toml` — add Lucide dependency coordinates.
- `android/app/build.gradle.kts` — add Lucide `implementation`.
- `android/app/src/main/java/com/psy/data/settings/SettingsState.kt` — remap `AccentPalette` enum.
- `android/app/src/main/java/com/psy/data/settings/SettingsRepositoryImpl.kt` — migrate legacy accent values on read.
- `android/app/src/main/java/com/psy/ui/theme/Color.kt` — HostGuardIQ tokens + `PsyColors` + accent map.
- `android/app/src/main/java/com/psy/ui/theme/PsyColors.kt` (new) — token data class + `CompositionLocal`.
- `android/app/src/main/java/com/psy/ui/theme/Theme.kt` — provide `LocalPsyColors`, map M3 scheme.
- `android/app/src/main/java/com/psy/ui/theme/Type.kt` — Space Grotesk + IBM Plex families + type scale.
- `android/app/src/main/java/com/psy/ui/theme/Shape.kt` — HostGuardIQ radii.
- `android/app/src/main/java/com/psy/ui/icons/LucideIcon.kt` (new) — name→ImageVector wrapper + lookup.
- `android/app/src/main/java/com/psy/data/icons/IconMigration.kt` (new) — emoji→Lucide map + migration.
- `android/app/src/main/java/com/psy/data/seed/DefaultDataSeeder.kt` — seed Lucide names.
- `android/app/src/main/java/com/psy/ui/components/IconColorPicker.kt` — replace EmojiPicker with searchable Lucide IconPicker; restyle ColorPicker palette.
- `android/app/src/main/java/com/psy/ui/components/` — new shared components (IconTile, EyebrowLabel, PsyPill, SegmentedControl, TransactionRow, HeroCard, EmptyState, PsySkeleton, PsyToast).

---

## Task 1: Add Lucide icon dependency

**Files:**
- Modify: `android/gradle/libs.versions.toml`
- Modify: `android/app/build.gradle.kts:82-126`

- [ ] **Step 1: Add version + library to the catalog**

In `[versions]` add:
```toml
composeIconsLucide = "1.1.1"
```
In `[libraries]` add:
```toml
compose-icons-lucide = { group = "br.com.devsrsouza.compose.icons", name = "lucide", version.ref = "composeIconsLucide" }
```

- [ ] **Step 2: Add the implementation dependency**

In `android/app/build.gradle.kts` dependencies block (near the other compose deps, ~line 91):
```kotlin
    implementation(libs.compose.icons.lucide)
```

- [ ] **Step 3: Verify it resolves**

Run: `cd android && ./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -i lucide`
Expected: shows `br.com.devsrsouza.compose.icons:lucide:1.1.1`. If the artifact/version is not found, check the latest on the Compose Icons GitHub releases and update `composeIconsLucide`.

- [ ] **Step 4: Commit**

```bash
git add android/gradle/libs.versions.toml android/app/build.gradle.kts
git commit -m "build(android): add Compose Icons Lucide dependency"
```

---

## Task 2: LucideIcon wrapper (name → ImageVector)

The Compose Icons library exposes icons as `Lucide.ShoppingBag` etc. We need to resolve a **stored string** (`"shopping-bag"`) to an `ImageVector` at runtime. Reflection over the whole set is slow; instead maintain an explicit lookup map of the names the app actually uses (seed + picker set), with a fallback.

**Files:**
- Create: `android/app/src/main/java/com/psy/ui/icons/LucideIcon.kt`

- [ ] **Step 1: Write the wrapper + lookup**

```kotlin
package com.psy.ui.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.icons.Lucide
import compose.icons.lucide.*

/**
 * Resolves a stored Lucide icon-name string (e.g. "shopping-bag") to an ImageVector.
 * The map covers the icons the app seeds + offers in the picker; unknown names fall
 * back to [Lucide.CircleDollarSign]. Names are the kebab-case Lucide ids used in the
 * cross-platform snapshot, so iOS must resolve the same strings.
 */
object LucideIcons {
    val byName: Map<String, ImageVector> = mapOf(
        "wallet" to Lucide.Wallet,
        "landmark" to Lucide.Landmark,
        "utensils" to Lucide.Utensils,
        "shopping-cart" to Lucide.ShoppingCart,
        "coffee" to Lucide.Coffee,
        "cup-soda" to Lucide.CupSoda,
        "bus" to Lucide.Bus,
        "bike" to Lucide.Bike,
        "fuel" to Lucide.Fuel,
        "train-front" to Lucide.TrainFront,
        "square-parking" to Lucide.SquareParking,
        "car" to Lucide.Car,
        "shopping-bag" to Lucide.ShoppingBag,
        "shirt" to Lucide.Shirt,
        "package" to Lucide.Package,
        "receipt" to Lucide.Receipt,
        "lightbulb" to Lucide.Lightbulb,
        "globe" to Lucide.Globe,
        "gamepad-2" to Lucide.Gamepad2,
        "banknote" to Lucide.Banknote,
        "gift" to Lucide.Gift,
        "circle-dollar-sign" to Lucide.CircleDollarSign,
        "house" to Lucide.House,
        "pill" to Lucide.Pill,
        "hospital" to Lucide.Hospital,
        "smartphone" to Lucide.Smartphone,
        "plane" to Lucide.Plane,
        "graduation-cap" to Lucide.GraduationCap,
        "dog" to Lucide.Dog,
        "credit-card" to Lucide.CreditCard,
        "trending-up" to Lucide.TrendingUp,
        "dumbbell" to Lucide.Dumbbell,
        "music" to Lucide.Music,
        "umbrella" to Lucide.Umbrella,
        "beer" to Lucide.Beer,
        "clapperboard" to Lucide.Clapperboard,
    )

    /** Icons offered in the picker (ordered). Extend freely. */
    val pickerSet: List<String> = byName.keys.toList()

    fun resolve(name: String): ImageVector = byName[name] ?: Lucide.CircleDollarSign
}

@Composable
fun LucideIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
) {
    Icon(
        imageVector = LucideIcons.resolve(name),
        contentDescription = null,
        tint = tint,
        modifier = modifier.then(Modifier),
    )
}
```

- [ ] **Step 2: Verify the icon symbols compile**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. If any `Lucide.Xxx` symbol is unresolved, the kebab name maps to a different PascalCase id — check the Compose Icons Lucide package and adjust the right-hand side (keep the string key stable, it is the portable id).

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/psy/ui/icons/LucideIcon.kt
git commit -m "feat(android): LucideIcon wrapper with name lookup"
```

---

## Task 3: HostGuardIQ color tokens + PsyColors

**Files:**
- Create: `android/app/src/main/java/com/psy/ui/theme/PsyColors.kt`
- Modify: `android/app/src/main/java/com/psy/ui/theme/Color.kt`

- [ ] **Step 1: Create the token data class + CompositionLocal**

```kotlin
package com.psy.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Full HostGuardIQ semantic token set (mirrors iOS PsyColors / the --c-* CSS vars). */
data class PsyColors(
    val bg: Color,
    val surface: Color,
    val sunken: Color,
    val hair: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val blue: Color,        // primary (rebound by accent)
    val blueSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val teal: Color,
    val tealSoft: Color,
    val green: Color,
    val greenSoft: Color,
    val red: Color,
    val redSoft: Color,
    val isDark: Boolean,
) {
    // Brand grounds (both themes)
    val navy get() = Color(0xFF0A2540)
    val navyDeep get() = Color(0xFF061A30)
    val incomeTint get() = Color(0xFF7BE3B0)   // on navy
    val expenseTint get() = Color(0xFFF8A09B)  // on navy
    val heroGradient: Brush
        get() = Brush.linearGradient(listOf(Color(0xFF103458), Color(0xFF0A2540), Color(0xFF061A30)))
    val accentLine: Brush
        get() = Brush.horizontalGradient(listOf(blue, Color(0xFF19E3E0)))
}

val LightPsyColors = PsyColors(
    bg = Color(0xFFF7F9FC), surface = Color(0xFFFFFFFF), sunken = Color(0xFFEEF2F8),
    hair = Color(0xFFDDE5EF), text = Color(0xFF0A2540), text2 = Color(0xFF33455C),
    text3 = Color(0xFF5B6B80), blue = Color(0xFF0A7CF6), blueSoft = Color(0xFFE8F2FE),
    amber = Color(0xFFF59E0B), amberSoft = Color(0xFFFEF0D4), teal = Color(0xFF0BB3B0),
    tealSoft = Color(0xFFDCF8F7), green = Color(0xFF1F9D62), greenSoft = Color(0xFFE6F6ED),
    red = Color(0xFFE0413A), redSoft = Color(0xFFFDECEC), isDark = false,
)

val DarkPsyColors = PsyColors(
    bg = Color(0xFF061A30), surface = Color(0xFF0D2A48), sunken = Color(0xFF103458),
    hair = Color(0xFF1C486F), text = Color(0xFFEEF2F8), text2 = Color(0xFFAEC4DA),
    text3 = Color(0xFF7E96AE), blue = Color(0xFF3D97F8), blueSoft = Color(0x2E3D97F8),
    amber = Color(0xFFFBB43D), amberSoft = Color(0x33FBB43D), teal = Color(0xFF19E3E0),
    tealSoft = Color(0x3319E3E0), green = Color(0xFF3CC987), greenSoft = Color(0x333CC987),
    red = Color(0xFFF06B65), redSoft = Color(0x33F06B65), isDark = true,
)

val LocalPsyColors = staticCompositionLocalOf { LightPsyColors }
```

- [ ] **Step 2: Replace Color.kt with accent definitions (blue/amber/teal)**

Replace the entire file `Color.kt` with:
```kotlin
package com.psy.ui.theme

import androidx.compose.ui.graphics.Color
import com.psy.data.settings.AccentPalette

/** Primary hue per accent choice; rebinds PsyColors.blue. */
fun accentPrimary(accent: AccentPalette, dark: Boolean): Color = when (accent) {
    AccentPalette.BLUE  -> if (dark) Color(0xFF3D97F8) else Color(0xFF0A7CF6)
    AccentPalette.AMBER -> if (dark) Color(0xFFFBB43D) else Color(0xFFF59E0B)
    AccentPalette.TEAL  -> if (dark) Color(0xFF19E3E0) else Color(0xFF0BB3B0)
}

/** Soft variant of the accent primary (for active pills/tiles). */
fun accentSoft(accent: AccentPalette, dark: Boolean): Color = when (accent) {
    AccentPalette.BLUE  -> if (dark) Color(0x2E3D97F8) else Color(0xFFE8F2FE)
    AccentPalette.AMBER -> if (dark) Color(0x33FBB43D) else Color(0xFFFEF0D4)
    AccentPalette.TEAL  -> if (dark) Color(0x3319E3E0) else Color(0xFFDCF8F7)
}
```

- [ ] **Step 3: Build (expects Theme.kt break — fixed in Task 5)**

This will not compile until Task 5 updates `Theme.kt` and Task 4 updates the enum. Proceed to Task 4.

---

## Task 4: Remap AccentPalette enum + migrate persisted values

**Files:**
- Modify: `android/app/src/main/java/com/psy/data/settings/SettingsState.kt`
- Modify: `android/app/src/main/java/com/psy/data/settings/SettingsRepositoryImpl.kt`

- [ ] **Step 1: Remap the enum + default**

In `SettingsState.kt`:
```kotlin
enum class AccentPalette { BLUE, AMBER, TEAL }

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPalette = AccentPalette.BLUE,
    val lockEnabled: Boolean = false,
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false,
)
```
(Keep `ThemeMode` unchanged.)

- [ ] **Step 2: Migrate legacy stored values on read**

In `SettingsRepositoryImpl.kt`, find where the accent string is parsed (e.g. `AccentPalette.valueOf(stored)`). Replace with a tolerant parse:
```kotlin
private fun parseAccent(raw: String?): AccentPalette = when (raw) {
    "BLUE", "AMBER", "TEAL" -> AccentPalette.valueOf(raw)
    "CANDY_PINK" -> AccentPalette.AMBER   // closest warm hue
    "CANDY_MINT" -> AccentPalette.TEAL
    else -> AccentPalette.BLUE            // CANDY_VIOLET, null, or unknown
}
```
Use `parseAccent(...)` wherever the stored accent string is read.

- [ ] **Step 3: Build (still expects Theme.kt break — Task 5)**

Proceed to Task 5.

---

## Task 5: Wire PsyTheme (M3 scheme + LocalPsyColors)

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/theme/Theme.kt`

- [ ] **Step 1: Replace Theme.kt**

```kotlin
package com.psy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.psy.data.settings.AccentPalette
import com.psy.data.settings.ThemeMode

@Composable
fun PsyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentPalette = AccentPalette.BLUE,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val base = if (dark) DarkPsyColors else LightPsyColors
    val psyColors = base.copy(
        blue = accentPrimary(accent, dark),
        blueSoft = accentSoft(accent, dark),
    )
    // Material3 scheme for built-in widgets; map onto our tokens.
    val scheme = if (dark) {
        darkColorScheme(
            primary = psyColors.blue, onPrimary = androidx.compose.ui.graphics.Color.White,
            surface = psyColors.surface, onSurface = psyColors.text,
            background = psyColors.bg, onBackground = psyColors.text,
            surfaceVariant = psyColors.sunken, outline = psyColors.hair,
            error = psyColors.red,
        )
    } else {
        lightColorScheme(
            primary = psyColors.blue, onPrimary = androidx.compose.ui.graphics.Color.White,
            surface = psyColors.surface, onSurface = psyColors.text,
            background = psyColors.bg, onBackground = psyColors.text,
            surfaceVariant = psyColors.sunken, outline = psyColors.hair,
            error = psyColors.red,
        )
    }
    CompositionLocalProvider(LocalPsyColors provides psyColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PsyTypography,
            shapes = PsyShapes,
            content = content,
        )
    }
}
```
(Note: `PsyTypography` and `PsyShapes` are introduced in Tasks 6–7; the old `CandyTypography`/`CandyShapes` names are removed there.)

- [ ] **Step 2: Defer build to after Tasks 6–7** (type/shape symbols not renamed yet).

---

## Task 6: Typography (Space Grotesk + IBM Plex)

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/theme/Type.kt`

- [ ] **Step 1: Replace Type.kt**

```kotlin
package com.psy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.psy.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val spaceGrotesk = GoogleFont("Space Grotesk")
private val plexSans = GoogleFont("IBM Plex Sans")
private val plexMono = GoogleFont("IBM Plex Mono")

val SpaceGrotesk = FontFamily(
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.Bold),
)
val PlexSans = FontFamily(
    Font(googleFont = plexSans, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = plexSans, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = plexSans, fontProvider = provider, weight = FontWeight.SemiBold),
)
val PlexMono = FontFamily(
    Font(googleFont = plexMono, fontProvider = provider, weight = FontWeight.SemiBold),
)

/** Body/UI = IBM Plex Sans; display slots use Space Grotesk (set per-call in composables). */
val PsyTypography = Typography(
    headlineMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelSmall = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.6.sp),
)
```

---

## Task 7: Shapes

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/theme/Shape.kt`

- [ ] **Step 1: Replace Shape.kt**

```kotlin
package com.psy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** HostGuardIQ radii: chips/inputs 8, buttons 10, cards 14–16, hero 20–24. */
val PsyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)
```

- [ ] **Step 2: Full build of the theme foundation**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Fix any remaining references to removed symbols (`CandyTypography`, `CandyShapes`, `accentColorsFor`, `AccentColors`, `CandyViolet`, `SurfaceLight`, etc.) by grepping: `grep -rn "Candy\|accentColorsFor\|SurfaceLight\|OnSurfaceLight\|SurfaceDark" android/app/src/main/java/com/psy`. AppearanceScreen references to `AccentPalette.CANDY_*` must become `BLUE/AMBER/TEAL` (these get fully restyled in Phase 2; for now just make them compile).

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/psy/ui/theme android/app/src/main/java/com/psy/data/settings
git commit -m "feat(android): HostGuardIQ theme foundation (tokens, fonts, shapes, accent remap)"
```

---

## Task 8: Icon data migration + seed rewrite

**Files:**
- Create: `android/app/src/main/java/com/psy/data/icons/IconMigration.kt`
- Modify: `android/app/src/main/java/com/psy/data/seed/DefaultDataSeeder.kt`
- Modify: startup call site (find via `grep -rn "seedIfEmpty" android/app/src/main/java`) — likely `PsyApplication.kt` or an initializer.

- [ ] **Step 1: Create the emoji→Lucide map + migration**

```kotlin
package com.psy.data.icons

import com.psy.domain.repository.*
import javax.inject.Inject

/** Maps the legacy seed emoji to portable Lucide names. Unknown → "circle-dollar-sign". */
object IconMap {
    val emojiToLucide: Map<String, String> = mapOf(
        "💵" to "wallet", "🏦" to "landmark", "🍜" to "utensils", "🛒" to "shopping-cart",
        "🍽️" to "utensils", "🍴" to "utensils", "☕" to "coffee", "🧋" to "cup-soda",
        "🥤" to "cup-soda", "🚌" to "bus", "🛵" to "bike", "⛽" to "fuel", "🅿️" to "square-parking",
        "🚇" to "train-front", "🚗" to "car", "🛍️" to "shopping-bag", "👕" to "shirt",
        "🧴" to "package", "📦" to "package", "🧾" to "receipt", "💡" to "lightbulb",
        "🌐" to "globe", "🎮" to "gamepad-2", "💰" to "banknote", "🎁" to "gift",
        "🏠" to "house", "💊" to "pill", "🏥" to "hospital", "📱" to "smartphone",
        "✈️" to "plane", "🎓" to "graduation-cap", "🐶" to "dog", "💳" to "credit-card",
        "🪙" to "banknote", "📈" to "trending-up", "🏋️" to "dumbbell", "🎵" to "music",
        "☂️" to "umbrella", "🍺" to "beer", "🎬" to "clapperboard", "wallet" to "wallet",
    )
    fun toLucide(icon: String): String =
        emojiToLucide[icon] ?: if (icon.firstOrNull()?.isLetter() == true) icon else "circle-dollar-sign"
}

/** One-time, idempotent: rewrites any emoji icon strings to Lucide names. */
class IconMigration @Inject constructor(
    private val accountRepo: AccountRepository,
    private val categoryGroupRepo: CategoryGroupRepository,
    private val categoryRepo: CategoryRepository,
) {
    suspend fun run() {
        accountRepo.all().forEach { a ->
            val mapped = IconMap.toLucide(a.icon)
            if (mapped != a.icon) accountRepo.upsert(a.copy(icon = mapped))
        }
        categoryGroupRepo.all().forEach { g ->
            val mapped = IconMap.toLucide(g.icon)
            if (mapped != g.icon) categoryGroupRepo.upsert(g.copy(icon = mapped))
        }
        categoryRepo.all().forEach { c ->
            val mapped = IconMap.toLucide(c.icon)
            if (mapped != c.icon) categoryRepo.upsert(c.copy(icon = mapped))
        }
    }
}
```
NOTE: confirm the repo "read all" method names (`all()`, `firstOrNull()`, `count()` exist; if the list accessor differs, adapt). Migration is idempotent because letter-starting strings (already Lucide names) pass through unchanged.

- [ ] **Step 2: Rewrite the seeder to emit Lucide names**

Replace the emoji literals in `DefaultDataSeeder.kt` (accounts + every `Seed`/leaf) with Lucide names using the same mapping, e.g.:
```kotlin
accountRepo.upsert(Account(name = "Tiền mặt", type = AccountType.CASH, icon = "wallet", color = 0xFF22C55E))
accountRepo.upsert(Account(name = "Ngân hàng", type = AccountType.BANK, icon = "landmark", color = 0xFF7FD8FF))
...
Seed("Ăn uống", "utensils", CategoryType.EXPENSE, listOf("Đi chợ" to "shopping-cart", "Nhà hàng" to "utensils", "Khác" to "utensils")),
Seed("Cà phê", "coffee", CategoryType.EXPENSE, listOf("Cà phê" to "coffee", "Trà sữa" to "cup-soda", "Khác" to "cup-soda")),
Seed("Vận tải", "bus", CategoryType.EXPENSE, listOf("Grab" to "bike", "Xăng" to "fuel", "Giữ xe" to "square-parking", "Metro" to "train-front", "Khác" to "car")),
Seed("Mua sắm", "shopping-bag", CategoryType.EXPENSE, listOf("Quần áo" to "shirt", "Đồ dùng" to "package", "Khác" to "package")),
Seed("Hoá đơn", "receipt", CategoryType.EXPENSE, listOf("Điện nước" to "lightbulb", "Internet" to "globe", "Khác" to "receipt")),
Seed("Giải trí", "gamepad-2", CategoryType.EXPENSE, listOf("Khác" to "gamepad-2")),
Seed("Lương", "banknote", CategoryType.INCOME, listOf("Khác" to "banknote")),
Seed("Thưởng", "gift", CategoryType.INCOME, listOf("Khác" to "gift")),
```
Also change the seed `palette` to DS colors (blue/amber/teal/green/red family) — optional but recommended for visual consistency:
```kotlin
val palette = listOf(0xFF0A7CF6L, 0xFFF59E0BL, 0xFF0BB3B0L, 0xFF1F9D62L, 0xFFE0413AL, 0xFF3D97F8L, 0xFFFBB43DL, 0xFF19E3E0L)
```

- [ ] **Step 3: Run migration on startup**

At the seed call site (after sign-in/DB ready, alongside `seedIfEmpty`), inject `IconMigration` and call `iconMigration.run()` once after seeding. Add `IconMigration` to Hilt graph if needed (it's `@Inject constructor`, so it's provided automatically).

- [ ] **Step 4: Build + verify on emulator with existing data**

Run: `cd android && ./gradlew :app:assembleDebug` → install on an emulator that already has emoji data → confirm icons now render as Lucide line glyphs (after migration) and fresh installs seed Lucide names.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/psy/data/icons android/app/src/main/java/com/psy/data/seed/DefaultDataSeeder.kt
git commit -m "feat(android): migrate category/account icons to Lucide names"
```

---

## Task 9: Searchable Lucide IconPicker + restyled ColorPicker

**Files:**
- Modify: `android/app/src/main/java/com/psy/ui/components/IconColorPicker.kt`

- [ ] **Step 1: Replace EmojiPicker with IconPicker (search + grid)**

```kotlin
@Composable
fun IconPicker(
    selected: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    var query by remember { mutableStateOf("") }
    val items = remember(query) {
        if (query.isBlank()) LucideIcons.pickerSet
        else LucideIcons.pickerSet.filter { it.contains(query.trim(), ignoreCase = true) }
    }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Tìm biểu tượng") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().height((6 * 52).dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(items) { name ->
                val isSel = name == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) colors.blueSoft else colors.sunken)
                        .clickable { onPick(name) },
                ) {
                    LucideIcon(name, tint = if (isSel) colors.blue else colors.text2, size = 22.dp)
                }
            }
        }
    }
}
```
Add the needed imports (`LocalPsyColors`, `LucideIcon`, `remember`, `mutableStateOf`, `OutlinedTextField`, `Column`, etc.).

- [ ] **Step 2: Restyle ColorPicker palette to DS colors**

Replace `COLOR_PALETTE` with:
```kotlin
private val COLOR_PALETTE: List<Long> = listOf(
    0xFF0A7CF6, 0xFFF59E0B, 0xFF0BB3B0, 0xFF1F9D62, 0xFFE0413A,
    0xFF3D97F8, 0xFFFBB43D, 0xFF19E3E0, 0xFF5B6B80, 0xFF0A2540,
)
```

- [ ] **Step 3: Update callers** (`grep -rn "EmojiPicker" android/app/src/main/java`) — rename usages to `IconPicker`. Editors in Manage screens pass the stored Lucide name as `selected`.

- [ ] **Step 4: Build**

Run: `cd android && ./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/psy/ui/components/IconColorPicker.kt
git commit -m "feat(android): searchable Lucide IconPicker + DS color palette"
```

---

## Task 10: Shared components — IconTile, EyebrowLabel, PsyPill, SegmentedControl

**Files:**
- Create: `android/app/src/main/java/com/psy/ui/components/Primitives.kt`

- [ ] **Step 1: Write the primitives**

```kotlin
package com.psy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psy.ui.icons.LucideIcon
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono

@Composable
fun IconTile(iconName: String, tint: Color, bg: Color, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.25f)).background(bg),
    ) { LucideIcon(iconName, tint = tint, size = size * 0.5f) }
}

@Composable
fun EyebrowLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val c = color ?: LocalPsyColors.current.text3
    Text(text.uppercase(), fontFamily = PlexMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.8.sp, color = c, modifier = modifier)
}

@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalPsyColors.current
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(colors.sunken).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.blue else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 9.dp),
            ) {
                Text(label, color = if (active) Color.White else colors.text2,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}
```

- [ ] **Step 2: Build** → BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/psy/ui/components/Primitives.kt
git commit -m "feat(android): shared primitives (IconTile, EyebrowLabel, SegmentedControl)"
```

---

## Task 11: TransactionRow, HeroCard, EmptyState, Skeleton, Toast

**Files:**
- Create: `android/app/src/main/java/com/psy/ui/components/Cards.kt`
- Create: `android/app/src/main/java/com/psy/ui/components/States.kt`

- [ ] **Step 1: HeroCard + TransactionRow (`Cards.kt`)**

```kotlin
package com.psy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono
import com.psy.ui.theme.SpaceGrotesk

@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalPsyColors.current
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.heroGradient)) {
        // 3px accent bar on top edge
        Box(Modifier.fillMaxWidth().height(3.dp).background(colors.accentLine).align(Alignment.TopCenter))
        Column(Modifier.padding(22.dp).padding(top = 3.dp), content = content)
    }
}

@Composable
fun TransactionRow(
    iconName: String, iconTint: Color, iconBg: Color,
    name: String, meta: String, amount: String, isIncome: Boolean, account: String,
    onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(colors.surface).border(1.dp, colors.hair, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        IconTile(iconName, iconTint, iconBg, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(name, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(meta, color = colors.text3, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(amount, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = if (isIncome) colors.green else colors.red)
            Text(account, color = colors.text3, fontSize = 11.sp)
        }
    }
}
```

- [ ] **Step 2: EmptyState + Skeleton + Toast (`States.kt`)**

```kotlin
package com.psy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psy.ui.icons.LucideIcon
import com.psy.ui.theme.LocalPsyColors

@Composable
fun EmptyState(iconName: String, title: String, caption: String, modifier: Modifier = Modifier) {
    val colors = LocalPsyColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(32.dp),
    ) {
        IconTile(iconName, colors.text3, colors.sunken, size = 56.dp)
        Text(title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(caption, color = colors.text3, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val colors = LocalPsyColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a",
    )
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(colors.sunken.copy(alpha = alpha)))
}
```
(Toast: use existing Snackbar host where present; a dedicated `PsyToast` can be a thin Snackbar wrapper — add when wiring screens in Phase 3.)

- [ ] **Step 3: Build** → BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/psy/ui/components/Cards.kt android/app/src/main/java/com/psy/ui/components/States.kt
git commit -m "feat(android): HeroCard, TransactionRow, EmptyState, Skeleton components"
```

---

## Self-Review Notes (addressed)
- **Spec coverage:** tokens (§3.1), fonts (§3.2), shapes (§3.3), accent remap + migration (§2.5), icon system + migration (§2.2–2.4), shared components (§4) — all have tasks. Screen re-skins are Phase 2 (separate plan).
- **Type consistency:** `PsyColors`, `LocalPsyColors`, `accentPrimary/accentSoft`, `PsyTypography`, `PsyShapes`, `LucideIcon`/`LucideIcons`, `IconPicker`, `IconTile`, `SegmentedControl`, `TransactionRow`, `HeroCard`, `EmptyState`, `Skeleton` — names used consistently across tasks.
- **Build-gated:** Tasks 3–7 are interdependent; the first green build is Task 7 Step 2 (expected).
