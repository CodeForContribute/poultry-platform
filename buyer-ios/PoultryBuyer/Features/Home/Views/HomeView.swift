import SwiftUI

struct HomeView: View {
    @State private var categories: [Category] = []
    @State private var searchText = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Search bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                        TextField("Search products...", text: $searchText)
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(12)
                    .padding(.horizontal)

                    // Categories
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Categories")
                            .font(.headline)
                            .padding(.horizontal)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 16) {
                                CategoryCard(name: "Chicken", icon: "bird.fill", color: .orange)
                                CategoryCard(name: "Eggs", icon: "oval.fill", color: .yellow)
                                CategoryCard(name: "Duck", icon: "bird.fill", color: .brown)
                                CategoryCard(name: "Feed", icon: "leaf.fill", color: .green)
                                CategoryCard(name: "Vaccines", icon: "syringe.fill", color: .blue)
                            }
                            .padding(.horizontal)
                        }
                    }

                    // Featured Products
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("Featured Products")
                                .font(.headline)
                            Spacer()
                            Button("See All") {}
                                .font(.subheadline)
                                .foregroundColor(.green)
                        }
                        .padding(.horizontal)

                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible())
                        ], spacing: 16) {
                            ForEach(0..<4) { _ in
                                ProductCard()
                            }
                        }
                        .padding(.horizontal)
                    }

                    // Recent Orders
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Your Recent Orders")
                            .font(.headline)
                            .padding(.horizontal)

                        VStack(spacing: 12) {
                            RecentOrderCard(orderNumber: "ORD-2025-001", status: "Delivered", amount: 4500)
                            RecentOrderCard(orderNumber: "ORD-2025-002", status: "In Transit", amount: 2800)
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.vertical)
            }
            .navigationTitle("Poultry")
        }
    }
}

struct CategoryCard: View {
    let name: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
                .frame(width: 50, height: 50)
                .background(color.opacity(0.1))
                .cornerRadius(12)

            Text(name)
                .font(.caption)
                .foregroundColor(.primary)
        }
    }
}

struct ProductCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemGray5))
                .frame(height: 120)
                .overlay(
                    Image(systemName: "photo")
                        .font(.largeTitle)
                        .foregroundColor(.secondary)
                )

            Text("Fresh Chicken")
                .font(.subheadline)
                .fontWeight(.medium)
                .lineLimit(2)

            Text("280/kg")
                .font(.headline)
                .foregroundColor(.green)

            Button("Add to Cart") {}
                .font(.caption)
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(Color.green)
                .foregroundColor(.white)
                .cornerRadius(8)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
}

struct RecentOrderCard: View {
    let orderNumber: String
    let status: String
    let amount: Int

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(orderNumber)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(status)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Text("\(amount)")
                .fontWeight(.semibold)
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

// Placeholder views
struct ProductSearchView: View {
    var body: some View {
        NavigationStack {
            Text("Search Products")
                .navigationTitle("Search")
        }
    }
}

struct CartListView: View {
    var body: some View {
        NavigationStack {
            Text("Your Carts")
                .navigationTitle("Cart")
        }
    }
}

struct OrderListView: View {
    var body: some View {
        NavigationStack {
            Text("Your Orders")
                .navigationTitle("Orders")
        }
    }
}

struct ProfileView: View {
    @EnvironmentObject var authManager: AuthManager

    var body: some View {
        NavigationStack {
            List {
                Section("Account") {
                    HStack {
                        Image(systemName: "person.circle.fill")
                            .font(.largeTitle)
                            .foregroundColor(.green)
                        VStack(alignment: .leading) {
                            Text(authManager.currentUser?.name ?? "User")
                                .fontWeight(.medium)
                            Text(authManager.currentUser?.phone ?? "")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section {
                    NavigationLink(destination: Text("Addresses")) {
                        Label("Addresses", systemImage: "mappin.circle")
                    }
                    NavigationLink(destination: Text("Devices")) {
                        Label("Devices", systemImage: "iphone")
                    }
                }

                Section {
                    Button(role: .destructive) {
                        Task {
                            await authManager.logout()
                        }
                    } label: {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
            .navigationTitle("Profile")
        }
    }
}

#Preview {
    HomeView()
}
