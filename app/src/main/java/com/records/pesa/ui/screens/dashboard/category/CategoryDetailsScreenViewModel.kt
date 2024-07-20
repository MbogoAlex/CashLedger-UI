package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.models.TransactionCategory
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.transactionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryDetailsScreenUiState(
    val categoryId: String = "",
    val category: TransactionCategory = transactionCategory
)
class CategoryDetailsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(CategoryDetailsScreenUiState())
    val uiState: StateFlow<CategoryDetailsScreenUiState> = _uiState.asStateFlow()

    private val categoryId: String? = savedStateHandle[CategoryDetailsScreenDestination.categoryId]
    fun getCategory() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getCategoryDetails(categoryId = uiState.value.categoryId.toInt())
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            category = response.body()?.data?.category!!,
                        )
                    }
                } else {
                    Log.e("CategoryDetailsScreenViewModel", "getCategory: $response")
                }
            } catch (e: Exception) {
                Log.e("CategoryDetailsScreenViewModel", "getCategory: $e")
            }
        }
    }

    init {
        if(categoryId != null) {
            _uiState.update {
                it.copy(
                    categoryId = categoryId
                )
            }
            getCategory()
        }
    }
}