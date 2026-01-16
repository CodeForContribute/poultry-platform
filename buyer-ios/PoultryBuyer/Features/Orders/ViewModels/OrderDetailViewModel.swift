import Foundation

@MainActor
class OrderDetailViewModel: ObservableObject {
    @Published var order: Order? = nil
    @Published var isLoading = false
    @Published var error: String? = nil
    @Published var isCancelling = false
    @Published var cancelSuccess = false
    @Published var cancelError: String? = nil

    private let orderId: String

    init(orderId: String) {
        self.orderId = orderId
        loadOrder()
    }

    func loadOrder() {
        Task {
            isLoading = true
            error = nil

            do {
                let response = try await APIClient.shared.getOrder(id: orderId)
                if response.success, let data = response.data {
                    order = data
                } else {
                    error = response.message ?? "Failed to load order"
                }
            } catch {
                self.error = "Network error. Please try again."
            }

            isLoading = false
        }
    }

    func cancelOrder(reason: String?) {
        guard let order = order, order.canCancel else { return }

        Task {
            isCancelling = true
            cancelError = nil
            cancelSuccess = false

            do {
                let response = try await APIClient.shared.cancelOrder(orderId: orderId, reason: reason)
                if response.success, let data = response.data {
                    self.order = data
                    cancelSuccess = true
                } else {
                    cancelError = response.message ?? "Failed to cancel order"
                }
            } catch {
                cancelError = "Network error. Please try again."
            }

            isCancelling = false
        }
    }

    func clearCancelStatus() {
        cancelSuccess = false
        cancelError = nil
    }

    func refresh() {
        loadOrder()
    }
}
