package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.CategoryEditPayload
import com.records.pesa.models.CategoryKeyword
import com.records.pesa.models.CategoryKeywordDeletePayload
import com.records.pesa.models.CategoryKeywordEditPayload
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactionCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DeletionStatus {
    INITIAL,
    LOADING,
    SUCCESS,
    FAIL
}

data class CategoryDetailsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val categoryId: String = "",
    val newCategoryName: String = "",
    val newMemberName: String = "",
    val categoryKeyword: CategoryKeyword = CategoryKeyword(0, "", ""),
    val category: TransactionCategory = transactionCategory,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val deletionStatus: DeletionStatus = DeletionStatus.INITIAL
)
class CategoryDetailsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(CategoryDetailsScreenUiState())
    val uiState: StateFlow<CategoryDetailsScreenUiState> = _uiState.asStateFlow()

    private val categoryId: String? = savedStateHandle[CategoryDetailsScreenDestination.categoryId]

    fun editCategoryName(name: String) {
        _uiState.update {
            it.copy(
                newCategoryName = name
            )
        }
    }

    fun editMemberName(name: String) {
        _uiState.update {
            it.copy(
                newMemberName = name
            )
        }
    }

    fun updateCategoryKeyword(categoryKeyword: CategoryKeyword) {
        _uiState.update {
            it.copy(
                categoryKeyword = categoryKeyword
            )
        }
    }

    fun updateCategoryName() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        val keywords = mutableListOf<String>()
        for(keyword in uiState.value.category.keywords){
            keywords.add(keyword.keyWord)
        }
        val category = CategoryEditPayload(
            userId = uiState.value.userDetails.userId,
            categoryName = uiState.value.newCategoryName,
            keywords = keywords,
        )
        viewModelScope.launch {
            try {
                val response = apiRepository.updateCategoryName(
                    token = uiState.value.userDetails.token,
                    categoryId = uiState.value.category.id,
                    category = category
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("editCategoryNameErrorResponse", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("editCategoryNameErrorException", e.toString())
            }
        }
    }

    fun updateMemberName() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }

        val keyword = CategoryKeywordEditPayload(
            id = uiState.value.categoryKeyword.id,
            keyWord = uiState.value.categoryKeyword.keyWord,
            nickName = uiState.value.newMemberName,
        )
        Log.i("KEYWORD_DETAILS", keyword.toString())
        viewModelScope.launch {
            try {
                val response = apiRepository.updateCategoryKeyword(
                    token = uiState.value.userDetails.token,
                    keyword = keyword,
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("editCategoryNameErrorResponse", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("editCategoryNameErrorException", e.toString())
            }
        }
    }

    fun removeCategoryMember(categoryId: Int, keywordId: Int) {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }

        viewModelScope.launch {
            try {
                val response = apiRepository.deleteCategoryKeyword(
                    token = uiState.value.userDetails.token,
                    categoryId = categoryId,
                    keywordId = keywordId
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("deleteCategoryKeywordErrorResponse", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("deleteCategoryKeywordErrorException", e.toString())
            }
        }
    }

    fun removeCategory(categoryId: Int) {
        _uiState.update {
            it.copy(
                deletionStatus = DeletionStatus.LOADING
            )
        }

        viewModelScope.launch {
            try {
                val response = apiRepository.deleteCategory(
                    token = uiState.value.userDetails.token,
                    categoryId = categoryId,
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            deletionStatus = DeletionStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            deletionStatus = DeletionStatus.FAIL
                        )
                    }
                    Log.e("deleteCategoryErrorResponse", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        deletionStatus = DeletionStatus.FAIL
                    )
                }
                Log.e("deleteCategoryErrorException", e.toString())
            }
        }
    }
    fun getCategory() {
        Log.d("getCategoryWithDetails", "${uiState.value.userDetails.token}, ${uiState.value.categoryId}")
        viewModelScope.launch {
            try {
                val response = apiRepository.getCategoryDetails(
                    token = uiState.value.userDetails.token,
                    categoryId = uiState.value.categoryId.toInt()
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            category = response.body()?.data?.category!!,
                        )
                    }
                } else {
                    Log.e("CategoryDetailsResponseError", "getCategory: $response")
                }
            } catch (e: Exception) {
                Log.e("CategoryDetailsException", "getCategory: $e")
            }
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL,
                deletionStatus = DeletionStatus.INITIAL
            )
        }
    }

    fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
            while(uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getCategory()
        }
    }

    init {
        _uiState.update {
            it.copy(
                categoryId = categoryId!!
            )
        }
        getUserDetails()
    }
}