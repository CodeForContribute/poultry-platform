import Foundation
import Combine

@MainActor
class SearchViewModel: ObservableObject {
    @Published var query: String = ""
    @Published var products: [Product] = []
    @Published var categories: [Category] = []
    @Published var selectedCategoryId: String? = nil
    @Published var isLoading = false
    @Published var isLoadingMore = false
    @Published var error: String? = nil
    @Published var hasMore = true

    private var currentPage = 0
    private let pageSize = 20
    private var searchTask: Task<Void, Never>?

    init() {
        loadCategories()
    }

    func loadCategories() {
        Task {
            do {
                let response = try await APIClient.shared.getCategories()
                if response.success, let data = response.data {
                    categories = data
                }
            } catch {
                // Categories are optional, don't show error
            }
        }
    }

    func onQueryChange(_ newQuery: String) {
        query = newQuery

        // Debounce search
        searchTask?.cancel()
        searchTask = Task {
            try? await Task.sleep(nanoseconds: 300_000_000) // 300ms debounce

            if Task.isCancelled { return }

            if !query.isEmpty {
                await searchProducts(resetPage: true)
            } else if selectedCategoryId != nil {
                await loadProductsByCategory(resetPage: true)
            } else {
                products = []
            }
        }
    }

    func onCategorySelected(_ categoryId: String?) {
        selectedCategoryId = categoryId
        query = ""

        if let categoryId = categoryId {
            Task {
                await loadProductsByCategory(resetPage: true)
            }
        } else {
            products = []
        }
    }

    func loadMore() {
        guard !isLoading, !isLoadingMore, hasMore else { return }

        Task {
            if !query.isEmpty {
                await searchProducts(resetPage: false)
            } else if selectedCategoryId != nil {
                await loadProductsByCategory(resetPage: false)
            }
        }
    }

    func refresh() async {
        if !query.isEmpty {
            await searchProducts(resetPage: true)
        } else if selectedCategoryId != nil {
            await loadProductsByCategory(resetPage: true)
        }
    }

    private func searchProducts(resetPage: Bool) async {
        let page = resetPage ? 0 : currentPage + 1

        if resetPage {
            isLoading = true
        } else {
            isLoadingMore = true
        }
        error = nil

        do {
            let response = try await APIClient.shared.searchProducts(query: query, page: page, size: pageSize)
            if response.success, let data = response.data {
                if resetPage {
                    products = data.content
                } else {
                    products.append(contentsOf: data.content)
                }
                currentPage = page
                hasMore = !data.last
            } else {
                error = response.message ?? "Search failed"
            }
        } catch {
            self.error = "Network error. Please try again."
        }

        isLoading = false
        isLoadingMore = false
    }

    private func loadProductsByCategory(resetPage: Bool) async {
        guard let categoryId = selectedCategoryId else { return }

        let page = resetPage ? 0 : currentPage + 1

        if resetPage {
            isLoading = true
        } else {
            isLoadingMore = true
        }
        error = nil

        do {
            let response = try await APIClient.shared.getProductsByCategory(categoryId: categoryId, page: page, size: pageSize)
            if response.success, let data = response.data {
                if resetPage {
                    products = data.content
                } else {
                    products.append(contentsOf: data.content)
                }
                currentPage = page
                hasMore = !data.last
            } else {
                error = response.message ?? "Failed to load products"
            }
        } catch {
            self.error = "Network error. Please try again."
        }

        isLoading = false
        isLoadingMore = false
    }

    func clearError() {
        error = nil
    }
}
