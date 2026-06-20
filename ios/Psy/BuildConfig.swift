import Foundation

/// Runtime access to build-time configuration injected via xcconfig → Info.plist.
/// Mirrors Android's `R.string.base_url` (resValue) approach.
enum BuildConfig {
    static var baseURL: String {
        Bundle.main.object(forInfoDictionaryKey: "BASE_URL") as? String ?? ""
    }
}
