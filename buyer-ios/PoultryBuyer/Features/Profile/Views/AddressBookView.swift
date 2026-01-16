import SwiftUI

struct AddressBookView: View {
    @ObservedObject var viewModel: ProfileViewModel
    @State private var showAddAddress = false
    @State private var editingAddress: SavedAddress?
    @State private var addressToDelete: SavedAddress?
    @State private var showDeleteConfirmation = false

    var body: some View {
        Group {
            if viewModel.isLoadingAddresses && viewModel.addresses.isEmpty {
                LoadingView()
            } else if viewModel.addresses.isEmpty {
                EmptyAddressView(showAddAddress: $showAddAddress)
            } else {
                AddressListView(
                    viewModel: viewModel,
                    editingAddress: $editingAddress,
                    addressToDelete: $addressToDelete,
                    showDeleteConfirmation: $showDeleteConfirmation
                )
            }
        }
        .navigationTitle("Address Book")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    viewModel.setupAddressForm()
                    showAddAddress = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddAddress) {
            NavigationStack {
                AddressFormView(viewModel: viewModel)
            }
        }
        .sheet(item: $editingAddress) { address in
            NavigationStack {
                AddressFormView(viewModel: viewModel, existingAddress: address)
            }
        }
        .confirmationDialog(
            "Delete Address",
            isPresented: $showDeleteConfirmation,
            titleVisibility: .visible
        ) {
            Button("Delete", role: .destructive) {
                if let address = addressToDelete {
                    viewModel.deleteAddress(address)
                }
                addressToDelete = nil
            }
            Button("Cancel", role: .cancel) {
                addressToDelete = nil
            }
        } message: {
            Text("Are you sure you want to delete this address? This action cannot be undone.")
        }
        .refreshable {
            await viewModel.refreshAddresses()
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.addressError != nil },
            set: { if !$0 { viewModel.clearAddressError() } }
        )) {
            Button("OK") {
                viewModel.clearAddressError()
            }
        } message: {
            Text(viewModel.addressError ?? "An error occurred")
        }
    }
}

// MARK: - Loading View

struct LoadingView: View {
    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text("Loading addresses...")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Empty Address View

struct EmptyAddressView: View {
    @Binding var showAddAddress: Bool

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "location.slash")
                .font(.system(size: 60))
                .foregroundColor(.secondary)

            VStack(spacing: 8) {
                Text("No Addresses Saved")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("Add a delivery address to make checkout faster")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Button {
                showAddAddress = true
            } label: {
                Label("Add Address", systemImage: "plus.circle.fill")
                    .font(.headline)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

// MARK: - Address List View

struct AddressListView: View {
    @ObservedObject var viewModel: ProfileViewModel
    @Binding var editingAddress: SavedAddress?
    @Binding var addressToDelete: SavedAddress?
    @Binding var showDeleteConfirmation: Bool

    var body: some View {
        List {
            ForEach(viewModel.addresses) { address in
                AddressCardView(
                    address: address,
                    isDeleting: viewModel.deletingAddressIds.contains(address.id),
                    isSettingDefault: viewModel.settingDefaultAddressId == address.id,
                    onSetDefault: {
                        viewModel.setDefaultAddress(address)
                    },
                    onEdit: {
                        viewModel.setupAddressForm(for: address)
                        editingAddress = address
                    }
                )
                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                    Button(role: .destructive) {
                        addressToDelete = address
                        showDeleteConfirmation = true
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                    .disabled(viewModel.deletingAddressIds.contains(address.id))
                }
                .swipeActions(edge: .leading) {
                    if !address.isDefault {
                        Button {
                            viewModel.setDefaultAddress(address)
                        } label: {
                            Label("Default", systemImage: "star.fill")
                        }
                        .tint(.orange)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}

// MARK: - Address Card View

struct AddressCardView: View {
    let address: SavedAddress
    let isDeleting: Bool
    let isSettingDefault: Bool
    let onSetDefault: () -> Void
    let onEdit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header with label and default badge
            HStack {
                HStack(spacing: 8) {
                    Image(systemName: addressIcon)
                        .foregroundColor(.blue)

                    Text(address.displayLabel)
                        .font(.headline)
                }

                Spacer()

                if address.isDefault {
                    DefaultBadge()
                }

                if isDeleting || isSettingDefault {
                    ProgressView()
                        .scaleEffect(0.8)
                }
            }

            // Address details
            VStack(alignment: .leading, spacing: 4) {
                Text(address.formattedAddress)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(3)

                Text(address.contactDetails)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // Action buttons
            HStack(spacing: 16) {
                if !address.isDefault {
                    Button {
                        onSetDefault()
                    } label: {
                        Label("Set as Default", systemImage: "star")
                            .font(.caption)
                    }
                    .buttonStyle(.bordered)
                    .disabled(isSettingDefault)
                }

                Spacer()

                Button {
                    onEdit()
                } label: {
                    Label("Edit", systemImage: "pencil")
                        .font(.caption)
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(.vertical, 8)
        .opacity(isDeleting ? 0.5 : 1.0)
    }

    private var addressIcon: String {
        let label = address.label?.lowercased() ?? ""
        if label.contains("home") {
            return "house.fill"
        } else if label.contains("work") || label.contains("office") {
            return "building.2.fill"
        } else {
            return "mappin.circle.fill"
        }
    }
}

// MARK: - Default Badge

struct DefaultBadge: View {
    var body: some View {
        Text("DEFAULT")
            .font(.caption2)
            .fontWeight(.bold)
            .foregroundColor(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Color.green)
            .clipShape(Capsule())
    }
}

#Preview {
    NavigationStack {
        AddressBookView(viewModel: ProfileViewModel())
    }
}
