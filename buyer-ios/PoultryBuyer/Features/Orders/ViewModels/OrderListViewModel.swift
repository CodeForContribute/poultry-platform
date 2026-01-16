import Foundation

@MainActor
class OrderListViewModel: ObservableObject {
    @Published var orders: [Order] = []
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var isLoadingMore = false
    @Published var error: String? = nil
    @Published var hasMore = true

    private var currentPage = 0
    private let pageSize = 20

    init() {
        loadOrders()
    }

    func loadOrders() {
        Task {
            isLoading = true
            error = nil

            do {
                let response = try await APIClient.shared.getOrders(page: 0, size: pageSize)
                if response.success, let data = response.data {
                    orders = data.content
                    currentPage = 0
                    hasMore = !data.last
                } else {
                    error = response.message ?? "Failed to load orders"
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
            let response = try await APIClient.shared.getOrders(page: 0, size: pageSize)
            if response.success, let data = response.data {
                orders = data.content
                currentPage = 0
                hasMore = !data.last
            } else {
                error = response.message ?? "Failed to refresh orders"
            }
        } catch {
            self.error = "Network error. Please try again."
        }

        isRefreshing = false
    }

    func loadMore() {
        guard !isLoading, !isLoadingMore, hasMore else { return }

        Task {
            isLoadingMore = true
            let nextPage = currentPage + 1

            do {
                let response = try await APIClient.shared.getOrders(page: nextPage, size: pageSize)
                if response.success, let data = response.data {
                    orders.append(contentsOf: data.content)
                    currentPage = nextPage
                    hasMore = !data.last
                } else {
                    error = response.message ?? "Failed to load more orders"
                }
            } catch {
                self.error = "Network error. Please try again."
            }

            isLoadingMore = false
        }
    }

    func clearError() {
        error = nil
    }
}
