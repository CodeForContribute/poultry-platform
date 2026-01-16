package com.poultry.buyer.ui.screens.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.domain.model.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCancelling: Boolean = false,
    val cancelSuccess: Boolean = false,
    val cancelError: String? = null
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId") ?: ""

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    private fun loadOrder() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = apiService.getOrder(orderId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        order = response.body()?.data,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Failed to load order"
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

    fun cancelOrder(reason: String?) {
        val order = _uiState.value.order ?: return
        if (!order.canCancel) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCancelling = true,
                cancelError = null,
                cancelSuccess = false
            )

            try {
                val response = apiService.cancelOrder(orderId, reason)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        order = response.body()?.data,
                        isCancelling = false,
                        cancelSuccess = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCancelling = false,
                        cancelError = response.body()?.message ?: "Failed to cancel order"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCancelling = false,
                    cancelError = "Network error. Please try again."
                )
            }
        }
    }

    fun clearCancelStatus() {
        _uiState.value = _uiState.value.copy(
            cancelSuccess = false,
            cancelError = null
        )
    }

    fun retry() {
        loadOrder()
    }

    fun refresh() {
        loadOrder()
    }
}
