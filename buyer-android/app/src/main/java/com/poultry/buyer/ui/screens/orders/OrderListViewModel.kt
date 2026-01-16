package com.poultry.buyer.ui.screens.orders

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

data class OrderListUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderListUiState())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = apiService.getOrders(page = 0, size = PAGE_SIZE)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    _uiState.value = _uiState.value.copy(
                        orders = data?.content ?: emptyList(),
                        currentPage = 0,
                        hasMore = !(data?.last ?: true),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.message ?: "Failed to load orders"
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
                val response = apiService.getOrders(page = 0, size = PAGE_SIZE)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    _uiState.value = _uiState.value.copy(
                        orders = data?.content ?: emptyList(),
                        currentPage = 0,
                        hasMore = !(data?.last ?: true),
                        isRefreshing = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = response.body()?.message ?: "Failed to refresh orders"
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

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            val nextPage = _uiState.value.currentPage + 1
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            try {
                val response = apiService.getOrders(page = nextPage, size = PAGE_SIZE)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val newOrders = data?.content ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        orders = _uiState.value.orders + newOrders,
                        currentPage = nextPage,
                        hasMore = !(data?.last ?: true),
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = response.body()?.message ?: "Failed to load more orders"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
