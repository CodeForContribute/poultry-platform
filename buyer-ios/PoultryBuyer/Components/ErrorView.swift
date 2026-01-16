import SwiftUI

// MARK: - Error View Component

/// A reusable error view component that displays errors with appropriate styling and actions.
struct ErrorView: View {
    let error: AppError
    let onRetry: (() -> Void)?
    let onDismiss: (() -> Void)?
    let onLogin: (() -> Void)?

    init(
        error: AppError,
        onRetry: (() -> Void)? = nil,
        onDismiss: (() -> Void)? = nil,
        onLogin: (() -> Void)? = nil
    ) {
        self.error = error
        self.onRetry = onRetry
        self.onDismiss = onDismiss
        self.onLogin = onLogin
    }

    var body: some View {
        VStack(spacing: 24) {
            // Icon
            errorIcon
                .font(.system(size: 48))
                .foregroundStyle(iconColor)
                .frame(width: 96, height: 96)
                .background(iconBackgroundColor)
                .clipShape(Circle())

            // Title
            Text(error.title)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundStyle(.primary)

            // Message
            Text(error.localizedMessage)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            // Action Buttons
            actionButtons
        }
        .padding(32)
    }

    // MARK: - Subviews

    @ViewBuilder
    private var actionButtons: some View {
        VStack(spacing: 12) {
            if error.requiresAuthentication, let onLogin = onLogin {
                Button(action: onLogin) {
                    HStack {
                        Image(systemName: "person.circle")
                        Text("Login")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.accentColor)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            } else if error.isRecoverable, let onRetry = onRetry {
                Button(action: onRetry) {
                    HStack {
                        Image(systemName: "arrow.clockwise")
                        Text("Try Again")
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.accentColor)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }

            if let onDismiss = onDismiss {
                Button(action: onDismiss) {
                    Text("Dismiss")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(.systemGray6))
                        .foregroundStyle(.primary)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // MARK: - Styling

    private var errorIcon: Image {
        switch error {
        case .noConnection:
            return Image(systemName: "wifi.slash")
        case .timeout:
            return Image(systemName: "clock.badge.exclamationmark")
        case .sslError:
            return Image(systemName: "lock.slash")
        case .serverError:
            return Image(systemName: "exclamationmark.icloud")
        case .unauthorized, .sessionExpired:
            return Image(systemName: "person.fill.xmark")
        case .forbidden:
            return Image(systemName: "hand.raised.slash")
        case .notFound:
            return Image(systemName: "magnifyingglass")
        case .validationError:
            return Image(systemName: "exclamationmark.triangle")
        case .rateLimited:
            return Image(systemName: "speedometer")
        case .insufficientStock:
            return Image(systemName: "cube.box")
        case .paymentFailed:
            return Image(systemName: "creditcard.trianglebadge.exclamationmark")
        case .orderCancelled:
            return Image(systemName: "xmark.circle")
        case .minimumOrderNotMet:
            return Image(systemName: "cart.badge.minus")
        case .invalidCoupon:
            return Image(systemName: "ticket")
        case .decodingError, .unknown:
            return Image(systemName: "exclamationmark.circle")
        }
    }

    private var iconColor: Color {
        switch error.category {
        case .network:
            return .orange
        case .server:
            return .red
        case .authentication:
            return .yellow
        case .client:
            return .orange
        case .business:
            return .blue
        case .unknown:
            return .red
        }
    }

    private var iconBackgroundColor: Color {
        iconColor.opacity(0.15)
    }
}

// MARK: - Error Banner

/// A compact error banner for displaying errors inline within content.
struct ErrorBanner: View {
    let error: AppError
    let onRetry: (() -> Void)?
    let onDismiss: () -> Void

    init(
        error: AppError,
        onRetry: (() -> Void)? = nil,
        onDismiss: @escaping () -> Void
    ) {
        self.error = error
        self.onRetry = onRetry
        self.onDismiss = onDismiss
    }

    var body: some View {
        HStack(spacing: 12) {
            // Icon
            errorIcon
                .font(.system(size: 20))
                .foregroundStyle(iconColor)

            // Content
            VStack(alignment: .leading, spacing: 2) {
                Text(error.title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(iconColor)

                Text(error.localizedMessage)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }

            Spacer()

            // Actions
            if error.isRecoverable, let onRetry = onRetry {
                Button(action: onRetry) {
                    Image(systemName: "arrow.clockwise")
                        .font(.system(size: 16))
                        .foregroundStyle(iconColor)
                }
            }

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
        .background(iconColor.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var errorIcon: Image {
        switch error.category {
        case .network:
            return Image(systemName: "wifi.exclamationmark")
        case .server:
            return Image(systemName: "server.rack")
        case .authentication:
            return Image(systemName: "person.badge.key")
        case .client:
            return Image(systemName: "exclamationmark.triangle")
        case .business:
            return Image(systemName: "info.circle")
        case .unknown:
            return Image(systemName: "exclamationmark.circle")
        }
    }

    private var iconColor: Color {
        switch error.category {
        case .network: return .orange
        case .server: return .red
        case .authentication: return .yellow
        case .client: return .orange
        case .business: return .blue
        case .unknown: return .red
        }
    }
}

// MARK: - Error Alert Modifier

/// A view modifier that presents an alert for errors.
struct ErrorAlertModifier: ViewModifier {
    @Binding var error: AppError?
    let onRetry: (() -> Void)?
    let onLogin: (() -> Void)?

    func body(content: Content) -> some View {
        content
            .alert(
                error?.title ?? "Error",
                isPresented: Binding(
                    get: { error != nil },
                    set: { if !$0 { error = nil } }
                )
            ) {
                if let error = error {
                    if error.requiresAuthentication, let onLogin = onLogin {
                        Button("Login", action: onLogin)
                    } else if error.isRecoverable, let onRetry = onRetry {
                        Button("Retry", action: onRetry)
                    }
                    Button("OK", role: .cancel) {
                        self.error = nil
                    }
                }
            } message: {
                if let error = error {
                    Text(error.localizedMessage)
                }
            }
    }
}

extension View {
    /// Presents an alert when an error occurs.
    func errorAlert(
        error: Binding<AppError?>,
        onRetry: (() -> Void)? = nil,
        onLogin: (() -> Void)? = nil
    ) -> some View {
        modifier(ErrorAlertModifier(error: error, onRetry: onRetry, onLogin: onLogin))
    }
}

// MARK: - Full Screen Error View

/// A full screen error view for displaying errors that block the entire screen.
struct FullScreenErrorView: View {
    let error: AppError
    let onRetry: (() -> Void)?
    let onBack: (() -> Void)?
    let onLogin: (() -> Void)?

    var body: some View {
        VStack {
            Spacer()
            ErrorView(
                error: error,
                onRetry: onRetry,
                onDismiss: onBack,
                onLogin: onLogin
            )
            Spacer()
        }
        .background(Color(.systemBackground))
    }
}

// MARK: - Empty State View

/// A view for displaying empty states with optional error information.
struct EmptyStateView: View {
    let title: String
    let message: String
    let systemImage: String
    let actionTitle: String?
    let action: (() -> Void)?

    init(
        title: String,
        message: String,
        systemImage: String = "tray",
        actionTitle: String? = nil,
        action: (() -> Void)? = nil
    ) {
        self.title = title
        self.message = message
        self.systemImage = systemImage
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: systemImage)
                .font(.system(size: 48))
                .foregroundStyle(.secondary)

            Text(title)
                .font(.title3)
                .fontWeight(.semibold)

            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            if let actionTitle = actionTitle, let action = action {
                Button(action: action) {
                    Text(actionTitle)
                        .fontWeight(.medium)
                }
                .padding(.top, 8)
            }
        }
        .padding(32)
    }
}

// MARK: - Loading Error View

/// A view that shows either a loading indicator or an error state.
struct LoadingErrorView<Content: View>: View {
    let isLoading: Bool
    let error: AppError?
    let onRetry: (() -> Void)?
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack {
            if isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
            } else if let error = error {
                ErrorView(error: error, onRetry: onRetry)
            } else {
                content()
            }
        }
    }
}

// MARK: - Previews

#Preview("Error View - No Connection") {
    ErrorView(
        error: .noConnection,
        onRetry: {},
        onDismiss: {}
    )
}

#Preview("Error View - Session Expired") {
    ErrorView(
        error: .sessionExpired,
        onDismiss: {},
        onLogin: {}
    )
}

#Preview("Error Banner") {
    VStack {
        ErrorBanner(
            error: .noConnection,
            onRetry: {},
            onDismiss: {}
        )
        .padding()

        ErrorBanner(
            error: .serverError(statusCode: 500, message: "Internal server error"),
            onRetry: {},
            onDismiss: {}
        )
        .padding()
    }
}

#Preview("Empty State") {
    EmptyStateView(
        title: "No Orders Yet",
        message: "Your orders will appear here once you make a purchase.",
        systemImage: "bag",
        actionTitle: "Browse Products",
        action: {}
    )
}
