package com.poultry.buyer.ui.screens.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.domain.model.AddToCartRequest
import com.poultry.buyer.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val quantity: Double = 1.0,
    val isLoading: Boolean = false,
    val isAddingToCart: Boolean = false,
    val error: String? = null,
    val addedToCart: Boolean = false,
    val cartError: String? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String = savedStateHandle.get<String>("productId") ?: ""

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = apiService.getProduct(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val product = response.body()?.data
                    _uiState.value = _uiState.value.copy(
                        product = product,
                        quantity = product?.minOrderQty ?: 1.0,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Failed to load product"
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

    fun updateQuantity(quantity: Double) {
        val product = _uiState.value.product ?: return
        val minQty = product.minOrderQty
        val maxQty = product.maxOrderQty

        val validatedQty = when {
            quantity < minQty -> minQty
            maxQty != null && quantity > maxQty -> maxQty
            else -> quantity
        }

        _uiState.value = _uiState.value.copy(quantity = validatedQty)
    }

    fun addToCart() {
        val product = _uiState.value.product ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAddingToCart = true,
                cartError = null,
                addedToCart = false
            )

            try {
                val request = AddToCartRequest(
                    productId = product.id,
                    quantity = _uiState.value.quantity,
                    notes = null
                )

                val response = apiService.addToCart(product.sellerId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isAddingToCart = false,
                        addedToCart = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isAddingToCart = false,
                        cartError = response.body()?.message ?: "Failed to add to cart"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingToCart = false,
                    cartError = "Network error. Please try again."
                )
            }
        }
    }

    fun clearCartStatus() {
        _uiState.value = _uiState.value.copy(
            addedToCart = false,
            cartError = null
        )
    }

    fun retry() {
        loadProduct()
    }

    fun calculateDiscountedPrice(): Double? {
        val product = _uiState.value.product ?: return null
        val price = product.currentPrice ?: return null
        val quantity = _uiState.value.quantity

        // Check for bulk discounts
        val applicableSlab = price.bulkDiscountSlabs
            ?.filter { quantity >= it.minQty && (it.maxQty == null || quantity <= it.maxQty) }
            ?.maxByOrNull { it.discountPercent }

        return if (applicableSlab != null) {
            price.basePrice * (1 - applicableSlab.discountPercent / 100)
        } else {
            null
        }
    }

    fun getLineTotal(): Double {
        val product = _uiState.value.product ?: return 0.0
        val price = product.currentPrice?.basePrice ?: return 0.0
        val discountedPrice = calculateDiscountedPrice() ?: price
        return discountedPrice * _uiState.value.quantity
    }
}
