//
//  PaymentViewModel.swift
//  PoultryBuyer
//
//  ViewModel for handling payment flow and state management
//

import Foundation
import UIKit
import Combine

// MARK: - Payment State

enum PaymentState: Equatable {
    case idle
    case creatingOrder
    case readyForPayment(PaymentOrder)
    case processingPayment
    case verifyingPayment
    case success(PaymentVerificationResponse)
    case failed(String)
    case cancelled

    var isLoading: Bool {
        switch self {
        case .creatingOrder, .processingPayment, .verifyingPayment:
            return true
        default:
            return false
        }
    }

    var statusMessage: String {
        switch self {
        case .idle:
            return "Ready to pay"
        case .creatingOrder:
            return "Preparing payment..."
        case .readyForPayment:
            return "Tap to pay"
        case .processingPayment:
            return "Processing payment..."
        case .verifyingPayment:
            return "Verifying payment..."
        case .success:
            return "Payment successful!"
        case .failed(let message):
            return message
        case .cancelled:
            return "Payment cancelled"
        }
    }

    static func == (lhs: PaymentState, rhs: PaymentState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle),
             (.creatingOrder, .creatingOrder),
             (.processingPayment, .processingPayment),
             (.verifyingPayment, .verifyingPayment),
             (.cancelled, .cancelled):
            return true
        case (.readyForPayment(let lOrder), .readyForPayment(let rOrder)):
            return lOrder.id == rOrder.id
        case (.success(let lResponse), .success(let rResponse)):
            return lResponse.paymentId == rResponse.paymentId
        case (.failed(let lMessage), .failed(let rMessage)):
            return lMessage == rMessage
        default:
            return false
        }
    }
}

// MARK: - Payment View Model

@MainActor
class PaymentViewModel: ObservableObject {

    // MARK: - Published Properties

    @Published var state: PaymentState = .idle
    @Published var paymentOrder: PaymentOrder?
    @Published var errorMessage: String?
    @Published var showError: Bool = false

    // Order details
    @Published var order: Order?
    @Published var isLoadingOrder: Bool = false

    // MARK: - Private Properties

    private let apiClient = APIClient.shared
    private let paymentManager = PaymentManager.shared
    private var cancellables = Set<AnyCancellable>()

    // Customer info for checkout prefill
    private var customerInfo: PaymentCustomerInfo?

    // Callbacks
    var onPaymentSuccess: ((Order) -> Void)?
    var onPaymentFailure: ((String) -> Void)?
    var onPaymentCancelled: (() -> Void)?

    // MARK: - Initialization

    init() {
        paymentManager.delegate = self
    }

    // MARK: - Public Methods

    /// Initialize payment flow for an order
    /// - Parameters:
    ///   - order: The order to pay for
    ///   - customerInfo: Customer info for prefill
    func initializePayment(
        for order: Order,
        customerInfo: PaymentCustomerInfo? = nil
    ) {
        self.order = order
        self.customerInfo = customerInfo
        createPaymentOrder()
    }

    /// Initialize payment flow with order ID
    /// - Parameters:
    ///   - orderId: The order ID to pay for
    ///   - customerInfo: Customer info for prefill
    func initializePayment(
        orderId: String,
        customerInfo: PaymentCustomerInfo? = nil
    ) async {
        self.customerInfo = customerInfo
        await loadOrderAndCreatePayment(orderId: orderId)
    }

    /// Start the Razorpay checkout
    /// - Parameter presentingController: UIViewController to present checkout
    func startPayment(presentingController: UIViewController) {
        guard case .readyForPayment(let paymentOrder) = state else {
            print("[PaymentViewModel] Cannot start payment - not ready")
            return
        }

        state = .processingPayment

        paymentManager.startPayment(
            paymentOrder: paymentOrder,
            customerInfo: customerInfo ?? PaymentCustomerInfo(),
            presentingController: presentingController
        )
    }

    /// Retry payment after failure
    func retryPayment() {
        guard let order = order else {
            errorMessage = "No order to retry"
            showError = true
            return
        }

        state = .idle
        createPaymentOrder()
    }

    /// Cancel the current payment
    func cancelPayment() {
        paymentManager.cancelCurrentPayment()
    }

    /// Clear any error state
    func clearError() {
        errorMessage = nil
        showError = false
    }

    /// Check payment status for an order
    func checkPaymentStatus(paymentId: String) async {
        do {
            let response = try await apiClient.getPaymentStatus(paymentId: paymentId)

            if response.success, let status = response.data {
                if status.status == "PAID" {
                    // Payment was successful, reload order
                    if let orderId = order?.id {
                        await reloadOrder(orderId: orderId)
                    }
                } else if status.status == "FAILED" {
                    state = .failed(status.failureReason ?? "Payment failed")
                }
            }
        } catch {
            print("[PaymentViewModel] Failed to check payment status: \(error)")
        }
    }

    // MARK: - Private Methods

    private func createPaymentOrder() {
        guard let order = order else {
            errorMessage = "No order available"
            showError = true
            return
        }

        state = .creatingOrder

        Task {
            do {
                let request = CreatePaymentRequest(
                    orderId: order.id,
                    amount: order.totalAmount,
                    currency: "INR",
                    receipt: order.orderNumber,
                    notes: [
                        "order_number": order.orderNumber,
                        "buyer_id": order.buyerId,
                        "seller_id": order.sellerId
                    ]
                )

                let response = try await apiClient.createPayment(request: request)

                if response.success, let paymentOrder = response.data {
                    self.paymentOrder = paymentOrder
                    state = .readyForPayment(paymentOrder)
                    print("[PaymentViewModel] Payment order created: \(paymentOrder.razorpayOrderId)")
                } else {
                    let message = response.message ?? "Failed to create payment"
                    state = .failed(message)
                    errorMessage = message
                    showError = true
                }
            } catch {
                let message = "Failed to create payment: \(error.localizedDescription)"
                state = .failed(message)
                errorMessage = message
                showError = true
                print("[PaymentViewModel] Error creating payment: \(error)")
            }
        }
    }

    private func loadOrderAndCreatePayment(orderId: String) async {
        isLoadingOrder = true

        do {
            let response = try await apiClient.getOrder(id: orderId)

            if response.success, let order = response.data {
                self.order = order
                isLoadingOrder = false
                createPaymentOrder()
            } else {
                isLoadingOrder = false
                let message = response.message ?? "Failed to load order"
                state = .failed(message)
                errorMessage = message
                showError = true
            }
        } catch {
            isLoadingOrder = false
            let message = "Failed to load order: \(error.localizedDescription)"
            state = .failed(message)
            errorMessage = message
            showError = true
        }
    }

    private func verifyPayment(successData: PaymentSuccessData) async {
        state = .verifyingPayment

        do {
            let request = VerifyPaymentRequest(
                razorpayOrderId: successData.razorpayOrderId,
                razorpayPaymentId: successData.razorpayPaymentId,
                razorpaySignature: successData.razorpaySignature
            )

            let response = try await apiClient.verifyPayment(request: request)

            if response.success, let verification = response.data {
                if verification.verified {
                    state = .success(verification)
                    print("[PaymentViewModel] Payment verified successfully")

                    // Reload order to get updated status
                    if let orderId = order?.id {
                        await reloadOrder(orderId: orderId)
                    }

                    if let order = order {
                        onPaymentSuccess?(order)
                    }
                } else {
                    let message = verification.message ?? "Payment verification failed"
                    state = .failed(message)
                    errorMessage = message
                    showError = true
                    onPaymentFailure?(message)
                }
            } else {
                let message = response.message ?? "Verification failed"
                state = .failed(message)
                errorMessage = message
                showError = true
                onPaymentFailure?(message)
            }
        } catch {
            let message = "Verification error: \(error.localizedDescription)"
            state = .failed(message)
            errorMessage = message
            showError = true
            onPaymentFailure?(message)
            print("[PaymentViewModel] Error verifying payment: \(error)")
        }
    }

    private func reloadOrder(orderId: String) async {
        do {
            let response = try await apiClient.getOrder(id: orderId)
            if response.success, let order = response.data {
                self.order = order
            }
        } catch {
            print("[PaymentViewModel] Failed to reload order: \(error)")
        }
    }
}

// MARK: - PaymentManagerDelegate

extension PaymentViewModel: PaymentManagerDelegate {

    func paymentManager(_ manager: PaymentManager, didCompleteWith result: PaymentResult) {
        switch result {
        case .success(let successData):
            print("[PaymentViewModel] Payment succeeded, verifying...")
            Task {
                await verifyPayment(successData: successData)
            }

        case .failure(let failureData):
            print("[PaymentViewModel] Payment failed: \(failureData.description)")
            let message = failureData.reason ?? failureData.description
            state = .failed(message)
            errorMessage = message
            showError = true
            onPaymentFailure?(message)

        case .cancelled:
            print("[PaymentViewModel] Payment cancelled by user")
            state = .cancelled
            onPaymentCancelled?()
        }
    }
}

// MARK: - Preview Helpers

#if DEBUG
extension PaymentViewModel {

    static func preview() -> PaymentViewModel {
        let vm = PaymentViewModel()
        vm.order = Order(
            id: "preview-order-1",
            orderNumber: "PO-2024-001",
            buyerId: "buyer-1",
            buyerName: "Test Buyer",
            sellerId: "seller-1",
            sellerName: "Test Seller",
            type: .regular,
            status: .placed,
            subtotal: 1000,
            discountAmount: 0,
            gstAmount: 50,
            deliveryCharge: 50,
            totalAmount: 1100,
            platformFee: 22,
            deliveryDate: nil,
            deliverySlot: nil,
            deliveryAddress: DeliveryAddress(
                label: "Home",
                line1: "123 Test Street",
                line2: nil,
                city: "Mumbai",
                state: "Maharashtra",
                pincode: "400001",
                landmark: nil,
                latitude: nil,
                longitude: nil,
                contactName: "Test",
                contactPhone: "9876543210"
            ),
            deliveryInstructions: nil,
            items: [],
            version: 1,
            expiresAt: nil,
            cancelledAt: nil,
            cancelledBy: nil,
            cancellationReason: nil,
            createdAt: Date(),
            updatedAt: Date(),
            allowedNextStates: [],
            canCancel: true
        )
        return vm
    }

    static func previewLoading() -> PaymentViewModel {
        let vm = preview()
        vm.state = .creatingOrder
        return vm
    }

    static func previewReady() -> PaymentViewModel {
        let vm = preview()
        let paymentOrder = PaymentOrder(
            id: "po-1",
            razorpayOrderId: "order_xyz123",
            orderId: "preview-order-1",
            amount: 110000,
            currency: "INR",
            status: .created,
            receipt: "PO-2024-001",
            notes: nil,
            createdAt: Date()
        )
        vm.paymentOrder = paymentOrder
        vm.state = .readyForPayment(paymentOrder)
        return vm
    }

    static func previewSuccess() -> PaymentViewModel {
        let vm = preview()
        vm.state = .success(PaymentVerificationResponse(
            verified: true,
            paymentId: "pay_123",
            orderId: "preview-order-1",
            status: "PAID",
            message: nil
        ))
        return vm
    }

    static func previewFailed() -> PaymentViewModel {
        let vm = preview()
        vm.state = .failed("Payment was declined by your bank")
        return vm
    }
}
#endif
