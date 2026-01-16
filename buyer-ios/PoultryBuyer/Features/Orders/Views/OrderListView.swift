import SwiftUI

struct OrderListView: View {
    @StateObject private var viewModel = OrderListViewModel()
    @State private var selectedOrderId: String? = nil

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView()
                } else if viewModel.orders.isEmpty {
                    EmptyOrdersView()
                } else {
                    List {
                        ForEach(viewModel.orders) { order in
                            OrderRowView(order: order)
                                .onTapGesture {
                                    selectedOrderId = order.id
                                }
                                .onAppear {
                                    if order.id == viewModel.orders.last?.id {
                                        viewModel.loadMore()
                                    }
                                }
                        }

                        if viewModel.isLoadingMore {
                            HStack {
                                Spacer()
                                ProgressView()
                                Spacer()
                            }
                        }
                    }
                    .refreshable {
                        await viewModel.refresh()
                    }
                }
            }
            .navigationTitle("Orders")
            .alert("Error", isPresented: Binding(
                get: { viewModel.error != nil },
                set: { if !$0 { viewModel.clearError() } }
            )) {
                Button("OK") {
                    viewModel.clearError()
                }
            } message: {
                Text(viewModel.error ?? "An error occurred")
            }
            .navigationDestination(item: $selectedOrderId) { orderId in
                OrderDetailView(orderId: orderId)
            }
        }
    }
}

struct EmptyOrdersView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "list.bullet.rectangle")
                .font(.system(size: 48))
                .foregroundColor(.secondary)

            Text("No orders yet")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Your orders will appear here once you make a purchase")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            NavigationLink(destination: ProductSearchView()) {
                Text("Browse Products")
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

struct OrderRowView: View {
    let order: Order

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading) {
                    Text("Order #\(order.orderNumber)")
                        .font(.headline)
                    Text(formatDate(order.createdAt))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                OrderStatusBadge(status: order.status)
            }

            Divider()

            HStack {
                VStack(alignment: .leading) {
                    Text(order.sellerName)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    Text("\(order.items.count) item\(order.items.count > 1 ? "s" : "")")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text("Total")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    PriceView(amount: order.totalAmount)
                }
            }

            if let date = order.deliveryDate {
                Text("Delivery: \(formatDeliveryDate(date))" + (order.deliverySlot.map { " • \($0)" } ?? ""))
                    .font(.caption)
                    .foregroundColor(.blue)
            }
        }
        .padding(.vertical, 8)
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy 'at' h:mm a"
        return formatter.string(from: date)
    }

    private func formatDeliveryDate(_ dateString: String) -> String {
        let inputFormatter = DateFormatter()
        inputFormatter.dateFormat = "yyyy-MM-dd"

        let outputFormatter = DateFormatter()
        outputFormatter.dateFormat = "EEE, MMM d"

        if let date = inputFormatter.date(from: dateString) {
            return outputFormatter.string(from: date)
        }
        return dateString
    }
}

#Preview {
    OrderListView()
}
