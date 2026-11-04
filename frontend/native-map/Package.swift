// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "PlugpassNativeMap",
    platforms: [.iOS(.v15)],
    products: [.library(name: "PlugpassNativeMap", targets: ["PlugpassNativeMap"])],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", exact: "8.5.3"),
        .package(url: "https://github.com/kakao-mapsSDK/KakaoMapsSDK-SPM.git", exact: "2.12.19")
    ],
    targets: [
        .target(name: "PlugpassNativeMap", dependencies: [
            .product(name: "Capacitor", package: "capacitor-swift-pm"),
            .product(name: "Cordova", package: "capacitor-swift-pm"),
            .product(name: "KakaoMapsSDK-SPM", package: "KakaoMapsSDK-SPM")
        ], path: "ios/Sources"),
        .testTarget(name: "PlugpassNativeMapTests", dependencies: ["PlugpassNativeMap"], path: "ios/Tests")
    ]
)
