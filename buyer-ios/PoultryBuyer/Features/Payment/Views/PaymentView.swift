//
//  PaymentView.swift
//  PoultryBuyer
//
//  SwiftUI view for payment checkout flow
//

import SwiftUI

// MARK: - Payment View

struct PaymentView: View {
    @StateObject private var viewModel: PaymentViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showCancelAlert = false

    private let order: Order
    private let customerInfo: PaymentCustomerInfo?
    private var onSuccess: ((Order) -> Void)?
    private var onFailure: ((String) -> Void)?

    init(
        order: Order,
        customerInfo: PaymentCustomerInfo? = nil,
        onSuccess: ((Order) -> Void)? = nil,
        onFailure: ((String) -> Void)? = nil
    ) {
        self.order = order
        self.customerInfo = customerInfo
        self.onSuccess = onSuccess
        self.onFailure = onFailure
        _viewModel = StateObject(wrappedValue: PaymentViewModel())
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                ScrollView {
                    VStack(spacing: 20) {
                        // Order Summary Card
                        PaymentOrderSummaryCard(order: viewModel.order ?? order)

                        // Payment Amount Card
                        PaymentAmountCard(order: viewModel.order ?? order)

                        // Payment Status
                        PaymentStatusCard(state: viewModel.state)

                        // Security Note
                        SecurityNoteView()
                    }
                    .padding()
                }

                // Bottom Payment Button
                PaymentBottomBar(
                    state: viewModel.state,
                    amount: order.totalAmount,
                    onPay: startPayment,
                    onRetry: viewModel.retryPayment
                )
            }
            .navigationTitle("Payment")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        if viewModel.state.isLoading {
                            showCancelAlert = true
                        } else {
                            dismiss()
                        }
                    }
                }
            }
            .alert("Cancel Payment?", isPresented: $showCancelAlert) {
                Button("Continue Payment", role: .cancel) {}
                Button("Cancel", role: .destructive) {
                    viewModel.cancelPayment()
                    dismiss()
                }
            } message: {
                Text("Are you sure you want to cancel this payment?")
            }
            .alert("Error", isPresented: $viewModel.showError) {
                Button("OK") {
                    viewModel.clearError()
                }
            } message: {
                Text(viewModel.errorMessage ?? "An error occurred")
            }
            .onAppear {
                setupCallbacks()
                viewModel.initializePayment(for: order, customerInfo: customerInfo)
            }
            .onChange(of: viewModel.state) { _, newState in
                handleStateChange(newState)
            }
        }
    }

    private func startPayment() {
        // Get the root view controller to present Razorpay checkout
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else {
            print("[PaymentView] Cannot find root view controller")
            return
        }

        // Find the topmost presented view controller
        var topVC = rootVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        viewModel.startPayment(presentingController: topVC)
    }

    private func setupCallbacks() {
        viewModel.onPaymentSuccess = { order in
            onSuccess?(order)
        }
        viewModel.onPaymentFailure = { message in
            onFailure?(message)
        }
        viewModel.onPaymentCancelled = {
            dismiss()
        }
    }

    private func handleStateChange(_ state: PaymentState) {
        switch state {
        case .success:
            // Auto-dismiss after success animation
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                dismiss()
            }
        default:
            break
        }
    }
}

// MARK: - Order Summary Card

struct PaymentOrderSummaryCard: View {
    let order: Order

    var body: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "doc.text.fill")
                        .foregroundColor(.blue)
                    Text("Order Summary")
                        .font(.headline)
                    Spacer()
                    Text(order.orderNumber)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Divider()

                VStack(spacing: 8) {
                    SummaryRow(label: "Seller", value: order.sellerName)
                    SummaryRow(label: "Items", value: "\(order.items.count) items")
                    SummaryRow(label: "Status", value: order.status.displayName)
                }
            }
        }
    }
}

// MARK: - Payment Amount Card

struct PaymentAmountCard: View {
    let order: Order

    var body: some View {
        GroupBox {
            VStack(spacing: 12) {
                HStack {
                    Image(systemName: "indianrupeesign.circle.fill")
                        .foregroundColor(.green)
                    Text("Payment Details")
                        .font(.headline)
                    Spacer()
                }

                Divider()

                VStack(spacing: 8) {
                    AmountRow(label: "Subtotal", amount: order.subtotal)

                    if order.discountAmount > 0 {
                        AmountRow(label: "Discount", amount: -order.discountAmount, isDiscount: true)
                    }

                    AmountRow(label: "GST", amount: order.gstAmount)

                    if order.deliveryCharge > 0 {
                        AmountRow(label: "Delivery", amount: order.deliveryCharge)
                    }

                    Divider()

                    HStack {
                        Text("Total Amount")
                            .font(.headline)
                        Spacer()
                        Text(formatPrice(order.totalAmount))
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                    }
                }
            }
        }
    }

    private func formatPrice(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "INR"
        formatter.currencySymbol = "Rs. "
        formatter.maximumFractionDigits = 2
        return formatter.string(from: NSNumber(value: amount)) ?? "Rs. \(String(format: "%.2f", amount))"
    }
}

// MARK: - Payment Status Card

struct PaymentStatusCard: View {
    let state: PaymentState

    var body: some View {
        GroupBox {
            VStack(spacing: 16) {
                statusIcon

                Text(state.statusMessage)
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .foregroundColor(statusColor)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
        }
    }

    @ViewBuilder
    private var statusIcon: some View {
        switch state {
        case .idle:
            Image(systemName: "creditcard.fill")
                .font(.largeTitle)
                .foregroundColor(.blue)

        case .creatingOrder, .processingPayment, .verifyingPayment:
            ProgressView()
                .scaleEffect(1.5)

        case .readyForPayment:
            Image(systemName: "hand.tap.fill")
                .font(.largeTitle)
                .foregroundColor(.blue)

        case .success:
            Image(systemName: "checkmark.circle.fill")
                .font(.largeTitle)
                .foregroundColor(.green)

        case .failed:
            Image(systemName: "xmark.circle.fill")
                .font(.largeTitle)
                .foregroundColor(.red)

        case .cancelled:
            Image(systemName: "xmark.circle.fill")
                .font(.largeTitle)
                .foregroundColor(.orange)
        }
    }

    private var statusColor: Color {
        switch state {
        case .success:
            return .green
        case .failed:
            return .red
        case .cancelled:
            return .orange
        default:
            return .secondary
        }
    }
}

// MARK: - Security Note

struct SecurityNoteView: View {
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "lock.shield.fill")
                .foregroundColor(.green)

            VStack(alignment: .leading, spacing: 2) {
                Text("Secure Payment")
                    .font(.caption)
                    .fontWeight(.medium)
                Text("Payments are processed securely via Razorpay")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding()
        .background(Color.green.opacity(0.1))
        .cornerRadius(8)
    }
}

// MARK: - Bottom Payment Bar

struct PaymentBottomBar: View {
    let state: PaymentState
    let amount: Double
    let onPay: () -> Void
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Divider()

            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Amount to Pay")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text(formatPrice(amount))
                        .font(.title3)
                        .fontWeight(.bold)
                }

                Spacer()

                actionButton
            }
            .padding()
            .background(.regularMaterial)
        }
    }

    @ViewBuilder
    private var actionButton: some View {
        switch state {
        case .idle, .creatingOrder:
            Button(action: {}) {
                HStack {
                    ProgressView()
                        .tint(.white)
                    Text("Preparing...")
                }
                .frame(minWidth: 140)
            }
            .buttonStyle(.borderedProminent)
            .disabled(true)

        case .readyForPayment:
            Button(action: onPay) {
                HStack {
                    Image(systemName: "creditcard.fill")
                    Text("Pay Now")
                }
                .frame(minWidth: 140)
            }
            .buttonStyle(.borderedProminent)

        case .processingPayment, .verifyingPayment:
            Button(action: {}) {
                HStack {
                    ProgressView()
                        .tint(.white)
                    Text(state == .processingPayment ? "Processing..." : "Verifying...")
                }
                .frame(minWidth: 140)
            }
            .buttonStyle(.borderedProminent)
            .disabled(true)

        case .success:
            Button(action: {}) {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                    Text("Paid")
                }
                .frame(minWidth: 140)
            }
            .buttonStyle(.borderedProminent)
            .tint(.green)
            .disabled(true)

        case .failed:
            Button(action: onRetry) {
                HStack {
                    Image(systemName: "arrow.clockwise")
                    Text("Retry Payment")
                }
                .frame(minWidth: 140)
            }
            .buttonStyle(.borderedProminent)
            .tint(.orange)

        case .cancelled:
            Button(action: onRetry) {
                HStack {
                    Image(systemName: "arrow.clockwise")
                    Text("Try Again")
                }
                .frame(minWidth: 140)
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private func formatPrice(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "INR"
        formatter.currencySymbol = "Rs. "
        formatter.maximumFractionDigits = 2
        return formatter.string(from: NSNumber(value: amount)) ?? "Rs. \(String(format: "%.2f", amount))"
    }
}

// MARK: - Helper Views

struct SummaryRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
        }
        .font(.subheadline)
    }
}

struct AmountRow: View {
    let label: String
    let amount: Double
    var isDiscount: Bool = false

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(formatAmount())
                .foregroundColor(isDiscount ? .green : .primary)
        }
        .font(.subheadline)
    }

    private func formatAmount() -> String {
        let prefix = isDiscount ? "-" : ""
        let absAmount = abs(amount)
        return "\(prefix)Rs. \(String(format: "%.2f", absAmount))"
    }
}

// MARK: - Payment Sheet Modifier

extension View {
    /// Present a payment sheet for an order
    func paymentSheet(
        order: Binding<Order?>,
        customerInfo: PaymentCustomerInfo? = nil,
        onSuccess: ((Order) -> Void)? = nil,
        onFailure: ((String) -> Void)? = nil
    ) -> some View {
        self.sheet(item: order) { orderValue in
            PaymentView(
                order: orderValue,
                customerInfo: customerInfo,
                onSuccess: onSuccess,
                onFailure: onFailure
            )
        }
    }
}

// MARK: - Order Extension for Identifiable

extension Order: Hashable {
    static func == (lhs: Order, rhs: Order) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

// MARK: - Previews

#Preview("Idle State") {
    PaymentView(
        order: Order(
            id: "preview-1",
            orderNumber: "PO-2024-001",
            buyerId: "buyer-1",
            buyerName: "Test Buyer",
            sellerId: "seller-1",
            sellerName: "Test Poultry Farm",
            type: .regular,
            status: .placed,
            subtotal: 1000,
            discountAmount: 50,
            gstAmount: 47.50,
            deliveryCharge: 50,
            totalAmount: 1047.50,
            platformFee: 21,
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
                contactName: "Test User",
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
    )
}

#Preview("Payment Status Card - Success") {
    VStack {
        PaymentStatusCard(state: .success(PaymentVerificationResponse(
            verified: true,
            paymentId: "pay_123",
            orderId: "order_123",
            status: "PAID",
            message: nil
        )))
    }
    .padding()
}

#Preview("Payment Status Card - Failed") {
    VStack {
        PaymentStatusCard(state: .failed("Payment was declined"))
    }
    .padding()
}
