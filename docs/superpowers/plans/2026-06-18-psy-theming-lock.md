# Psy Theming & Lock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.
> **NO UNIT TESTS** (user preference). Verify each task by `./gradlew :app:assembleDebug`; final gate adds lint + manual emulator. Spec: `docs/superpowers/specs/2026-06-18-psy-theming-lock-design.md`.

**Goal:** User-controlled theme (light/dark/system + accent palettes + rounded Quicksand font) and an app lock (PIN + biometric, gating on launch and return-from-background), persisted via DataStore.

**Environment for every gradle command:** `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` then `cd /Users/hoalam/Codes/psy/android && ./gradlew ...`

**Tech:** Kotlin, Compose Material3, Hilt, Preferences DataStore (present), androidx.biometric, ui-text-google-fonts. Candy Pop.

---

## Task 1: Settings DataStore + SettingsRepository

**Files:**
- `data/settings/SettingsState.kt` (new): `enum class ThemeMode { SYSTEM, LIGHT, DARK }`, `enum class AccentPalette { CANDY_VIOLET, CANDY_PINK, CANDY_MINT }`, `data class SettingsState(themeMode = SYSTEM, accent = CANDY_VIOLET, lockEnabled = false, pinHash: String? = null, biometricEnabled = false)`.
- `data/settings/SettingsRepository.kt` (new, interface): `val settings: Flow<SettingsState>`; `suspend fun setThemeMode(ThemeMode)`, `setAccent(AccentPalette)`, `setLockEnabled(Boolean)`, `setPin(pin: String)`, `clearPin()`, `setBiometricEnabled(Boolean)`, `verifyPin(pin: String): Boolean`.
- `data/settings/SettingsRepositoryImpl.kt` (new): `@Inject constructor(private val dataStore: DataStore<Preferences>)`. Define preference keys (stringPreferencesKey/booleanPreferencesKey). `settings` = `dataStore.data.map { prefs -> SettingsState(themeMode = ThemeMode.valueOf(prefs[KEY_MODE] ?: "SYSTEM"), accent = AccentPalette.valueOf(prefs[KEY_ACCENT] ?: "CANDY_VIOLET"), lockEnabled = prefs[KEY_LOCK] ?: false, pinHash = prefs[KEY_PIN_HASH], biometricEnabled = prefs[KEY_BIO] ?: false) }` (guard valueOf with runCatching → default). Setters `dataStore.edit { it[KEY]=... }`. `setPin`: compute salted SHA-256 — `hash = sha256("psy_salt:" + pin)` (hex string) — store in KEY_PIN_HASH. `verifyPin`: read current hash from `settings.first()` and compare to `sha256(...)`. `clearPin`: remove KEY_PIN_HASH. Provide a private `sha256(String): String` using `java.security.MessageDigest`.
- `di/SettingsModule.kt` (new): provide `DataStore<Preferences>` — top-level `private val Context.settingsDataStore by preferencesDataStore("settings")`; `@Provides @Singleton fun provideDataStore(@ApplicationContext c: Context) = c.settingsDataStore`. `@Binds @Singleton` SettingsRepositoryImpl → SettingsRepository (a separate `@Module abstract class` or add the @Binds to RepositoryModule; keep DataStore @Provides in an `object` module).

- [ ] Step 1: Implement; ensure `androidx.datastore:datastore-preferences` import resolves (already a dep).
- [ ] Step 2: `./gradlew :app:assembleDebug` + `:app:testDebugUnitTest` → green.
- [ ] Step 3: Commit `feat(data): settings DataStore + repository (theme, accent, lock prefs)`.

---

## Task 2: Theme — accent palettes + PsyTheme(mode, accent) + Quicksand font

**Files:**
- `gradle/libs.versions.toml` + `app/build.gradle.kts`: add `androidx.compose.ui:ui-text-google-fonts` (BOM-managed, version-less like the other compose libs).
- `ui/theme/Color.kt`: keep existing Candy colors; add color sets for the 3 accents (each: primary, secondary, tertiary, and a gradient pair). E.g. CANDY_VIOLET (CandyViolet/CandySky/CandyPink — current), CANDY_PINK (CandyPink/CandyPinkDeep/CandyViolet), CANDY_MINT (CandyGreen/CandySky/CandyViolet). Define as simple data or when-mapped values.
- `ui/theme/Type.kt`: add a Quicksand `FontFamily` via Google Fonts: a `GoogleFont.Provider` (authority `"com.google.android.gms.fonts"`, package `"com.google.android.gms"`, certs `R.array.com_google_android_gms_fonts_certs` — this cert array ships with `ui-text-google-fonts`/play-services; if the resource isn't resolvable, fall back to FontFamily.Default and note it). `val Quicksand = FontFamily(Font(GoogleFont("Quicksand"), provider, FontWeight.Medium), ... Bold, ExtraBold, SemiBold)`. `CandyTypography` uses `Quicksand` for its text styles.
- `ui/theme/Theme.kt`: change `PsyTheme` to `@Composable fun PsyTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, accent: AccentPalette = AccentPalette.CANDY_VIOLET, content: ...)`. `val dark = when(themeMode){ SYSTEM->isSystemInDarkTheme(); LIGHT->false; DARK->true }`. Build light/dark `ColorScheme` from the `accent`'s color set (helper `fun accentColors(accent, dark): ColorScheme`). Keep CandyTypography + CandyShapes. (Screens already read `colorScheme.primary/secondary/surface`; the existing `CandyViolet`/`CandySky` constant references in screens that hardcode gradient — keep them OR have them read colorScheme; for v1 it's acceptable that summary-card gradients still use the constant CandyViolet→CandySky. Do NOT refactor all screens; just make PsyTheme drive Material colors. Note any hardcoded-color screens in the report.)

- [ ] Step 1: Implement (deps, palettes, font, PsyTheme signature). Note: callers of `PsyTheme {}` (MainActivity) currently call it with no args — after this change it still has defaults so it compiles; Task 3 wires the real mode/accent.
- [ ] Step 2: `./gradlew :app:assembleDebug` → green (Quicksand resolves or falls back). If the certs array `com_google_android_gms_fonts_certs` is unresolved, add the standard certs (the `ui-text-google-fonts` artifact provides it via play-services-basement; if not present, fall back to FontFamily.Default and report).
- [ ] Step 3: Commit `feat(ui): accent palettes, theme mode, Quicksand font`.

---

## Task 3: App root (theme + lock gate) + MainActivity → FragmentActivity

**Files:**
- `app/build.gradle.kts` + catalog: add `androidx.biometric:biometric` (stable, e.g. 1.1.0) and `androidx.fragment:fragment-ktx` if not transitively present; add `androidx.lifecycle:lifecycle-process` for ProcessLifecycleOwner (or use the activity lifecycle).
- `ui/app/AppViewModel.kt` (new): `@HiltViewModel`, inject SettingsRepository. Expose `val settings: StateFlow<SettingsState>` (stateIn). Lock gate: `private val _unlockedThisProcess`, `private var lastBackgroundedAt`; `val isLocked: StateFlow<Boolean>`. Methods: `onAppStart()` (called from lifecycle ON_START: if settings.lockEnabled && (!unlockedThisProcess || elapsed>2000ms) → lock), `onAppStop()` (record time), `unlock()` (set unlocked). Combine settings into the locked decision. Keep it simple: a MutableStateFlow<Boolean> isLocked initialized from first settings (lockEnabled).
- `ui/app/AppRoot.kt` (new): `@Composable fun AppRoot()` — `val vm: AppViewModel = hiltViewModel(); val settings by vm.settings.collectAsStateWithLifecycle(); val locked by vm.isLocked.collectAsStateWithLifecycle()`. Observe lifecycle via `DisposableEffect(LocalLifecycleOwner.current)` registering a `LifecycleEventObserver` → vm.onAppStart/onAppStop. Wrap in `PsyTheme(settings.themeMode, settings.accent) { if (locked) LockScreen(onUnlock = vm::unlock, biometricEnabled = settings.biometricEnabled, verifyPin = { vm.verifyPin(it) }) else PsyNavHost() }`. (Expose `verifyPin` on the VM delegating to repo.)
- `MainActivity.kt`: change base class `ComponentActivity` → `androidx.fragment.app.FragmentActivity`; keep `@AndroidEntryPoint`; `setContent { AppRoot() }`. (FragmentActivity is required for BiometricPrompt later.)

- [ ] Step 1: Implement deps + AppViewModel + AppRoot + MainActivity change. LockScreen may be a temporary stub `@Composable fun LockScreen(onUnlock, biometricEnabled, verifyPin)` (Task 5 builds the real UI) — but include the signature now so AppRoot compiles; a minimal stub that just shows a button calling onUnlock is fine for this task.
- [ ] Step 2: `./gradlew :app:assembleDebug` → green; run app — theme follows settings (default SYSTEM/VIOLET); with lockEnabled=false the stub never shows.
- [ ] Step 3: Commit `feat(ui): app root with theme wiring + lock gate scaffold; MainActivity → FragmentActivity`.

---

## Task 4: Appearance settings screen

**Files:**
- `ui/navigation/Routes.kt`: add `APPEARANCE = "appearance"`.
- `ui/settings/SettingsScreen.kt`: add a row "Giao diện" → onAppearance; (keep the existing manage rows). Update `PsyNavHost` to pass `onAppearance = { nav.navigate(Routes.APPEARANCE) }` and add `composable(Routes.APPEARANCE){ AppearanceScreen(onBack = { nav.popBackStack() }) }`.
- `ui/settings/AppearanceViewModel.kt` (new): `@HiltViewModel`, inject SettingsRepository. Expose settings; `setThemeMode(mode)`, `setAccent(accent)` (delegate to repo).
- `ui/settings/AppearanceScreen.kt` (new): Scaffold + TopAppBar("Giao diện", back). Section "Chế độ": 3 options (Theo hệ thống / Sáng / Tối) as a segmented control or radio rows → setThemeMode. Section "Màu chủ đạo": a Row of accent swatches (CANDY_VIOLET/PINK/MINT) each a circle of that accent's primary, selected ringed → setAccent. Changes apply immediately (the root recomposes). collectAsStateWithLifecycle.

- [ ] Step 1: Implement.
- [ ] Step 2: `./gradlew :app:assembleDebug` → green.
- [ ] Step 3: Commit `feat(ui): appearance settings (theme mode + accent)`.

---

## Task 5: App lock — LockScreen, biometric, lock settings

**Files:**
- `ui/lock/BiometricAuthenticator.kt` (new): a helper `fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit)` using `androidx.biometric.BiometricPrompt` + `BiometricManager` (check `canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL?)` — use BIOMETRIC_STRONG/WEAK). `fun isAvailable(context): Boolean`.
- `ui/lock/LockScreen.kt` (REPLACE the Task 3 stub): `@Composable fun LockScreen(onUnlock: () -> Unit, biometricEnabled: Boolean, verifyPin: suspend (String) -> Boolean)`. Candy-styled: app icon/title, a row of PIN dots (filled per entered digit), a numeric keypad (1–9, 0, ⌫) building a PIN string (4–6 digits; auto-submit at 6 or on a confirm). On submit → `verifyPin(pin)` (in a coroutine via rememberCoroutineScope) → success `onUnlock()` else clear + show error (shake/haptic). If `biometricEnabled` show a "Vân tay / Khuôn mặt" button → get the `LocalContext` as FragmentActivity → BiometricAuthenticator.authenticate(... onSuccess = onUnlock). Intercept system back while locked: `BackHandler(enabled = true) { /* no-op */ }`.
- `ui/navigation/Routes.kt`: add `LOCK_SETTINGS = "lockSettings"`. `SettingsScreen`: add row "Khoá ứng dụng" → onLockSettings. `PsyNavHost`: add `composable(LOCK_SETTINGS){ LockSettingsScreen(onBack) }`.
- `ui/settings/LockSettingsViewModel.kt` (new): inject SettingsRepository. Expose settings; `enableLock()`/`disableLock()`, `setPin(pin)`, `setBiometricEnabled(bool)`. Editor state for the set-PIN flow (enter + confirm). `isBiometricAvailable` (from BiometricAuthenticator.isAvailable).
- `ui/settings/LockSettingsScreen.kt` (new): Scaffold + TopAppBar("Khoá ứng dụng", back). A switch "Khoá ứng dụng" (lockEnabled) — turning ON with no pinHash opens a set-PIN dialog (enter PIN + confirm PIN, must match, ≥4 digits) → setPin + setLockEnabled(true). "Đổi PIN" row (when lock on) → set-PIN dialog. A switch "Mở bằng vân tay/khuôn mặt" (biometricEnabled) — disabled if !isBiometricAvailable or lock off. Turning lock OFF → disableLock (clearPin optional). collectAsStateWithLifecycle.

- [ ] Step 1: Implement biometric helper, LockScreen, lock settings, routes/rows. Ensure `AppRoot`/`AppViewModel` already passes `biometricEnabled` + `verifyPin` to LockScreen (from Task 3).
- [ ] Step 2: `./gradlew :app:assembleDebug` → green.
- [ ] Step 3: Commit `feat(ui): app lock (PIN keypad + biometric) and lock settings`.

---

## Task 6: Verification gate (build + lint + manual)

- [ ] Step 1: `./gradlew :app:assembleDebug :app:lintDebug` → SUCCESSFUL, 0 lint errors.
- [ ] Step 2: Manual run on Pixel_10_Pro:
  1. Settings (⚙️) → Giao diện: switch Sáng/Tối/Hệ thống → whole app updates instantly; pick a different accent → primary colors change; the rounded Quicksand font is visible (or default fallback if no Play Services).
  2. Settings → Khoá ứng dụng: toggle on → set a PIN (enter + confirm). Background the app (Home) and reopen → LockScreen appears; wrong PIN errors; correct PIN unlocks. Cold-kill & reopen → LockScreen shows.
  3. Enroll a fingerprint in the emulator (Settings → Security) → enable "Mở bằng vân tay", reopen locked app → biometric prompt unlocks. (If biometric not enrolled, the option is disabled — acceptable.)
  4. Disable lock → app opens without LockScreen.
- [ ] Step 3: `git --no-pager log --oneline && git status -s` clean.

---

## Self-Review Notes
- Spec coverage: DataStore settings (Task 1); theme mode/accent/font (Task 2); app-root theme wiring + lock gate + FragmentActivity (Task 3); appearance screen (Task 4); lock screen + biometric + lock settings (Task 5). All covered.
- No tests; verification = compile + lint + manual.
- Type consistency: `SettingsState`/`ThemeMode`/`AccentPalette` defined in Task 1 used by Theme (Task 2), AppViewModel/AppRoot (Task 3), Appearance/Lock VMs (Tasks 4/5); `PsyTheme(themeMode, accent)` signature set in Task 2, called in Task 3; `LockScreen(onUnlock, biometricEnabled, verifyPin)` stub in Task 3 → real in Task 5; `Routes.APPEARANCE/LOCK_SETTINGS` shared by SettingsScreen + NavHost.
- Risk/contingency notes baked in: Quicksand GoogleFont certs may need fallback; FragmentActivity required for BiometricPrompt; biometric availability gates the option.
