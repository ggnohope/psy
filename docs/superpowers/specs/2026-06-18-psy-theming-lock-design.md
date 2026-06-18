# Psy — Theming & Lock Design Spec

**Date:** 2026-06-18
**Status:** Approved (brainstorming)
**Author:** hoalam

## Overview

Add user-controlled theming (light/dark/system mode, multiple accent palettes, a rounded "cute" font)
and an app lock (PIN + biometric, locking on launch and on return from background) to Psy. Introduces
the first DataStore-backed settings persistence. Builds on the merged core/polish/stats/budget
(offline-first Kotlin/Compose/Room/Hilt/MVVM, `com.psy`, Candy Pop theme, Settings hub).

**No unit tests** (user preference). Verify by `./gradlew :app:assembleDebug :app:lintDebug` (0 errors)
+ manual emulator run.

## Scope (approved)

Theme: light/dark/system toggle; multiple accent palettes; rounded cute font (Quicksand via Google Fonts).
Lock: PIN (4–6 digits) + BiometricPrompt; locks on cold start AND on return from background after a short
timeout. Settings persisted via Preferences DataStore.

Deferred: per-widget theming; custom user colors; PIN recovery/email reset; auto-lock interval configuration UI.

## Settings persistence (DataStore)

`data/settings/SettingsRepository` (interface + impl), backed by Preferences DataStore (dep
`androidx.datastore:datastore-preferences` already present). A single `SettingsDataStore` (one
`DataStore<Preferences>` provided via Hilt, e.g. `Context.dataStore` by `preferencesDataStore("settings")`).

Keys + `SettingsState`:
- `themeMode: ThemeMode` (SYSTEM | LIGHT | DARK; stored as name; default SYSTEM)
- `accent: AccentPalette` (enum; default CANDY_VIOLET)
- `lockEnabled: Boolean` (default false)
- `pinHash: String?` (salted SHA-256; null if unset)
- `biometricEnabled: Boolean` (default false)

`SettingsRepository`: `val settings: Flow<SettingsState>`; suspend setters `setThemeMode`, `setAccent`,
`setLockEnabled`, `setPin(pin)` (computes+stores salted hash), `clearPin`, `setBiometricEnabled`,
`verifyPin(pin): Boolean`. Hilt @Binds the impl; provide the DataStore via a Hilt module.

## Theme

- `ThemeMode { SYSTEM, LIGHT, DARK }`. `AccentPalette { CANDY_VIOLET, CANDY_PINK, CANDY_MINT }` (3 palettes;
  each defines primary/secondary/tertiary + a gradient pair for cards). CANDY_VIOLET = current look.
- `ui/theme/Theme.kt`: `PsyTheme(themeMode, accent, content)` — `darkTheme = when(themeMode){ SYSTEM -> isSystemInDarkTheme(); LIGHT -> false; DARK -> true }`; build the light/dark `ColorScheme` from the
  selected `accent`'s color set. Keep `CandyShapes`. Expose the accent's gradient colors to screens via a
  small `LocalCandyGradient` CompositionLocal (so summary cards use the active accent's gradient) — or keep
  cards reading `colorScheme.primary`/`secondary` (simpler). Use the simpler colorScheme-driven approach;
  gradient cards derive from `primary`→`secondary`.
- **Rounded font**: add `androidx.compose.ui:ui-text-google-fonts`; define a `GoogleFont.Provider` (standard
  `com.google.android.gms` certs from `R.array.com_google_android_gms_fonts_certs`) and a `Quicksand`
  `FontFamily` via `GoogleFont("Quicksand")` with weights used in `CandyTypography`. Falls back to the
  system font automatically if the provider is unavailable.
- App root: `MainActivity` hosts an `AppRoot` composable that collects settings (via a root `@HiltViewModel`
  `AppViewModel` exposing `themeMode`, `accent`, and lock state) and wraps content in `PsyTheme(mode, accent)`.
  Theme/accent changes recompose immediately.

## App lock

- Lock state lives in `AppViewModel`: `isLocked: StateFlow<Boolean>`. On init, if `lockEnabled` → locked.
  Observe process lifecycle (`androidx.lifecycle.ProcessLifecycleOwner` or the Activity's lifecycle): on
  `ON_STOP` record `lastBackgroundedAt`; on `ON_START`, if `lockEnabled` and (never unlocked this process OR
  now - lastBackgroundedAt > ~2s) → set `isLocked = true`. `unlock()` sets it false.
- `AppRoot`: if `isLocked` → show `LockScreen` (full-screen, not part of NavHost, back does not bypass);
  else show `PsyNavHost`.
- `ui/lock/LockScreen.kt`: Candy-styled PIN entry — dots showing entered length, a numeric keypad
  (0–9 + delete), error shake/message on wrong PIN; if `biometricEnabled` a "Dùng vân tay/khuôn mặt" button
  triggering `BiometricPrompt`. On correct PIN or biometric success → `onUnlock()`.
- **PIN storage**: salted SHA-256. `SettingsRepository.setPin` generates/stores a random salt (or a fixed
  app salt + the pin) and the hash; `verifyPin` recomputes and compares. (Adequate for a learning app;
  not Keystore-grade.)
- **Biometric**: add `androidx.biometric:biometric`. BiometricPrompt requires a `FragmentActivity`, so
  change `MainActivity` from `ComponentActivity` to `androidx.fragment.app.FragmentActivity` (add
  `androidx.fragment:fragment-ktx` if needed; `@AndroidEntryPoint` works on FragmentActivity; Compose
  `setContent` works the same). A `BiometricAuthenticator` helper wraps the prompt and reports
  success/failure/unavailable.

## Settings sub-screens (in the Settings hub)

Add two rows to `SettingsScreen` (existing): "Giao diện" → `Routes.APPEARANCE`; "Khoá ứng dụng" → `Routes.LOCK_SETTINGS`.
- `ui/settings/AppearanceScreen.kt` + VM: choose ThemeMode (3 segmented/radio options) and AccentPalette
  (a row of palette swatches); changes call `SettingsRepository` and reflect immediately app-wide.
- `ui/settings/LockSettingsScreen.kt` + VM: a "Khoá ứng dụng" switch (lockEnabled). When enabling with no
  PIN → prompt to set a PIN (a set-PIN flow: enter + confirm). "Đổi PIN" action. A "Mở bằng vân tay/khuôn mặt"
  switch (biometricEnabled, only enableable if device has biometric enrolled). Disabling lock clears the gate
  (optionally clears PIN).

## Navigation

New routes: `APPEARANCE = "appearance"`, `LOCK_SETTINGS = "lockSettings"` (+ a set-PIN route or in-screen
flow). Both are pushed screens (no bottom bar). Added to `PsyNavHost`.

## Dependencies to add
- `androidx.biometric:biometric` (BiometricPrompt)
- `androidx.compose.ui:ui-text-google-fonts` (Quicksand downloadable font)
- `androidx.fragment:fragment-ktx` if needed for FragmentActivity (often already transitive)
(DataStore-preferences already present.)

## Error / Edge handling
- Lock screen cannot be dismissed by back (intercept back while locked).
- Biometric unavailable/not enrolled → hide/disable the biometric option; PIN always available as fallback.
- Wrong PIN → clear entry + error message (+ optional haptic); no lockout in v1.
- Enabling lock requires a PIN to be set first (guided flow); disabling lock unlocks immediately.
- Font provider unavailable (no Play Services) → Compose GoogleFont falls back to the system font; app still works.
- Theme/accent change persists and applies immediately on next recomposition.
- DB unaffected (settings are separate DataStore).

## Testing
No unit tests. Verification = build + lint green + manual emulator:
- Settings → Giao diện: switch Light/Dark/System and accent → whole app updates instantly; rounded font visible.
- Settings → Khoá ứng dụng: enable lock, set a PIN → background the app and reopen → LockScreen appears,
  correct PIN unlocks, wrong PIN errors; enable biometric (after enrolling a fingerprint in the emulator) →
  biometric prompt unlocks. Cold start while locked shows LockScreen.

## File Structure (new/changed)
```
data/settings/{SettingsRepository.kt, SettingsRepositoryImpl.kt, SettingsState.kt(ThemeMode, AccentPalette)} (new)
di/{SettingsModule.kt (DataStore + repo bind) or extend existing modules}
ui/theme/{Theme.kt(PsyTheme(mode,accent)), Color.kt(+accent palettes), Type.kt(Quicksand GoogleFont)}
ui/app/{AppRoot.kt, AppViewModel.kt} (new — theme + lock gate at root)  ; MainActivity.kt (→ FragmentActivity, host AppRoot)
ui/lock/{LockScreen.kt, BiometricAuthenticator.kt} (new)
ui/settings/{SettingsScreen.kt(+2 rows), AppearanceScreen.kt, AppearanceViewModel.kt, LockSettingsScreen.kt, LockSettingsViewModel.kt} (new/changed)
ui/navigation/{Routes.kt(+APPEARANCE, LOCK_SETTINGS), PsyNavHost.kt(+composables)}
res/font/ or font certs array; gradle/libs.versions.toml + app/build.gradle.kts (+biometric, +ui-text-google-fonts)
```
