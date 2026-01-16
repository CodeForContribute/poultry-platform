import Foundation

@MainActor
class ProductDetailViewModel: ObservableObject {
    @Published var product: Product? = nil
    @Published var quantity: Double = 1.0
    @Published var isLoading = false
    @Published var isAddingToCart = false
    @Published var error: String? = nil
    @Published var addedToCart = false
    @Published var cartError: String? = nil

    private let productId: String

    init(productId: String) {
        self.productId = productId
        loadProduct()
    }

    func loadProduct() {
        Task {
            isLoading = true
            error = nil

            do {
                let response = try await APIClient.shared.getProduct(id: productId)
                if response.success, let data = response.data {
                    product = data
                    quantity = data.minOrderQty
                } else {
                    error = response.message ?? "Failed to load product"
                }
            } catch {
                self.error = "Network error. Please try again."
            }

            isLoading = false
        }
    }

    func updateQuantity(_ newQuantity: Double) {
        guard let product = product else { return }

        var validatedQty = newQuantity

        if validatedQty < product.minOrderQty {
            validatedQty = product.minOrderQty
        }

        if let max = product.maxOrderQty, validatedQty > max {
            validatedQty = max
        }

        quantity = validatedQty
    }

    func addToCart() {
        guard let product = product else { return }

        Task {
            isAddingToCart = true
            cartError = nil
            addedToCart = false

            do {
                let request = AddToCartRequest(
                    productId: product.id,
                    quantity: quantity,
                    notes: nil
                )

                let response = try await APIClient.shared.addToCart(sellerId: product.sellerId, request: request)
                if response.success {
                    addedToCart = true
                } else {
                    cartError = response.message ?? "Failed to add to cart"
                }
            } catch {
                cartError = "Network error. Please try again."
            }

            isAddingToCart = false
        }
    }

    func clearCartStatus() {
        addedToCart = false
        cartError = nil
    }

    func calculateDiscountedPrice() -> Double? {
        guard let product = product,
              let price = product.currentPrice,
              let slabs = price.bulkDiscountSlabs else {
            return nil
        }

        let applicableSlab = slabs
            .filter { quantity >= $0.minQty && ($0.maxQty == nil || quantity <= $0.maxQty!) }
            .max { $0.discountPercent < $1.discountPercent }

        if let slab = applicableSlab {
            return price.basePrice * (1 - slab.discountPercent / 100)
        }

        return nil
    }

    func getLineTotal() -> Double {
        guard let product = product,
              let price = product.currentPrice else {
            return 0
        }

        let effectivePrice = calculateDiscountedPrice() ?? price.basePrice
        return effectivePrice * quantity
    }
}
