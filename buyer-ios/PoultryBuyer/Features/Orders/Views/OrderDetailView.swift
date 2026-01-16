import SwiftUI

struct OrderDetailView: View {
    @StateObject private var viewModel: OrderDetailViewModel
    @State private var showCancelDialog = false
    @State private var cancelReason = ""

    init(orderId: String) {
        _viewModel = StateObject(wrappedValue: OrderDetailViewModel(orderId: orderId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
            } else if let error = viewModel.error {
                VStack(spacing: 16) {
                    Text(error)
                        .foregroundColor(.secondary)
                    Button("Retry") {
                        viewModel.loadOrder()
                    }
                    .buttonStyle(.bordered)
                }
            } else if let order = viewModel.order {
                ScrollView {
                    VStack(spacing: 16) {
                        // Status Card
                        OrderStatusCard(order: order)

                        // Delivery Info
                        DeliveryInfoCard(order: order)

                        // Items
                        OrderItemsCard(order: order)

                        // Price Summary
                        PriceSummaryCard(order: order)

                        // Cancellation Info
                        if order.status == .cancelledByBuyer || order.status == .cancelledBySeller {
                            CancellationInfoCard(order: order)
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("Order #\(viewModel.order?.orderNumber ?? "")")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let order = viewModel.order, order.canCancel {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showCancelDialog = true
                    } label: {
                        if viewModel.isCancelling {
                            ProgressView()
                        } else {
                            Image(systemName: "xmark.circle")
                                .foregroundColor(.red)
                        }
                    }
                    .disabled(viewModel.isCancelling)
                }
            }
        }
        .alert("Cancel Order", isPresented: $showCancelDialog) {
            TextField("Reason (optional)", text: $cancelReason)
            Button("Keep Order", role: .cancel) {
                cancelReason = ""
            }
            Button("Cancel Order", role: .destructive) {
                viewModel.cancelOrder(reason: cancelReason.isEmpty ? nil : cancelReason)
                cancelReason = ""
            }
        } message: {
            Text("Are you sure you want to cancel this order?")
        }
        .alert("Order Cancelled", isPresented: Binding(
            get: { viewModel.cancelSuccess },
            set: { if !$0 { viewModel.clearCancelStatus() } }
        )) {
            Button("OK") {
                viewModel.clearCancelStatus()
            }
        } message: {
            Text("Your order has been cancelled successfully.")
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.cancelError != nil },
            set: { if !$0 { viewModel.clearCancelStatus() } }
        )) {
            Button("OK") {
                viewModel.clearCancelStatus()
            }
        } message: {
            Text(viewModel.cancelError ?? "An error occurred")
        }
    }
}

struct OrderStatusCard: View {
    let order: Order

    var body: some View {
        GroupBox {
            HStack {
                VStack(alignment: .leading) {
                    Text("Order Status")
                        .font(.headline)
                    Text("Seller: \(order.sellerName)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                OrderStatusBadge(status: order.status)
            }
        }
    }
}

struct DeliveryInfoCard: View {
    let order: Order

    var body: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "shippingbox.fill")
                        .foregroundColor(.blue)
                    Text("Delivery Details")
                        .font(.headline)
                }

                if let date = order.deliveryDate {
                    Text("Date: \(formatDeliveryDate(date))")
                        .font(.subheadline)
                }
                if let slot = order.deliverySlot {
                    Text("Time: \(slot)")
                        .font(.subheadline)
                }

                Divider()

                HStack(alignment: .top) {
                    Image(systemName: "location.fill")
                        .foregroundColor(.blue)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(order.deliveryAddress.contactName)
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Text(order.deliveryAddress.contactPhone)
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text(formatAddress(order.deliveryAddress))
                            .font(.caption)
                    }
                }

                if let instructions = order.deliveryInstructions {
                    Text("Instructions: \(instructions)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
    }

    private func formatDeliveryDate(_ dateString: String) -> String {
        let inputFormatter = DateFormatter()
        inputFormatter.dateFormat = "yyyy-MM-dd"

        let outputFormatter = DateFormatter()
        outputFormatter.dateFormat = "EEEE, MMMM d, yyyy"

        if let date = inputFormatter.date(from: dateString) {
            return outputFormatter.string(from: date)
        }
        return dateString
    }

    private func formatAddress(_ addr: DeliveryAddress) -> String {
        var lines = [addr.line1]
        if let line2 = addr.line2 { lines.append(line2) }
        lines.append("\(addr.city), \(addr.state) - \(addr.pincode)")
        if let landmark = addr.landmark { lines.append("Landmark: \(landmark)") }
        return lines.joined(separator: "\n")
    }
}

struct OrderItemsCard: View {
    let order: Order

    var body: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 12) {
                Text("Items (\(order.items.count))")
                    .font(.headline)

                ForEach(order.items) { item in
                    VStack(spacing: 8) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.productName)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                Text("SKU: \(item.productSku)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text("\(String(format: "%.0f", item.quantity)) x ₹\(String(format: "%.2f", item.unitPrice))")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                if let discount = item.discountPercent, discount > 0 {
                                    Text("\(Int(discount))% discount applied")
                                        .font(.caption)
                                        .foregroundColor(.blue)
                                }
                            }
                            Spacer()
                            VStack(alignment: .trailing) {
                                PriceView(amount: item.lineTotal)
                                if let delivered = item.deliveredQuantity {
                                    Text("Delivered: \(String(format: "%.0f", delivered))")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }

                        if item.id != order.items.last?.id {
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

struct PriceSummaryCard: View {
    let order: Order

    var body: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 8) {
                Text("Price Details")
                    .font(.headline)

                PriceRow(label: "Subtotal", amount: order.subtotal)

                if order.discountAmount > 0 {
                    PriceRow(label: "Discount", amount: -order.discountAmount, isDiscount: true)
                }

                PriceRow(label: "GST", amount: order.gstAmount)

                if order.deliveryCharge > 0 {
                    PriceRow(label: "Delivery Charge", amount: order.deliveryCharge)
                }

                Divider()

                HStack {
                    Text("Total")
                        .font(.headline)
                    Spacer()
                    Text("₹\(String(format: "%.2f", order.totalAmount))")
                        .font(.headline)
                        .foregroundColor(.blue)
                }
            }
        }
    }
}

struct PriceRow: View {
    let label: String
    let amount: Double
    var isDiscount: Bool = false

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(isDiscount ? "-₹\(String(format: "%.2f", abs(amount)))" : "₹\(String(format: "%.2f", amount))")
                .foregroundColor(isDiscount ? .blue : .primary)
        }
        .font(.subheadline)
    }
}

struct CancellationInfoCard: View {
    let order: Order

    var body: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 8) {
                Text("Cancellation Details")
                    .font(.headline)
                    .foregroundColor(.red)

                if let cancelledAt = order.cancelledAt {
                    Text("Cancelled on: \(formatDate(cancelledAt))")
                        .font(.caption)
                }

                if let cancelledBy = order.cancelledBy {
                    Text("Cancelled by: \(cancelledBy.replacingOccurrences(of: "_", with: " ").capitalized)")
                        .font(.caption)
                }

                if let reason = order.cancellationReason {
                    Text("Reason: \(reason)")
                        .font(.caption)
                }
            }
        }
        .backgroundStyle(Color.red.opacity(0.1))
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy 'at' h:mm a"
        return formatter.string(from: date)
    }
}

#Preview {
    NavigationStack {
        OrderDetailView(orderId: "test-order")
    }
}
