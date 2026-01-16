//
//  PaymentManager.swift
//  PoultryBuyer
//
//  Razorpay payment gateway integration wrapper
//

import Foundation
import UIKit

#if canImport(Razorpay)
import Razorpay
typealias RazorpayCheckoutProtocol = RazorpayCheckout
#else
/// Protocol for Razorpay SDK interaction
/// Implemented by the SDK when available.
protocol RazorpayCheckoutProtocol: AnyObject {
    func open(options: [String: Any], displayController: UIViewController)
    func setExternalWalletSelectionDelegate(_ delegate: Any)
}

/// Razorpay payment data delegate protocol
protocol RazorpayPaymentCompletionProtocol: AnyObject {
    func onPaymentSuccess(_ payment_id: String, andData response: [AnyHashable: Any]?)
    func onPaymentError(_ code: Int32, description str: String, andData response: [AnyHashable: Any]?)
}

/// External wallet selection delegate protocol
protocol ExternalWalletSelectionProtocol: AnyObject {
    func onExternalWalletSelected(_ walletName: String, withPaymentData paymentData: [AnyHashable: Any]?)
}
#endif

// MARK: - Payment Models

/// Represents a Razorpay payment order created on the server
struct PaymentOrder: Codable {
    let id: String
    let razorpayOrderId: String
    let orderId: String
    let amount: Int  // Amount in paise (smallest currency unit)
    let currency: String
    let status: PaymentOrderStatus
    let receipt: String?
    let notes: [String: String]?
    let createdAt: Date?

    enum CodingKeys: String, CodingKey {
        case id
        case razorpayOrderId
        case orderId
        case amount
        case currency
        case status
        case receipt
        case notes
        case createdAt
    }
}

enum PaymentOrderStatus: String, Codable {
    case created = "CREATED"
    case attempted = "ATTEMPTED"
    case paid = "PAID"
    case failed = "FAILED"
}

/// Request to create a payment order
struct CreatePaymentRequest: Codable {
    let orderId: String
    let amount: Int  // Amount in paise
    let currency: String
    let receipt: String?
    let notes: [String: String]?

    init(orderId: String, amount: Double, currency: String = "INR", receipt: String? = nil, notes: [String: String]? = nil) {
        self.orderId = orderId
        self.amount = Int(amount * 100)  // Convert to paise
        self.currency = currency
        self.receipt = receipt
        self.notes = notes
    }
}

/// Request to verify a payment
struct VerifyPaymentRequest: Codable {
    let razorpayOrderId: String
    let razorpayPaymentId: String
    let razorpaySignature: String
}

/// Response from payment verification
struct PaymentVerificationResponse: Codable {
    let verified: Bool
    let paymentId: String
    let orderId: String
    let status: String
    let message: String?
}

/// Payment status response
struct PaymentStatusResponse: Codable {
    let id: String
    let orderId: String
    let status: String
    let amount: Int
    let currency: String
    let razorpayPaymentId: String?
    let paidAt: Date?
    let failureReason: String?
}

// MARK: - Payment Result

/// Result of a Razorpay payment attempt
enum PaymentResult {
    case success(PaymentSuccessData)
    case failure(PaymentFailureData)
    case cancelled
}

struct PaymentSuccessData {
    let razorpayPaymentId: String
    let razorpayOrderId: String
    let razorpaySignature: String
}

struct PaymentFailureData {
    let code: Int
    let description: String
    let source: String?
    let step: String?
    let reason: String?
    let orderId: String?
    let paymentId: String?
}

// MARK: - Payment Delegate

protocol PaymentManagerDelegate: AnyObject {
    func paymentManager(_ manager: PaymentManager, didCompleteWith result: PaymentResult)
}

// MARK: - Razorpay Protocol (SDK Abstraction)

// MARK: - Payment Manager

/// Manages Razorpay payment integration
///
/// Usage:
/// 1. Initialize with delegate
/// 2. Call startPayment with order details
/// 3. Handle result via delegate callback
///
/// Note: Requires Razorpay iOS SDK to be installed via CocoaPods or SPM
/// Add to Podfile: pod 'razorpay-pod', '~> 1.3.0'
/// Or add SPM package: https://github.com/nicksth/razorpay-swift-package.git
@MainActor
class PaymentManager: NSObject {

    // MARK: - Properties

    static let shared = PaymentManager()

    weak var delegate: PaymentManagerDelegate?

    /// Razorpay SDK instance - set when SDK is configured
    /// In production, this would be: Razorpay.initWithKey(razorpayKey, andDelegate: self)
    private var razorpay: RazorpayCheckoutProtocol?

    /// Current payment order being processed
    private var currentPaymentOrder: PaymentOrder?

    /// Current order ID for tracking
    private var currentOrderId: String?

    /// Razorpay API key (loaded from configuration)
    private var razorpayKey: String {
        // In production, load from AppConfig or environment
        #if DEBUG
        return AppConfig.razorpayKeyId
        #else
        return AppConfig.razorpayKeyId
        #endif
    }

    // MARK: - Initialization

    private override init() {
        super.init()
        setupRazorpay()
    }

    private func setupRazorpay() {
        #if canImport(Razorpay)
        razorpay = RazorpayCheckout.initWithKey(razorpayKey, andDelegate: self)
        razorpay?.setExternalWalletSelectionDelegate(self)
        #else
        razorpay = nil
        #endif

        print("[PaymentManager] Initialized with key: \(razorpayKey.prefix(8))...")
    }

    // MARK: - Public Methods

    /// Start a payment flow for an order
    /// - Parameters:
    ///   - paymentOrder: The payment order created on the server
    ///   - customerInfo: Customer details for prefilling checkout
    ///   - presentingController: UIViewController to present checkout on
    func startPayment(
        paymentOrder: PaymentOrder,
        customerInfo: PaymentCustomerInfo,
        presentingController: UIViewController
    ) {
        currentPaymentOrder = paymentOrder
        currentOrderId = paymentOrder.orderId

        let options = buildPaymentOptions(
            paymentOrder: paymentOrder,
            customerInfo: customerInfo
        )

        print("[PaymentManager] Starting payment for order: \(paymentOrder.orderId)")
        print("[PaymentManager] Razorpay Order ID: \(paymentOrder.razorpayOrderId)")
        print("[PaymentManager] Amount: \(paymentOrder.amount) paise")

        // Open Razorpay checkout if SDK is available
        razorpay?.open(options: options, displayController: presentingController)

        // For development without SDK, simulate a successful payment after 2 seconds
        #if DEBUG
        if razorpay == nil {
            simulatePaymentForDevelopment(paymentOrder: paymentOrder)
        }
        #endif
    }

    /// Cancel any ongoing payment
    func cancelCurrentPayment() {
        print("[PaymentManager] Payment cancelled by user")
        currentPaymentOrder = nil
        currentOrderId = nil
        delegate?.paymentManager(self, didCompleteWith: .cancelled)
    }

    // MARK: - Private Methods

    private func buildPaymentOptions(
        paymentOrder: PaymentOrder,
        customerInfo: PaymentCustomerInfo
    ) -> [String: Any] {
        var options: [String: Any] = [
            "key": razorpayKey,
            "amount": paymentOrder.amount,
            "currency": paymentOrder.currency,
            "order_id": paymentOrder.razorpayOrderId,
            "name": AppConfig.merchantName,
            "description": "Order #\(paymentOrder.receipt ?? paymentOrder.orderId)",
            "timeout": 300,  // 5 minutes timeout
            "send_sms_hash": true,
            "remember_customer": true
        ]

        // Prefill customer information
        var prefill: [String: String] = [:]
        if let name = customerInfo.name {
            prefill["name"] = name
        }
        if let email = customerInfo.email {
            prefill["email"] = email
        }
        if let phone = customerInfo.phone {
            prefill["contact"] = phone
        }
        options["prefill"] = prefill

        // Theme customization
        options["theme"] = [
            "color": "#FF6B35"  // App primary color
        ]

        // Notes for tracking
        var notes: [String: String] = [
            "app_order_id": paymentOrder.orderId
        ]
        if let additionalNotes = paymentOrder.notes {
            notes.merge(additionalNotes) { _, new in new }
        }
        options["notes"] = notes

        // Retry configuration
        options["retry"] = [
            "enabled": true,
            "max_count": 3
        ]

        // Payment method preferences
        options["config"] = [
            "display": [
                "blocks": [
                    "banks": [
                        "name": "Pay via UPI/Bank",
                        "instruments": [
                            ["method": "upi"],
                            ["method": "netbanking"]
                        ]
                    ],
                    "other": [
                        "name": "Other Payment Methods",
                        "instruments": [
                            ["method": "card"],
                            ["method": "wallet"]
                        ]
                    ]
                ],
                "sequence": ["block.banks", "block.other"],
                "preferences": [
                    "show_default_blocks": true
                ]
            ]
        ]

        return options
    }

    #if DEBUG
    /// Simulate payment for development testing without SDK
    private func simulatePaymentForDevelopment(paymentOrder: PaymentOrder) {
        print("[PaymentManager] DEVELOPMENT MODE: Simulating payment...")

        // Simulate payment processing delay
        Task {
            try? await Task.sleep(nanoseconds: 2_000_000_000)  // 2 seconds

            // Simulate successful payment
            let successData = PaymentSuccessData(
                razorpayPaymentId: "pay_\(UUID().uuidString.prefix(14))",
                razorpayOrderId: paymentOrder.razorpayOrderId,
                razorpaySignature: "simulated_signature_\(UUID().uuidString)"
            )

            print("[PaymentManager] DEVELOPMENT MODE: Payment simulated successfully")
            self.delegate?.paymentManager(self, didCompleteWith: .success(successData))
        }
    }
    #endif
}

// MARK: - Razorpay SDK Delegate Implementation

#if canImport(Razorpay)
extension PaymentManager: RazorpayPaymentCompletionProtocol {

    func onPaymentSuccess(_ payment_id: String, andData response: [AnyHashable: Any]?) {
        print("[PaymentManager] Payment succeeded: \(payment_id)")

        guard let response = response,
              let razorpayOrderId = response["razorpay_order_id"] as? String,
              let razorpaySignature = response["razorpay_signature"] as? String else {
            print("[PaymentManager] Error: Missing payment response data")
            let failureData = PaymentFailureData(
                code: -1,
                description: "Invalid payment response",
                source: nil,
                step: nil,
                reason: "Missing signature or order ID",
                orderId: currentOrderId,
                paymentId: payment_id
            )
            delegate?.paymentManager(self, didCompleteWith: .failure(failureData))
            return
        }

        let successData = PaymentSuccessData(
            razorpayPaymentId: payment_id,
            razorpayOrderId: razorpayOrderId,
            razorpaySignature: razorpaySignature
        )

        currentPaymentOrder = nil
        currentOrderId = nil

        delegate?.paymentManager(self, didCompleteWith: .success(successData))
    }

    func onPaymentError(_ code: Int32, description str: String, andData response: [AnyHashable: Any]?) {
        print("[PaymentManager] Payment failed: \(code) - \(str)")

        let failureData = PaymentFailureData(
            code: Int(code),
            description: str,
            source: response?["source"] as? String,
            step: response?["step"] as? String,
            reason: response?["reason"] as? String,
            orderId: response?["order_id"] as? String ?? currentOrderId,
            paymentId: response?["payment_id"] as? String
        )

        currentPaymentOrder = nil
        currentOrderId = nil

        delegate?.paymentManager(self, didCompleteWith: .failure(failureData))
    }
}

extension PaymentManager: ExternalWalletSelectionProtocol {
    func onExternalWalletSelected(_ walletName: String, withPaymentData paymentData: [AnyHashable: Any]?) {
        print("[PaymentManager] External wallet selected: \(walletName)")
    }
}
#endif

// MARK: - Customer Info

struct PaymentCustomerInfo {
    let name: String?
    let email: String?
    let phone: String?

    init(name: String? = nil, email: String? = nil, phone: String? = nil) {
        self.name = name
        self.email = email
        self.phone = phone
    }
}

// MARK: - App Config Extension for Payment

extension AppConfig {

    /// Razorpay Key ID
    static var razorpayKeyId: String {
        switch environment {
        case .development:
            // Test key for development
            return "rzp_test_XXXXXXXXXX"
        case .staging:
            return "rzp_test_XXXXXXXXXX"
        case .production:
            // Live key for production
            return "rzp_live_XXXXXXXXXX"
        }
    }

    /// Merchant name displayed in checkout
    static var merchantName: String {
        return "Poultry Platform"
    }
}
