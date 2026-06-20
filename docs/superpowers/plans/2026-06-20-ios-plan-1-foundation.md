# iOS Port — Plan 1: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold the `ios/` package: a CLI-testable pure-logic Swift package (`PsyCore`) with domain models + `Money`, plus an XcodeGen-generated SwiftUI app that launches with the Candy Pop theme and Quicksand font.

**Architecture:** Two-layer split mirroring Android's `domain/` (pure) vs app layers. `PsyCore` is a platform-agnostic Swift Package (value-type models, `Money`, later the pure stats/calendar/budget engines) — testable via `swift test` on the macOS host with only Command Line Tools. The Xcode app target (`Psy`) is SwiftUI/SwiftData and depends on `PsyCore`. This is the key reason logic is CLI-verifiable before full Xcode is installed.

**Tech Stack:** Swift 6, Swift Package Manager (PsyCore), XcodeGen, SwiftUI, Quicksand font.

**Reference (source of truth for the port):**
- `android/app/src/main/java/com/psy/domain/model/*.kt` — models
- `android/app/src/main/java/com/psy/domain/util/Money.kt` — money formatting
- `android/app/src/main/java/com/psy/ui/theme/{Color,Theme,Type,Shape}.kt` — theme
- `android/app/src/main/res/values/strings.xml` — `google_web_client_id`

**Prerequisites (manual, by user):** `brew install xcodegen`; install full Xcode for app-target build (Tasks 5–8 verification). Tasks 1–4 (`PsyCore`) verify with Command Line Tools only.

---

## File Structure

```
ios/
  PsyCore/                                  # CLI-testable Swift Package (pure logic)
    Package.swift
    Sources/PsyCore/
      Enums.swift                           # TxType, AccountType, CategoryType
      Models.swift                          # Ledger, Account, Category, Transaction, Budget
      Currency.swift                        # Currency (VND, USD)
      Money.swift                           # formatMinor (ported 1:1)
    Tests/PsyCoreTests/
      MoneyTests.swift
  project.yml                               # XcodeGen spec for the app
  Config/
    Debug.xcconfig                          # BASE_URL = http://localhost:8080/
    Release.xcconfig                        # BASE_URL = https://psy-backend.duckdns.org/
  Psy/
    PsyApp.swift                            # @main App + placeholder root view
    BuildConfig.swift                       # reads BASE_URL from Info.plist
    Info.plist
    UI/Theme/
      Color.swift                           # Candy palette + Color(argb:)
      Theme.swift                           # PsyTheme env, accent/mode → colors
      Shape.swift                           # corner radii
      Typography.swift                      # Quicksand font registration + styles
    Resources/
      Fonts/Quicksand.ttf                   # variable font (OFL), downloaded
      Assets.xcassets/                      # AppIcon placeholder, AccentColor
```

---

## Task 1: PsyCore package skeleton

**Files:**
- Create: `ios/PsyCore/Package.swift`
- Create: `ios/PsyCore/Sources/PsyCore/Enums.swift` (temporary placeholder, replaced in Task 2)

- [ ] **Step 1: Create the package manifest**

`ios/PsyCore/Package.swift`:
```swift
// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "PsyCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "PsyCore", targets: ["PsyCore"]),
    ],
    targets: [
        .target(name: "PsyCore"),
        .testTarget(name: "PsyCoreTests", dependencies: ["PsyCore"]),
    ]
)
```

- [ ] **Step 2: Add a placeholder source so the target compiles**

`ios/PsyCore/Sources/PsyCore/Enums.swift`:
```swift
// Domain enums. Raw values MUST match Kotlin enum .name for snapshot JSON compatibility.
public enum TxType: String, Codable, Sendable, CaseIterable {
    case income = "INCOME"
    case expense = "EXPENSE"
    case transfer = "TRANSFER"
}
```

- [ ] **Step 3: Verify it builds (CLT only)**

Run: `cd ios/PsyCore && swift build`
Expected: `Build complete!` (no errors).

- [ ] **Step 4: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Package.swift ios/PsyCore/Sources/PsyCore/Enums.swift
git commit -m "feat(ios): scaffold PsyCore swift package"
```

---

## Task 2: Domain enums and models

**Files:**
- Modify: `ios/PsyCore/Sources/PsyCore/Enums.swift`
- Create: `ios/PsyCore/Sources/PsyCore/Models.swift`

- [ ] **Step 1: Complete the enums**

Replace `ios/PsyCore/Sources/PsyCore/Enums.swift` with:
```swift
// Domain enums. Raw values MUST match Kotlin enum .name for snapshot JSON compatibility.
public enum TxType: String, Codable, Sendable, CaseIterable {
    case income = "INCOME"
    case expense = "EXPENSE"
    case transfer = "TRANSFER"
}

public enum AccountType: String, Codable, Sendable, CaseIterable {
    case cash = "CASH"
    case bank = "BANK"
    case credit = "CREDIT"
    case asset = "ASSET"
}

public enum CategoryType: String, Codable, Sendable, CaseIterable {
    case income = "INCOME"
    case expense = "EXPENSE"
}
```

- [ ] **Step 2: Create the value-type models**

`ios/PsyCore/Sources/PsyCore/Models.swift` (mirrors `domain/model/*.kt`; `color` is ARGB packed as in Android):
```swift
public struct Ledger: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var icon: String
    public var currency: String
    public var createdAt: Int64
    public init(id: Int64 = 0, name: String, icon: String, currency: String, createdAt: Int64) {
        self.id = id; self.name = name; self.icon = icon
        self.currency = currency; self.createdAt = createdAt
    }
}

public struct Account: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var type: AccountType
    public var icon: String
    public var color: Int64 // ARGB packed
    public init(id: Int64 = 0, name: String, type: AccountType, icon: String, color: Int64) {
        self.id = id; self.name = name; self.type = type; self.icon = icon; self.color = color
    }
}

public struct Category: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var name: String
    public var icon: String
    public var color: Int64 // ARGB packed
    public var type: CategoryType
    public var sortOrder: Int
    public init(id: Int64 = 0, name: String, icon: String, color: Int64, type: CategoryType, sortOrder: Int) {
        self.id = id; self.name = name; self.icon = icon
        self.color = color; self.type = type; self.sortOrder = sortOrder
    }
}

public struct Transaction: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var ledgerId: Int64
    public var type: TxType
    public var amountMinor: Int64
    public var categoryId: Int64?
    public var accountId: Int64
    public var toAccountId: Int64?
    public var note: String
    public var date: Int64        // epoch millis of the bill's day
    public var createdAt: Int64
    public var updatedAt: Int64
    public var photoUri: String?
    public init(id: Int64 = 0, ledgerId: Int64, type: TxType, amountMinor: Int64,
                categoryId: Int64? = nil, accountId: Int64, toAccountId: Int64? = nil,
                note: String, date: Int64, createdAt: Int64, updatedAt: Int64, photoUri: String? = nil) {
        self.id = id; self.ledgerId = ledgerId; self.type = type; self.amountMinor = amountMinor
        self.categoryId = categoryId; self.accountId = accountId; self.toAccountId = toAccountId
        self.note = note; self.date = date; self.createdAt = createdAt
        self.updatedAt = updatedAt; self.photoUri = photoUri
    }
}

public struct Budget: Identifiable, Hashable, Sendable {
    public var id: Int64
    public var ledgerId: Int64
    public var categoryId: Int64?
    public var amountMinor: Int64
    public init(id: Int64 = 0, ledgerId: Int64, categoryId: Int64?, amountMinor: Int64) {
        self.id = id; self.ledgerId = ledgerId; self.categoryId = categoryId; self.amountMinor = amountMinor
    }
}
```

- [ ] **Step 3: Verify build**

Run: `cd ios/PsyCore && swift build`
Expected: `Build complete!`

- [ ] **Step 4: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/Enums.swift ios/PsyCore/Sources/PsyCore/Models.swift
git commit -m "feat(ios): domain enums and value-type models in PsyCore"
```

---

## Task 3: Currency

**Files:**
- Create: `ios/PsyCore/Sources/PsyCore/Currency.swift`

- [ ] **Step 1: Port Currency (mirrors `domain/model/Currency.kt`)**

`ios/PsyCore/Sources/PsyCore/Currency.swift`:
```swift
/// Minimal currency metadata for formatting. v1 supports VND + USD.
public struct Currency: Hashable, Sendable {
    public let code: String
    public let symbol: String
    public let fractionDigits: Int

    public init(code: String, symbol: String, fractionDigits: Int) {
        self.code = code; self.symbol = symbol; self.fractionDigits = fractionDigits
    }

    public static let vnd = Currency(code: "VND", symbol: "đ", fractionDigits: 0)
    public static let usd = Currency(code: "USD", symbol: "$", fractionDigits: 2)

    public static func of(_ code: String) -> Currency {
        switch code {
        case "USD": return .usd
        default: return .vnd
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `cd ios/PsyCore && swift build`
Expected: `Build complete!`

- [ ] **Step 3: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/Currency.swift
git commit -m "feat(ios): Currency (VND, USD) in PsyCore"
```

---

## Task 4: Money formatting (TDD)

**Files:**
- Create: `ios/PsyCore/Tests/PsyCoreTests/MoneyTests.swift`
- Create: `ios/PsyCore/Sources/PsyCore/Money.swift`

- [ ] **Step 1: Write the failing tests**

`ios/PsyCore/Tests/PsyCoreTests/MoneyTests.swift` (cases mirror `Money.kt` semantics: integer arithmetic, US comma grouping, suffix after a space, sign preserved):
```swift
import XCTest
@testable import PsyCore

final class MoneyTests: XCTestCase {
    func testVndGrouping() {
        XCTAssertEqual(Money.formatMinor(1_234_567, fractionDigits: 0, suffix: "đ"), "1,234,567 đ")
    }
    func testVndZero() {
        XCTAssertEqual(Money.formatMinor(0, fractionDigits: 0, suffix: "đ"), "0 đ")
    }
    func testVndNegative() {
        XCTAssertEqual(Money.formatMinor(-5000, fractionDigits: 0, suffix: "đ"), "-5,000 đ")
    }
    func testUsdTwoDecimals() {
        XCTAssertEqual(Money.formatMinor(1234, fractionDigits: 2, suffix: "$"), "12.34 $")
    }
    func testUsdPadsFraction() {
        XCTAssertEqual(Money.formatMinor(5, fractionDigits: 2, suffix: "$"), "0.05 $")
    }
    func testUsdWholeKeepsTwoDecimals() {
        XCTAssertEqual(Money.formatMinor(100, fractionDigits: 2, suffix: "$"), "1.00 $")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd ios/PsyCore && swift test`
Expected: FAIL — `cannot find 'Money' in scope`.

- [ ] **Step 3: Implement Money (port of `Money.kt`)**

`ios/PsyCore/Sources/PsyCore/Money.swift`:
```swift
/// Formatting helpers for amounts stored as minor units (cents, đồng).
/// Integer arithmetic throughout so precision is never lost (amounts are Int64 minor units).
public enum Money {
    /// Renders a minor-unit amount as a grouped decimal string with a currency suffix.
    /// Always shows exactly `fractionDigits` decimal places.
    public static func formatMinor(_ amountMinor: Int64, fractionDigits: Int, suffix: String) -> String {
        var divisor: Int64 = 1
        for _ in 0..<fractionDigits { divisor *= 10 }

        let absAmount = amountMinor < 0 ? -amountMinor : amountMinor
        let whole = absAmount / divisor
        let frac = absAmount % divisor

        let groupedWhole = groupedDecimal(whole)
        let sign = amountMinor < 0 ? "-" : ""

        if fractionDigits > 0 {
            let fracStr = leftPad(String(frac), to: fractionDigits, with: "0")
            return "\(sign)\(groupedWhole).\(fracStr) \(suffix)"
        } else {
            return "\(sign)\(groupedWhole) \(suffix)"
        }
    }

    /// Groups a non-negative integer with commas every 3 digits (DecimalFormat "#,##0", US).
    private static func groupedDecimal(_ value: Int64) -> String {
        let digits = Array(String(value))
        var out = ""
        for (i, c) in digits.enumerated() {
            if i > 0 && (digits.count - i) % 3 == 0 { out.append(",") }
            out.append(c)
        }
        return out
    }

    private static func leftPad(_ s: String, to length: Int, with pad: Character) -> String {
        s.count >= length ? s : String(repeating: pad, count: length - s.count) + s
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd ios/PsyCore && swift test`
Expected: PASS — all 6 tests succeed.

- [ ] **Step 5: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/PsyCore/Sources/PsyCore/Money.swift ios/PsyCore/Tests/PsyCoreTests/MoneyTests.swift
git commit -m "feat(ios): Money.formatMinor with tests (ported from Money.kt)"
```

---

## Task 5: XcodeGen app project + config + placeholder app

**Files:**
- Create: `ios/project.yml`
- Create: `ios/Config/Debug.xcconfig`
- Create: `ios/Config/Release.xcconfig`
- Create: `ios/Psy/Info.plist`
- Create: `ios/Psy/PsyApp.swift`
- Create: `ios/Psy/BuildConfig.swift`
- Create: `ios/.gitignore`

- [ ] **Step 1: XcodeGen spec**

`ios/project.yml` (GoogleSignIn package is intentionally NOT here yet — Plan 5 adds it):
```yaml
name: Psy
options:
  bundleIdPrefix: com
  deploymentTarget:
    iOS: "17.0"
  createIntermediateGroups: true
packages:
  PsyCore:
    path: PsyCore
settings:
  base:
    SWIFT_VERSION: "6.0"
    MARKETING_VERSION: "1.0.0"
    CURRENT_PROJECT_VERSION: "1"
    DEVELOPMENT_TEAM: ""
    CODE_SIGN_STYLE: Automatic
targets:
  Psy:
    type: application
    platform: iOS
    sources:
      - Psy
    configFiles:
      Debug: Config/Debug.xcconfig
      Release: Config/Release.xcconfig
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.psy
        INFOPLIST_FILE: Psy/Info.plist
        GENERATE_INFOPLIST_FILE: NO
        TARGETED_DEVICE_FAMILY: "1,2"
        ASSETCATALOG_COMPILER_APPICON_NAME: AppIcon
    dependencies:
      - package: PsyCore
        product: PsyCore
  PsyTests:
    type: bundle.unit-test
    platform: iOS
    sources:
      - PsyTests
    dependencies:
      - target: Psy
schemes:
  Psy:
    build:
      targets:
        Psy: all
    run:
      config: Debug
    test:
      config: Debug
      targets:
        - PsyTests
```

- [ ] **Step 2: xcconfig files (note the `/$()/ ` trick to escape `//` from xcconfig comments)**

`ios/Config/Debug.xcconfig`:
```
// Simulator reaches the host machine directly via localhost (unlike Android emulator's 10.0.2.2).
BASE_URL = http:/$()/localhost:8080/
```

`ios/Config/Release.xcconfig`:
```
BASE_URL = https:/$()/psy-backend.duckdns.org/
```

- [ ] **Step 3: Info.plist (SwiftUI lifecycle; BASE_URL from xcconfig; localhost cleartext via NSAllowsLocalNetworking)**

`ios/Psy/Info.plist`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>vi</string>
    <key>CFBundleDisplayName</key>
    <string>Psy</string>
    <key>CFBundleExecutable</key>
    <string>$(EXECUTABLE_NAME)</string>
    <key>CFBundleIdentifier</key>
    <string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
    <key>CFBundleName</key>
    <string>$(PRODUCT_NAME)</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>$(MARKETING_VERSION)</string>
    <key>CFBundleVersion</key>
    <string>$(CURRENT_PROJECT_VERSION)</string>
    <key>BASE_URL</key>
    <string>$(BASE_URL)</string>
    <key>UILaunchScreen</key>
    <dict/>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
    </array>
    <key>NSAppTransportSecurity</key>
    <dict>
        <key>NSAllowsLocalNetworking</key>
        <true/>
    </dict>
</dict>
</plist>
```

- [ ] **Step 4: BuildConfig**

`ios/Psy/BuildConfig.swift`:
```swift
import Foundation

/// Runtime access to build-time configuration injected via xcconfig → Info.plist.
/// Mirrors Android's `R.string.base_url` (resValue) approach.
enum BuildConfig {
    static var baseURL: String {
        Bundle.main.object(forInfoDictionaryKey: "BASE_URL") as? String ?? ""
    }
}
```

- [ ] **Step 5: Placeholder app entry**

`ios/Psy/PsyApp.swift`:
```swift
import SwiftUI

@main
struct PsyApp: App {
    var body: some Scene {
        WindowGroup {
            PlaceholderRootView()
        }
    }
}

private struct PlaceholderRootView: View {
    var body: some View {
        VStack(spacing: 12) {
            Text("Psy 🐷")
                .font(.largeTitle.bold())
            Text(BuildConfig.baseURL)
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}
```

- [ ] **Step 6: gitignore for generated project**

`ios/.gitignore`:
```
Psy.xcodeproj/
.build/
DerivedData/
*.xcuserstate
xcuserdata/
```

- [ ] **Step 7: Generate and build (requires full Xcode + xcodegen)**

Run:
```bash
cd ios && xcodegen generate
xcodebuild -project Psy.xcodeproj -scheme Psy \
  -destination 'generic/platform=iOS Simulator' build
```
Expected: `** BUILD SUCCEEDED **`.

> If Xcode is not yet installed: skip the build, confirm `xcodegen generate` produces `ios/Psy.xcodeproj`, and mark the build sub-step pending until Xcode is available.

- [ ] **Step 8: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/project.yml ios/Config ios/Psy/Info.plist ios/Psy/PsyApp.swift ios/Psy/BuildConfig.swift ios/.gitignore
git commit -m "feat(ios): XcodeGen app project, xcconfig BASE_URL, placeholder app"
```

---

## Task 6: Candy Pop colors, theme, shapes

**Files:**
- Create: `ios/Psy/UI/Theme/Color.swift`
- Create: `ios/Psy/UI/Theme/Shape.swift`
- Create: `ios/Psy/UI/Theme/Theme.swift`

- [ ] **Step 1: Colors + ARGB helper (port of `Color.kt`)**

`ios/Psy/UI/Theme/Color.swift`:
```swift
import SwiftUI

extension Color {
    /// Build a Color from an ARGB-packed Int64 (0xAARRGGBB), matching Android's packed Long colors.
    init(argb: Int64) {
        let a = Double((argb >> 24) & 0xFF) / 255.0
        let r = Double((argb >> 16) & 0xFF) / 255.0
        let g = Double((argb >> 8) & 0xFF) / 255.0
        let b = Double(argb & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}

enum CandyColor {
    static let violet     = Color(argb: 0xFFA18CFF)
    static let sky        = Color(argb: 0xFF7FD8FF)
    static let pink       = Color(argb: 0xFFFF8FD6)
    static let pinkDeep   = Color(argb: 0xFFFF5FA2)
    static let green      = Color(argb: 0xFF22C55E)

    static let surfaceLight   = Color(argb: 0xFFF4F0FF)
    static let onSurfaceLight  = Color(argb: 0xFF2B2640)
    static let surfaceDark     = Color(argb: 0xFF1C1830)
    static let onSurfaceDark   = Color(argb: 0xFFEDE9FF)
}

/// Accent color triplet (primary, secondary, tertiary) — mirrors AccentColors.
struct AccentColors {
    let primary: Color
    let secondary: Color
    let tertiary: Color
}

enum AccentPalette: String, CaseIterable, Codable {
    case candyViolet = "CANDY_VIOLET"
    case candyPink   = "CANDY_PINK"
    case candyMint   = "CANDY_MINT"

    var colors: AccentColors {
        switch self {
        case .candyViolet: AccentColors(primary: CandyColor.violet, secondary: CandyColor.sky, tertiary: CandyColor.pink)
        case .candyPink:   AccentColors(primary: CandyColor.pink, secondary: CandyColor.pinkDeep, tertiary: CandyColor.violet)
        case .candyMint:   AccentColors(primary: CandyColor.green, secondary: CandyColor.sky, tertiary: CandyColor.violet)
        }
    }
}

enum ThemeMode: String, CaseIterable, Codable {
    case system = "SYSTEM"
    case light  = "LIGHT"
    case dark   = "DARK"
}
```

- [ ] **Step 2: Shapes (port of `Shape.kt` — large candy radii)**

`ios/Psy/UI/Theme/Shape.swift`:
```swift
import SwiftUI

enum CandyShape {
    static let small: CGFloat = 12
    static let medium: CGFloat = 18
    static let large: CGFloat = 28
}
```

- [ ] **Step 3: Theme environment (resolves accent + mode into a usable palette)**

`ios/Psy/UI/Theme/Theme.swift`:
```swift
import SwiftUI

/// Resolved theme colors for the current accent + light/dark, exposed via Environment.
/// Mirrors Compose's MaterialTheme color scheme so views read semantic colors, not raw ones.
struct PsyColors {
    let primary: Color
    let secondary: Color
    let tertiary: Color
    let surface: Color
    let onSurface: Color
    let background: Color
    let onBackground: Color

    static func resolve(accent: AccentPalette, dark: Bool) -> PsyColors {
        let a = accent.colors
        if dark {
            return PsyColors(
                primary: a.primary, secondary: a.secondary, tertiary: a.tertiary,
                surface: CandyColor.surfaceDark, onSurface: CandyColor.onSurfaceDark,
                background: CandyColor.surfaceDark, onBackground: CandyColor.onSurfaceDark
            )
        } else {
            return PsyColors(
                primary: a.primary, secondary: a.secondary, tertiary: a.tertiary,
                surface: CandyColor.surfaceLight, onSurface: CandyColor.onSurfaceLight,
                background: CandyColor.surfaceLight, onBackground: CandyColor.onSurfaceLight
            )
        }
    }
}

private struct PsyColorsKey: EnvironmentKey {
    static let defaultValue = PsyColors.resolve(accent: .candyViolet, dark: false)
}

extension EnvironmentValues {
    var psyColors: PsyColors {
        get { self[PsyColorsKey.self] }
        set { self[PsyColorsKey.self] = newValue }
    }
}

/// Applies the Candy Pop theme: resolves colors for the chosen accent/mode, injects them
/// into the environment, and tints the SwiftUI accent. Wrap the app root in this.
struct PsyTheme: ViewModifier {
    let mode: ThemeMode
    let accent: AccentPalette
    @Environment(\.colorScheme) private var systemScheme

    private var isDark: Bool {
        switch mode {
        case .system: systemScheme == .dark
        case .light:  false
        case .dark:   true
        }
    }

    func body(content: Content) -> some View {
        let colors = PsyColors.resolve(accent: accent, dark: isDark)
        content
            .environment(\.psyColors, colors)
            .tint(colors.primary)
            .preferredColorScheme(mode == .system ? nil : (isDark ? .dark : .light))
            .background(colors.background.ignoresSafeArea())
    }
}

extension View {
    func psyTheme(mode: ThemeMode, accent: AccentPalette) -> some View {
        modifier(PsyTheme(mode: mode, accent: accent))
    }
}
```

- [ ] **Step 4: Verify build (requires Xcode) — or visual review if Xcode pending**

Run:
```bash
cd ios && xcodegen generate && xcodebuild -project Psy.xcodeproj -scheme Psy \
  -destination 'generic/platform=iOS Simulator' build
```
Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 5: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/UI/Theme
git commit -m "feat(ios): Candy Pop colors, shapes, theme environment"
```

---

## Task 7: Quicksand font + typography

**Files:**
- Create: `ios/Psy/Resources/Fonts/Quicksand.ttf` (downloaded)
- Create: `ios/Psy/UI/Theme/Typography.swift`
- Modify: `ios/Psy/Info.plist` (add `UIAppFonts`)
- Modify: `ios/project.yml` (ensure `Resources` is in sources — already covered by `sources: [Psy]`, no change needed; verify font is bundled)

- [ ] **Step 1: Download the Quicksand variable font (OFL, safe to commit)**

Run:
```bash
mkdir -p ios/Psy/Resources/Fonts
curl -L -o ios/Psy/Resources/Fonts/Quicksand.ttf \
  "https://github.com/google/fonts/raw/main/ofl/quicksand/Quicksand%5Bwght%5D.ttf"
# verify it is a real font file (non-trivial size, TrueType magic)
ls -l ios/Psy/Resources/Fonts/Quicksand.ttf
file ios/Psy/Resources/Fonts/Quicksand.ttf
```
Expected: file size > 50 KB; `file` reports `TrueType Font` / `OpenType`.

- [ ] **Step 2: Register the font in Info.plist**

In `ios/Psy/Info.plist`, add this key/value inside the top-level `<dict>` (e.g. after the `BASE_URL` entry):
```xml
    <key>UIAppFonts</key>
    <array>
        <string>Quicksand.ttf</string>
    </array>
```

- [ ] **Step 3: Typography styles (port of `Type.kt`; family name "Quicksand")**

`ios/Psy/UI/Theme/Typography.swift`:
```swift
import SwiftUI

/// Quicksand typography mirroring CandyTypography in Type.kt.
/// Weights come from the variable font; sizes/weights match the Android styles.
enum PsyFont {
    private static let family = "Quicksand"

    static let headlineMedium = Font.custom(family, size: 24).weight(.heavy)   // ExtraBold
    static let titleMedium    = Font.custom(family, size: 16).weight(.bold)
    static let bodyMedium     = Font.custom(family, size: 14).weight(.medium)
    static let labelSmall     = Font.custom(family, size: 11).weight(.semibold)
}
```

- [ ] **Step 4: Use the font in the placeholder to confirm it loads**

Replace the body of `PlaceholderRootView` in `ios/Psy/PsyApp.swift`:
```swift
private struct PlaceholderRootView: View {
    @Environment(\.psyColors) private var colors
    var body: some View {
        VStack(spacing: 12) {
            Text("Psy 🐷").font(PsyFont.headlineMedium)
            Text(BuildConfig.baseURL)
                .font(PsyFont.labelSmall)
                .foregroundStyle(colors.onSurface.opacity(0.6))
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(colors.background)
    }
}
```
And wrap it with the theme in `PsyApp.body`:
```swift
        WindowGroup {
            PlaceholderRootView()
                .psyTheme(mode: .system, accent: .candyViolet)
        }
```

- [ ] **Step 5: Build and run on a simulator (requires Xcode) — confirm Quicksand renders**

Run:
```bash
cd ios && xcodegen generate
xcrun simctl list devices available | grep "iPhone 16" | head -1   # pick an available device
xcodebuild -project Psy.xcodeproj -scheme Psy \
  -destination 'platform=iOS Simulator,name=iPhone 16' build
```
Expected: `** BUILD SUCCEEDED **`. Then boot + install + launch to eyeball the font:
```bash
xcrun simctl boot "iPhone 16" || true
xcrun simctl install booted "$(xcodebuild -project ios/Psy.xcodeproj -scheme Psy -showBuildSettings -destination 'platform=iOS Simulator,name=iPhone 16' | awk -F' = ' '/ TARGET_BUILD_DIR /{d=$2} / FULL_PRODUCT_NAME /{p=$2} END{print d"/"p}')"
xcrun simctl launch booted com.psy
```
Expected: app launches showing "Psy 🐷" in rounded Quicksand + the base URL.

> If Xcode pending: confirm font downloaded and Info.plist/Typography compile-ready; defer simulator launch.

- [ ] **Step 6: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Resources/Fonts/Quicksand.ttf ios/Psy/UI/Theme/Typography.swift ios/Psy/Info.plist ios/Psy/PsyApp.swift
git commit -m "feat(ios): bundle Quicksand font + typography styles"
```

---

## Task 8: Asset catalog (app icon + accent placeholder)

**Files:**
- Create: `ios/Psy/Resources/Assets.xcassets/Contents.json`
- Create: `ios/Psy/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json`
- Create: `ios/Psy/Resources/Assets.xcassets/AccentColor.colorset/Contents.json`

- [ ] **Step 1: Asset catalog root**

`ios/Psy/Resources/Assets.xcassets/Contents.json`:
```json
{ "info" : { "author" : "xcode", "version" : 1 } }
```

- [ ] **Step 2: App icon set (single 1024 slot; artwork added later from `design/`)**

`ios/Psy/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json`:
```json
{
  "images" : [
    { "idiom" : "universal", "platform" : "ios", "size" : "1024x1024" }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
```

- [ ] **Step 3: Accent color (Candy Violet)**

`ios/Psy/Resources/Assets.xcassets/AccentColor.colorset/Contents.json`:
```json
{
  "colors" : [
    {
      "color" : {
        "color-space" : "srgb",
        "components" : { "alpha" : "1.000", "blue" : "1.000", "green" : "0.549", "red" : "0.631" }
      },
      "idiom" : "universal"
    }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
```

- [ ] **Step 4: Verify build (requires Xcode)**

Run:
```bash
cd ios && xcodegen generate && xcodebuild -project Psy.xcodeproj -scheme Psy \
  -destination 'generic/platform=iOS Simulator' build
```
Expected: `** BUILD SUCCEEDED **` with no asset-catalog warnings about AppIcon.

- [ ] **Step 5: Commit**

```bash
cd /Users/hoalam/Codes/psy
git add ios/Psy/Resources/Assets.xcassets
git commit -m "feat(ios): asset catalog with AppIcon slot and Candy accent color"
```

---

## Self-Review (completed during authoring)

**Spec coverage (Plan 1 portion of the spec):**
- §1 tech mapping → Tasks 1–8 establish SwiftUI + SwiftData-ready project, PsyCore split, Combine deferred to Plan 2/4.
- §2 directory structure → matches File Structure above (Domain, UI/Theme, Config, Resources). Data/Features deferred to Plans 2/4/5.
- §3 domain model + Money + Currency + Int64 ids + ARGB color → Tasks 2,3,4,6.
- §5 theme + Quicksand → Tasks 6,7.
- §6 BASE_URL via xcconfig→Info.plist + localhost ATS → Task 5.
- §8 verification (swift test for logic; xcodebuild for app) → Tasks 4 (CLT) and 5–8 (Xcode).

**Deferred to later plans (intentional, not gaps):** SwiftData entities/repositories/seed/SnapshotDTO (Plan 2); backend multi-audience (Plan 3); all feature screens + ViewModels + charts (Plan 4); auth/Keychain/GoogleSignIn/lock/sync/app gate (Plan 5).

**Placeholder scan:** none — every step has concrete content/commands.

**Type consistency:** `AccentPalette`/`ThemeMode` raw strings ("CANDY_VIOLET" etc.) match Android `SettingsState` enum names, so Plan 5 settings persistence stays compatible. `Color(argb:)` signature used consistently. `PsyColors.resolve(accent:dark:)` and `psyTheme(mode:accent:)` names are consistent across Tasks 6–7.

---

## Execution Handoff

Plan 1 complete. After executing it, I'll write **Plan 2 (Persistence & repositories)** against the landed foundation.
