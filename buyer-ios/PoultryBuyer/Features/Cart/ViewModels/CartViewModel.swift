import Foundation

@MainActor
class CartViewModel: ObservableObject {
    @Published var carts: [Cart] = []
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var error: String? = nil
    @Published var updatingItems: Set<String> = []
    @Published var removingItems: Set<String> = []

    init() {
        loadCarts()
    }

    func loadCarts() {
        Task {
            isLoading = true
            error = nil

            do {
                let response = try await APIClient.shared.getCarts()
                if response.success, let data = response.data {
                    carts = data
                } else {
                    error = response.message ?? "Failed to load carts"
                }
            } catch {
                self.error = "Network error. Please try again."
            }

            isLoading = false
        }
    }

    func refresh() async {
        isRefreshing = true
        error = nil

        do {
            let response = try await APIClient.shared.getCarts()
            if response.success, let data = response.data {
                carts = data
            } else {
                error = response.message ?? "Failed to refresh carts"
            }
        } catch {
            self.error = "Network error. Please try again."
        }

        isRefreshing = false
    }

    func updateItemQuantity(sellerId: String, productId: String, quantity: Double) {
        updatingItems.insert(productId)

        Task {
            do {
                let request = UpdateCartItemRequest(quantity: quantity, notes: nil)
                let response = try await APIClient.shared.updateCartItem(sellerId: sellerId, productId: productId, request: request)

                if response.success, let updatedCart = response.data {
                    carts = carts.map { cart in
                        cart.sellerId == sellerId ? updatedCart : cart
                    }
                } else {
                    error = response.message ?? "Failed to update quantity"
                }
            } catch {
                self.error = "Network error. Please try again."
            }

            updatingItems.remove(productId)
        }
    }

    func removeItem(sellerId: String, productId: String) {
        removingItems.insert(productId)

        Task {
            do {
                let response = try await APIClient.shared.removeFromCart(sellerId: sellerId, productId: productId)

                if response.success {
                    carts = carts.compactMap { cart in
                        if cart.sellerId == sellerId {
                            let updatedItems = cart.items.filter { $0.productId != productId }
                            if updatedItems.isEmpty {
                                return nil
                            }
                            // We need to reload the cart to get updated totals
                            Task {
                                await refresh()
                            }
                            return cart
                        }
                        return cart
                    }
                } else {
                    error = response.message ?? "Failed to remove item"
                }
            } catch {
                self.error = "Network error. Please try again."
            }

            removingItems.remove(productId)
        }
    }

    func clearError() {
        error = nil
    }

    var totalItemCount: Int {
        carts.reduce(0) { $0 + $1.itemCount }
    }

    var grandTotal: Double {
        carts.reduce(0) { $0 + $1.totalAmount }
    }
}
