package com.records.pesa.ui.screens.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.TransactionEditPayload
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.itemCategories
import com.records.pesa.reusables.transactions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

data class TransactionDetailsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val transactionId: String? = null,
    val nickname: String = "",
    val comment: String = "",
    val transaction: TransactionItem = transactionEx,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val updatingAliasStatus: UpdatingAliasStatus = UpdatingAliasStatus.INITIAL,
    val updatingCommentStatus: UpdatingCommentStatus = UpdatingCommentStatus.INITIAL
)

class TransactionDetailsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val savedStateHandle: SavedStateHandle
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
            while (uiState.value.userDetails.userId == 0 && uiState.value.transactionId == null) {
                delay(1000L)
            }
            getTransaction()
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
        _uiState.update {
            it.copy(
                comment = ""
            )
        }
        updateTransactionComment()
    }

    fun getTransaction() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
               val response = apiRepository.getSingleTransaction(
                   token = uiState.value.userDetails.token,
                   transactionId = uiState.value.transactionId!!.toInt()
               )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            transaction = response.body()?.data?.transaction!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
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
        viewModelScope.launch {
            try {
                val response = apiRepository.updateTransaction(
                    token = uiState.value.userDetails.token,
                    transactionEditPayload = transactionEditPayload
                )
                if(response.isSuccessful) {
                    getTransaction()
                    _uiState.update {
                        it.copy(
                            updatingAliasStatus = UpdatingAliasStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            updatingAliasStatus = UpdatingAliasStatus.FAIL
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        updatingAliasStatus = UpdatingAliasStatus.FAIL
                    )
                }
            }
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
            try {
                val response = apiRepository.updateTransaction(
                    token = uiState.value.userDetails.token,
                    transactionEditPayload = transactionEditPayload
                )
                if(response.isSuccessful) {
                    getTransaction()
                    _uiState.update {
                        it.copy(
                            updatingCommentStatus = UpdatingCommentStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            updatingCommentStatus = UpdatingCommentStatus.FAIL
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        updatingCommentStatus = UpdatingCommentStatus.FAIL
                    )
                }
            }
        }

    }

    fun resetUpdatingStatus() {
        _uiState.update {
            it.copy(
                updatingAliasStatus = UpdatingAliasStatus.INITIAL,
                updatingCommentStatus = UpdatingCommentStatus.INITIAL
            )
        }
    }

    init {
        getUserDetails()
    }
}