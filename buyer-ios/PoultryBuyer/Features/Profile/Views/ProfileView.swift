import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var viewModel = ProfileViewModel()

    var body: some View {
        NavigationStack {
            List {
                // User Info Section
                ProfileHeaderSection(viewModel: viewModel)

                // Statistics Section
                if viewModel.stats != nil {
                    ProfileStatsSection(viewModel: viewModel)
                }

                // Account Section
                Section("Account") {
                    NavigationLink {
                        EditProfileView(viewModel: viewModel)
                    } label: {
                        Label("Edit Profile", systemImage: "person.fill")
                    }

                    NavigationLink {
                        AddressBookView(viewModel: viewModel)
                    } label: {
                        HStack {
                            Label("Address Book", systemImage: "location.fill")
                            Spacer()
                            if viewModel.addresses.count > 0 {
                                Text("\(viewModel.addresses.count)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(Color.gray.opacity(0.2))
                                    .clipShape(Capsule())
                            }
                        }
                    }
                }

                // Settings Section
                Section("Settings") {
                    NavigationLink {
                        SettingsView(viewModel: viewModel, authManager: authManager)
                    } label: {
                        Label("App Settings", systemImage: "gearshape.fill")
                    }
                }

                // Support Section
                Section("Support") {
                    NavigationLink {
                        HelpView()
                    } label: {
                        Label("Help & FAQ", systemImage: "questionmark.circle.fill")
                    }

                    Link(destination: URL(string: "mailto:support@poultryplatform.com")!) {
                        Label("Contact Support", systemImage: "envelope.fill")
                            .foregroundColor(.primary)
                    }
                }

                // About Section
                Section("About") {
                    HStack {
                        Label("Version", systemImage: "info.circle.fill")
                        Spacer()
                        Text(appVersion)
                            .foregroundColor(.secondary)
                    }
                }

                // Logout Section
                Section {
                    Button(role: .destructive) {
                        Task {
                            await authManager.logout()
                        }
                    } label: {
                        HStack {
                            Spacer()
                            Label("Sign Out", systemImage: "arrow.right.square.fill")
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Profile")
            .refreshable {
                viewModel.loadProfile()
                await viewModel.refreshAddresses()
            }
        }
    }

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }
}

// MARK: - Profile Header Section

struct ProfileHeaderSection: View {
    @ObservedObject var viewModel: ProfileViewModel

    var body: some View {
        Section {
            HStack(spacing: 16) {
                // Profile Picture
                ProfileAvatarView(
                    imageUrl: viewModel.profile?.profileImageUrl,
                    name: viewModel.profile?.displayName ?? "User"
                )

                VStack(alignment: .leading, spacing: 4) {
                    if viewModel.isLoadingProfile {
                        ProgressView()
                    } else if let profile = viewModel.profile {
                        Text(profile.displayName)
                            .font(.title2)
                            .fontWeight(.bold)

                        if let phone = profile.formattedPhone {
                            Text(phone)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }

                        if let email = profile.email, !email.isEmpty {
                            Text(email)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    } else {
                        Text("User")
                            .font(.title2)
                            .fontWeight(.bold)
                        Text("Tap to edit profile")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()
            }
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Profile Avatar View

struct ProfileAvatarView: View {
    let imageUrl: String?
    let name: String

    var body: some View {
        ZStack {
            Circle()
                .fill(LinearGradient(
                    colors: [.blue, .blue.opacity(0.7)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ))
                .frame(width: 70, height: 70)

            if let imageUrl = imageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        initialsView
                    @unknown default:
                        initialsView
                    }
                }
                .frame(width: 66, height: 66)
                .clipShape(Circle())
            } else {
                initialsView
            }
        }
    }

    private var initialsView: some View {
        Text(initials)
            .font(.title)
            .fontWeight(.bold)
            .foregroundColor(.white)
    }

    private var initials: String {
        let components = name.split(separator: " ")
        if components.count >= 2 {
            return "\(components[0].prefix(1))\(components[1].prefix(1))".uppercased()
        } else if let first = components.first {
            return String(first.prefix(2)).uppercased()
        }
        return "U"
    }
}

// MARK: - Profile Stats Section

struct ProfileStatsSection: View {
    @ObservedObject var viewModel: ProfileViewModel

    var body: some View {
        Section {
            HStack(spacing: 0) {
                StatItemView(
                    value: viewModel.formattedOrderCount,
                    label: "Orders",
                    icon: "bag.fill"
                )

                Divider()
                    .frame(height: 40)

                StatItemView(
                    value: viewModel.formattedTotalSpent,
                    label: "Total Spent",
                    icon: "indianrupeesign.circle.fill",
                    prefix: "Rs"
                )

                Divider()
                    .frame(height: 40)

                StatItemView(
                    value: "\(viewModel.addresses.count)",
                    label: "Addresses",
                    icon: "location.fill"
                )
            }
            .padding(.vertical, 8)
        }
    }
}

struct StatItemView: View {
    let value: String
    let label: String
    let icon: String
    var prefix: String? = nil

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(.blue)

            HStack(spacing: 2) {
                if let prefix = prefix {
                    Text(prefix)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                Text(value)
                    .font(.headline)
                    .fontWeight(.bold)
            }

            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Help View

struct HelpView: View {
    var body: some View {
        List {
            Section("Frequently Asked Questions") {
                FAQItemView(
                    question: "How do I track my order?",
                    answer: "Go to Orders tab and tap on any order to see its current status and tracking details."
                )

                FAQItemView(
                    question: "How do I cancel an order?",
                    answer: "You can cancel an order from the order details page if it hasn't been shipped yet."
                )

                FAQItemView(
                    question: "What payment methods are accepted?",
                    answer: "We accept Cash on Delivery (COD), UPI, and major credit/debit cards."
                )

                FAQItemView(
                    question: "How do I add a new delivery address?",
                    answer: "Go to Profile > Address Book and tap the + button to add a new address."
                )

                FAQItemView(
                    question: "What is the delivery time?",
                    answer: "Delivery times vary by location and product availability. You can see estimated delivery times during checkout."
                )
            }

            Section("Contact Us") {
                Link(destination: URL(string: "tel:+911234567890")!) {
                    Label("Call Support", systemImage: "phone.fill")
                }

                Link(destination: URL(string: "mailto:support@poultryplatform.com")!) {
                    Label("Email Support", systemImage: "envelope.fill")
                }
            }
        }
        .navigationTitle("Help & FAQ")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct FAQItemView: View {
    let question: String
    let answer: String
    @State private var isExpanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                withAnimation {
                    isExpanded.toggle()
                }
            } label: {
                HStack {
                    Text(question)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.leading)

                    Spacer()

                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .buttonStyle(.plain)

            if isExpanded {
                Text(answer)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .transition(.opacity)
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    ProfileView()
        .environmentObject(AuthManager())
}
