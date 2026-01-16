package com.poultry.buyer.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.domain.model.Cart
import com.poultry.buyer.domain.model.UpdateCartItemRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartListUiState(
    val carts: List<Cart> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val updatingItems: Set<String> = emptySet(), // productId set for items being updated
    val removingItems: Set<String> = emptySet()  // productId set for items being removed
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartListUiState())
    val uiState: StateFlow<CartListUiState> = _uiState.asStateFlow()

    init {
        loadCarts()
    }

    fun loadCarts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = apiService.getCarts()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        carts = response.body()?.data ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Failed to load carts"
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

            try {
                val response = apiService.getCarts()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        carts = response.body()?.data ?: emptyList(),
                        isRefreshing = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = response.body()?.message ?: "Failed to refresh carts"
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

    fun updateItemQuantity(sellerId: String, productId: String, quantity: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                updatingItems = _uiState.value.updatingItems + productId
            )

            try {
                val request = UpdateCartItemRequest(quantity = quantity, notes = null)
                val response = apiService.updateCartItem(sellerId, productId, request)

                if (response.isSuccessful && response.body()?.success == true) {
                    // Update the cart in the list with the response
                    val updatedCart = response.body()?.data
                    if (updatedCart != null) {
                        _uiState.value = _uiState.value.copy(
                            carts = _uiState.value.carts.map { cart ->
                                if (cart.sellerId == sellerId) updatedCart else cart
                            },
                            updatingItems = _uiState.value.updatingItems - productId
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            updatingItems = _uiState.value.updatingItems - productId
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        updatingItems = _uiState.value.updatingItems - productId,
                        error = response.body()?.message ?: "Failed to update quantity"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    updatingItems = _uiState.value.updatingItems - productId,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun removeItem(sellerId: String, productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                removingItems = _uiState.value.removingItems + productId
            )

            try {
                val response = apiService.removeFromCart(sellerId, productId)

                if (response.isSuccessful && response.body()?.success == true) {
                    // Remove the item from the cart locally
                    val updatedCarts = _uiState.value.carts.mapNotNull { cart ->
                        if (cart.sellerId == sellerId) {
                            val updatedItems = cart.items.filter { it.productId != productId }
                            if (updatedItems.isEmpty()) {
                                null // Remove entire cart if no items left
                            } else {
                                cart.copy(
                                    items = updatedItems,
                                    itemCount = updatedItems.size,
                                    totalAmount = updatedItems.sumOf { it.lineTotal }
                                )
                            }
                        } else {
                            cart
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        carts = updatedCarts,
                        removingItems = _uiState.value.removingItems - productId
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        removingItems = _uiState.value.removingItems - productId,
                        error = response.body()?.message ?: "Failed to remove item"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    removingItems = _uiState.value.removingItems - productId,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getTotalItemCount(): Int {
        return _uiState.value.carts.sumOf { it.itemCount }
    }

    fun getGrandTotal(): Double {
        return _uiState.value.carts.sumOf { it.totalAmount }
    }
}
