package com.poultry.buyer.ui.screens.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

/**
 * Payment method options
 */
enum class PaymentMethod {
    ONLINE,     // Razorpay payment
    COD         // Cash on Delivery (if supported)
}

data class CheckoutUiState(
    val cart: Cart? = null,
    val isLoadingCart: Boolean = false,
    val cartError: String? = null,

    // Delivery Address
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val landmark: String = "",
    val contactName: String = "",
    val contactPhone: String = "",

    // Delivery Details
    val deliveryDate: String? = null,
    val deliverySlot: String? = null,
    val deliveryInstructions: String = "",

    // Payment
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.ONLINE,
    val isCodAvailable: Boolean = false,

    // Order Creation
    val isCreatingOrder: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val createdOrder: Order? = null,
    val placedOrder: Order? = null,
    val orderError: String? = null,

    // Navigation to payment
    val navigateToPayment: String? = null, // Order ID to navigate to payment screen

    // Validation
    val addressErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val apiService: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sellerId: String = savedStateHandle.get<String>("sellerId") ?: ""

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    val deliverySlots = listOf(
        "Morning (6 AM - 9 AM)",
        "Mid-Morning (9 AM - 12 PM)",
        "Afternoon (12 PM - 3 PM)",
        "Evening (3 PM - 6 PM)"
    )

    init {
        loadCart()
    }

    private fun loadCart() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCart = true, cartError = null)

            try {
                val response = apiService.getCart(sellerId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        cart = response.body()?.data,
                        isLoadingCart = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingCart = false,
                        cartError = response.body()?.message ?: "Failed to load cart"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingCart = false,
                    cartError = "Network error. Please try again."
                )
            }
        }
    }

    fun updateAddressLine1(value: String) {
        _uiState.value = _uiState.value.copy(
            addressLine1 = value,
            addressErrors = _uiState.value.addressErrors - "addressLine1"
        )
    }

    fun updateAddressLine2(value: String) {
        _uiState.value = _uiState.value.copy(addressLine2 = value)
    }

    fun updateCity(value: String) {
        _uiState.value = _uiState.value.copy(
            city = value,
            addressErrors = _uiState.value.addressErrors - "city"
        )
    }

    fun updateState(value: String) {
        _uiState.value = _uiState.value.copy(
            state = value,
            addressErrors = _uiState.value.addressErrors - "state"
        )
    }

    fun updatePincode(value: String) {
        _uiState.value = _uiState.value.copy(
            pincode = value.filter { it.isDigit() }.take(6),
            addressErrors = _uiState.value.addressErrors - "pincode"
        )
    }

    fun updateLandmark(value: String) {
        _uiState.value = _uiState.value.copy(landmark = value)
    }

    fun updateContactName(value: String) {
        _uiState.value = _uiState.value.copy(
            contactName = value,
            addressErrors = _uiState.value.addressErrors - "contactName"
        )
    }

    fun updateContactPhone(value: String) {
        _uiState.value = _uiState.value.copy(
            contactPhone = value.filter { it.isDigit() }.take(10),
            addressErrors = _uiState.value.addressErrors - "contactPhone"
        )
    }

    fun updateDeliveryDate(date: String?) {
        _uiState.value = _uiState.value.copy(deliveryDate = date)
    }

    fun updateDeliverySlot(slot: String?) {
        _uiState.value = _uiState.value.copy(deliverySlot = slot)
    }

    fun updateDeliveryInstructions(value: String) {
        _uiState.value = _uiState.value.copy(deliveryInstructions = value)
    }

    fun getAvailableDates(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return (1..7).map { days ->
            LocalDate.now().plusDays(days.toLong()).format(formatter)
        }
    }

    fun formatDateForDisplay(date: String): String {
        return try {
            val parsed = LocalDate.parse(date)
            parsed.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        } catch (e: Exception) {
            date
        }
    }

    private fun validateAddress(): Boolean {
        val errors = mutableMapOf<String, String>()

        if (_uiState.value.addressLine1.isBlank()) {
            errors["addressLine1"] = "Address is required"
        }
        if (_uiState.value.city.isBlank()) {
            errors["city"] = "City is required"
        }
        if (_uiState.value.state.isBlank()) {
            errors["state"] = "State is required"
        }
        if (_uiState.value.pincode.length != 6) {
            errors["pincode"] = "Valid 6-digit pincode required"
        }
        if (_uiState.value.contactName.isBlank()) {
            errors["contactName"] = "Contact name is required"
        }
        if (_uiState.value.contactPhone.length != 10) {
            errors["contactPhone"] = "Valid 10-digit phone required"
        }

        _uiState.value = _uiState.value.copy(addressErrors = errors)
        return errors.isEmpty()
    }

    /**
     * Update selected payment method
     */
    fun updatePaymentMethod(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = method)
    }

    /**
     * Place order - creates order and navigates to payment if online payment selected
     */
    fun placeOrder() {
        if (!validateAddress()) return

        val cart = _uiState.value.cart ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCreatingOrder = true,
                orderError = null
            )

            try {
                val state = _uiState.value
                val deliveryAddress = DeliveryAddress(
                    label = "Delivery Address",
                    line1 = state.addressLine1,
                    line2 = state.addressLine2.ifBlank { null },
                    city = state.city,
                    state = state.state,
                    pincode = state.pincode,
                    landmark = state.landmark.ifBlank { null },
                    latitude = null,
                    longitude = null,
                    contactName = state.contactName,
                    contactPhone = state.contactPhone
                )

                val orderRequest = CreateOrderRequest(
                    idempotencyKey = UUID.randomUUID().toString(),
                    sellerId = sellerId,
                    type = "REGULAR",
                    items = cart.items.map { item ->
                        CreateOrderItemRequest(
                            productId = item.productId,
                            quantity = item.quantity
                        )
                    },
                    deliveryAddress = deliveryAddress,
                    deliveryDate = state.deliveryDate,
                    deliverySlot = state.deliverySlot,
                    deliveryInstructions = state.deliveryInstructions.ifBlank { null }
                )

                val createResponse = apiService.createOrder(orderRequest)
                if (createResponse.isSuccessful && createResponse.body()?.success == true) {
                    val createdOrder = createResponse.body()?.data
                    _uiState.value = _uiState.value.copy(
                        createdOrder = createdOrder,
                        isCreatingOrder = false
                    )

                    if (createdOrder != null) {
                        when (state.selectedPaymentMethod) {
                            PaymentMethod.ONLINE -> {
                                // Navigate to payment screen for online payment
                                _uiState.value = _uiState.value.copy(
                                    navigateToPayment = createdOrder.id
                                )
                            }
                            PaymentMethod.COD -> {
                                // For COD, directly place the order
                                placeOrderDirectly(createdOrder.id)
                            }
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCreatingOrder = false,
                        orderError = createResponse.body()?.message ?: "Failed to create order"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreatingOrder = false,
                    isPlacingOrder = false,
                    orderError = "Network error. Please try again."
                )
            }
        }
    }

    /**
     * Place order directly (for COD or after payment)
     */
    private suspend fun placeOrderDirectly(orderId: String) {
        _uiState.value = _uiState.value.copy(isPlacingOrder = true)

        try {
            val placeResponse = apiService.placeOrder(orderId)
            if (placeResponse.isSuccessful && placeResponse.body()?.success == true) {
                _uiState.value = _uiState.value.copy(
                    placedOrder = placeResponse.body()?.data,
                    isPlacingOrder = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    orderError = placeResponse.body()?.message ?: "Failed to place order"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isPlacingOrder = false,
                orderError = "Network error. Please try again."
            )
        }
    }

    /**
     * Clear navigation state after navigating to payment
     */
    fun onNavigatedToPayment() {
        _uiState.value = _uiState.value.copy(navigateToPayment = null)
    }

    fun clearOrderError() {
        _uiState.value = _uiState.value.copy(orderError = null)
    }

    fun retry() {
        loadCart()
    }
}
