package com.poultry.buyer.ui.screens.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.core.payment.PaymentManager
import com.poultry.buyer.core.payment.PaymentResult
import com.poultry.buyer.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Payment status for tracking UI state
 */
enum class PaymentStatus {
    IDLE,
    LOADING_ORDER,
    CREATING_PAYMENT,
    AWAITING_PAYMENT,
    PROCESSING,
    VERIFYING,
    SUCCESS,
    FAILED,
    CANCELLED
}

/**
 * UI state for payment screen
 */
data class PaymentUiState(
    val status: PaymentStatus = PaymentStatus.IDLE,
    val order: Order? = null,
    val payment: Payment? = null,
    val error: String? = null,
    val errorCode: Int? = null,

    // Payment result data
    val razorpayPaymentId: String? = null,
    val razorpayOrderId: String? = null,
    val razorpaySignature: String? = null,

    // Verification
    val isVerified: Boolean = false,
    val verificationError: String? = null
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val apiService: ApiService,
    private val paymentManager: PaymentManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val orderId: String = savedStateHandle.get<String>("orderId") ?: ""

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        observePaymentResult()
        if (orderId.isNotEmpty()) {
            loadOrderAndCreatePayment()
        }
    }

    /**
     * Observe payment results from PaymentManager
     */
    private fun observePaymentResult() {
        viewModelScope.launch {
            paymentManager.paymentResult.collectLatest { result ->
                when (result) {
                    is PaymentResult.Idle -> {
                        // No action needed
                    }
                    is PaymentResult.Processing -> {
                        _uiState.value = _uiState.value.copy(
                            status = PaymentStatus.PROCESSING
                        )
                    }
                    is PaymentResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            status = PaymentStatus.VERIFYING,
                            razorpayPaymentId = result.razorpayPaymentId,
                            razorpayOrderId = result.razorpayOrderId,
                            razorpaySignature = result.razorpaySignature
                        )
                        verifyPayment(
                            result.razorpayPaymentId,
                            result.razorpayOrderId,
                            result.razorpaySignature
                        )
                    }
                    is PaymentResult.Failure -> {
                        val errorMessage = when (result.errorCode) {
                            com.razorpay.Checkout.PAYMENT_CANCELED -> "Payment cancelled"
                            com.razorpay.Checkout.NETWORK_ERROR -> "Network error. Please check your connection."
                            else -> result.errorDescription
                        }
                        _uiState.value = _uiState.value.copy(
                            status = if (result.errorCode == com.razorpay.Checkout.PAYMENT_CANCELED) {
                                PaymentStatus.CANCELLED
                            } else {
                                PaymentStatus.FAILED
                            },
                            error = errorMessage,
                            errorCode = result.errorCode
                        )
                    }
                    is PaymentResult.ExternalWallet -> {
                        // Handle external wallet selection if needed
                        _uiState.value = _uiState.value.copy(
                            status = PaymentStatus.PROCESSING
                        )
                    }
                }
            }
        }
    }

    /**
     * Load order details and create payment
     */
    fun loadOrderAndCreatePayment() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = PaymentStatus.LOADING_ORDER,
                error = null
            )

            try {
                // Load order details
                val orderResponse = apiService.getOrder(orderId)
                if (!orderResponse.isSuccessful || orderResponse.body()?.success != true) {
                    _uiState.value = _uiState.value.copy(
                        status = PaymentStatus.FAILED,
                        error = orderResponse.body()?.message ?: "Failed to load order"
                    )
                    return@launch
                }

                val order = orderResponse.body()?.data!!
                _uiState.value = _uiState.value.copy(
                    order = order,
                    status = PaymentStatus.CREATING_PAYMENT
                )

                // Create payment
                createPayment(order.id)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = PaymentStatus.FAILED,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    /**
     * Create payment order via API
     */
    private suspend fun createPayment(orderId: String) {
        try {
            val response = apiService.createPayment(CreatePaymentRequest(orderId = orderId))

            if (response.isSuccessful && response.body()?.success == true) {
                val payment = response.body()?.data!!
                _uiState.value = _uiState.value.copy(
                    payment = payment,
                    status = PaymentStatus.AWAITING_PAYMENT
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    status = PaymentStatus.FAILED,
                    error = response.body()?.message ?: "Failed to create payment"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                status = PaymentStatus.FAILED,
                error = "Network error while creating payment."
            )
        }
    }

    /**
     * Get payment object for Razorpay checkout
     */
    fun getPaymentForCheckout(): Payment? = _uiState.value.payment

    /**
     * Get PaymentManager instance for Activity to use
     */
    fun getPaymentManager(): PaymentManager = paymentManager

    /**
     * Verify payment on backend after successful Razorpay payment
     */
    private fun verifyPayment(
        razorpayPaymentId: String,
        razorpayOrderId: String,
        razorpaySignature: String
    ) {
        viewModelScope.launch {
            try {
                val paymentId = _uiState.value.payment?.id ?: return@launch

                // Poll payment status to verify
                val response = apiService.getPayment(paymentId)

                if (response.isSuccessful && response.body()?.success == true) {
                    val payment = response.body()?.data!!

                    when (payment.status.uppercase()) {
                        "CAPTURED", "SUCCESS", "PAID" -> {
                            _uiState.value = _uiState.value.copy(
                                status = PaymentStatus.SUCCESS,
                                payment = payment,
                                isVerified = true
                            )
                        }
                        "PENDING", "CREATED", "AUTHORIZED" -> {
                            // Payment still processing - wait and retry
                            kotlinx.coroutines.delay(2000)
                            verifyPayment(razorpayPaymentId, razorpayOrderId, razorpaySignature)
                        }
                        "FAILED", "REFUNDED" -> {
                            _uiState.value = _uiState.value.copy(
                                status = PaymentStatus.FAILED,
                                error = payment.errorDescription ?: "Payment verification failed",
                                isVerified = false
                            )
                        }
                        else -> {
                            // Unknown status - mark as success if we have payment ID
                            _uiState.value = _uiState.value.copy(
                                status = PaymentStatus.SUCCESS,
                                payment = payment,
                                isVerified = true
                            )
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        status = PaymentStatus.SUCCESS, // Razorpay succeeded, just verification API failed
                        verificationError = "Could not verify payment status. Please check your orders."
                    )
                }
            } catch (e: Exception) {
                // Even if verification fails, Razorpay payment succeeded
                _uiState.value = _uiState.value.copy(
                    status = PaymentStatus.SUCCESS,
                    verificationError = "Could not verify payment status. Please check your orders."
                )
            }
        }
    }

    /**
     * Retry payment after failure
     */
    fun retryPayment() {
        paymentManager.resetPaymentState()
        _uiState.value = PaymentUiState()
        loadOrderAndCreatePayment()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset payment state
     */
    fun resetState() {
        paymentManager.resetPaymentState()
        _uiState.value = PaymentUiState()
    }

    override fun onCleared() {
        super.onCleared()
        paymentManager.resetPaymentState()
    }
}
