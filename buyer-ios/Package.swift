// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "PoultryBuyer",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(name: "PoultryBuyer", targets: ["PoultryBuyer"])
    ],
    dependencies: [
        .package(url: "https://github.com/Alamofire/Alamofire.git", .upToNextMajor(from: "5.8.0")),
        .package(url: "https://github.com/onevcat/Kingfisher.git", .upToNextMajor(from: "7.10.0")),
        .package(url: "https://github.com/kishikawakatsumi/KeychainAccess.git", .upToNextMajor(from: "4.2.0")),
        .package(url: "https://github.com/nicksth/razorpay-swift-package.git", .upToNextMajor(from: "1.3.0")),
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", .upToNextMajor(from: "10.0.0")),
    ],
    targets: [
        .target(
            name: "PoultryBuyer",
            dependencies: [
                "Alamofire",
                "Kingfisher",
                "KeychainAccess",
                "Razorpay",
                .product(name: "FirebaseCore", package: "firebase-ios-sdk"),
                .product(name: "FirebaseMessaging", package: "firebase-ios-sdk")
            ],
            path: "PoultryBuyer"
        ),
        .testTarget(
            name: "PoultryBuyerTests",
            dependencies: ["PoultryBuyer"],
            path: "Tests"
        )
    ]
)
