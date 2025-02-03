// swift-tools-version:5.7
import PackageDescription

let package = Package(
    name: "ComposeFauthUi",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "ComposeFauthUi",
            targets: ["common"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "common",
            path: "./common.xcframework"
        ),
    ]
)
