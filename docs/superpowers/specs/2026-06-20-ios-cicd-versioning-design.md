# iOS CI/CD + unified versioning — design

> Spec: add iOS CI + ad-hoc release to GitHub Actions, and make a single git tag `v*` drive the version of **both** Android and iOS at once.

## Goal
- `ios-ci`: build + test the iOS app on every PR / push touching `ios/**`.
- Unified release on tag `v*`: build a signed Android APK **and** an ad-hoc-signed iOS IPA, both versioned from the tag, both attached to one GitHub Release.
- One source of truth for the user-facing version: the git tag.

## Decisions (from brainstorming)
- iOS distribution: **ad-hoc IPA → GitHub Release** (paid Apple account; device UDIDs registered in the profile).
- Versioning: **derive from git tag `v*`** for both platforms.
- Release workflow: **single `release.yml`** (replaces `android-release.yml`) with `android` + `ios` jobs sharing one `github.run_number` → identical version *and* build number.

## Workflows

### `ios-ci.yml` (new) — no secrets
- Trigger: `push` to `main` + `pull_request`, `paths: ['ios/**', '.github/workflows/ios-ci.yml']`.
- Runner: `macos-15` (Xcode 16). Steps: select Xcode, `brew install xcodegen`, `swift test` in `ios/PsyCore`, `cd ios && xcodegen generate`, `xcodebuild test -scheme Psy -destination 'platform=iOS Simulator,name=iPhone 16'` (fallback to any available iPhone). GoogleSignIn SPM resolves over network.

### `release.yml` (new, replaces `android-release.yml`) — tag `v*`
- Trigger: `push.tags: ['v*']`. `permissions: contents: write`.
- Version (computed in each job, same run): `VERSION="${GITHUB_REF_NAME#v}"`, `BUILD="${{ github.run_number }}"`.
- Job `android` (ubuntu): restore keystore + `keystore.properties` (existing secrets), `./gradlew :app:assembleRelease` with env `VERSION_NAME=$VERSION VERSION_CODE=$BUILD`, upload `app-release.apk` to the `v*` Release.
- Job `ios` (macos-15): import dist cert (.p12) + ad-hoc profile into a temp keychain, `xcodegen generate`, `xcodebuild archive` (Release, manual signing, `MARKETING_VERSION=$VERSION CURRENT_PROJECT_VERSION=$BUILD`), `xcodebuild -exportArchive` with a generated `ExportOptions.plist` (method `release-testing`, manual signing, profile mapped by bundle id), upload `Psy.ipa` to the same Release. Cleanup keychain `if: always()`.

Both jobs use `softprops/action-gh-release@v2` with the tag → artifacts land on one Release.

## Version wiring
- **Android** `android/app/build.gradle.kts`:
  - `versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()`
  - `versionName = System.getenv("VERSION_NAME") ?: "1.0"`
  - Local builds keep `1 / 1.0`; CI release overrides from the tag.
- **iOS**: no project change — pass `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` as `xcodebuild` build settings (Info.plist already references `$(MARKETING_VERSION)` / `$(CURRENT_PROJECT_VERSION)`).

## iOS signing (manual, ad-hoc)
New GitHub secrets:
| Secret | Value |
|---|---|
| `IOS_DIST_CERT_P12_BASE64` | base64 of the Apple Distribution cert `.p12` |
| `IOS_DIST_CERT_PASSWORD` | password of the `.p12` |
| `IOS_PROVISIONING_PROFILE_BASE64` | base64 of the ad-hoc `.mobileprovision` for `com.hoalam.psy` |
| `IOS_TEAM_ID` | 10-char Apple Team ID |
| `IOS_KEYCHAIN_PASSWORD` | any ephemeral string for the temp keychain |

Workflow extracts the profile **name** from the installed profile (`security cms -D`) so `ExportOptions.plist` maps `com.hoalam.psy` → that name without hardcoding.

## Manual prerequisites (user, in Apple Developer portal)
1. Create/Export an **Apple Distribution** certificate → `.p12`.
2. Create an **Ad Hoc** provisioning profile for App ID `com.hoalam.psy`, with each test device's **UDID** registered.
3. Add the 5 secrets above. (Ad-hoc limitation: a new device ⇒ regenerate the profile + update `IOS_PROVISIONING_PROFILE_BASE64`. TestFlight would remove this constraint — future option.)

## Release procedure (both platforms at once)
```bash
git tag v1.2.0
git push origin v1.2.0
```
→ `release.yml` runs once → Android `1.2.0 (build N)` APK + iOS `1.2.0 (build N)` ad-hoc IPA → both attached to GitHub Release `v1.2.0`. No manual version edits.

## Docs
- `docs/CICD.md`: add `ios-ci` + `release` rows, the iOS secrets table, and the release procedure. Note `android-release.yml` is superseded by `release.yml`.
- `README.md`: update the Release section with the tag-based dual-platform flow.

## Out of scope
TestFlight/App Store submission, `fastlane match`, Xcode Cloud, automated UDID sync. Ad-hoc + GitHub Release only, per the chosen approach.

## Verification
- `ios-ci`: runs on GitHub (the `swift test` + `xcodebuild test` commands already pass locally).
- `release.yml`: validate YAML + logic by review; the iOS signing path is exercised on the first real tag (needs the user's secrets). Android job mirrors the proven `android-release.yml`.
- Android `build.gradle.kts` env change verified by a local `assembleDebug` (fallback path) — no behavior change without env.
