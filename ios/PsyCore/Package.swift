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
