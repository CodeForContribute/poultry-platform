package com.poultry.buyer.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poultry.buyer.core.data.preferences.SecureTokenManager
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: BuyerProfile? = null,
    val statistics: OrderStatistics? = null,
    val addresses: List<Address> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val profileUpdateSuccess: Boolean = false,
    val isUpdatingProfile: Boolean = false,
    val isUpdatingNotifications: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountDeleted: Boolean = false
)

data class AddressUiState(
    val addresses: List<Address> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAddress: Address? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false
)

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

data class AddressFormUiState(
    val addressId: String? = null,
    val label: String = "",
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val landmark: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val isDefault: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val emailNotificationsEnabled: Boolean = true,
    val smsNotificationsEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val isUpdating: Boolean = false,
    val cacheCleared: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: SecureTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _addressUiState = MutableStateFlow(AddressUiState())
    val addressUiState: StateFlow<AddressUiState> = _addressUiState.asStateFlow()

    private val _editProfileUiState = MutableStateFlow(EditProfileUiState())
    val editProfileUiState: StateFlow<EditProfileUiState> = _editProfileUiState.asStateFlow()

    private val _addressFormUiState = MutableStateFlow(AddressFormUiState())
    val addressFormUiState: StateFlow<AddressFormUiState> = _addressFormUiState.asStateFlow()

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    init {
        loadProfile()
    }

    // ============= Profile Methods =============

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = apiService.getProfile()
                if (response.isSuccessful && response.body()?.success == true) {
                    val profile = response.body()?.data
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isLoading = false
                    )
                    // Update settings state with notification preferences
                    profile?.let {
                        _settingsUiState.value = _settingsUiState.value.copy(
                            notificationsEnabled = it.notificationsEnabled,
                            emailNotificationsEnabled = it.emailNotificationsEnabled,
                            smsNotificationsEnabled = it.smsNotificationsEnabled,
                            pushNotificationsEnabled = it.pushNotificationsEnabled
                        )
                    }
                    // Load statistics
                    loadStatistics()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Failed to load profile"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

            try {
                val response = apiService.getProfile()
                if (response.isSuccessful && response.body()?.success == true) {
                    val profile = response.body()?.data
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isRefreshing = false
                    )
                    loadStatistics()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = response.body()?.message ?: "Failed to refresh profile"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val response = apiService.getOrderStatistics()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        statistics = response.body()?.data
                    )
                }
            } catch (_: Exception) {
                // Silently fail for statistics
            }
        }
    }

    // ============= Edit Profile Methods =============

    fun initEditProfile() {
        val profile = _uiState.value.profile
        _editProfileUiState.value = EditProfileUiState(
            name = profile?.name ?: "",
            email = profile?.email ?: ""
        )
    }

    fun updateEditName(name: String) {
        _editProfileUiState.value = _editProfileUiState.value.copy(
            name = name,
            nameError = null
        )
    }

    fun updateEditEmail(email: String) {
        _editProfileUiState.value = _editProfileUiState.value.copy(
            email = email,
            emailError = null
        )
    }

    fun saveProfile() {
        val state = _editProfileUiState.value
        val errors = mutableMapOf<String, String>()

        // Validate
        if (state.name.isBlank()) {
            errors["name"] = "Name is required"
        } else if (state.name.length < 2) {
            errors["name"] = "Name must be at least 2 characters"
        }

        if (state.email.isNotBlank() && !isValidEmail(state.email)) {
            errors["email"] = "Invalid email format"
        }

        if (errors.isNotEmpty()) {
            _editProfileUiState.value = _editProfileUiState.value.copy(
                nameError = errors["name"],
                emailError = errors["email"]
            )
            return
        }

        viewModelScope.launch {
            _editProfileUiState.value = _editProfileUiState.value.copy(isSaving = true, error = null)

            try {
                val response = apiService.updateProfile(
                    UpdateProfileRequest(
                        name = state.name.trim(),
                        email = state.email.trim().takeIf { it.isNotBlank() }
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val updatedProfile = response.body()?.data
                    _uiState.value = _uiState.value.copy(profile = updatedProfile)
                    _editProfileUiState.value = _editProfileUiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                    // Update token manager with new name
                    updatedProfile?.let {
                        tokenManager.saveUser(it.id, it.name)
                    }
                } else {
                    _editProfileUiState.value = _editProfileUiState.value.copy(
                        isSaving = false,
                        error = response.body()?.message ?: "Failed to update profile"
                    )
                }
            } catch (e: Exception) {
                _editProfileUiState.value = _editProfileUiState.value.copy(
                    isSaving = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun clearEditProfileSuccess() {
        _editProfileUiState.value = _editProfileUiState.value.copy(saveSuccess = false)
    }

    // ============= Address Methods =============

    fun loadAddresses() {
        viewModelScope.launch {
            _addressUiState.value = _addressUiState.value.copy(isLoading = true, error = null)

            try {
                val response = apiService.getAddresses()
                if (response.isSuccessful && response.body()?.success == true) {
                    _addressUiState.value = _addressUiState.value.copy(
                        addresses = response.body()?.data ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _addressUiState.value = _addressUiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Failed to load addresses"
                    )
                }
            } catch (e: Exception) {
                _addressUiState.value = _addressUiState.value.copy(
                    isLoading = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun initAddressForm(addressId: String? = null) {
        if (addressId == null) {
            // New address
            val profile = _uiState.value.profile
            _addressFormUiState.value = AddressFormUiState(
                contactName = profile?.name ?: "",
                contactPhone = profile?.phone ?: ""
            )
        } else {
            // Edit existing address
            viewModelScope.launch {
                _addressFormUiState.value = _addressFormUiState.value.copy(isLoading = true)

                try {
                    val response = apiService.getAddress(addressId)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val address = response.body()?.data!!
                        _addressFormUiState.value = AddressFormUiState(
                            addressId = address.id,
                            label = address.label ?: "",
                            line1 = address.line1,
                            line2 = address.line2 ?: "",
                            city = address.city,
                            state = address.state,
                            pincode = address.pincode,
                            landmark = address.landmark ?: "",
                            contactName = address.contactName,
                            contactPhone = address.contactPhone,
                            isDefault = address.isDefault,
                            isLoading = false
                        )
                    } else {
                        _addressFormUiState.value = _addressFormUiState.value.copy(
                            isLoading = false,
                            error = response.body()?.message ?: "Failed to load address"
                        )
                    }
                } catch (e: Exception) {
                    _addressFormUiState.value = _addressFormUiState.value.copy(
                        isLoading = false,
                        error = "Network error. Please try again."
                    )
                }
            }
        }
    }

    fun updateAddressLabel(label: String) {
        _addressFormUiState.value = _addressFormUiState.value.copy(
            label = label,
            errors = _addressFormUiState.value.errors - "label"
        )
    }

    fun updateAddressLine1(line1: String) {
        _addressFormUiState.value = _addressFormUiState.value.copy(
            line1 = line1,
            errors = _addressFormUiState.value.errors - "line1"
        )
    }

    fun updateAddressLine2(line2: String) {
        _addressFormUiState.value = _addressFormUiState.value.copy(line2 = line2)
    }

    fun updateAddressCity(city: String) {
        _addressFormUiState.value = _addressFormUiState.value.copy(
            city = city,
            errors = _addressFormUiState.value.errors - "city"
        )
    }

    fun updateAddressState(state: String) {
        _addressFormUiState.value = _addressFormUiState.value.copy(
            state = state,
            errors = _addressFormUiState.value.errors - "state"
        )
    }

    fun updateAddressPincode(pincode: String) {
        // Only allow digits and max 6 characters
        val sanitized = pincode.filter { it.isDigit() }.take(6)
        _addressFormUiState.value = _addressFormUiState.value.copy(
            pincode = sanitized,
            errors = _addressFormUiState.value.errors - "pincode"
        )
    }

    fun updateAddressLandmark(landmark: String) {
        _addressFormUiState.value = _addressFormUiState.value.copy(landmark = landmark)
    }

    fun updateAddressContactName(name: String) {
        _addressFormUiState.value = _addressFormUiState.value.copy(
            contactName = name,
            errors = _addressFormUiState.value.errors - "contactName"
        )
    }

    fun updateAddressContactPhone(phone: String) {
        // Only allow digits and max 10 characters
        val sanitized = phone.filter { it.isDigit() }.take(10)
        _addressFormUiState.value = _addressFormUiState.value.copy(
            contactPhone = sanitized,
            errors = _addressFormUiState.value.errors - "contactPhone"
        )
    }

    fun updateAddressIsDefault(isDefault: Boolean) {
        _addressFormUiState.value = _addressFormUiState.value.copy(isDefault = isDefault)
    }

    fun saveAddress() {
        val state = _addressFormUiState.value
        val errors = mutableMapOf<String, String>()

        // Validate
        if (state.line1.isBlank()) {
            errors["line1"] = "Address line 1 is required"
        }
        if (state.city.isBlank()) {
            errors["city"] = "City is required"
        }
        if (state.state.isBlank()) {
            errors["state"] = "State is required"
        }
        if (state.pincode.isBlank()) {
            errors["pincode"] = "Pincode is required"
        } else if (!isValidPincode(state.pincode)) {
            errors["pincode"] = "Invalid pincode (6 digits required)"
        }
        if (state.contactName.isBlank()) {
            errors["contactName"] = "Contact name is required"
        }
        if (state.contactPhone.isBlank()) {
            errors["contactPhone"] = "Contact phone is required"
        } else if (!isValidPhone(state.contactPhone)) {
            errors["contactPhone"] = "Invalid phone number"
        }

        if (errors.isNotEmpty()) {
            _addressFormUiState.value = _addressFormUiState.value.copy(errors = errors)
            return
        }

        viewModelScope.launch {
            _addressFormUiState.value = _addressFormUiState.value.copy(isSaving = true, error = null)

            try {
                val response = if (state.addressId == null) {
                    // Create new address
                    apiService.createAddress(
                        CreateAddressRequest(
                            label = state.label.trim().takeIf { it.isNotBlank() },
                            line1 = state.line1.trim(),
                            line2 = state.line2.trim().takeIf { it.isNotBlank() },
                            city = state.city.trim(),
                            state = state.state.trim(),
                            pincode = state.pincode,
                            landmark = state.landmark.trim().takeIf { it.isNotBlank() },
                            latitude = null,
                            longitude = null,
                            contactName = state.contactName.trim(),
                            contactPhone = state.contactPhone,
                            isDefault = state.isDefault
                        )
                    )
                } else {
                    // Update existing address
                    apiService.updateAddress(
                        state.addressId,
                        UpdateAddressRequest(
                            label = state.label.trim().takeIf { it.isNotBlank() },
                            line1 = state.line1.trim(),
                            line2 = state.line2.trim().takeIf { it.isNotBlank() },
                            city = state.city.trim(),
                            state = state.state.trim(),
                            pincode = state.pincode,
                            landmark = state.landmark.trim().takeIf { it.isNotBlank() },
                            latitude = null,
                            longitude = null,
                            contactName = state.contactName.trim(),
                            contactPhone = state.contactPhone,
                            isDefault = state.isDefault
                        )
                    )
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    _addressFormUiState.value = _addressFormUiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                    // Refresh address list
                    loadAddresses()
                } else {
                    _addressFormUiState.value = _addressFormUiState.value.copy(
                        isSaving = false,
                        error = response.body()?.message ?: "Failed to save address"
                    )
                }
            } catch (e: Exception) {
                _addressFormUiState.value = _addressFormUiState.value.copy(
                    isSaving = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            _addressUiState.value = _addressUiState.value.copy(isDeleting = true, error = null)

            try {
                val response = apiService.deleteAddress(addressId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _addressUiState.value = _addressUiState.value.copy(
                        addresses = _addressUiState.value.addresses.filter { it.id != addressId },
                        isDeleting = false,
                        deleteSuccess = true
                    )
                } else {
                    _addressUiState.value = _addressUiState.value.copy(
                        isDeleting = false,
                        error = response.body()?.message ?: "Failed to delete address"
                    )
                }
            } catch (e: Exception) {
                _addressUiState.value = _addressUiState.value.copy(
                    isDeleting = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun setDefaultAddress(addressId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.setDefaultAddress(addressId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Update local state
                    _addressUiState.value = _addressUiState.value.copy(
                        addresses = _addressUiState.value.addresses.map { address ->
                            address.copy(isDefault = address.id == addressId)
                        }
                    )
                }
            } catch (_: Exception) {
                // Silently fail
            }
        }
    }

    fun clearAddressFormSuccess() {
        _addressFormUiState.value = _addressFormUiState.value.copy(saveSuccess = false)
    }

    fun clearAddressDeleteSuccess() {
        _addressUiState.value = _addressUiState.value.copy(deleteSuccess = false)
    }

    // ============= Settings Methods =============

    fun updateNotificationsEnabled(enabled: Boolean) {
        updateNotificationPreference { it.copy(notificationsEnabled = enabled) }
    }

    fun updateEmailNotificationsEnabled(enabled: Boolean) {
        updateNotificationPreference { it.copy(emailNotificationsEnabled = enabled) }
    }

    fun updateSmsNotificationsEnabled(enabled: Boolean) {
        updateNotificationPreference { it.copy(smsNotificationsEnabled = enabled) }
    }

    fun updatePushNotificationsEnabled(enabled: Boolean) {
        updateNotificationPreference { it.copy(pushNotificationsEnabled = enabled) }
    }

    private fun updateNotificationPreference(update: (SettingsUiState) -> SettingsUiState) {
        val newState = update(_settingsUiState.value)
        _settingsUiState.value = newState.copy(isUpdating = true)

        viewModelScope.launch {
            try {
                val response = apiService.updateNotificationPreferences(
                    UpdateNotificationPreferencesRequest(
                        notificationsEnabled = newState.notificationsEnabled,
                        emailNotificationsEnabled = newState.emailNotificationsEnabled,
                        smsNotificationsEnabled = newState.smsNotificationsEnabled,
                        pushNotificationsEnabled = newState.pushNotificationsEnabled
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    _settingsUiState.value = _settingsUiState.value.copy(isUpdating = false)
                    // Update profile state
                    _uiState.value = _uiState.value.copy(profile = response.body()?.data)
                } else {
                    // Revert on failure
                    val profile = _uiState.value.profile
                    _settingsUiState.value = _settingsUiState.value.copy(
                        isUpdating = false,
                        notificationsEnabled = profile?.notificationsEnabled ?: true,
                        emailNotificationsEnabled = profile?.emailNotificationsEnabled ?: true,
                        smsNotificationsEnabled = profile?.smsNotificationsEnabled ?: true,
                        pushNotificationsEnabled = profile?.pushNotificationsEnabled ?: true,
                        error = response.body()?.message ?: "Failed to update preferences"
                    )
                }
            } catch (e: Exception) {
                // Revert on error
                val profile = _uiState.value.profile
                _settingsUiState.value = _settingsUiState.value.copy(
                    isUpdating = false,
                    notificationsEnabled = profile?.notificationsEnabled ?: true,
                    emailNotificationsEnabled = profile?.emailNotificationsEnabled ?: true,
                    smsNotificationsEnabled = profile?.smsNotificationsEnabled ?: true,
                    pushNotificationsEnabled = profile?.pushNotificationsEnabled ?: true,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun clearCache() {
        // In a real app, this would clear cached data
        _settingsUiState.value = _settingsUiState.value.copy(cacheCleared = true)
    }

    fun clearCacheFlag() {
        _settingsUiState.value = _settingsUiState.value.copy(cacheCleared = false)
    }

    fun deleteAccount(reason: String?, confirmPhone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAccount = true, error = null)

            try {
                val response = apiService.deleteAccount(
                    DeleteAccountRequest(
                        reason = reason?.trim()?.takeIf { it.isNotBlank() },
                        confirmPhone = confirmPhone
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    tokenManager.clearAll()
                    _uiState.value = _uiState.value.copy(
                        isDeletingAccount = false,
                        accountDeleted = true
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDeletingAccount = false,
                        error = response.body()?.message ?: "Failed to delete account"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            try {
                apiService.logout()
            } catch (_: Exception) {}
            tokenManager.clearAll()
            onLogout()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _addressUiState.value = _addressUiState.value.copy(error = null)
        _editProfileUiState.value = _editProfileUiState.value.copy(error = null)
        _addressFormUiState.value = _addressFormUiState.value.copy(error = null)
        _settingsUiState.value = _settingsUiState.value.copy(error = null)
    }

    // ============= Validation Helpers =============

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPincode(pincode: String): Boolean {
        return pincode.length == 6 && pincode.all { it.isDigit() }
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length == 10 && phone.all { it.isDigit() }
    }

    fun getUserPhone(): String {
        return _uiState.value.profile?.phone ?: ""
    }

    companion object {
        const val APP_VERSION = "1.0.0"
    }
}
