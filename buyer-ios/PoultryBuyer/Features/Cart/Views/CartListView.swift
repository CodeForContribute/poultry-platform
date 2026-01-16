import SwiftUI

struct CartListView: View {
    @StateObject private var viewModel = CartViewModel()
    @State private var selectedSellerId: String? = nil

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView()
                } else if viewModel.carts.isEmpty {
                    EmptyCartView()
                } else {
                    List {
                        ForEach(viewModel.carts) { cart in
                            CartSellerSection(
                                cart: cart,
                                updatingItems: viewModel.updatingItems,
                                removingItems: viewModel.removingItems,
                                onQuantityChange: { productId, quantity in
                                    viewModel.updateItemQuantity(sellerId: cart.sellerId, productId: productId, quantity: quantity)
                                },
                                onRemoveItem: { productId in
                                    viewModel.removeItem(sellerId: cart.sellerId, productId: productId)
                                },
                                onCheckout: {
                                    selectedSellerId = cart.sellerId
                                }
                            )
                        }

                        // Grand total if multiple carts
                        if viewModel.carts.count > 1 {
                            Section {
                                HStack {
                                    Text("Grand Total")
                                        .font(.headline)
                                    Spacer()
                                    PriceView(amount: viewModel.grandTotal, style: .large)
                                }
                            }
                        }
                    }
                    .refreshable {
                        await viewModel.refresh()
                    }
                }
            }
            .navigationTitle("Cart")
            .toolbar {
                if !viewModel.carts.isEmpty {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Text("\(viewModel.totalItemCount) items")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
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
            .navigationDestination(item: $selectedSellerId) { sellerId in
                CheckoutView(sellerId: sellerId)
            }
        }
    }
}

struct EmptyCartView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "cart")
                .font(.system(size: 48))
                .foregroundColor(.secondary)

            Text("Your cart is empty")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Add products to your cart to see them here")
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

struct CartSellerSection: View {
    let cart: Cart
    let updatingItems: Set<String>
    let removingItems: Set<String>
    let onQuantityChange: (String, Double) -> Void
    let onRemoveItem: (String) -> Void
    let onCheckout: () -> Void

    var body: some View {
        Section {
            // Items
            ForEach(cart.items) { item in
                CartItemRow(
                    item: item,
                    isUpdating: updatingItems.contains(item.productId),
                    isRemoving: removingItems.contains(item.productId),
                    onQuantityChange: { quantity in
                        onQuantityChange(item.productId, quantity)
                    },
                    onRemove: {
                        onRemoveItem(item.productId)
                    }
                )
            }

            // Subtotal & Checkout
            HStack {
                VStack(alignment: .leading) {
                    Text("Subtotal")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    PriceView(amount: cart.totalAmount, style: .large)
                }

                Spacer()

                Button("Checkout", action: onCheckout)
                    .buttonStyle(.borderedProminent)
            }
            .padding(.vertical, 8)

        } header: {
            HStack {
                VStack(alignment: .leading) {
                    Text(cart.sellerBusinessName ?? cart.sellerName)
                        .font(.headline)
                    Text("\(cart.itemCount) item\(cart.itemCount > 1 ? "s" : "")")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
        }
    }
}

struct CartItemRow: View {
    let item: CartItem
    let isUpdating: Bool
    let isRemoving: Bool
    let onQuantityChange: (Double) -> Void
    let onRemove: () -> Void

    @State private var quantity: Double

    init(item: CartItem, isUpdating: Bool, isRemoving: Bool, onQuantityChange: @escaping (Double) -> Void, onRemove: @escaping () -> Void) {
        self.item = item
        self.isUpdating = isUpdating
        self.isRemoving = isRemoving
        self.onQuantityChange = onQuantityChange
        self.onRemove = onRemove
        _quantity = State(initialValue: item.quantity)
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Product Info
            VStack(alignment: .leading, spacing: 4) {
                Text(item.productName)
                    .font(.subheadline)
                    .fontWeight(.medium)

                Text("SKU: \(item.productSku)")
                    .font(.caption)
                    .foregroundColor(.secondary)

                PriceView(amount: item.unitPrice, unit: item.productUnit, style: .small)
            }

            Spacer()

            // Actions
            VStack(alignment: .trailing, spacing: 8) {
                if isRemoving {
                    ProgressView()
                        .frame(width: 24, height: 24)
                } else {
                    Button {
                        onRemove()
                    } label: {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                    }
                    .buttonStyle(.plain)
                }

                if isUpdating {
                    ProgressView()
                        .frame(height: 32)
                } else {
                    QuantitySelectorView(
                        quantity: $quantity,
                        minQuantity: 1,
                        unit: item.productUnit
                    )
                    .onChange(of: quantity) { _, newValue in
                        onQuantityChange(newValue)
                    }
                }

                VStack(alignment: .trailing) {
                    Text("Total:")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    PriceView(amount: item.lineTotal)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    CartListView()
}
