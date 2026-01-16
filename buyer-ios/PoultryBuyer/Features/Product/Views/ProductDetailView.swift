import SwiftUI

struct ProductDetailView: View {
    @StateObject private var viewModel: ProductDetailViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showCartAlert = false

    init(productId: String) {
        _viewModel = StateObject(wrappedValue: ProductDetailViewModel(productId: productId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
            } else if let error = viewModel.error {
                VStack(spacing: 16) {
                    Text(error)
                        .foregroundColor(.secondary)
                    Button("Retry") {
                        viewModel.loadProduct()
                    }
                    .buttonStyle(.bordered)
                }
            } else if let product = viewModel.product {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // Image Gallery
                        TabView {
                            ForEach(product.imageUrls, id: \.self) { urlString in
                                if let url = URL(string: urlString) {
                                    AsyncImage(url: url) { image in
                                        image
                                            .resizable()
                                            .aspectRatio(contentMode: .fill)
                                    } placeholder: {
                                        Color.gray.opacity(0.2)
                                            .overlay {
                                                Image(systemName: "photo")
                                                    .foregroundColor(.gray)
                                            }
                                    }
                                }
                            }
                        }
                        .tabViewStyle(.page)
                        .frame(height: 300)

                        VStack(alignment: .leading, spacing: 16) {
                            // Category & Name
                            VStack(alignment: .leading, spacing: 4) {
                                Text(product.category.name)
                                    .font(.caption)
                                    .foregroundColor(.blue)

                                Text(product.name)
                                    .font(.title2)
                                    .fontWeight(.bold)

                                if let nameHi = product.nameHi {
                                    Text(nameHi)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                            }

                            // Seller Info
                            GroupBox {
                                HStack {
                                    VStack(alignment: .leading) {
                                        Text("Sold by")
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                        Text(product.sellerName)
                                            .font(.headline)
                                    }
                                    Spacer()
                                }
                            }

                            // Price Section
                            GroupBox {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Price")
                                        .font(.headline)

                                    if let price = product.currentPrice {
                                        HStack(alignment: .center) {
                                            if let discounted = viewModel.calculateDiscountedPrice() {
                                                Text("₹\(String(format: "%.2f", price.basePrice))")
                                                    .strikethrough()
                                                    .foregroundColor(.secondary)

                                                PriceView(amount: discounted, unit: product.unit.rawValue)
                                            } else {
                                                PriceView(amount: price.basePrice, unit: product.unit.rawValue)
                                            }
                                        }

                                        // Bulk Discounts
                                        if let slabs = price.bulkDiscountSlabs, !slabs.isEmpty {
                                            Divider()

                                            Text("Bulk Discounts")
                                                .font(.subheadline)
                                                .fontWeight(.medium)

                                            ForEach(slabs, id: \.minQty) { slab in
                                                Text("• \(Int(slab.minQty))\(slab.maxQty != nil ? "-\(Int(slab.maxQty!))" : "+") \(product.unit.rawValue.lowercased()): \(Int(slab.discountPercent))% off")
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                            }
                                        }
                                    }
                                }
                            }

                            // Quantity Selector
                            GroupBox {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Quantity")
                                        .font(.headline)

                                    HStack {
                                        QuantitySelectorView(
                                            quantity: Binding(
                                                get: { viewModel.quantity },
                                                set: { viewModel.updateQuantity($0) }
                                            ),
                                            minQuantity: product.minOrderQty,
                                            maxQuantity: product.maxOrderQty,
                                            unit: product.unit.rawValue
                                        )
                                        Spacer()
                                    }

                                    Text("Min: \(Int(product.minOrderQty)) \(product.unit.rawValue.lowercased())" +
                                         (product.maxOrderQty != nil ? " • Max: \(Int(product.maxOrderQty!)) \(product.unit.rawValue.lowercased())" : ""))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }

                            // Description
                            if let description = product.description {
                                GroupBox {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text("Description")
                                            .font(.headline)
                                        Text(description)
                                            .font(.body)
                                    }
                                }
                            }

                            // Product Details
                            GroupBox {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Product Details")
                                        .font(.headline)

                                    DetailRow(label: "SKU", value: product.sku)
                                    DetailRow(label: "Unit", value: product.unit.rawValue)
                                    DetailRow(label: "Category", value: product.category.name)
                                    if let hsn = product.category.hsnCode {
                                        DetailRow(label: "HSN Code", value: hsn)
                                    }
                                    DetailRow(label: "GST", value: "\(Int(product.category.gstRate))%")
                                }
                            }
                        }
                        .padding(.horizontal)

                        Spacer(minLength: 100)
                    }
                }
                .safeAreaInset(edge: .bottom) {
                    // Bottom Bar
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Total")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            PriceView(amount: viewModel.getLineTotal(), style: .large)
                        }

                        Spacer()

                        Button {
                            viewModel.addToCart()
                        } label: {
                            HStack {
                                if viewModel.isAddingToCart {
                                    ProgressView()
                                        .tint(.white)
                                } else {
                                    Image(systemName: "cart.badge.plus")
                                }
                                Text("Add to Cart")
                            }
                            .frame(minWidth: 140)
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(viewModel.isAddingToCart || product.status != .active)
                    }
                    .padding()
                    .background(.regularMaterial)
                }
            }
        }
        .navigationTitle("Product Details")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: viewModel.addedToCart) { _, added in
            if added {
                showCartAlert = true
            }
        }
        .alert("Added to Cart!", isPresented: $showCartAlert) {
            Button("Continue Shopping") {
                viewModel.clearCartStatus()
            }
            Button("View Cart") {
                viewModel.clearCartStatus()
                // Navigate to cart - handled by parent
            }
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.cartError != nil },
            set: { if !$0 { viewModel.clearCartStatus() } }
        )) {
            Button("OK") {
                viewModel.clearCartStatus()
            }
        } message: {
            Text(viewModel.cartError ?? "An error occurred")
        }
    }
}

struct DetailRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
        .font(.subheadline)
    }
}

#Preview {
    NavigationStack {
        ProductDetailView(productId: "test-product")
    }
}
