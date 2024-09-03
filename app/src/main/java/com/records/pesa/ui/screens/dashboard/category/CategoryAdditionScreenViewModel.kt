package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.models.CategoryEditPayload
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.category.CategoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class CategoryAdditionScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val categoryName: String = "",
    val categoryId: Int = 0,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class CategoryAdditionScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService
): ViewModel() {
    private val _uiState = MutableStateFlow(CategoryAdditionScreenUiState())
    val uiState: StateFlow<CategoryAdditionScreenUiState> = _uiState.asStateFlow()

    fun updateCategoryName(name: String) {
        _uiState.update {
            it.copy(
                categoryName = name
            )
        }
    }

    fun createCategory() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        val categoryEditPayload = CategoryEditPayload(
            userId = uiState.value.userDetails.userId,
            categoryName = uiState.value.categoryName,
            keywords = emptyList(),
        )
        viewModelScope.launch {
            val category = TransactionCategory(
                name = uiState.value.categoryName,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                updatedTimes = 0.0,
                contains = emptyList()
            )
            withContext(Dispatchers.IO) {
                try {
                   val categoryId = categoryService.insertTransactionCategory(category)
                    _uiState.update {
                        it.copy(
                            categoryId = categoryId.toInt(),
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("createCategoryException", e.toString())
                }
            }
//            try {
//               val response = apiRepository.createCategory(
//                   token = uiState.value.userDetails.token,
//                   userId = uiState.value.userDetails.userId,
//                   category = categoryEditPayload
//
//               )
//                if(response.isSuccessful) {
//                    _uiState.update {
//                        it.copy(
//                            categoryId = response.body()?.data?.category?.id!!,
//                            loadingStatus = LoadingStatus.SUCCESS
//                        )
//                    }
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            loadingStatus = LoadingStatus.FAIL
//                        )
//                    }
//                    Log.e("createCategoryErrorResponse", response.toString())
//                }
//            } catch (e: Exception) {
//                _uiState.update {
//                    it.copy(
//                        loadingStatus = LoadingStatus.FAIL
//                    )
//                }
//                Log.e("createCategoryException", e.toString())
//            }
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL
            )
        }
    }

    private fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
        }
    }

    init {
        getUserDetails()
    }
}