package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val categories: List<TransactionCategory> = emptyList(),
    val name: String = "",
    val orderOptions: List<String> = listOf("latest", "amount"),
    val orderBy: String = "latest",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class CategoriesScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
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

    fun updateName(name: String) {
        _uiState.update {
            it.copy(
                name = name
            )
        }
        getUserCategories()
    }

    fun clearName() {
        _uiState.update {
            it.copy(
                name = ""
            )
        }
        getUserCategories()
    }

    fun getUserCategories() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getUserCategories(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    categoryId = null,
                    name = uiState.value.name,
                    orderBy = uiState.value.orderBy
                )
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

    private fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getUserCategories()
        }
    }

    init {
        getUserDetails()
    }

}