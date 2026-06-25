import Foundation
import CryptoKit

@MainActor @Observable
final class SettingsStore {
    private let d = UserDefaults.standard
    private enum K {
        static let mode = "theme_mode", accent = "accent_palette", lock = "lock_enabled"
        static let pin = "pin_hash", bio = "biometric_enabled"
    }

    var themeMode: ThemeMode { didSet { d.set(themeMode.rawValue, forKey: K.mode) } }
    var accent: AccentPalette { didSet { d.set(accent.rawValue, forKey: K.accent) } }
    var lockEnabled: Bool { didSet { d.set(lockEnabled, forKey: K.lock) } }
    var biometricEnabled: Bool { didSet { d.set(biometricEnabled, forKey: K.bio) } }
    private(set) var pinHash: String?

    init() {
        themeMode = ThemeMode(rawValue: d.string(forKey: K.mode) ?? "") ?? .system
        accent = Self.parseAccent(d.string(forKey: K.accent))
        lockEnabled = d.bool(forKey: K.lock)
        biometricEnabled = d.bool(forKey: K.bio)
        pinHash = d.string(forKey: K.pin)
    }

    func setPin(_ pin: String) { pinHash = Self.hash(pin); d.set(pinHash, forKey: K.pin) }
    func clearPin() { pinHash = nil; d.removeObject(forKey: K.pin) }
    func verifyPin(_ pin: String) -> Bool { pinHash != nil && pinHash == Self.hash(pin) }

    /// Tolerant parse: migrate legacy candy accent values to the HostGuardIQ palette.
    private static func parseAccent(_ raw: String?) -> AccentPalette {
        switch raw {
        case "BLUE", "AMBER", "TEAL": return AccentPalette(rawValue: raw!)!
        case "CANDY_PINK": return .amber   // closest warm hue
        case "CANDY_MINT": return .teal
        default: return .blue              // CANDY_VIOLET, nil, or unknown
        }
    }

    private static func hash(_ pin: String) -> String {
        let digest = SHA256.hash(data: Data("psy_salt:\(pin)".utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
