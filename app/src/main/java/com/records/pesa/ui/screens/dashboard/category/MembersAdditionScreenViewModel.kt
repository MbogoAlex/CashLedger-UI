package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.CategoryEditPayload
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactionCategory
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class MembersAdditionScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val newMembers: List<TransactionItem> = emptyList(),
    val currentMembers: List<TransactionItem> = emptyList(),
    val membersToAdd: List<TransactionItem> = emptyList(),
    val membersToDisplay: List<TransactionItem> = emptyList(),
    val transactions: List<TransactionItem> = emptyList(),
    val distinctTransactions: List<TransactionItem> = emptyList(),
    val category: TransactionCategory = transactionCategory,
    val entity: String = "",
    val addAllMembersThatContainEntity: Boolean = false,
    val categoryId: String = "",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
@RequiresApi(Build.VERSION_CODES.O)
class MembersAdditionScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService,
    private val categoryService: CategoryService
): ViewModel() {

    private val _uiState = MutableStateFlow(MembersAdditionScreenUiState())
    val uiState: StateFlow<MembersAdditionScreenUiState> = _uiState.asStateFlow()

    private val membersToDisplay = mutableStateListOf<TransactionItem>()
    private val addedKeywords = mutableStateListOf<String>()
    private val membersToAdd = mutableStateListOf<TransactionItem>()
    val categoryKeywords = mutableStateListOf<String>()
    private val categoryId: String? = savedStateHandle[MembersAdditionScreenDestination.categoryId]

    private val endDate = LocalDate.now().toString()

    private var filterJob: Job? = null

    fun updateSearchText(searchText: String) {
        _uiState.update {
            it.copy(
                entity = searchText
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            getTransactions()
        }
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

    fun addMembersThatContainsEntity(add: Boolean) {
        Log.d("DISTINCT_TRANSACTIONS_SIZE", uiState.value.distinctTransactions.size.toString())
        if(add) {
            membersToDisplay.clear()
            membersToAdd.clear()
            addedKeywords.clear()
            for(transaction in uiState.value.distinctTransactions) {
                if(transaction.entity.lowercase().contains(uiState.value.entity.lowercase())) {
                    if(!categoryKeywords.contains(transaction.entity)) {
                        membersToAdd.add(transaction)
                        membersToDisplay.remove(transaction)
                    }
                    if(transaction.transactionAmount < 0) {
                        addedKeywords.add(transaction.recipient)
                    } else if(transaction.transactionAmount > 0) {
                        addedKeywords.add(transaction.sender)
                    }
                }
            }
        } else {
            membersToDisplay.clear()
            membersToAdd.clear()
            addedKeywords.clear()
            for(transaction in uiState.value.distinctTransactions) {
                if(transaction.entity.lowercase().contains(uiState.value.entity.lowercase())) {
                    if(!categoryKeywords.contains(transaction.entity)) {
                        membersToAdd.remove(transaction)
                        membersToDisplay.add(transaction)
                    }

                    if(transaction.transactionAmount < 0) {
                        addedKeywords.add(transaction.recipient)
                    } else if(transaction.transactionAmount > 0) {
                        addedKeywords.add(transaction.sender)
                    }
                }
            }
        }
        _uiState.update {
            it.copy(
                addAllMembersThatContainEntity = add,
                membersToAdd = membersToAdd,
                membersToDisplay = membersToDisplay
            )
        }
    }

    fun addMembersToCategory() {
        filterJob?.cancel()
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

        filterJob = viewModelScope.launch {
            delay(500L)
            withContext(Dispatchers.IO) {
                if(uiState.value.addAllMembersThatContainEntity) {
                    try {
                        val category = categoryService.getRawCategoryById(uiState.value.categoryId.toInt()).first()
                        val updatedContains = category.contains.toMutableList()
                        updatedContains.add(uiState.value.entity)
                        categoryService.updateCategory(
                            category.copy(
                                contains = updatedContains
                            )
                        )
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                loadingStatus = LoadingStatus.FAIL
                            )
                        }
                        Log.e("AddContainsToCategoryException", e.toString())
                    }

                }

                for(transaction in uiState.value.membersToAdd) {
                    val query = transactionService.createUserTransactionQuery(
                        userId = uiState.value.userDetails.userId,
                        entity = transaction.entity,
                        categoryId = null,
                        budgetId = null,
                        transactionType = null,
                        moneyDirection = null,
                        startDate = LocalDate.now().minusYears(10),
                        endDate = LocalDate.now(),
                        latest = true
                    )
                    val categoryKeyword = CategoryKeyword(
                        keyword = transaction.entity,
                        nickName = null,
                        categoryId = uiState.value.categoryId.toInt()
                    )
                    try {
                        categoryService.insertCategoryKeyword(categoryKeyword)
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                loadingStatus = LoadingStatus.FAIL
                            )
                        }
                    }
                    val transactions = transactionService.getUserTransactions(query).first().map { it.toTransactionItem() }
                    for(transaction2 in transactions) {

                        val transactionCategoryCrossRef = TransactionCategoryCrossRef(
                            categoryId = uiState.value.categoryId.toInt(),
                            transactionId = transaction2.transactionId!!
                        )
                        try {
                            categoryService.insertCategoryTransactionMapping(transactionCategoryCrossRef)
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
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.SUCCESS
                    )
                }
            }
//            try {
//               val response = apiRepository.addCategoryMembers(
//                   token = uiState.value.userDetails.token,
//                   categoryId = uiState.value.categoryId.toInt(),
//                   category = category
//               )
//                if(response.isSuccessful) {
//                    _uiState.update {
//                        it.copy(
//                            loadingStatus = LoadingStatus.SUCCESS
//                        )
//                    }
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            loadingStatus = LoadingStatus.FAIL
//                        )
//                    }
//                    Log.e("AddMembersToCategoryResponseError", response.toString())
//                }
//            } catch (e: Exception) {
//                _uiState.update {
//                    it.copy(
//                        loadingStatus = LoadingStatus.FAIL
//                    )
//                }
//                Log.e("AddMembersToCategoryException", e.toString())
//            }
        }
    }

    fun getTransactions() {
        Log.i("CALLED", "SEARCHING FOR ${uiState.value.entity}")
        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.userId,
            entity = uiState.value.entity,
            categoryId = null,
            budgetId = null,
            transactionType = null,
            moneyDirection = null,
            startDate = LocalDate.now().minusYears(10),
            endDate = LocalDate.now(),
            latest = true
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    membersToDisplay.clear()
                    val transactions = transactionService.getUserTransactions(query).first().map { it.toTransactionItem() }
                    val distinctTransactions = transactions.distinctBy { it.entity }
                    val newKeywords = mutableListOf<String>()

                    Log.i("DISTINCT_TRANSACTIONS_SIZE", distinctTransactions.size.toString())

                    for(transaction in distinctTransactions) {
                        val keyword = transaction.entity
                        if(!categoryKeywords.contains(keyword) && !newKeywords.contains(keyword) && !addedKeywords.contains(keyword)) {
                            membersToDisplay.add(transaction)
                            newKeywords.add(keyword)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            transactions = transactions,
                            distinctTransactions = distinctTransactions,
                            membersToDisplay = membersToDisplay
                        )
                    }
                } catch (e: Exception) {
                    Log.e("GetTransactionsException", e.toString())
                }
            }
//            try {
//                val response = apiRepository.getTransactions(
//                    token = uiState.value.userDetails.token,
//                    userId = uiState.value.userDetails.userId,
//                    entity = uiState.value.entity,
//                    categoryId = null,
//                    budgetId = null,
//                    transactionType = null,
//                    latest = true,
//                    moneyDirection = null,
//                    startDate = "2002-03-06",
//                    endDate = endDate
//                )
//                if(response.isSuccessful) {
//                    membersToDisplay.clear()
//                    Log.i("CATEGORY_TRANSACTIONS_SIZE", uiState.value.category.transactions.size.toString())
//                    Log.i("ALL_TRANSACTIONS_SIZE", response.body()?.data?.transaction?.transactions!!.size.toString())
//                    val transactions: List<TransactionItem> = response.body()?.data?.transaction?.transactions!!
//                    val distinctTransactions = transactions.distinct()
//                    val newKeywords = mutableListOf<String>()
//
//                    Log.i("DISTINCT_TRANSACTIONS_SIZE", distinctTransactions.size.toString())
//
//                    for(transaction in distinctTransactions) {
//                        var keyword = ""
//                        if(transaction.transactionAmount < 0) {
//                            keyword = transaction.recipient
//                        } else if(transaction.transactionAmount > 0) {
//                            keyword = transaction.sender
//                        }
//                        if(!categoryKeywords.contains(keyword) && !newKeywords.contains(keyword) && !addedKeywords.contains(keyword)) {
//                            membersToDisplay.add(transaction)
//                            if(transaction.transactionAmount < 0) {
//                                newKeywords.add(transaction.recipient)
//                            } else if(transaction.transactionAmount > 0) {
//                                newKeywords.add(transaction.sender)
//                            }
//                        }
//                    }
//
//                    _uiState.update {
//                        it.copy(
//                            transactions = transactions,
//                            membersToDisplay = membersToDisplay
//                        )
//                    }
//                } else {
//                    Log.e("GetTransactionsResponseError", response.toString())
//                }
//
//            } catch (e: Exception) {
//                Log.e("GetTransactionsException", e.toString())
//            }
        }
    }

    fun getCategory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    categoryService.getCategoryById(uiState.value.categoryId.toInt()).collect() {category ->
                        categoryKeywords.clear()
                        for(keyword in category.keywords) {
                            categoryKeywords.add(keyword.keyword)
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
//                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("CategoryDetailsScreenViewModel", "getCategory: $e")
                }
            }
//            try {
//                val response = apiRepository.getCategoryDetails(
//                    token = uiState.value.userDetails.token,
//                    categoryId = uiState.value.categoryId.toInt()
//                )
//                if(response.isSuccessful) {
//                    categoryKeywords.clear()
//                    categoryKeywords.addAll(response.body()?.data?.category!!.keywords.map { it.keyWord })
//                    for(keyword in categoryKeywords) {
//                        Log.i("CATEGORY_KEYWORD: ", keyword)
//                    }
//
//                    _uiState.update {
//                        it.copy(
//                            category = response.body()?.data?.category!!
//                        )
//                    }
//                } else {
//                    Log.e("CategoryDetailsScreenViewModel", "getCategory: $response")
//                }
//            } catch (e: Exception) {
//                Log.e("CategoryDetailsScreenViewModel", "getCategory: $e")
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