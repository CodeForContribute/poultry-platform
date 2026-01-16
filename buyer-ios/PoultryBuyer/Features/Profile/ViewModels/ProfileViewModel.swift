import Foundation
import SwiftUI

@MainActor
class ProfileViewModel: ObservableObject {
    // MARK: - Published Properties

    // Profile
    @Published var profile: UserProfile?
    @Published var stats: ProfileStats?
    @Published var isLoadingProfile = false
    @Published var profileError: String?

    // Addresses
    @Published var addresses: [SavedAddress] = []
    @Published var isLoadingAddresses = false
    @Published var addressError: String?
    @Published var deletingAddressIds: Set<String> = []
    @Published var settingDefaultAddressId: String?

    // Edit Profile
    @Published var editName: String = ""
    @Published var editEmail: String = ""
    @Published var isSavingProfile = false
    @Published var saveProfileError: String?
    @Published var profileSaved = false

    // Address Form
    @Published var addressFormLabel: String = ""
    @Published var addressFormLine1: String = ""
    @Published var addressFormLine2: String = ""
    @Published var addressFormCity: String = ""
    @Published var addressFormState: String = ""
    @Published var addressFormPincode: String = ""
    @Published var addressFormLandmark: String = ""
    @Published var addressFormContactName: String = ""
    @Published var addressFormContactPhone: String = ""
    @Published var addressFormIsDefault: Bool = false
    @Published var isSavingAddress = false
    @Published var saveAddressError: String?
    @Published var addressSaved = false
    @Published var addressFormErrors: [String: String] = [:]

    // Settings
    @Published var notificationPreferences: NotificationPreferences = .default
    @Published var isLoadingPreferences = false
    @Published var isSavingPreferences = false
    @Published var preferencesError: String?

    // Delete Account
    @Published var isDeletingAccount = false
    @Published var deleteAccountError: String?
    @Published var showDeleteConfirmation = false

    // MARK: - Editing State

    private var editingAddressId: String?

    var isEditingAddress: Bool {
        editingAddressId != nil
    }

    // MARK: - Computed Properties

    var defaultAddress: SavedAddress? {
        addresses.first { $0.isDefault }
    }

    var hasAddresses: Bool {
        !addresses.isEmpty
    }

    var formattedOrderCount: String {
        guard let count = stats?.totalOrders else { return "0" }
        if count >= 1000 {
            return "\(count / 1000)k+"
        }
        return "\(count)"
    }

    var formattedTotalSpent: String {
        guard let amount = stats?.totalSpent else { return "0" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "INR"
        formatter.currencySymbol = ""
        formatter.maximumFractionDigits = 0
        return formatter.string(from: NSNumber(value: amount)) ?? "0"
    }

    // MARK: - Initialization

    init() {
        loadProfile()
        loadAddresses()
        loadNotificationPreferences()
    }

    // MARK: - Profile Methods

    func loadProfile() {
        Task {
            isLoadingProfile = true
            profileError = nil

            do {
                async let profileResponse = APIClient.shared.getProfile()
                async let statsResponse = APIClient.shared.getProfileStats()

                let (profileResult, statsResult) = try await (profileResponse, statsResponse)

                if profileResult.success, let profileData = profileResult.data {
                    profile = profileData
                    editName = profileData.name ?? ""
                    editEmail = profileData.email ?? ""
                } else {
                    profileError = profileResult.message ?? "Failed to load profile"
                }

                if statsResult.success, let statsData = statsResult.data {
                    stats = statsData
                }
            } catch {
                profileError = "Network error. Please try again."
            }

            isLoadingProfile = false
        }
    }

    func saveProfile() {
        guard validateProfileForm() else { return }

        Task {
            isSavingProfile = true
            saveProfileError = nil

            do {
                let response = try await APIClient.shared.updateProfile(
                    name: editName.isEmpty ? nil : editName,
                    email: editEmail.isEmpty ? nil : editEmail
                )

                if response.success, let updatedProfile = response.data {
                    profile = updatedProfile
                    profileSaved = true
                } else {
                    saveProfileError = response.message ?? "Failed to save profile"
                }
            } catch {
                saveProfileError = "Network error. Please try again."
            }

            isSavingProfile = false
        }
    }

    private func validateProfileForm() -> Bool {
        if !editEmail.isEmpty {
            let emailRegex = #"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"#
            let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
            if !emailPredicate.evaluate(with: editEmail) {
                saveProfileError = "Please enter a valid email address"
                return false
            }
        }
        return true
    }

    func resetProfileForm() {
        editName = profile?.name ?? ""
        editEmail = profile?.email ?? ""
        saveProfileError = nil
        profileSaved = false
    }

    // MARK: - Address Methods

    func loadAddresses() {
        Task {
            isLoadingAddresses = true
            addressError = nil

            do {
                let response = try await APIClient.shared.getAddresses()
                if response.success, let data = response.data {
                    addresses = data.sorted { ($0.isDefault && !$1.isDefault) }
                } else {
                    addressError = response.message ?? "Failed to load addresses"
                }
            } catch {
                addressError = "Network error. Please try again."
            }

            isLoadingAddresses = false
        }
    }

    func refreshAddresses() async {
        addressError = nil

        do {
            let response = try await APIClient.shared.getAddresses()
            if response.success, let data = response.data {
                addresses = data.sorted { ($0.isDefault && !$1.isDefault) }
            } else {
                addressError = response.message ?? "Failed to refresh addresses"
            }
        } catch {
            addressError = "Network error. Please try again."
        }
    }

    func setupAddressForm(for address: SavedAddress? = nil) {
        if let address = address {
            editingAddressId = address.id
            addressFormLabel = address.label ?? ""
            addressFormLine1 = address.line1
            addressFormLine2 = address.line2 ?? ""
            addressFormCity = address.city
            addressFormState = address.state
            addressFormPincode = address.pincode
            addressFormLandmark = address.landmark ?? ""
            addressFormContactName = address.contactName
            addressFormContactPhone = address.contactPhone
            addressFormIsDefault = address.isDefault
        } else {
            editingAddressId = nil
            addressFormLabel = ""
            addressFormLine1 = ""
            addressFormLine2 = ""
            addressFormCity = ""
            addressFormState = ""
            addressFormPincode = ""
            addressFormLandmark = ""
            addressFormContactName = profile?.name ?? ""
            addressFormContactPhone = profile?.phone ?? ""
            addressFormIsDefault = addresses.isEmpty
        }
        addressFormErrors = [:]
        saveAddressError = nil
        addressSaved = false
    }

    func saveAddress() {
        guard validateAddressForm() else { return }

        Task {
            isSavingAddress = true
            saveAddressError = nil

            let addressRequest = AddressRequest(
                label: addressFormLabel.isEmpty ? nil : addressFormLabel,
                line1: addressFormLine1,
                line2: addressFormLine2.isEmpty ? nil : addressFormLine2,
                city: addressFormCity,
                state: addressFormState,
                pincode: addressFormPincode,
                landmark: addressFormLandmark.isEmpty ? nil : addressFormLandmark,
                latitude: nil,
                longitude: nil,
                contactName: addressFormContactName,
                contactPhone: addressFormContactPhone,
                isDefault: addressFormIsDefault
            )

            do {
                let response: APIResponse<SavedAddress>
                if let editingId = editingAddressId {
                    response = try await APIClient.shared.updateAddress(id: editingId, address: addressRequest)
                } else {
                    response = try await APIClient.shared.addAddress(address: addressRequest)
                }

                if response.success, let savedAddress = response.data {
                    // Update local list
                    if let editingId = editingAddressId {
                        if let index = addresses.firstIndex(where: { $0.id == editingId }) {
                            addresses[index] = savedAddress
                        }
                    } else {
                        addresses.append(savedAddress)
                    }

                    // If set as default, update other addresses
                    if savedAddress.isDefault {
                        addresses = addresses.map { addr in
                            if addr.id != savedAddress.id && addr.isDefault {
                                var updated = addr
                                // Note: This is a local update; server handles the actual state
                                return SavedAddress(
                                    id: updated.id,
                                    label: updated.label,
                                    line1: updated.line1,
                                    line2: updated.line2,
                                    city: updated.city,
                                    state: updated.state,
                                    pincode: updated.pincode,
                                    landmark: updated.landmark,
                                    latitude: updated.latitude,
                                    longitude: updated.longitude,
                                    contactName: updated.contactName,
                                    contactPhone: updated.contactPhone,
                                    isDefault: false
                                )
                            }
                            return addr
                        }
                    }

                    // Sort with default first
                    addresses.sort { ($0.isDefault && !$1.isDefault) }
                    addressSaved = true
                } else {
                    saveAddressError = response.message ?? "Failed to save address"
                }
            } catch {
                saveAddressError = "Network error. Please try again."
            }

            isSavingAddress = false
        }
    }

    private func validateAddressForm() -> Bool {
        addressFormErrors = [:]

        if addressFormLine1.trimmingCharacters(in: .whitespaces).isEmpty {
            addressFormErrors["line1"] = "Address line 1 is required"
        }

        if addressFormCity.trimmingCharacters(in: .whitespaces).isEmpty {
            addressFormErrors["city"] = "City is required"
        }

        if addressFormState.trimmingCharacters(in: .whitespaces).isEmpty {
            addressFormErrors["state"] = "State is required"
        }

        let pincodeDigits = addressFormPincode.filter { $0.isNumber }
        if pincodeDigits.count != 6 {
            addressFormErrors["pincode"] = "Pincode must be 6 digits"
        }

        if addressFormContactName.trimmingCharacters(in: .whitespaces).isEmpty {
            addressFormErrors["contactName"] = "Contact name is required"
        }

        let phoneDigits = addressFormContactPhone.filter { $0.isNumber }
        if phoneDigits.count != 10 {
            addressFormErrors["contactPhone"] = "Phone must be 10 digits"
        }

        return addressFormErrors.isEmpty
    }

    func deleteAddress(_ address: SavedAddress) {
        deletingAddressIds.insert(address.id)

        Task {
            do {
                let response = try await APIClient.shared.deleteAddress(id: address.id)
                if response.success {
                    addresses.removeAll { $0.id == address.id }
                } else {
                    addressError = response.message ?? "Failed to delete address"
                }
            } catch {
                addressError = "Network error. Please try again."
            }

            deletingAddressIds.remove(address.id)
        }
    }

    func setDefaultAddress(_ address: SavedAddress) {
        guard !address.isDefault else { return }

        settingDefaultAddressId = address.id

        Task {
            do {
                let response = try await APIClient.shared.setDefaultAddress(id: address.id)
                if response.success {
                    // Update local list
                    addresses = addresses.map { addr in
                        var updated = addr
                        let isDefault = addr.id == address.id
                        return SavedAddress(
                            id: updated.id,
                            label: updated.label,
                            line1: updated.line1,
                            line2: updated.line2,
                            city: updated.city,
                            state: updated.state,
                            pincode: updated.pincode,
                            landmark: updated.landmark,
                            latitude: updated.latitude,
                            longitude: updated.longitude,
                            contactName: updated.contactName,
                            contactPhone: updated.contactPhone,
                            isDefault: isDefault
                        )
                    }
                    // Sort with default first
                    addresses.sort { ($0.isDefault && !$1.isDefault) }
                } else {
                    addressError = response.message ?? "Failed to set default address"
                }
            } catch {
                addressError = "Network error. Please try again."
            }

            settingDefaultAddressId = nil
        }
    }

    // MARK: - Settings Methods

    func loadNotificationPreferences() {
        Task {
            isLoadingPreferences = true
            preferencesError = nil

            do {
                let response = try await APIClient.shared.getNotificationPreferences()
                if response.success, let data = response.data {
                    notificationPreferences = data
                }
            } catch {
                // Use defaults if failed to load
                notificationPreferences = .default
            }

            isLoadingPreferences = false
        }
    }

    func saveNotificationPreferences() {
        Task {
            isSavingPreferences = true
            preferencesError = nil

            do {
                let response = try await APIClient.shared.updateNotificationPreferences(
                    preferences: notificationPreferences
                )
                if !response.success {
                    preferencesError = response.message ?? "Failed to save preferences"
                }
            } catch {
                preferencesError = "Network error. Please try again."
            }

            isSavingPreferences = false
        }
    }

    func deleteAccount() async -> Bool {
        isDeletingAccount = true
        deleteAccountError = nil

        do {
            let response = try await APIClient.shared.deleteAccount()
            if response.success {
                isDeletingAccount = false
                return true
            } else {
                deleteAccountError = response.message ?? "Failed to delete account"
            }
        } catch {
            deleteAccountError = "Network error. Please try again."
        }

        isDeletingAccount = false
        return false
    }

    // MARK: - Error Clearing

    func clearProfileError() {
        profileError = nil
    }

    func clearAddressError() {
        addressError = nil
    }

    func clearSaveProfileError() {
        saveProfileError = nil
    }

    func clearSaveAddressError() {
        saveAddressError = nil
    }

    func clearDeleteAccountError() {
        deleteAccountError = nil
    }
}
