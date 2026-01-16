import SwiftUI

struct EditProfileView: View {
    @ObservedObject var viewModel: ProfileViewModel
    @Environment(\.dismiss) private var dismiss
    @FocusState private var focusedField: Field?

    enum Field: Hashable {
        case name
        case email
    }

    var body: some View {
        Form {
            // Profile Picture Section
            Section {
                HStack {
                    Spacer()
                    VStack(spacing: 12) {
                        ProfileAvatarView(
                            imageUrl: viewModel.profile?.profileImageUrl,
                            name: viewModel.editName.isEmpty ? "User" : viewModel.editName
                        )

                        Button {
                            // Future: Implement profile picture upload
                        } label: {
                            Text("Change Photo")
                                .font(.caption)
                        }
                        .disabled(true) // Disabled until image upload is implemented
                    }
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }

            // Personal Information Section
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Name")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    TextField("Enter your name", text: $viewModel.editName)
                        .textContentType(.name)
                        .autocorrectionDisabled()
                        .focused($focusedField, equals: .name)
                }
                .padding(.vertical, 4)

                VStack(alignment: .leading, spacing: 4) {
                    Text("Email")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    TextField("Enter your email", text: $viewModel.editEmail)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .autocorrectionDisabled()
                        .focused($focusedField, equals: .email)
                }
                .padding(.vertical, 4)

                // Phone (Read-only)
                if let phone = viewModel.profile?.formattedPhone {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Phone")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        HStack {
                            Text(phone)
                                .foregroundColor(.primary)

                            Spacer()

                            Image(systemName: "lock.fill")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            } header: {
                Text("Personal Information")
            } footer: {
                Text("Phone number cannot be changed. Contact support if you need to update it.")
                    .font(.caption)
            }

            // Save Button Section
            Section {
                Button {
                    focusedField = nil
                    viewModel.saveProfile()
                } label: {
                    HStack {
                        Spacer()
                        if viewModel.isSavingProfile {
                            ProgressView()
                                .tint(.white)
                            Text("Saving...")
                        } else {
                            Text("Save Changes")
                        }
                        Spacer()
                    }
                }
                .disabled(viewModel.isSavingProfile || !hasChanges)
                .listRowBackground(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(hasChanges && !viewModel.isSavingProfile ? Color.blue : Color.gray.opacity(0.3))
                )
                .foregroundColor(.white)
            }
        }
        .navigationTitle("Edit Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") {
                    focusedField = nil
                }
            }
        }
        .onAppear {
            viewModel.resetProfileForm()
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.saveProfileError != nil },
            set: { if !$0 { viewModel.clearSaveProfileError() } }
        )) {
            Button("OK") {
                viewModel.clearSaveProfileError()
            }
        } message: {
            Text(viewModel.saveProfileError ?? "An error occurred")
        }
        .onChange(of: viewModel.profileSaved) { _, saved in
            if saved {
                dismiss()
            }
        }
    }

    private var hasChanges: Bool {
        let originalName = viewModel.profile?.name ?? ""
        let originalEmail = viewModel.profile?.email ?? ""
        return viewModel.editName != originalName || viewModel.editEmail != originalEmail
    }
}

#Preview {
    NavigationStack {
        EditProfileView(viewModel: ProfileViewModel())
    }
}
