import Foundation

/// Stores attached photos in the app's Documents/photos directory. Returns the absolute file path
/// (mirrors Android's internal-storage path stored in Transaction.photoUri).
enum PhotoStorage {
    private static var dir: URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let d = docs.appendingPathComponent("photos", isDirectory: true)
        try? FileManager.default.createDirectory(at: d, withIntermediateDirectories: true)
        return d
    }

    static func save(data: Data, name: String) throws -> String {
        let url = dir.appendingPathComponent("\(name).jpg")
        try data.write(to: url)
        return url.path
    }

    static func delete(path: String) {
        try? FileManager.default.removeItem(atPath: path)
    }
}
