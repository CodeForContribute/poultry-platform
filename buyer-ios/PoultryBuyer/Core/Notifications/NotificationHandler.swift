//
//  NotificationHandler.swift
//  PoultryBuyer
//
//  Handles notification actions and deep linking
//

import Foundation
import SwiftUI

// MARK: - Deep Link Route

/// Represents a deep link destination in the app
enum DeepLinkRoute: Equatable {
    case home
    case orders
    case orderDetail(orderId: String)
    case productDetail(productId: String)
    case cart
    case checkout(sellerId: String)
    case payment(orderId: String)
    case profile
    case search(query: String?)
    case category(categoryId: String)
    case promotions
    case settings

    /// Parse a deep link URL string into a route
    static func from(url: String) -> DeepLinkRoute? {
        guard let url = URL(string: url) else { return nil }
        return from(url: url)
    }

    /// Parse a URL into a route
    static func from(url: URL) -> DeepLinkRoute? {
        let pathComponents = url.pathComponents.filter { $0 != "/" }
        let queryItems = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems

        guard let firstComponent = pathComponents.first else {
            return .home
        }

        switch firstComponent {
        case "orders":
            if pathComponents.count > 1 {
                return .orderDetail(orderId: pathComponents[1])
            }
            return .orders

        case "products", "product":
            if pathComponents.count > 1 {
                return .productDetail(productId: pathComponents[1])
            }
            return .home

        case "cart":
            return .cart

        case "checkout":
            if pathComponents.count > 1 {
                return .checkout(sellerId: pathComponents[1])
            }
            return .cart

        case "payment", "pay":
            if pathComponents.count > 1 {
                return .payment(orderId: pathComponents[1])
            }
            return .orders

        case "profile":
            return .profile

        case "search":
            let query = queryItems?.first(where: { $0.name == "q" })?.value
            return .search(query: query)

        case "category", "categories":
            if pathComponents.count > 1 {
                return .category(categoryId: pathComponents[1])
            }
            return .home

        case "promotions", "offers":
            return .promotions

        case "settings":
            return .settings

        default:
            return nil
        }
    }

    /// Generate a URL path for this route
    var urlPath: String {
        switch self {
        case .home:
            return "/"
        case .orders:
            return "/orders"
        case .orderDetail(let orderId):
            return "/orders/\(orderId)"
        case .productDetail(let productId):
            return "/products/\(productId)"
        case .cart:
            return "/cart"
        case .checkout(let sellerId):
            return "/checkout/\(sellerId)"
        case .payment(let orderId):
            return "/payment/\(orderId)"
        case .profile:
            return "/profile"
        case .search(let query):
            if let query = query {
                return "/search?q=\(query)"
            }
            return "/search"
        case .category(let categoryId):
            return "/category/\(categoryId)"
        case .promotions:
            return "/promotions"
        case .settings:
            return "/settings"
        }
    }
}

// MARK: - Notification Handler

/// Handles notification actions and navigation
///
/// Usage:
/// 1. Set the router reference for navigation
/// 2. Call handleNotification() when a notification is tapped
/// 3. Use handleDeepLink() for URL-based deep links
@MainActor
class NotificationHandler: ObservableObject {

    // MARK: - Properties

    static let shared = NotificationHandler()

    /// Current pending route to navigate to
    @Published var pendingRoute: DeepLinkRoute?

    /// Whether a navigation is pending
    @Published var hasPendingNavigation: Bool = false

    /// Navigation path for SwiftUI NavigationStack
    @Published var navigationPath = NavigationPath()

    /// Selected tab for tab-based navigation
    @Published var selectedTab: AppTab = .home

    // MARK: - Initialization

    private init() {
        setupNotificationObservers()
    }

    // MARK: - Setup

    private func setupNotificationObservers() {
        NotificationCenter.default.addObserver(
            forName: .didTapPushNotification,
            object: nil,
            queue: .main
        ) { [weak self] notification in
            guard let pushNotification = notification.userInfo?["notification"] as? PushNotification else {
                return
            }
            Task { @MainActor in
                self?.handleNotification(pushNotification)
            }
        }
    }

    // MARK: - Notification Handling

    /// Handle a notification tap action
    func handleNotification(_ notification: PushNotification) {
        print("[NotificationHandler] Handling notification: \(notification.type.rawValue)")

        // First check for explicit deep link
        if let deepLink = notification.deepLink,
           let route = DeepLinkRoute.from(url: deepLink) {
            navigate(to: route)
            return
        }

        // Route based on notification type and data
        let route = routeForNotification(notification)
        navigate(to: route)
    }

    /// Determine the appropriate route for a notification
    private func routeForNotification(_ notification: PushNotification) -> DeepLinkRoute {
        switch notification.type {
        case .orderUpdate, .orderPlaced, .orderConfirmed, .orderDispatched, .orderDelivered, .orderCancelled:
            if let orderId = notification.orderId {
                return .orderDetail(orderId: orderId)
            }
            return .orders

        case .paymentSuccess, .paymentFailed:
            if let orderId = notification.orderId {
                return .orderDetail(orderId: orderId)
            }
            return .orders

        case .priceAlert, .newProduct:
            if let productId = notification.productId {
                return .productDetail(productId: productId)
            }
            return .home

        case .promotion:
            return .promotions

        case .general:
            return .home
        }
    }

    // MARK: - Deep Link Handling

    /// Handle a deep link URL
    func handleDeepLink(_ url: URL) -> Bool {
        print("[NotificationHandler] Handling deep link: \(url.absoluteString)")

        guard let route = DeepLinkRoute.from(url: url) else {
            print("[NotificationHandler] Could not parse deep link")
            return false
        }

        navigate(to: route)
        return true
    }

    /// Handle a deep link URL string
    func handleDeepLink(_ urlString: String) -> Bool {
        guard let url = URL(string: urlString) else {
            return false
        }
        return handleDeepLink(url)
    }

    // MARK: - Navigation

    /// Navigate to a specific route
    func navigate(to route: DeepLinkRoute) {
        print("[NotificationHandler] Navigating to: \(route.urlPath)")

        // Set the appropriate tab first
        switch route {
        case .home, .productDetail, .category, .search, .promotions:
            selectedTab = .home
        case .orders, .orderDetail, .payment:
            selectedTab = .orders
        case .cart, .checkout:
            selectedTab = .cart
        case .profile, .settings:
            selectedTab = .profile
        }

        // Set pending route for detail navigation
        pendingRoute = route
        hasPendingNavigation = true

        // Post notification for view controllers to handle
        NotificationCenter.default.post(
            name: .navigateToRoute,
            object: self,
            userInfo: ["route": route]
        )
    }

    /// Clear pending navigation
    func clearPendingNavigation() {
        pendingRoute = nil
        hasPendingNavigation = false
    }

    /// Reset navigation state
    func resetNavigation() {
        navigationPath = NavigationPath()
        selectedTab = .home
        clearPendingNavigation()
    }
}

// MARK: - App Tab

/// Main app tabs
enum AppTab: Int, CaseIterable, Hashable {
    case home = 0
    case orders = 1
    case cart = 2
    case profile = 3

    var title: String {
        switch self {
        case .home: return "Home"
        case .orders: return "Orders"
        case .cart: return "Cart"
        case .profile: return "Profile"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .orders: return "list.bullet.rectangle.fill"
        case .cart: return "cart.fill"
        case .profile: return "person.fill"
        }
    }
}

// MARK: - Notification Name

extension Notification.Name {
    static let navigateToRoute = Notification.Name("navigateToRoute")
}

// MARK: - Navigation Router View Modifier

/// View modifier to handle deep link navigation
struct DeepLinkNavigationModifier: ViewModifier {
    @ObservedObject var handler: NotificationHandler

    let onOrderDetail: ((String) -> Void)?
    let onProductDetail: ((String) -> Void)?
    let onPayment: ((String) -> Void)?

    func body(content: Content) -> some View {
        content
            .onChange(of: handler.pendingRoute) { _, route in
                guard let route = route else { return }
                handleRoute(route)
            }
    }

    private func handleRoute(_ route: DeepLinkRoute) {
        switch route {
        case .orderDetail(let orderId):
            onOrderDetail?(orderId)
        case .productDetail(let productId):
            onProductDetail?(productId)
        case .payment(let orderId):
            onPayment?(orderId)
        default:
            break
        }

        // Clear after handling
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            handler.clearPendingNavigation()
        }
    }
}

extension View {
    /// Handle deep link navigation
    func handleDeepLinks(
        handler: NotificationHandler = .shared,
        onOrderDetail: ((String) -> Void)? = nil,
        onProductDetail: ((String) -> Void)? = nil,
        onPayment: ((String) -> Void)? = nil
    ) -> some View {
        modifier(DeepLinkNavigationModifier(
            handler: handler,
            onOrderDetail: onOrderDetail,
            onProductDetail: onProductDetail,
            onPayment: onPayment
        ))
    }
}

// MARK: - Environment Key for Notification Handler

struct NotificationHandlerKey: EnvironmentKey {
    static let defaultValue: NotificationHandler = .shared
}

extension EnvironmentValues {
    var notificationHandler: NotificationHandler {
        get { self[NotificationHandlerKey.self] }
        set { self[NotificationHandlerKey.self] = newValue }
    }
}
