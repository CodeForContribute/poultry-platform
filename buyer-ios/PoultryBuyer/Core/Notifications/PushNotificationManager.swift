//
//  PushNotificationManager.swift
//  PoultryBuyer
//
//  Central manager for push notifications and FCM integration
//

import Foundation
import UIKit
import UserNotifications

#if canImport(FirebaseMessaging)
import FirebaseMessaging
#endif

// MARK: - Notification Types

/// Types of notifications the app can receive
enum NotificationType: String, CaseIterable {
    case orderUpdate = "ORDER_UPDATE"
    case orderPlaced = "ORDER_PLACED"
    case orderConfirmed = "ORDER_CONFIRMED"
    case orderDispatched = "ORDER_DISPATCHED"
    case orderDelivered = "ORDER_DELIVERED"
    case orderCancelled = "ORDER_CANCELLED"
    case paymentSuccess = "PAYMENT_SUCCESS"
    case paymentFailed = "PAYMENT_FAILED"
    case promotion = "PROMOTION"
    case priceAlert = "PRICE_ALERT"
    case newProduct = "NEW_PRODUCT"
    case general = "GENERAL"

    var title: String {
        switch self {
        case .orderUpdate: return "Order Update"
        case .orderPlaced: return "Order Placed"
        case .orderConfirmed: return "Order Confirmed"
        case .orderDispatched: return "Order Dispatched"
        case .orderDelivered: return "Order Delivered"
        case .orderCancelled: return "Order Cancelled"
        case .paymentSuccess: return "Payment Successful"
        case .paymentFailed: return "Payment Failed"
        case .promotion: return "Special Offer"
        case .priceAlert: return "Price Alert"
        case .newProduct: return "New Product"
        case .general: return "Notification"
        }
    }

    var defaultIcon: String {
        switch self {
        case .orderUpdate, .orderPlaced, .orderConfirmed, .orderDispatched, .orderDelivered:
            return "shippingbox.fill"
        case .orderCancelled:
            return "xmark.circle.fill"
        case .paymentSuccess:
            return "checkmark.circle.fill"
        case .paymentFailed:
            return "exclamationmark.circle.fill"
        case .promotion:
            return "tag.fill"
        case .priceAlert:
            return "bell.badge.fill"
        case .newProduct:
            return "star.fill"
        case .general:
            return "bell.fill"
        }
    }
}

// MARK: - Push Notification Model

/// Represents a parsed push notification
struct PushNotification {
    let id: String
    let type: NotificationType
    let title: String
    let body: String
    let data: [String: Any]
    let receivedAt: Date

    /// Order ID if this is an order-related notification
    var orderId: String? {
        data["orderId"] as? String ?? data["order_id"] as? String
    }

    /// Product ID if this is a product-related notification
    var productId: String? {
        data["productId"] as? String ?? data["product_id"] as? String
    }

    /// Deep link URL if provided
    var deepLink: String? {
        data["deepLink"] as? String ?? data["deep_link"] as? String
    }

    /// Image URL if provided
    var imageUrl: String? {
        data["imageUrl"] as? String ?? data["image_url"] as? String
    }

    init(userInfo: [AnyHashable: Any]) {
        // Extract notification ID
        self.id = userInfo["notification_id"] as? String ??
                  userInfo["google.message_id"] as? String ??
                  UUID().uuidString

        // Extract notification type
        let typeString = userInfo["type"] as? String ?? userInfo["notification_type"] as? String ?? ""
        self.type = NotificationType(rawValue: typeString) ?? .general

        // Extract title and body from different possible locations
        if let aps = userInfo["aps"] as? [String: Any],
           let alert = aps["alert"] as? [String: Any] {
            self.title = alert["title"] as? String ?? type.title
            self.body = alert["body"] as? String ?? ""
        } else if let aps = userInfo["aps"] as? [String: Any],
                  let alertString = aps["alert"] as? String {
            self.title = type.title
            self.body = alertString
        } else {
            self.title = userInfo["title"] as? String ?? type.title
            self.body = userInfo["body"] as? String ?? userInfo["message"] as? String ?? ""
        }

        // Extract data payload
        var extractedData: [String: Any] = [:]
        for (key, value) in userInfo {
            if let keyString = key as? String,
               keyString != "aps" && keyString != "gcm.message_id" {
                extractedData[keyString] = value
            }
        }
        self.data = extractedData

        self.receivedAt = Date()
    }
}

// MARK: - Push Notification Manager Delegate

protocol PushNotificationManagerDelegate: AnyObject {
    func pushNotificationManager(_ manager: PushNotificationManager, didReceive notification: PushNotification)
    func pushNotificationManager(_ manager: PushNotificationManager, didTap notification: PushNotification)
    func pushNotificationManager(_ manager: PushNotificationManager, didRegisterWithToken token: String)
    func pushNotificationManager(_ manager: PushNotificationManager, didFailToRegisterWith error: Error)
}

// Make delegate methods optional
extension PushNotificationManagerDelegate {
    func pushNotificationManager(_ manager: PushNotificationManager, didReceive notification: PushNotification) {}
    func pushNotificationManager(_ manager: PushNotificationManager, didTap notification: PushNotification) {}
    func pushNotificationManager(_ manager: PushNotificationManager, didRegisterWithToken token: String) {}
    func pushNotificationManager(_ manager: PushNotificationManager, didFailToRegisterWith error: Error) {}
}

// MARK: - Push Notification Manager

/// Central manager for handling push notifications
///
/// Usage:
/// 1. Call configure() in AppDelegate's didFinishLaunchingWithOptions
/// 2. Set delegate to receive notification events
/// 3. Call requestPermission() to prompt user for notification access
///
/// Firebase Integration:
/// - Requires Firebase SDK to be installed
/// - GoogleService-Info.plist must be added to the project
/// - See FIREBASE_SETUP.md for complete setup instructions
@MainActor
class PushNotificationManager: NSObject, ObservableObject {

    // MARK: - Properties

    static let shared = PushNotificationManager()

    weak var delegate: PushNotificationManagerDelegate?

    /// Current notification authorization status
    @Published var authorizationStatus: UNAuthorizationStatus = .notDetermined

    /// Whether notifications are enabled
    @Published var isNotificationsEnabled: Bool = false

    /// Device token for push notifications
    @Published private(set) var deviceToken: String?

    /// Last received notification (for UI binding)
    @Published private(set) var lastNotification: PushNotification?

    /// Badge count
    @Published var badgeCount: Int = 0 {
        didSet {
            updateBadge()
        }
    }

    /// Pending notification that triggered app launch
    private var pendingLaunchNotification: PushNotification?

    // MARK: - Initialization

    private override init() {
        super.init()
    }

    // MARK: - Configuration

    /// Configure push notification manager
    /// Call this in AppDelegate's didFinishLaunchingWithOptions
    func configure() {
        UNUserNotificationCenter.current().delegate = self
        checkAuthorizationStatus()

        #if canImport(FirebaseMessaging)
        Messaging.messaging().delegate = self
        #endif

        print("[PushNotificationManager] Configured")
    }

    // MARK: - Permission Management

    /// Request notification permission from the user
    /// - Returns: Whether permission was granted
    @discardableResult
    func requestPermission() async -> Bool {
        do {
            let options: UNAuthorizationOptions = [.alert, .badge, .sound, .provisional]
            let granted = try await UNUserNotificationCenter.current().requestAuthorization(options: options)

            await checkAuthorizationStatus()

            if granted {
                await registerForRemoteNotifications()
            }

            print("[PushNotificationManager] Permission granted: \(granted)")
            return granted
        } catch {
            print("[PushNotificationManager] Permission request failed: \(error)")
            return false
        }
    }

    /// Check current authorization status
    func checkAuthorizationStatus() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        self.authorizationStatus = settings.authorizationStatus
        self.isNotificationsEnabled = settings.authorizationStatus == .authorized ||
                                       settings.authorizationStatus == .provisional

        print("[PushNotificationManager] Authorization status: \(authorizationStatus.description)")
    }

    /// Register for remote notifications
    func registerForRemoteNotifications() async {
        await MainActor.run {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }

    /// Unregister from remote notifications
    func unregisterForRemoteNotifications() {
        UIApplication.shared.unregisterForRemoteNotifications()
        deviceToken = nil
    }

    // MARK: - Token Handling

    /// Handle successful device token registration
    /// Call this from AppDelegate's didRegisterForRemoteNotificationsWithDeviceToken
    func handleDeviceToken(_ deviceToken: Data) {
        let tokenString = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        self.deviceToken = tokenString

        print("[PushNotificationManager] Device token: \(tokenString.prefix(20))...")

        FCMTokenManager.shared.updateAPNSToken(deviceToken)

        delegate?.pushNotificationManager(self, didRegisterWithToken: tokenString)
    }

    /// Handle device token registration failure
    /// Call this from AppDelegate's didFailToRegisterForRemoteNotificationsWithError
    func handleDeviceTokenError(_ error: Error) {
        print("[PushNotificationManager] Failed to register for remote notifications: \(error)")
        delegate?.pushNotificationManager(self, didFailToRegisterWith: error)
    }

    // MARK: - Notification Handling

    /// Handle received remote notification
    /// Call this from AppDelegate's didReceiveRemoteNotification
    func handleRemoteNotification(
        _ userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        let notification = PushNotification(userInfo: userInfo)
        lastNotification = notification

        print("[PushNotificationManager] Received notification: \(notification.type.rawValue)")

        delegate?.pushNotificationManager(self, didReceive: notification)

        // Post notification for observers
        NotificationCenter.default.post(
            name: .didReceivePushNotification,
            object: self,
            userInfo: ["notification": notification]
        )

        completionHandler(.newData)
    }

    /// Handle notification that launched the app
    /// Call this after app has finished launching
    func handleLaunchNotification(_ userInfo: [AnyHashable: Any]) {
        let notification = PushNotification(userInfo: userInfo)
        pendingLaunchNotification = notification

        print("[PushNotificationManager] App launched from notification: \(notification.type.rawValue)")
    }

    /// Process any pending launch notification
    /// Call this when the main view is ready
    func processPendingLaunchNotification() {
        guard let notification = pendingLaunchNotification else { return }
        pendingLaunchNotification = nil

        delegate?.pushNotificationManager(self, didTap: notification)

        NotificationCenter.default.post(
            name: .didTapPushNotification,
            object: self,
            userInfo: ["notification": notification]
        )
    }

    // MARK: - Badge Management

    private func updateBadge() {
        Task {
            do {
                try await UNUserNotificationCenter.current().setBadgeCount(badgeCount)
            } catch {
                print("[PushNotificationManager] Failed to update badge: \(error)")
            }
        }
    }

    func clearBadge() {
        badgeCount = 0
    }

    func incrementBadge() {
        badgeCount += 1
    }

    // MARK: - Local Notifications

    /// Schedule a local notification
    func scheduleLocalNotification(
        title: String,
        body: String,
        type: NotificationType = .general,
        delay: TimeInterval = 0,
        data: [String: Any] = [:]
    ) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        var userInfo = data
        userInfo["type"] = type.rawValue
        content.userInfo = userInfo

        let trigger: UNNotificationTrigger?
        if delay > 0 {
            trigger = UNTimeIntervalNotificationTrigger(timeInterval: delay, repeats: false)
        } else {
            trigger = nil
        }

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: trigger
        )

        do {
            try await UNUserNotificationCenter.current().add(request)
            print("[PushNotificationManager] Local notification scheduled: \(title)")
        } catch {
            print("[PushNotificationManager] Failed to schedule notification: \(error)")
        }
    }

    /// Cancel all pending local notifications
    func cancelAllPendingNotifications() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }

    /// Cancel specific notification by identifier
    func cancelNotification(identifier: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [identifier])
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension PushNotificationManager: UNUserNotificationCenterDelegate {

    /// Handle notification when app is in foreground
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        let pushNotification = PushNotification(userInfo: userInfo)

        print("[PushNotificationManager] Notification received in foreground: \(pushNotification.type.rawValue)")

        Task { @MainActor in
            self.lastNotification = pushNotification
            self.delegate?.pushNotificationManager(self, didReceive: pushNotification)

            NotificationCenter.default.post(
                name: .didReceivePushNotification,
                object: self,
                userInfo: ["notification": pushNotification]
            )
        }

        // Show notification even when app is in foreground
        completionHandler([.banner, .sound, .badge])
    }

    /// Handle notification tap
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let notification = PushNotification(userInfo: userInfo)

        print("[PushNotificationManager] Notification tapped: \(notification.type.rawValue)")

        Task { @MainActor in
            self.delegate?.pushNotificationManager(self, didTap: notification)

            NotificationCenter.default.post(
                name: .didTapPushNotification,
                object: self,
                userInfo: ["notification": notification]
            )
        }

        completionHandler()
    }
}

// MARK: - Firebase Messaging Delegate

#if canImport(FirebaseMessaging)
extension PushNotificationManager: MessagingDelegate {

    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }

        print("[PushNotificationManager] FCM token received: \(token.prefix(20))...")

        Task { @MainActor in
            FCMTokenManager.shared.updateToken(token)
        }
    }
}
#endif

// MARK: - Notification Names

extension Notification.Name {
    static let didReceivePushNotification = Notification.Name("didReceivePushNotification")
    static let didTapPushNotification = Notification.Name("didTapPushNotification")
}

// MARK: - Authorization Status Description

extension UNAuthorizationStatus {
    var description: String {
        switch self {
        case .notDetermined: return "Not Determined"
        case .denied: return "Denied"
        case .authorized: return "Authorized"
        case .provisional: return "Provisional"
        case .ephemeral: return "Ephemeral"
        @unknown default: return "Unknown"
        }
    }
}
