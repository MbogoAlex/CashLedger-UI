package com.records.pesa.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.CategoryWithTransactions
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.DeletedTransaction
import com.records.pesa.db.models.ManualTransaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.mapper.toTransaction
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.TransactionEditPayload
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.itemCategories
import com.records.pesa.reusables.transactions
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

val transactionEx = TransactionItem(
    transactionId = 1,
    nickName = "",
    transactionCode = "",
    transactionType = "",
    transactionAmount = 0.0,
    transactionCost = 0.0,
    date = "",
    time = "",
    sender = "",
    recipient = "",
    balance = 0.0,
    entity = "",
    categories = emptyList(),
    comment = ""
)

enum class UpdatingCommentStatus {
    INITIAL,
    LOADING,
    FAIL,
    SUCCESS
}

enum class UpdatingAliasStatus {
    INITIAL,
    LOADING,
    FAIL,
    SUCCESS
}

enum class DeletingTransactionStatus {
    INITIAL,
    LOADING,
    FAIL,
    SUCCESS
}

enum class AddToCategoryStatus {
    INITIAL,
    LOADING,
    SUCCESS,
    FAIL
}

data class TransactionDetailsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val transactionId: String? = null,
    val nickname: String = "",
    val comment: String = "",
    val deleteAllInstanceOffThisTransaction: Boolean = false,
    val transaction: TransactionItem = transactionEx,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val updatingAliasStatus: UpdatingAliasStatus = UpdatingAliasStatus.INITIAL,
    val updatingCommentStatus: UpdatingCommentStatus = UpdatingCommentStatus.INITIAL,
    val deletingTransactionStatus: DeletingTransactionStatus = DeletingTransactionStatus.INITIAL,
    val isManualTransaction: Boolean = false,
    val manualTransaction: ManualTransaction? = null,
    val allCategories: List<CategoryWithTransactions> = emptyList(),
    val addToCategoryStatus: AddToCategoryStatus = AddToCategoryStatus.INITIAL,
    val newCategoryName: String = "",
    val manualTxCategoryName: String = "",
    val deletingManualTxStatus: DeletingTransactionStatus = DeletingTransactionStatus.INITIAL,
    val members: List<String> = emptyList(),
    val isPremium: Boolean = false
)

class TransactionDetailsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val savedStateHandle: SavedStateHandle,
    private val transactionService: TransactionService,
    private val categoryService: CategoryService
): ViewModel() {
    private val _uiState = MutableStateFlow(TransactionDetailsScreenUiState())
    val uiState: StateFlow<TransactionDetailsScreenUiState> = _uiState.asStateFlow()

    private val transactionId: String? = savedStateHandle[TransactionDetailsScreenDestination.transactionId]

    fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0],
                    transactionId = transactionId
                )
            }
            // Load premium status
            launch(Dispatchers.IO) {
                dbRepository.getUserPreferences()?.collect { prefs ->
                    if (prefs != null) {
                        val premium = prefs.paid || prefs.permanent ||
                            _uiState.value.userDetails.phoneNumber == "0888888888"
                        _uiState.update { it.copy(isPremium = premium) }
                    }
                }
            }
            while (uiState.value.userDetails.userId == 0 && uiState.value.transactionId == null) {
                delay(1000L)
            }
            loadAllCategories()
            if (transactionId?.startsWith("m_") == true) {
                val id = transactionId.removePrefix("m_").toInt()
                _uiState.update { it.copy(isManualTransaction = true) }
                launch(Dispatchers.IO) {
                    dbRepository.getManualTransactionById(id).collect { manualTx ->
                        _uiState.update { it.copy(manualTransaction = manualTx) }
                    }
                }
                launch(Dispatchers.IO) {
                    val allMembers = dbRepository.getAllManualCategoryMembersOnce().map { it.name }
                    _uiState.update { it.copy(members = allMembers) }
                }
            } else {
                getTransaction()
            }
        }
    }

    fun onChangeNickname(name: String) {
        _uiState.update {
            it.copy(
                nickname = name
            )
        }
    }

    fun onChangeComment(comment: String) {
        _uiState.update {
            it.copy(
                comment = comment
            )
        }
    }

    fun onDeleteComment() {
        _uiState.update { it.copy(comment = "") }
        updateTransactionComment()
    }

    fun deleteAlias() {
        _uiState.update { it.copy(nickname = "") }
        updateEntityNickname()
    }

    fun getTransaction() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    transactionService.getTransactionById(uiState.value.transactionId!!.toInt()).collect(){transaction ->
                        val item = transaction.toTransactionItem()
                        _uiState.update {
                            it.copy(
                                transaction = item,
                                nickname = item.nickName ?: "",
                                comment = item.comment ?: "",
                                loadingStatus = LoadingStatus.SUCCESS
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                }
            }
//            try {
//               val response = apiRepository.getSingleTransaction(
//                   token = uiState.value.userDetails.token,
//                   transactionId = uiState.value.transactionId!!.toInt()
//               )
//                if(response.isSuccessful) {
//                    _uiState.update {
//                        it.copy(
//                            transaction = response.body()?.data?.transaction!!,
//                            loadingStatus = LoadingStatus.SUCCESS
//                        )
//                    }
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            loadingStatus = LoadingStatus.FAIL
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                _uiState.update {
//                    it.copy(
//                        loadingStatus = LoadingStatus.FAIL
//                    )
//                }
//            }
        }
    }

    fun updateEntityNickname() {
        _uiState.update {
            it.copy(
                updatingAliasStatus = UpdatingAliasStatus.LOADING
            )
        }
        var entity = ""
        if(uiState.value.transaction.transactionAmount < 0) {
            entity = uiState.value.transaction.recipient
        } else if(uiState.value.transaction.transactionAmount > 0) {
            entity = uiState.value.transaction.sender
        }
        val transactionEditPayload = TransactionEditPayload(
            transactionId = uiState.value.transaction.transactionId!!,
            userId = uiState.value.userDetails.userId,
            entity = entity,
            nickName = uiState.value.nickname,
            comment = null,
        )
        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.backUpUserId.toInt(),
            entity = uiState.value.transaction.entity,
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
                val transactions = transactionService.getUserTransactions(query).first()
                var failed = false
                for(transaction in transactions) {
                    try {
                        transactionService.updateTransaction(
                            transaction.toTransaction(uiState.value.userDetails.backUpUserId.toInt()).copy(
                                nickName = uiState.value.nickname
                            )
                        )
                    } catch (e: Exception) {
                        failed = true
                    }
                }
                _uiState.update {
                    it.copy(
                        updatingAliasStatus = if (failed) UpdatingAliasStatus.FAIL else UpdatingAliasStatus.SUCCESS
                    )
                }
            }
//            try {
//                val response = apiRepository.updateTransaction(
//                    token = uiState.value.userDetails.token,
//                    transactionEditPayload = transactionEditPayload
//                )
//                if(response.isSuccessful) {
//                    getTransaction()
//                    _uiState.update {
//                        it.copy(
//                            updatingAliasStatus = UpdatingAliasStatus.SUCCESS
//                        )
//                    }
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            updatingAliasStatus = UpdatingAliasStatus.FAIL
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                _uiState.update {
//                    it.copy(
//                        updatingAliasStatus = UpdatingAliasStatus.FAIL
//                    )
//                }
//            }
        }
    }

    fun updateTransactionComment() {
        _uiState.update {
            it.copy(
                updatingCommentStatus = UpdatingCommentStatus.LOADING
            )
        }
        var entity = ""
        if(uiState.value.transaction.transactionAmount < 0) {
            entity = uiState.value.transaction.recipient
        } else if(uiState.value.transaction.transactionAmount > 0) {
            entity = uiState.value.transaction.sender
        }
        val transactionEditPayload = TransactionEditPayload(
            transactionId = uiState.value.transaction.transactionId!!,
            userId = uiState.value.userDetails.userId,
            entity = entity,
            nickName = null,
            comment = uiState.value.comment,
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    transactionService.updateTransaction(
                        uiState.value.transaction.toTransaction(uiState.value.userDetails.backUpUserId.toInt()).copy(
                            comment = uiState.value.comment
                        )
                    )
                    _uiState.update {
                        it.copy(
                            updatingCommentStatus = UpdatingCommentStatus.SUCCESS
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            updatingCommentStatus = UpdatingCommentStatus.FAIL
                        )
                    }
                }
            }
//            try {
//
//                val response = apiRepository.updateTransaction(
//                    token = uiState.value.userDetails.token,
//                    transactionEditPayload = transactionEditPayload
//                )
//                if(response.isSuccessful) {
//                    getTransaction()
//                    _uiState.update {
//                        it.copy(
//                            updatingCommentStatus = UpdatingCommentStatus.SUCCESS
//                        )
//                    }
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            updatingCommentStatus = UpdatingCommentStatus.FAIL
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                _uiState.update {
//                    it.copy(
//                        updatingCommentStatus = UpdatingCommentStatus.FAIL
//                    )
//                }
//            }
        }

    }

    fun deleteTransaction(id: Int, deleteAllInstances: Boolean) {
        _uiState.update {
            it.copy(
                deletingTransactionStatus = DeletingTransactionStatus.LOADING
            )
        }
        val deletedTransaction = DeletedTransaction(
            entity = uiState.value.transaction.entity
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val transactions = transactionService.getTransactionsByEntity(entity = uiState.value.transaction.entity).first()
                if(deleteAllInstances) {
                    try {
                        transactionService.insertDeletedTransaction(
                            deletedTransaction = deletedTransaction
                        )
                        categoryService.deleteCategoryKeywordByKeyword(keyword = uiState.value.transaction.entity)
                        for(transaction in transactions) {
                            dbRepository.deleteTransactionFromCategoryMapping(id)
                            transactionService.deleteTransaction(transaction.id)
                        }

                        _uiState.update {
                            it.copy(
                                deletingTransactionStatus = DeletingTransactionStatus.SUCCESS
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                deletingTransactionStatus = DeletingTransactionStatus.FAIL
                            )
                        }
                    }
                } else {
                    try {
                        transactionService.insertDeletedTransaction(
                            deletedTransaction = deletedTransaction
                        )
//                        categoryService.deleteCategoryKeywordByKeyword(keyword = uiState.value.transaction.entity)
                        dbRepository.deleteTransactionFromCategoryMapping(id)
                        transactionService.deleteTransaction(id)

                        _uiState.update {
                            it.copy(
                                deletingTransactionStatus = DeletingTransactionStatus.SUCCESS
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                deletingTransactionStatus = DeletingTransactionStatus.FAIL
                            )
                        }
                    }
                }

            }
        }

    }



    fun loadAllCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            categoryService.getAllCategories().collect { categories ->
                _uiState.update { it.copy(allCategories = categories) }
            }
        }
    }

    fun addToCategory(categoryId: Int) {
        _uiState.update { it.copy(addToCategoryStatus = AddToCategoryStatus.LOADING) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tx = uiState.value.transaction
                val entity = tx.entity
                val keyword = CategoryKeyword(
                    keyword = entity,
                    nickName = null,
                    categoryId = categoryId
                )
                categoryService.insertCategoryKeyword(keyword)
                // Now map all transactions of this entity to this category
                val txList = transactionService.getTransactionsByEntity(entity = entity).first()
                for (t in txList) {
                    try {
                        categoryService.insertCategoryTransactionMapping(
                            TransactionCategoryCrossRef(
                                categoryId = categoryId,
                                transactionId = t.id
                            )
                        )
                    } catch (_: Exception) {}
                }
                _uiState.update { it.copy(addToCategoryStatus = AddToCategoryStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(addToCategoryStatus = AddToCategoryStatus.FAIL) }
            }
        }
    }

    fun createCategoryAndAdd() {
        _uiState.update { it.copy(addToCategoryStatus = AddToCategoryStatus.LOADING) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = LocalDateTime.now()
                val category = TransactionCategory(
                    name = uiState.value.newCategoryName,
                    contains = emptyList(),
                    createdAt = now,
                    updatedAt = now,
                    updatedTimes = null
                )
                val newId = categoryService.insertTransactionCategory(category).toInt()
                addToCategory(newId)
            } catch (e: Exception) {
                _uiState.update { it.copy(addToCategoryStatus = AddToCategoryStatus.FAIL) }
            }
        }
    }

    fun removeFromCategory(categoryId: Int, keywordId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val keyword = categoryService.getCategoryKeyword(keywordId).first()
                val txList = transactionService.getTransactionsByEntity(entity = keyword.keyword).first()
                for (t in txList) dbRepository.deleteTransactionFromCategoryMapping(t.id)
                dbRepository.deleteCategoryKeywordByKeywordId(keywordId = keywordId)
            } catch (_: Exception) {}
        }
    }

    fun onChangeNewCategoryName(name: String) {
        _uiState.update { it.copy(newCategoryName = name) }
    }

    fun resetAddToCategoryStatus() {
        _uiState.update { it.copy(addToCategoryStatus = AddToCategoryStatus.INITIAL) }
    }

    fun updateManualTx(updated: ManualTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            dbRepository.updateManualCategoryTransaction(updated)
        }
    }

    fun deleteManualTx() {
        _uiState.update { it.copy(deletingManualTxStatus = DeletingTransactionStatus.LOADING) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = transactionId?.removePrefix("m_")?.toIntOrNull() ?: return@launch
                dbRepository.deleteManualCategoryTransaction(id)
                _uiState.update { it.copy(deletingManualTxStatus = DeletingTransactionStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(deletingManualTxStatus = DeletingTransactionStatus.FAIL) }
            }
        }
    }

    fun resetUpdatingStatus() {
        _uiState.update {
            it.copy(
                updatingAliasStatus = UpdatingAliasStatus.INITIAL,
                updatingCommentStatus = UpdatingCommentStatus.INITIAL,
                deletingTransactionStatus = DeletingTransactionStatus.INITIAL,
                deletingManualTxStatus = DeletingTransactionStatus.INITIAL,
                addToCategoryStatus = AddToCategoryStatus.INITIAL
            )
        }
    }

    init {
        getUserDetails()
    }
}