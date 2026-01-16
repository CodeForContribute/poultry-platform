import SwiftUI

struct AddressFormView: View {
    @ObservedObject var viewModel: ProfileViewModel
    @Environment(\.dismiss) private var dismiss
    @FocusState private var focusedField: Field?

    let existingAddress: SavedAddress?

    enum Field: Hashable {
        case label
        case line1
        case line2
        case city
        case state
        case pincode
        case landmark
        case contactName
        case contactPhone
    }

    init(viewModel: ProfileViewModel, existingAddress: SavedAddress? = nil) {
        self.viewModel = viewModel
        self.existingAddress = existingAddress
    }

    var body: some View {
        Form {
            // Address Label Section
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Label (Optional)")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    TextField("e.g., Home, Office, Farm", text: $viewModel.addressFormLabel)
                        .focused($focusedField, equals: .label)
                }
                .padding(.vertical, 4)

                // Quick labels
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        QuickLabelButton(label: "Home", icon: "house.fill", selectedLabel: $viewModel.addressFormLabel)
                        QuickLabelButton(label: "Office", icon: "building.2.fill", selectedLabel: $viewModel.addressFormLabel)
                        QuickLabelButton(label: "Farm", icon: "leaf.fill", selectedLabel: $viewModel.addressFormLabel)
                        QuickLabelButton(label: "Other", icon: "mappin.circle.fill", selectedLabel: $viewModel.addressFormLabel)
                    }
                }
                .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 8, trailing: 16))
            } header: {
                Text("Address Label")
            }

            // Address Details Section
            Section {
                AddressTextField(
                    label: "Address Line 1 *",
                    placeholder: "House/Building No., Street Name",
                    text: $viewModel.addressFormLine1,
                    error: viewModel.addressFormErrors["line1"],
                    field: .line1,
                    focusedField: $focusedField
                )

                AddressTextField(
                    label: "Address Line 2",
                    placeholder: "Area, Colony (optional)",
                    text: $viewModel.addressFormLine2,
                    field: .line2,
                    focusedField: $focusedField
                )

                AddressTextField(
                    label: "Landmark",
                    placeholder: "Near famous place (optional)",
                    text: $viewModel.addressFormLandmark,
                    field: .landmark,
                    focusedField: $focusedField
                )
            } header: {
                Text("Address Details")
            }

            // Location Section
            Section {
                AddressTextField(
                    label: "City *",
                    placeholder: "Enter city name",
                    text: $viewModel.addressFormCity,
                    error: viewModel.addressFormErrors["city"],
                    field: .city,
                    focusedField: $focusedField
                )

                // State Picker
                VStack(alignment: .leading, spacing: 4) {
                    Text("State *")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Picker("Select State", selection: $viewModel.addressFormState) {
                        Text("Select State").tag("")
                        ForEach(IndianState.allCases, id: \.rawValue) { state in
                            Text(state.rawValue).tag(state.rawValue)
                        }
                    }
                    .pickerStyle(.menu)
                    .tint(.primary)

                    if let error = viewModel.addressFormErrors["state"] {
                        Text(error)
                            .font(.caption2)
                            .foregroundColor(.red)
                    }
                }
                .padding(.vertical, 4)

                // Pincode with validation
                VStack(alignment: .leading, spacing: 4) {
                    Text("Pincode *")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack {
                        TextField("6-digit pincode", text: $viewModel.addressFormPincode)
                            .keyboardType(.numberPad)
                            .focused($focusedField, equals: .pincode)
                            .onChange(of: viewModel.addressFormPincode) { _, newValue in
                                // Only allow digits and limit to 6
                                let filtered = newValue.filter { $0.isNumber }
                                viewModel.addressFormPincode = String(filtered.prefix(6))
                            }

                        if viewModel.addressFormPincode.count == 6 {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                        }
                    }

                    if let error = viewModel.addressFormErrors["pincode"] {
                        Text(error)
                            .font(.caption2)
                            .foregroundColor(.red)
                    } else {
                        Text("Enter a valid 6-digit pincode")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.vertical, 4)
            } header: {
                Text("Location")
            }

            // Contact Details Section
            Section {
                AddressTextField(
                    label: "Contact Name *",
                    placeholder: "Person to contact for delivery",
                    text: $viewModel.addressFormContactName,
                    error: viewModel.addressFormErrors["contactName"],
                    field: .contactName,
                    focusedField: $focusedField,
                    contentType: .name
                )

                VStack(alignment: .leading, spacing: 4) {
                    Text("Contact Phone *")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack {
                        Text("+91")
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 8)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(6)

                        TextField("10-digit phone number", text: $viewModel.addressFormContactPhone)
                            .keyboardType(.phonePad)
                            .textContentType(.telephoneNumber)
                            .focused($focusedField, equals: .contactPhone)
                            .onChange(of: viewModel.addressFormContactPhone) { _, newValue in
                                // Only allow digits and limit to 10
                                let filtered = newValue.filter { $0.isNumber }
                                viewModel.addressFormContactPhone = String(filtered.prefix(10))
                            }

                        if viewModel.addressFormContactPhone.count == 10 {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                        }
                    }

                    if let error = viewModel.addressFormErrors["contactPhone"] {
                        Text(error)
                            .font(.caption2)
                            .foregroundColor(.red)
                    }
                }
                .padding(.vertical, 4)
            } header: {
                Text("Contact Details")
            } footer: {
                Text("This contact will be used for delivery coordination.")
            }

            // Default Address Toggle
            Section {
                Toggle(isOn: $viewModel.addressFormIsDefault) {
                    HStack {
                        Image(systemName: "star.fill")
                            .foregroundColor(.orange)
                        Text("Set as default address")
                    }
                }
            } footer: {
                Text("Default address will be pre-selected during checkout.")
            }

            // Save Button Section
            Section {
                Button {
                    focusedField = nil
                    viewModel.saveAddress()
                } label: {
                    HStack {
                        Spacer()
                        if viewModel.isSavingAddress {
                            ProgressView()
                                .tint(.white)
                            Text("Saving...")
                        } else {
                            Image(systemName: existingAddress != nil ? "checkmark.circle.fill" : "plus.circle.fill")
                            Text(existingAddress != nil ? "Update Address" : "Save Address")
                        }
                        Spacer()
                    }
                }
                .disabled(viewModel.isSavingAddress)
                .listRowBackground(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(viewModel.isSavingAddress ? Color.gray.opacity(0.3) : Color.blue)
                )
                .foregroundColor(.white)
            }
        }
        .navigationTitle(existingAddress != nil ? "Edit Address" : "Add Address")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Cancel") {
                    dismiss()
                }
            }

            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") {
                    focusedField = nil
                }
            }
        }
        .onAppear {
            if let address = existingAddress {
                viewModel.setupAddressForm(for: address)
            } else {
                viewModel.setupAddressForm()
            }
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.saveAddressError != nil },
            set: { if !$0 { viewModel.clearSaveAddressError() } }
        )) {
            Button("OK") {
                viewModel.clearSaveAddressError()
            }
        } message: {
            Text(viewModel.saveAddressError ?? "An error occurred")
        }
        .onChange(of: viewModel.addressSaved) { _, saved in
            if saved {
                dismiss()
            }
        }
    }
}

// MARK: - Address Text Field

struct AddressTextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var error: String? = nil
    let field: AddressFormView.Field
    var focusedField: FocusState<AddressFormView.Field?>.Binding
    var contentType: UITextContentType? = nil
    var keyboardType: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            TextField(placeholder, text: $text)
                .textContentType(contentType)
                .keyboardType(keyboardType)
                .focused(focusedField, equals: field)

            if let error = error {
                Text(error)
                    .font(.caption2)
                    .foregroundColor(.red)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Quick Label Button

struct QuickLabelButton: View {
    let label: String
    let icon: String
    @Binding var selectedLabel: String

    var isSelected: Bool {
        selectedLabel.lowercased() == label.lowercased()
    }

    var body: some View {
        Button {
            selectedLabel = label
        } label: {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.caption)
                Text(label)
                    .font(.caption)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(isSelected ? Color.blue : Color.gray.opacity(0.15))
            .foregroundColor(isSelected ? .white : .primary)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack {
        AddressFormView(viewModel: ProfileViewModel())
    }
}
