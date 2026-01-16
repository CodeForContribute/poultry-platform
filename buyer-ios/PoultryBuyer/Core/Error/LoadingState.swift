import Foundation

/// Generic enum representing different loading states for data operations.
/// Provides a type-safe way to handle idle, loading, success, and error states.
enum LoadingState<T: Equatable>: Equatable {
    case idle
    case loading(cached: T? = nil)
    case success(T)
    case error(AppError, cached: T? = nil)

    // MARK: - Computed Properties

    /// Check if currently loading
    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }

    /// Check if there's an error
    var isError: Bool {
        if case .error = self { return true }
        return false
    }

    /// Check if successful
    var isSuccess: Bool {
        if case .success = self { return true }
        return false
    }

    /// Check if idle
    var isIdle: Bool {
        if case .idle = self { return true }
        return false
    }

    /// Get data if available (from success or cached in error/loading states)
    var data: T? {
        switch self {
        case .idle:
            return nil
        case .loading(let cached):
            return cached
        case .success(let data):
            return data
        case .error(_, let cached):
            return cached
        }
    }

    /// Get error if available
    var error: AppError? {
        if case .error(let error, _) = self {
            return error
        }
        return nil
    }

    // MARK: - Equatable

    static func == (lhs: LoadingState<T>, rhs: LoadingState<T>) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle):
            return true
        case (.loading(let l), .loading(let r)):
            return l == r
        case (.success(let l), .success(let r)):
            return l == r
        case (.error(let le, let lc), .error(let re, let rc)):
            return le == re && lc == rc
        default:
            return false
        }
    }

    // MARK: - Transformations

    /// Map the success data to a new type
    func map<U: Equatable>(_ transform: (T) -> U) -> LoadingState<U> {
        switch self {
        case .idle:
            return .idle
        case .loading(let cached):
            return .loading(cached: cached.map(transform))
        case .success(let data):
            return .success(transform(data))
        case .error(let error, let cached):
            return .error(error, cached: cached.map(transform))
        }
    }

    /// Execute action on success
    func onSuccess(_ action: (T) -> Void) {
        if case .success(let data) = self {
            action(data)
        }
    }

    /// Execute action on error
    func onError(_ action: (AppError) -> Void) {
        if case .error(let error, _) = self {
            action(error)
        }
    }

    /// Execute action on loading
    func onLoading(_ action: () -> Void) {
        if case .loading = self {
            action()
        }
    }
}

// MARK: - Result Conversion

extension LoadingState {
    /// Creates a LoadingState from a Result
    static func from(_ result: Result<T, AppError>) -> LoadingState<T> {
        switch result {
        case .success(let data):
            return .success(data)
        case .failure(let error):
            return .error(error)
        }
    }

    /// Creates a LoadingState from an optional value and error
    static func from(data: T?, error: AppError?) -> LoadingState<T> {
        if let error = error {
            return .error(error, cached: data)
        }
        if let data = data {
            return .success(data)
        }
        return .idle
    }
}

// MARK: - Action Result

/// Result type for operations that don't return data
enum ActionResult: Equatable {
    case idle
    case loading
    case success
    case error(AppError)

    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }

    var isSuccess: Bool {
        if case .success = self { return true }
        return false
    }

    var isError: Bool {
        if case .error = self { return true }
        return false
    }

    var error: AppError? {
        if case .error(let error) = self {
            return error
        }
        return nil
    }

    static func from(_ result: Result<Void, AppError>) -> ActionResult {
        switch result {
        case .success:
            return .success
        case .failure(let error):
            return .error(error)
        }
    }
}

// MARK: - Paginated State

/// State for paginated data loading
struct PaginatedState<T: Equatable>: Equatable {
    var items: [T] = []
    var isLoading: Bool = false
    var isLoadingMore: Bool = false
    var error: AppError? = nil
    var hasMore: Bool = true
    var currentPage: Int = 0

    var isEmpty: Bool {
        items.isEmpty && !isLoading
    }

    var canLoadMore: Bool {
        hasMore && !isLoading && !isLoadingMore && error == nil
    }

    // MARK: - State Transitions

    mutating func startLoading(isRefresh: Bool = false) {
        isLoading = isRefresh || items.isEmpty
        isLoadingMore = !isRefresh && !items.isEmpty
        error = nil
    }

    mutating func success(newItems: [T], hasMore: Bool, page: Int) {
        if page == 0 {
            items = newItems
        } else {
            items.append(contentsOf: newItems)
        }
        isLoading = false
        isLoadingMore = false
        self.hasMore = hasMore
        currentPage = page
    }

    mutating func failure(_ error: AppError) {
        isLoading = false
        isLoadingMore = false
        self.error = error
    }

    mutating func clearError() {
        error = nil
    }

    mutating func reset() {
        items = []
        isLoading = false
        isLoadingMore = false
        error = nil
        hasMore = true
        currentPage = 0
    }
}

// MARK: - Async State Manager

/// A property wrapper that manages loading state for async operations
@MainActor
final class AsyncStateManager<T: Equatable>: ObservableObject {
    @Published private(set) var state: LoadingState<T> = .idle

    func load(_ operation: @escaping () async throws -> T) async {
        let cached = state.data
        state = .loading(cached: cached)

        do {
            let result = try await operation()
            state = .success(result)
        } catch {
            let appError = await ErrorHandler.shared.handle(error)
            state = .error(appError, cached: cached)
        }
    }

    func reset() {
        state = .idle
    }

    func clearError() {
        if case .error(_, let cached) = state {
            if let cached = cached {
                state = .success(cached)
            } else {
                state = .idle
            }
        }
    }
}
