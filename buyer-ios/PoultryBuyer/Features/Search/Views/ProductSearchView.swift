import SwiftUI

struct ProductSearchView: View {
    @StateObject private var viewModel = SearchViewModel()
    @State private var selectedProduct: Product? = nil

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search Bar
                TextField("Search for products...", text: $viewModel.query)
                    .textFieldStyle(.roundedBorder)
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .onChange(of: viewModel.query) { _, newValue in
                        viewModel.onQueryChange(newValue)
                    }

                // Category Filters
                if !viewModel.categories.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            CategoryChip(
                                name: "All",
                                isSelected: viewModel.selectedCategoryId == nil
                            ) {
                                viewModel.onCategorySelected(nil)
                            }

                            ForEach(viewModel.categories) { category in
                                CategoryChip(
                                    name: category.name,
                                    isSelected: viewModel.selectedCategoryId == category.id
                                ) {
                                    viewModel.onCategorySelected(category.id)
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                    }
                }

                // Error Banner
                if let error = viewModel.error {
                    HStack {
                        Text(error)
                            .foregroundColor(.red)
                        Spacer()
                        Button("Dismiss") {
                            viewModel.clearError()
                        }
                        .font(.caption)
                    }
                    .padding()
                    .background(Color.red.opacity(0.1))
                }

                // Content
                if viewModel.isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else if viewModel.products.isEmpty && viewModel.query.isEmpty && viewModel.selectedCategoryId == nil {
                    EmptySearchState(
                        title: "Search for Products",
                        message: "Enter a search term or select a category to find products"
                    )
                } else if viewModel.products.isEmpty {
                    EmptySearchState(
                        title: "No Products Found",
                        message: "Try a different search term or category"
                    )
                } else {
                    ScrollView {
                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible())
                        ], spacing: 12) {
                            ForEach(viewModel.products) { product in
                                ProductCardView(product: product)
                                    .onTapGesture {
                                        selectedProduct = product
                                    }
                                    .onAppear {
                                        if product.id == viewModel.products.last?.id {
                                            viewModel.loadMore()
                                        }
                                    }
                            }
                        }
                        .padding()

                        if viewModel.isLoadingMore {
                            ProgressView()
                                .padding()
                        }
                    }
                    .refreshable {
                        await viewModel.refresh()
                    }
                }
            }
            .navigationTitle("Search")
            .navigationDestination(item: $selectedProduct) { product in
                ProductDetailView(productId: product.id)
            }
        }
    }
}

struct CategoryChip: View {
    let name: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(name)
                .font(.subheadline)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? Color.blue : Color.gray.opacity(0.2))
                .foregroundColor(isSelected ? .white : .primary)
                .clipShape(Capsule())
        }
    }
}

struct ProductCardView: View {
    let product: Product

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Image placeholder
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.gray.opacity(0.2))
                .frame(height: 120)
                .overlay {
                    if let imageUrl = product.imageUrls.first,
                       let url = URL(string: imageUrl) {
                        AsyncImage(url: url) { image in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        } placeholder: {
                            Image(systemName: "photo")
                                .foregroundColor(.gray)
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    } else {
                        Image(systemName: "photo")
                            .foregroundColor(.gray)
                    }
                }
                .clipped()

            VStack(alignment: .leading, spacing: 4) {
                Text(product.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)

                Text(product.sellerName)
                    .font(.caption)
                    .foregroundColor(.secondary)

                if let price = product.currentPrice {
                    PriceView(amount: price.basePrice, unit: product.unit.rawValue)
                }
            }
            .padding(.horizontal, 8)
            .padding(.bottom, 8)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}

struct EmptySearchState: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundColor(.secondary)

            Text(title)
                .font(.title2)
                .fontWeight(.semibold)

            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    ProductSearchView()
}
