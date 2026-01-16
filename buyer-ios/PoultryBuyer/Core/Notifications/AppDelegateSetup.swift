//
//  AppDelegateSetup.swift
//  PoultryBuyer
//
//  AppDelegate implementation for Firebase Cloud Messaging (FCM) integration.
//  This file contains the complete AppDelegate code needed for push notifications.
//
//  SETUP INSTRUCTIONS:
//  1. Add Firebase SDK to your project (via SPM or CocoaPods)
//  2. Add GoogleService-Info.plist to your project
//  3. Enable Push Notifications capability in Xcode
//  4. Enable Background Modes > Remote notifications
//  5. Create or update your AppDelegate with the code below
//

import UIKit
import UserNotifications

#if canImport(FirebaseCore)
import FirebaseCore
#endif

#if canImport(FirebaseMessaging)
import FirebaseMessaging
#endif

// MARK: - App Delegate

/// AppDelegate for handling push notifications and Firebase integration
///
/// If you're using SwiftUI with @main, you need to use UIApplicationDelegateAdaptor:
///
/// ```swift
/// @main
/// struct PoultryBuyerApp: App {
///     @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
///
///     var body: some Scene {
///         WindowGroup {
///             ContentView()
///         }
///     }
/// }
/// ```
class AppDelegate: NSObject, UIApplicationDelegate {

    // MARK: - Application Lifecycle

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {

        // Configure Firebase
        configureFirebase()

        // Configure Push Notifications
        configurePushNotifications()

        // Check if app was launched from notification
        if let notificationUserInfo = launchOptions?[.remoteNotification] as? [AnyHashable: Any] {
            Task { @MainActor in
                PushNotificationManager.shared.handleLaunchNotification(notificationUserInfo)
            }
        }

        return true
    }

    // MARK: - Firebase Configuration

    private func configureFirebase() {
        #if canImport(FirebaseCore)
        FirebaseApp.configure()
        #endif

        #if canImport(FirebaseMessaging)
        Messaging.messaging().delegate = self
        #endif

        print("[AppDelegate] Firebase configured")
    }

    // MARK: - Push Notification Configuration

    private func configurePushNotifications() {
        // Configure PushNotificationManager
        Task { @MainActor in
            PushNotificationManager.shared.configure()

            // Request permission (can be done later based on UX preference)
            // await PushNotificationManager.shared.requestPermission()
        }
    }

    // MARK: - Remote Notification Registration

    /// Called when registration for remote notifications succeeds
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        print("[AppDelegate] Registered for remote notifications")

        Task { @MainActor in
            // Handle device token
            PushNotificationManager.shared.handleDeviceToken(deviceToken)
        }

        #if canImport(FirebaseMessaging)
        Messaging.messaging().apnsToken = deviceToken
        #endif
    }

    /// Called when registration for remote notifications fails
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("[AppDelegate] Failed to register for remote notifications: \(error)")

        Task { @MainActor in
            PushNotificationManager.shared.handleDeviceTokenError(error)
        }
    }

    // MARK: - Remote Notification Handling

    /// Called when a remote notification is received
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        print("[AppDelegate] Received remote notification")

        #if canImport(FirebaseMessaging)
        if Messaging.messaging().appDidReceiveMessage(userInfo) {
            completionHandler(.newData)
            return
        }
        #endif

        Task { @MainActor in
            PushNotificationManager.shared.handleRemoteNotification(userInfo, fetchCompletionHandler: completionHandler)
        }
    }

    // MARK: - URL Handling (Deep Links)

    /// Handle URL scheme deep links
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        print("[AppDelegate] Handling URL: \(url)")

        // Handle deep link
        return Task { @MainActor in
            NotificationHandler.shared.handleDeepLink(url)
        }.value
    }

    // MARK: - Universal Links

    /// Handle universal links
    func application(
        _ application: UIApplication,
        continue userActivity: NSUserActivity,
        restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
    ) -> Bool {
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
              let url = userActivity.webpageURL else {
            return false
        }

        print("[AppDelegate] Handling universal link: \(url)")

        return Task { @MainActor in
            NotificationHandler.shared.handleDeepLink(url)
        }.value
    }
}

// MARK: - Firebase Messaging Delegate

#if canImport(FirebaseMessaging)
extension AppDelegate: MessagingDelegate {

    /// Called when FCM token is updated
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else {
            print("[AppDelegate] FCM token is nil")
            return
        }

        print("[AppDelegate] FCM token received: \(token.prefix(20))...")

        // Update FCM token manager
        Task { @MainActor in
            FCMTokenManager.shared.updateToken(token)
        }

        // Post notification for other parts of the app
        let dataDict: [String: String] = ["token": token]
        NotificationCenter.default.post(
            name: Notification.Name("FCMToken"),
            object: nil,
            userInfo: dataDict
        )
    }
}
#endif

// MARK: - Scene Delegate (If using SceneDelegate)

/// SceneDelegate for handling scenes and notifications
///
/// If you're using SceneDelegate, add this code to handle deep links:
class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        // Handle notification response that launched the scene
        if let notificationResponse = connectionOptions.notificationResponse {
            let userInfo = notificationResponse.notification.request.content.userInfo
            Task { @MainActor in
                PushNotificationManager.shared.handleLaunchNotification(userInfo)
            }
        }

        // Handle URL contexts (deep links)
        if let urlContext = connectionOptions.urlContexts.first {
            Task { @MainActor in
                _ = NotificationHandler.shared.handleDeepLink(urlContext.url)
            }
        }

        // Handle user activities (universal links)
        if let userActivity = connectionOptions.userActivities.first,
           userActivity.activityType == NSUserActivityTypeBrowsingWeb,
           let url = userActivity.webpageURL {
            Task { @MainActor in
                _ = NotificationHandler.shared.handleDeepLink(url)
            }
        }
    }

    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        guard let url = URLContexts.first?.url else { return }

        Task { @MainActor in
            _ = NotificationHandler.shared.handleDeepLink(url)
        }
    }

    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
              let url = userActivity.webpageURL else { return }

        Task { @MainActor in
            _ = NotificationHandler.shared.handleDeepLink(url)
        }
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        // Process any pending launch notifications
        Task { @MainActor in
            PushNotificationManager.shared.processPendingLaunchNotification()
        }
    }
}

// MARK: - SwiftUI App Integration Example

/*
 Complete SwiftUI App setup example:

 ```swift
 import SwiftUI
 import FirebaseCore

 @main
 struct PoultryBuyerApp: App {
     // Use UIApplicationDelegateAdaptor for AppDelegate
     @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

     // State objects for app-wide state
     @StateObject private var authManager = AuthManager.shared
     @StateObject private var notificationHandler = NotificationHandler.shared
     @StateObject private var pushManager = PushNotificationManager.shared

     var body: some Scene {
         WindowGroup {
             ContentView()
                 .environmentObject(authManager)
                 .environmentObject(notificationHandler)
                 .environment(\.notificationHandler, notificationHandler)
                 .onAppear {
                     // Process any pending notifications when app becomes visible
                     pushManager.processPendingLaunchNotification()
                 }
                 .onOpenURL { url in
                     // Handle deep links
                     _ = notificationHandler.handleDeepLink(url)
                 }
         }
     }
 }
 ```
*/

// MARK: - Info.plist Configuration

/*
 Add the following to your Info.plist:

 <!-- Background Modes -->
 <key>UIBackgroundModes</key>
 <array>
     <string>remote-notification</string>
 </array>

 <!-- URL Schemes for Deep Links -->
 <key>CFBundleURLTypes</key>
 <array>
     <dict>
         <key>CFBundleTypeRole</key>
         <string>Editor</string>
         <key>CFBundleURLName</key>
         <string>com.poultryplatform.buyer</string>
         <key>CFBundleURLSchemes</key>
         <array>
             <string>poultrybuyer</string>
         </array>
     </dict>
 </array>

 <!-- Associated Domains (for Universal Links) -->
 <!-- Add in Signing & Capabilities: Associated Domains -->
 <!-- applinks:poultry-platform.com -->
*/

// MARK: - Entitlements

/*
 Ensure your .entitlements file includes:

 <?xml version="1.0" encoding="UTF-8"?>
 <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
 <plist version="1.0">
 <dict>
     <key>aps-environment</key>
     <string>development</string> <!-- Change to "production" for release -->
     <key>com.apple.developer.associated-domains</key>
     <array>
         <string>applinks:poultry-platform.com</string>
     </array>
 </dict>
 </plist>
*/

// MARK: - Package Dependencies

/*
 Add Firebase to your Package.swift:

 dependencies: [
     .package(url: "https://github.com/firebase/firebase-ios-sdk.git", .upToNextMajor(from: "10.0.0")),
 ],
 targets: [
     .target(
         name: "PoultryBuyer",
         dependencies: [
             .product(name: "FirebaseMessaging", package: "firebase-ios-sdk"),
             .product(name: "FirebaseAnalytics", package: "firebase-ios-sdk"),
         ]
     )
 ]

 Or add to Podfile:

 pod 'Firebase/Messaging'
 pod 'Firebase/Analytics'
*/
