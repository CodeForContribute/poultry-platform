import SwiftUI

struct CheckoutView: View {
    @StateObject private var viewModel: CheckoutViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var navigateToOrder = false

    init(sellerId: String) {
        _viewModel = StateObject(wrappedValue: CheckoutViewModel(sellerId: sellerId))
    }

    var body: some View {
        Group {
            if viewModel.isLoadingCart {
                ProgressView()
            } else if let error = viewModel.cartError {
                VStack(spacing: 16) {
                    Text(error)
                        .foregroundColor(.secondary)
                    Button("Retry") {
                        viewModel.loadCart()
                    }
                    .buttonStyle(.bordered)
                }
            } else if let cart = viewModel.cart {
                ScrollView {
                    VStack(spacing: 16) {
                        // Order Summary
                        OrderSummaryCard(cart: cart)

                        // Delivery Address
                        DeliveryAddressForm(viewModel: viewModel)

                        // Delivery Schedule
                        DeliveryScheduleForm(viewModel: viewModel)

                        Spacer(minLength: 100)
                    }
                    .padding()
                }
                .safeAreaInset(edge: .bottom) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Total")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            PriceView(amount: cart.totalAmount, style: .large)
                        }

                        Spacer()

                        Button {
                            viewModel.placeOrder()
                        } label: {
                            HStack {
                                if viewModel.isCreatingOrder || viewModel.isPlacingOrder {
                                    ProgressView()
                                        .tint(.white)
                                    Text(viewModel.isCreatingOrder ? "Creating..." : "Placing...")
                                } else {
                                    Image(systemName: "creditcard.fill")
                                    Text("Place Order & Pay")
                                }
                            }
                            .frame(minWidth: 160)
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(viewModel.isCreatingOrder || viewModel.isPlacingOrder)
                    }
                    .padding()
                    .background(.regularMaterial)
                }
            }
        }
        .navigationTitle("Checkout")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Error", isPresented: Binding(
            get: { viewModel.orderError != nil },
            set: { if !$0 { viewModel.clearOrderError() } }
        )) {
            Button("OK") {
                viewModel.clearOrderError()
            }
            // Show retry button if payment failed but order exists
            if viewModel.orderForPayment != nil {
                Button("Retry Payment") {
                    viewModel.retryPayment()
                }
            }
        } message: {
            Text(viewModel.orderError ?? "An error occurred")
        }
        .onChange(of: viewModel.placedOrder) { _, order in
            if order != nil && viewModel.paymentSuccessful {
                navigateToOrder = true
            }
        }
        .navigationDestination(isPresented: $navigateToOrder) {
            if let order = viewModel.placedOrder {
                OrderDetailView(orderId: order.id)
            }
        }
        // Payment Sheet
        .sheet(isPresented: $viewModel.showPaymentSheet) {
            if let order = viewModel.orderForPayment {
                PaymentView(
                    order: order,
                    customerInfo: viewModel.customerInfo,
                    onSuccess: { updatedOrder in
                        viewModel.handlePaymentSuccess(order: updatedOrder)
                    },
                    onFailure: { message in
                        viewModel.handlePaymentFailure(message: message)
                    }
                )
                .interactiveDismissDisabled()
            }
        }
    }
}

struct OrderSummaryCard: View {
    let cart: Cart

    var body: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Order Summary")
                        .font(.headline)
                    Spacer()
                }

                Text("From \(cart.sellerBusinessName ?? cart.sellerName)")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Divider()

                ForEach(cart.items) { item in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(item.productName)
                                .font(.subheadline)
                            Text("\(String(format: "%.0f", item.quantity)) \(item.productUnit.lowercased()) x ₹\(String(format: "%.2f", item.unitPrice))")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        PriceView(amount: item.lineTotal, style: .small)
                    }
                }

                Divider()

                HStack {
                    Text("Subtotal (\(cart.itemCount) items)")
                        .fontWeight(.medium)
                    Spacer()
                    PriceView(amount: cart.totalAmount)
                }
            }
        }
    }
}

struct DeliveryAddressForm: View {
    @ObservedObject var viewModel: CheckoutViewModel

    var body: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 16) {
                Text("Delivery Address")
                    .font(.headline)

                VStack(spacing: 12) {
                    FormTextField(
                        label: "Address Line 1 *",
                        placeholder: "House/Building No., Street",
                        text: $viewModel.addressLine1,
                        error: viewModel.addressErrors["addressLine1"]
                    )

                    FormTextField(
                        label: "Address Line 2",
                        placeholder: "Area, Colony (optional)",
                        text: $viewModel.addressLine2
                    )

                    HStack(spacing: 12) {
                        FormTextField(
                            label: "City *",
                            placeholder: "",
                            text: $viewModel.city,
                            error: viewModel.addressErrors["city"]
                        )

                        FormTextField(
                            label: "State *",
                            placeholder: "",
                            text: $viewModel.state,
                            error: viewModel.addressErrors["state"]
                        )
                    }

                    HStack(spacing: 12) {
                        FormTextField(
                            label: "Pincode *",
                            placeholder: "",
                            text: $viewModel.pincode,
                            error: viewModel.addressErrors["pincode"],
                            keyboardType: .numberPad
                        )
                        .onChange(of: viewModel.pincode) { _, newValue in
                            viewModel.pincode = String(newValue.filter { $0.isNumber }.prefix(6))
                        }

                        FormTextField(
                            label: "Landmark",
                            placeholder: "",
                            text: $viewModel.landmark
                        )
                    }

                    Divider()

                    Text("Contact Details")
                        .font(.subheadline)
                        .fontWeight(.medium)

                    HStack(spacing: 12) {
                        FormTextField(
                            label: "Contact Name *",
                            placeholder: "",
                            text: $viewModel.contactName,
                            error: viewModel.addressErrors["contactName"]
                        )

                        FormTextField(
                            label: "Phone *",
                            placeholder: "",
                            text: $viewModel.contactPhone,
                            error: viewModel.addressErrors["contactPhone"],
                            keyboardType: .phonePad
                        )
                        .onChange(of: viewModel.contactPhone) { _, newValue in
                            viewModel.contactPhone = String(newValue.filter { $0.isNumber }.prefix(10))
                        }
                    }
                }
            }
        }
    }
}

struct FormTextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var error: String? = nil
    var keyboardType: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            TextField(placeholder, text: $text)
                .textFieldStyle(.roundedBorder)
                .keyboardType(keyboardType)

            if let error = error {
                Text(error)
                    .font(.caption2)
                    .foregroundColor(.red)
            }
        }
    }
}

struct DeliveryScheduleForm: View {
    @ObservedObject var viewModel: CheckoutViewModel

    var body: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 16) {
                Text("Delivery Schedule")
                    .font(.headline)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Preferred Delivery Date")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(viewModel.getAvailableDates(), id: \.self) { date in
                                Button {
                                    if viewModel.deliveryDate == date {
                                        viewModel.deliveryDate = nil
                                    } else {
                                        viewModel.deliveryDate = date
                                    }
                                } label: {
                                    Text(viewModel.formatDateForDisplay(date))
                                        .font(.caption)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .background(viewModel.deliveryDate == date ? Color.blue : Color.gray.opacity(0.2))
                                        .foregroundColor(viewModel.deliveryDate == date ? .white : .primary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Preferred Time Slot")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(viewModel.deliverySlots, id: \.self) { slot in
                                Button {
                                    if viewModel.deliverySlot == slot {
                                        viewModel.deliverySlot = nil
                                    } else {
                                        viewModel.deliverySlot = slot
                                    }
                                } label: {
                                    Text(slot)
                                        .font(.caption)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 8)
                                        .background(viewModel.deliverySlot == slot ? Color.blue : Color.gray.opacity(0.2))
                                        .foregroundColor(viewModel.deliverySlot == slot ? .white : .primary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text("Delivery Instructions")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    TextField("Any special instructions for delivery", text: $viewModel.deliveryInstructions, axis: .vertical)
                        .textFieldStyle(.roundedBorder)
                        .lineLimit(2...4)
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        CheckoutView(sellerId: "test-seller")
    }
}
