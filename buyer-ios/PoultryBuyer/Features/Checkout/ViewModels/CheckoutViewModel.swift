import Foundation

@MainActor
class CheckoutViewModel: ObservableObject {
    @Published var cart: Cart? = nil
    @Published var isLoadingCart = false
    @Published var cartError: String? = nil

    // Delivery Address
    @Published var addressLine1 = ""
    @Published var addressLine2 = ""
    @Published var city = ""
    @Published var state = ""
    @Published var pincode = ""
    @Published var landmark = ""
    @Published var contactName = ""
    @Published var contactPhone = ""

    // Delivery Details
    @Published var deliveryDate: String? = nil
    @Published var deliverySlot: String? = nil
    @Published var deliveryInstructions = ""

    // Order Creation
    @Published var isCreatingOrder = false
    @Published var isPlacingOrder = false
    @Published var placedOrder: Order? = nil
    @Published var orderError: String? = nil

    // Payment Integration
    @Published var showPaymentSheet = false
    @Published var orderForPayment: Order? = nil
    @Published var paymentSuccessful = false
    @Published var paymentError: String? = nil

    // Validation
    @Published var addressErrors: [String: String] = [:]

    let deliverySlots = [
        "Morning (6 AM - 9 AM)",
        "Mid-Morning (9 AM - 12 PM)",
        "Afternoon (12 PM - 3 PM)",
        "Evening (3 PM - 6 PM)"
    ]

    private let sellerId: String

    /// Customer info for payment prefill
    var customerInfo: PaymentCustomerInfo {
        PaymentCustomerInfo(
            name: contactName.isEmpty ? nil : contactName,
            email: nil,  // Can be fetched from user profile if available
            phone: contactPhone.isEmpty ? nil : contactPhone
        )
    }

    init(sellerId: String) {
        self.sellerId = sellerId
        loadCart()
    }

    func loadCart() {
        Task {
            isLoadingCart = true
            cartError = nil

            do {
                let response = try await APIClient.shared.getCart(sellerId: sellerId)
                if response.success, let data = response.data {
                    cart = data
                } else {
                    cartError = response.message ?? "Failed to load cart"
                }
            } catch {
                cartError = "Network error. Please try again."
            }

            isLoadingCart = false
        }
    }

    func getAvailableDates() -> [String] {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"

        return (1...7).map { days in
            let date = Calendar.current.date(byAdding: .day, value: days, to: Date())!
            return formatter.string(from: date)
        }
    }

    func formatDateForDisplay(_ date: String) -> String {
        let inputFormatter = DateFormatter()
        inputFormatter.dateFormat = "yyyy-MM-dd"

        let outputFormatter = DateFormatter()
        outputFormatter.dateFormat = "EEE, MMM d"

        if let parsedDate = inputFormatter.date(from: date) {
            return outputFormatter.string(from: parsedDate)
        }
        return date
    }

    private func validateAddress() -> Bool {
        addressErrors = [:]

        if addressLine1.trimmingCharacters(in: .whitespaces).isEmpty {
            addressErrors["addressLine1"] = "Address is required"
        }
        if city.trimmingCharacters(in: .whitespaces).isEmpty {
            addressErrors["city"] = "City is required"
        }
        if state.trimmingCharacters(in: .whitespaces).isEmpty {
            addressErrors["state"] = "State is required"
        }
        if pincode.count != 6 {
            addressErrors["pincode"] = "Valid 6-digit pincode required"
        }
        if contactName.trimmingCharacters(in: .whitespaces).isEmpty {
            addressErrors["contactName"] = "Contact name is required"
        }
        if contactPhone.count != 10 {
            addressErrors["contactPhone"] = "Valid 10-digit phone required"
        }

        return addressErrors.isEmpty
    }

    func placeOrder() {
        guard validateAddress() else { return }
        guard let cart = cart else { return }

        Task {
            isCreatingOrder = true
            orderError = nil

            do {
                let deliveryAddress = DeliveryAddress(
                    label: "Delivery Address",
                    line1: addressLine1,
                    line2: addressLine2.isEmpty ? nil : addressLine2,
                    city: city,
                    state: state,
                    pincode: pincode,
                    landmark: landmark.isEmpty ? nil : landmark,
                    latitude: nil,
                    longitude: nil,
                    contactName: contactName,
                    contactPhone: contactPhone
                )

                let orderRequest = CreateOrderRequest(
                    idempotencyKey: UUID().uuidString,
                    sellerId: sellerId,
                    type: "REGULAR",
                    items: cart.items.map { item in
                        CreateOrderItemRequest(productId: item.productId, quantity: item.quantity)
                    },
                    deliveryAddress: deliveryAddress,
                    deliveryDate: deliveryDate,
                    deliverySlot: deliverySlot,
                    deliveryInstructions: deliveryInstructions.isEmpty ? nil : deliveryInstructions
                )

                let createResponse = try await APIClient.shared.createOrder(request: orderRequest)

                if createResponse.success, let createdOrder = createResponse.data {
                    isCreatingOrder = false
                    isPlacingOrder = true

                    let placeResponse = try await APIClient.shared.placeOrder(orderId: createdOrder.id)

                    if placeResponse.success, let placed = placeResponse.data {
                        // Initiate payment flow instead of completing directly
                        initiatePayment(for: placed)
                    } else {
                        orderError = placeResponse.message ?? "Failed to place order"
                    }
                } else {
                    orderError = createResponse.message ?? "Failed to create order"
                }
            } catch {
                orderError = "Network error. Please try again."
            }

            isCreatingOrder = false
            isPlacingOrder = false
        }
    }

    // MARK: - Payment Integration

    /// Initiate payment flow for an order
    private func initiatePayment(for order: Order) {
        orderForPayment = order
        showPaymentSheet = true
        print("[CheckoutViewModel] Initiating payment for order: \(order.orderNumber)")
    }

    /// Handle successful payment
    func handlePaymentSuccess(order: Order) {
        paymentSuccessful = true
        placedOrder = order
        showPaymentSheet = false
        orderForPayment = nil
        print("[CheckoutViewModel] Payment successful for order: \(order.orderNumber)")
    }

    /// Handle payment failure
    func handlePaymentFailure(message: String) {
        paymentError = message
        orderError = "Payment failed: \(message)"
        showPaymentSheet = false
        // Keep orderForPayment to allow retry
        print("[CheckoutViewModel] Payment failed: \(message)")
    }

    /// Handle payment cancellation
    func handlePaymentCancelled() {
        showPaymentSheet = false
        orderForPayment = nil
        print("[CheckoutViewModel] Payment cancelled by user")
    }

    /// Retry payment for the current order
    func retryPayment() {
        guard let order = orderForPayment else {
            orderError = "No order available for payment"
            return
        }
        showPaymentSheet = true
    }

    /// Clear payment-related errors
    func clearPaymentError() {
        paymentError = nil
    }

    func clearOrderError() {
        orderError = nil
    }
}
