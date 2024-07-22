package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.models.TransactionCategory
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesScreenUiState(
    val categories: List<TransactionCategory> = emptyList(),
    val name: String = "",
    val orderOptions: List<String> = listOf("latest", "amount"),
    val orderBy: String = "latest",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class CategoriesScreenViewModel(
    private val apiRepository: ApiRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesScreenUiState())
    val uiState: StateFlow<CategoriesScreenUiState> = _uiState.asStateFlow()

    fun updateOrderBy(orderBy: String) {
        _uiState.update {
            it.copy(
                orderBy = orderBy
            )
        }
        getUserCategories()
    }
    fun getUserCategories() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getUserCategories(1, null, uiState.value.orderBy)
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            categories = response.body()?.data?.category!!
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("getUserCategoriesErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("getUserCategoriesErrorException", e.toString())
            }
        }
    }

    init {
        getUserCategories()
    }

}