import SwiftUI

struct SettingsView: View {
    @ObservedObject var viewModel: ProfileViewModel
    @ObservedObject var authManager: AuthManager
    @Environment(\.dismiss) private var dismiss
    @State private var showDeleteAccountAlert = false
    @State private var showSignOutAlert = false

    var body: some View {
        List {
            // Notification Preferences Section
            NotificationPreferencesSection(viewModel: viewModel)

            // Legal Section
            LegalSection()

            // App Info Section
            AppInfoSection()

            // Account Actions Section
            AccountActionsSection(
                showSignOutAlert: $showSignOutAlert,
                showDeleteAccountAlert: $showDeleteAccountAlert
            )
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Sign Out", isPresented: $showSignOutAlert) {
            Button("Cancel", role: .cancel) { }
            Button("Sign Out", role: .destructive) {
                Task {
                    await authManager.logout()
                }
            }
        } message: {
            Text("Are you sure you want to sign out? You will need to verify your phone number again to sign in.")
        }
        .alert("Delete Account", isPresented: $showDeleteAccountAlert) {
            Button("Cancel", role: .cancel) { }
            Button("Delete Account", role: .destructive) {
                Task {
                    let success = await viewModel.deleteAccount()
                    if success {
                        await authManager.logout()
                    }
                }
            }
        } message: {
            Text("Are you sure you want to delete your account? This action cannot be undone. All your data, including order history and saved addresses, will be permanently deleted.")
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.deleteAccountError != nil },
            set: { if !$0 { viewModel.clearDeleteAccountError() } }
        )) {
            Button("OK") {
                viewModel.clearDeleteAccountError()
            }
        } message: {
            Text(viewModel.deleteAccountError ?? "Failed to delete account")
        }
    }
}

// MARK: - Notification Preferences Section

struct NotificationPreferencesSection: View {
    @ObservedObject var viewModel: ProfileViewModel

    var body: some View {
        Section {
            if viewModel.isLoadingPreferences {
                HStack {
                    Spacer()
                    ProgressView()
                    Spacer()
                }
            } else {
                Toggle(isOn: $viewModel.notificationPreferences.orderUpdates) {
                    NotificationToggleLabel(
                        title: "Order Updates",
                        description: "Get notified about order status changes",
                        icon: "bag.fill"
                    )
                }
                .onChange(of: viewModel.notificationPreferences.orderUpdates) { _, _ in
                    viewModel.saveNotificationPreferences()
                }

                Toggle(isOn: $viewModel.notificationPreferences.promotions) {
                    NotificationToggleLabel(
                        title: "Promotions & Offers",
                        description: "Receive special deals and discounts",
                        icon: "tag.fill"
                    )
                }
                .onChange(of: viewModel.notificationPreferences.promotions) { _, _ in
                    viewModel.saveNotificationPreferences()
                }

                Toggle(isOn: $viewModel.notificationPreferences.priceAlerts) {
                    NotificationToggleLabel(
                        title: "Price Alerts",
                        description: "Get notified when prices drop",
                        icon: "indianrupeesign.circle.fill"
                    )
                }
                .onChange(of: viewModel.notificationPreferences.priceAlerts) { _, _ in
                    viewModel.saveNotificationPreferences()
                }

                Toggle(isOn: $viewModel.notificationPreferences.newProducts) {
                    NotificationToggleLabel(
                        title: "New Products",
                        description: "Be the first to know about new products",
                        icon: "sparkles"
                    )
                }
                .onChange(of: viewModel.notificationPreferences.newProducts) { _, _ in
                    viewModel.saveNotificationPreferences()
                }
            }
        } header: {
            Text("Notifications")
        } footer: {
            if viewModel.isSavingPreferences {
                HStack(spacing: 8) {
                    ProgressView()
                        .scaleEffect(0.7)
                    Text("Saving preferences...")
                        .font(.caption)
                }
            }
        }
    }
}

struct NotificationToggleLabel: View {
    let title: String
    let description: String
    let icon: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.blue)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

// MARK: - Legal Section

struct LegalSection: View {
    var body: some View {
        Section("Legal") {
            Link(destination: URL(string: "https://poultryplatform.com/terms")!) {
                HStack {
                    Label("Terms of Service", systemImage: "doc.text.fill")
                    Spacer()
                    Image(systemName: "arrow.up.right.square")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .foregroundColor(.primary)
            }

            Link(destination: URL(string: "https://poultryplatform.com/privacy")!) {
                HStack {
                    Label("Privacy Policy", systemImage: "hand.raised.fill")
                    Spacer()
                    Image(systemName: "arrow.up.right.square")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .foregroundColor(.primary)
            }

            Link(destination: URL(string: "https://poultryplatform.com/licenses")!) {
                HStack {
                    Label("Open Source Licenses", systemImage: "chevron.left.forwardslash.chevron.right")
                    Spacer()
                    Image(systemName: "arrow.up.right.square")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .foregroundColor(.primary)
            }
        }
    }
}

// MARK: - App Info Section

struct AppInfoSection: View {
    var body: some View {
        Section("About") {
            HStack {
                Label("Version", systemImage: "info.circle.fill")
                Spacer()
                Text(appVersion)
                    .foregroundColor(.secondary)
            }

            HStack {
                Label("Build", systemImage: "hammer.fill")
                Spacer()
                Text(buildNumber)
                    .foregroundColor(.secondary)
            }

            #if DEBUG
            HStack {
                Label("Environment", systemImage: "ladybug.fill")
                Spacer()
                Text("Development")
                    .foregroundColor(.orange)
                    .font(.caption)
                    .fontWeight(.medium)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Color.orange.opacity(0.1))
                    .clipShape(Capsule())
            }
            #endif
        }
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private var buildNumber: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    }
}

// MARK: - Account Actions Section

struct AccountActionsSection: View {
    @Binding var showSignOutAlert: Bool
    @Binding var showDeleteAccountAlert: Bool

    var body: some View {
        Section {
            Button {
                showSignOutAlert = true
            } label: {
                HStack {
                    Spacer()
                    Label("Sign Out", systemImage: "arrow.right.square.fill")
                        .foregroundColor(.blue)
                    Spacer()
                }
            }
        }

        Section {
            Button(role: .destructive) {
                showDeleteAccountAlert = true
            } label: {
                HStack {
                    Spacer()
                    Label("Delete Account", systemImage: "trash.fill")
                    Spacer()
                }
            }
        } footer: {
            Text("Deleting your account will permanently remove all your data including order history, saved addresses, and payment methods.")
                .font(.caption)
        }
    }
}

#Preview {
    NavigationStack {
        SettingsView(viewModel: ProfileViewModel(), authManager: AuthManager())
    }
}
