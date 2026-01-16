package com.poultry.buyer.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poultry.buyer.core.data.preferences.SecureTokenManager
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.domain.model.FcmTokenRequest
import com.poultry.buyer.domain.model.OtpRequest
import com.poultry.buyer.domain.model.OtpVerifyRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val verified: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: SecureTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = tokenManager.isLoggedIn()
        }
    }

    fun requestOtp(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = apiService.requestOtp(
                    OtpRequest(
                        phone = phone,
                        deviceId = getDeviceId(),
                        deviceFingerprint = null,
                        deviceInfo = "Android"
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpSent = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Failed to send OTP"
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

    fun verifyOtp(phone: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val fcmToken = tokenManager.getFcmToken()
                val response = apiService.verifyOtp(
                    OtpVerifyRequest(
                        phone = phone,
                        otp = otp,
                        deviceId = getDeviceId(),
                        deviceFingerprint = null,
                        deviceInfo = "Android",
                        fcmToken = fcmToken
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data!!
                    tokenManager.saveTokens(data.accessToken, data.refreshToken)
                    tokenManager.saveUser(data.userId, data.name)

                    if (tokenManager.hasPendingFcmToken()) {
                        sendPendingFcmToken()
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        verified = true
                    )
                    _isLoggedIn.value = true
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Invalid OTP"
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

    fun logout() {
        viewModelScope.launch {
            try {
                apiService.logout()
            } catch (_: Exception) {}
            tokenManager.clearAll()
            _isLoggedIn.value = false
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun getDeviceId(): String {
        return tokenManager.getDeviceId()
    }

    private fun sendPendingFcmToken() {
        val token = tokenManager.getFcmToken() ?: return
        viewModelScope.launch {
            try {
                val response = apiService.updateFcmToken(
                    FcmTokenRequest(
                        fcmToken = token,
                        deviceId = tokenManager.getDeviceId()
                    )
                )
                if (response.isSuccessful) {
                    tokenManager.markFcmTokenSent()
                }
            } catch (_: Exception) {
            }
        }
    }
}
