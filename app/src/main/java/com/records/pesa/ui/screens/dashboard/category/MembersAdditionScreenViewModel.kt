package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.CategoryEditPayload
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.TransactionItem
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
import java.time.LocalDate

data class MembersAdditionScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val newMembers: List<TransactionItem> = emptyList(),
    val currentMembers: List<TransactionItem> = emptyList(),
    val membersToAdd: List<TransactionItem> = emptyList(),
    val membersToDisplay: List<TransactionItem> = emptyList(),
    val transactions: List<TransactionItem> = emptyList(),
    val category: TransactionCategory = transactionCategory,
    val entity: String = "",
    val categoryId: String = "",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
@RequiresApi(Build.VERSION_CODES.O)
class MembersAdditionScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(MembersAdditionScreenUiState())
    val uiState: StateFlow<MembersAdditionScreenUiState> = _uiState.asStateFlow()

    private val membersToDisplay = mutableStateListOf<TransactionItem>()
    private val addedKeywords = mutableStateListOf<String>()
    private val membersToAdd = mutableStateListOf<TransactionItem>()
    val categoryKeywords = mutableStateListOf<String>()
    private val categoryId: String? = savedStateHandle[MembersAdditionScreenDestination.categoryId]

    @RequiresApi(Build.VERSION_CODES.O)
    private val endDate = LocalDate.now().toString()

    fun updateSearchText(searchText: String) {
        _uiState.update {
            it.copy(
                entity = searchText
            )
        }
        getTransactions()
    }

    fun addMember(transaction: TransactionItem) {
        membersToAdd.add(transaction)
        membersToDisplay.remove(transaction)
        if(transaction.transactionAmount < 0) {
            addedKeywords.add(transaction.recipient)
        } else if(transaction.transactionAmount > 0) {
            addedKeywords.add(transaction.sender)
        }
        _uiState.update {
            it.copy(
                membersToAdd = membersToAdd,
                membersToDisplay = membersToDisplay
            )
        }
    }

    fun removeMember(transaction: TransactionItem) {
        membersToAdd.remove(transaction)
        membersToDisplay.add(transaction)
        if(transaction.transactionAmount < 0) {
            addedKeywords.remove(transaction.recipient)
        } else if(transaction.transactionAmount > 0) {
            addedKeywords.remove(transaction.sender)
        }
        _uiState.update {
            it.copy(
                membersToAdd = membersToAdd,
                membersToDisplay = membersToDisplay
            )
        }
    }

    fun addMembersToCategory() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        val newMembers = mutableListOf<String>()
        for(member in uiState.value.membersToAdd) {
            if(member.transactionAmount < 0) {
                newMembers.add(member.recipient)
            } else if(member.transactionAmount > 0) {
                newMembers.add(member.sender)
            }

        }

        val category = CategoryEditPayload(
            userId = uiState.value.userDetails.userId,
            categoryName = uiState.value.category.name,
            keywords = newMembers
        )

        viewModelScope.launch {
            try {
               val response = apiRepository.addCategoryMembers(
                   token = uiState.value.userDetails.token,
                   categoryId = uiState.value.categoryId.toInt(),
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
                    Log.e("AddMembersToCategoryResponseError", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("AddMembersToCategoryException", e.toString())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getTransactions() {
        Log.i("CALLED", "SEARCHING FOR ${uiState.value.entity}")
        viewModelScope.launch {
            try {
                val response = apiRepository.getTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = null,
                    budgetId = null,
                    transactionType = null,
                    latest = true,
                    startDate = "2002-03-06",
                    endDate = endDate
                )
                if(response.isSuccessful) {
                    membersToDisplay.clear()
                    Log.i("CATEGORY_TRANSACTIONS_SIZE", uiState.value.category.transactions.size.toString())
                    Log.i("ALL_TRANSACTIONS_SIZE", response.body()?.data?.transaction?.transactions!!.size.toString())
                    val transactions: List<TransactionItem> = response.body()?.data?.transaction?.transactions!!
                    val distinctTransactions = transactions.distinct()
                    val newKeywords = mutableListOf<String>()

                    Log.i("DISTINCT_TRANSACTIONS_SIZE", distinctTransactions.size.toString())

                    for(transaction in distinctTransactions) {
                        var keyword = ""
                        if(transaction.transactionAmount < 0) {
                            keyword = transaction.recipient
                        } else if(transaction.transactionAmount > 0) {
                            keyword = transaction.sender
                        }
                        if(!categoryKeywords.contains(keyword) && !newKeywords.contains(keyword) && !addedKeywords.contains(keyword)) {
                            membersToDisplay.add(transaction)
                            if(transaction.transactionAmount < 0) {
                                newKeywords.add(transaction.recipient)
                            } else if(transaction.transactionAmount > 0) {
                                newKeywords.add(transaction.sender)
                            }
                        }
                    }

                    _uiState.update {
                        it.copy(
                            transactions = transactions,
                            membersToDisplay = membersToDisplay
                        )
                    }
                } else {
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }

    fun getCategory() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getCategoryDetails(
                    token = uiState.value.userDetails.token,
                    categoryId = uiState.value.categoryId.toInt()
                )
                if(response.isSuccessful) {
                    categoryKeywords.clear()
                    categoryKeywords.addAll(response.body()?.data?.category!!.keywords.map { it.keyWord })
                    for(keyword in categoryKeywords) {
                        Log.i("CATEGORY_KEYWORD: ", keyword)
                    }

                    _uiState.update {
                        it.copy(
                            category = response.body()?.data?.category!!
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

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL
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
            getTransactions()
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