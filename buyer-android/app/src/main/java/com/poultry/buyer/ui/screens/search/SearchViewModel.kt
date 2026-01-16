package com.poultry.buyer.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.domain.model.Category
import com.poultry.buyer.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val response = apiService.getCategories()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        categories = response.body()?.data ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                // Categories are optional, don't show error
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            if (query.isNotBlank()) {
                searchProducts(query, resetPage = true)
            } else if (_uiState.value.selectedCategoryId != null) {
                loadProductsByCategory(_uiState.value.selectedCategoryId!!, resetPage = true)
            } else {
                _uiState.value = _uiState.value.copy(products = emptyList())
            }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId,
            query = ""
        )

        if (categoryId != null) {
            loadProductsByCategory(categoryId, resetPage = true)
        } else {
            _uiState.value = _uiState.value.copy(products = emptyList())
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        val state = _uiState.value
        if (state.query.isNotBlank()) {
            searchProducts(state.query, resetPage = false)
        } else if (state.selectedCategoryId != null) {
            loadProductsByCategory(state.selectedCategoryId, resetPage = false)
        }
    }

    fun refresh() {
        val state = _uiState.value
        if (state.query.isNotBlank()) {
            searchProducts(state.query, resetPage = true)
        } else if (state.selectedCategoryId != null) {
            loadProductsByCategory(state.selectedCategoryId, resetPage = true)
        }
    }

    private fun searchProducts(query: String, resetPage: Boolean) {
        viewModelScope.launch {
            val page = if (resetPage) 0 else _uiState.value.currentPage + 1

            _uiState.value = _uiState.value.copy(
                isLoading = resetPage,
                isLoadingMore = !resetPage,
                error = null
            )

            try {
                val response = apiService.searchProducts(query, page, PAGE_SIZE)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val newProducts = data?.content ?: emptyList()

                    _uiState.value = _uiState.value.copy(
                        products = if (resetPage) newProducts else _uiState.value.products + newProducts,
                        currentPage = page,
                        hasMore = !(data?.last ?: true),
                        isLoading = false,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = response.body()?.message ?: "Search failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = "Network error. Please try again."
                )
            }
        }
    }

    private fun loadProductsByCategory(categoryId: String, resetPage: Boolean) {
        viewModelScope.launch {
            val page = if (resetPage) 0 else _uiState.value.currentPage + 1

            _uiState.value = _uiState.value.copy(
                isLoading = resetPage,
                isLoadingMore = !resetPage,
                error = null
            )

            try {
                val response = apiService.getProductsByCategory(categoryId, page, PAGE_SIZE)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val newProducts = data?.content ?: emptyList()

                    _uiState.value = _uiState.value.copy(
                        products = if (resetPage) newProducts else _uiState.value.products + newProducts,
                        currentPage = page,
                        hasMore = !(data?.last ?: true),
                        isLoading = false,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = response.body()?.message ?: "Failed to load products"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
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
