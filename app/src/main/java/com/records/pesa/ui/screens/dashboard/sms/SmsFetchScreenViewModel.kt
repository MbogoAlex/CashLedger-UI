package com.records.pesa.ui.screens.dashboard.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.mapper.toTransactionCategory
import com.records.pesa.models.MessageData
import com.records.pesa.models.SmsMessage
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsFetchScreenUiState(
    val preferences: UserPreferences = userPreferences,
    val messagesSize: Float = 0.0f,
    val messagesSent: Float = 0.0f,
    val existingTransactionCodes: List<String> = emptyList(),
    val userDetails: UserDetails = UserDetails(),
    val errorCode: Int = 0,
    val delayTime: Float = 0.0f,
    val currentDelayTime: Float = 0.0f,
    val counterOn: Boolean = false,
    val fromLogin: String? = null,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class SmsFetchScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val transactionsService: TransactionService,
    private val userAccountService: UserAccountService,
    private val categoryService: CategoryService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(value = SmsFetchScreenUiState())
    val uiState: StateFlow<SmsFetchScreenUiState> = _uiState.asStateFlow()

    private val fromLogin: String? = savedStateHandle[SMSFetchScreenDestination.fromLogin]

    private var allMessagesSent = mutableStateOf(false)

    fun getUserDetails() {
        viewModelScope.launch {
            Log.d("USERS", dbRepository.getUsers().first()[0].toString())
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
        }
    }


    fun fetchSmsMessages(context: Context) {
        val messages = mutableListOf<SmsMessage>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
        val selection = "${Telephony.Sms.ADDRESS} LIKE ?"
        val selectionArgs = arrayOf("%MPESA%")
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (it.moveToNext()) {
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val dateMillis = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val date = Date(dateMillis)
                val formattedDate = dateFormat.format(date)
                val formattedTime = timeFormat.format(date)
                messages.add(SmsMessage(body, formattedDate, formattedTime))
            }
        }


        val messagesToSend = filterMessagesToSend(messages)
        Log.d("MESSAGES_ADDITION", "ADDED ${messagesToSend.size} MESSAGES")

    }

    private fun filterMessagesToSend(messages: List<SmsMessage>): List<SmsMessage> {
        val messagesToSend = mutableListOf<SmsMessage>()
        val newTransactionCodes = getNewTransactionCodes(messages);
        Log.d("NEW_MESSAGES", "GOT ${newTransactionCodes.size} MESSAGES")
        viewModelScope.launch {
            Dispatchers.IO
            val existing: String? = transactionsService.getLatestTransactionCode().first()
            Log.d("EXISTING", existing.toString())
            if(newTransactionCodes.isNotEmpty() && !existing.isNullOrEmpty()) {
                for(code in newTransactionCodes) {
                    Log.d("COMPARISON", "${code["code"]} $existing")
                    if(code["code"] == existing.lowercase()) {
                        Log.d("BREAK_LOOP", "BREAK")
                        break
                    }
                    messagesToSend.add(code["message"] as SmsMessage)
                }
            } else if(existing.isNullOrEmpty() && newTransactionCodes.isNotEmpty()) {
                messagesToSend.addAll(newTransactionCodes.map { it["message"] as SmsMessage })
                Log.d("NEW_TRANSACTIONS_CODE_SIZE", newTransactionCodes.size.toString())
                Log.d("MESSAGES_TO_SEND_SIZE", messagesToSend.size.toString())
            }
            if(messagesToSend.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        messagesSize = messagesToSend.size.toFloat()
                    )
                }
                extractAndInsertTransactions(messagesToSend)
//
            } else {
                _uiState.update {
                    it.copy(
                        messagesSize = 1.0f,
                        messagesSent = 1.0f,
                        counterOn = true,
                        loadingStatus = LoadingStatus.SUCCESS
                    )
                }
            }

        }

        return messagesToSend;
    }

    fun extractAndInsertTransactions(messages: List<SmsMessage>) {
        Log.d("INSERTION", "Inserting ${messages.size} transactions")
        var count = 0
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val userAccount = userAccountService.getUserAccount(userId = uiState.value.userDetails.userId).first()
                val categories = categoryService.getAllCategories().first()
                for(message in messages) {
                    count += 1
                    Log.d("COUNT_VALUE", uiState.value.messagesSent.toString())
                    try {
                        transactionsService.extractTransactionDetails(message.toMessageData(), userAccount, categories.map { it.toTransactionCategory() })
                    } catch (e: Exception) {
                        Log.e("transactionInsertException", e.toString())
                    }
                    _uiState.update {
                        it.copy(
                            messagesSent = count.toFloat()
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.SUCCESS,
                    )
                }
            }

        }
    }

    fun SmsMessage.toMessageData(): MessageData = MessageData(
        body = body,
        time = time,
        date = date
    )

    private fun getNewTransactionCodes(messages: List<SmsMessage>): List<Map<String, Any>> {
        Log.d("ANALYSIS", "ANALYZING ${messages.size} MESSAGES")
        val transactionCodes = mutableListOf<Map<String, Any>>()
        for(message in messages) {
            try {
                val transactionCodeMatcher = Regex("\\b\\w{10}\\b").find(message.body)
                if (transactionCodeMatcher != null) {
                    val transactionCode = transactionCodeMatcher.value
                    transactionCodes.add(mapOf("code" to transactionCode.trim().lowercase(), "message" to message))
                }
            } catch (e: Exception) {
                Log.e("Exception:", e.toString())
            }
        }
        return transactionCodes;
    }
    private fun postMessagesInBatches(messages: List<SmsMessage>) {
        _uiState.update {
            it.copy(
                counterOn = true
            )
        }

        val batchSize = 2000
        val totalBatches = (messages.size + batchSize - 1) / batchSize

        viewModelScope.launch {
            var messagesSent = 0
            for (i in 0 until totalBatches) {
                val fromIndex = i * batchSize
                val toIndex = minOf(fromIndex + batchSize, messages.size)
                val batch = messages.subList(fromIndex, toIndex)
                messagesSent += batch.size
                _uiState.update {
                    it.copy(
                        messagesSent = messagesSent.toFloat()
                    )
                }
                postMessages(batch)
            }
            allMessagesSent.value = true
        }
    }

    private fun postMessages(messages: List<SmsMessage>) {
        Log.i("SENDING", " ${messages.size} messages")
        viewModelScope.launch {
            try {
                val response = apiRepository.postMessages(
                    token = uiState.value.userDetails.token,
                    messages = messages,
                    id = uiState.value.userDetails.userId
                )

                if (response.isSuccessful) {
                    if(allMessagesSent.value) {
                        _uiState.update {
                            it.copy(
                                loadingStatus = LoadingStatus.SUCCESS
                            )
                        }
                        Log.i("SUCCESS", "SUCCESS")
                    }

                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("FAILURE_RESPONSE", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("FAILURE_EXCEPTION", e.toString())
            }
        }
    }

//    fun getLatestTransactionCodes(context: Context) {
//        Log.d("USERS_WHEN_GETTING_LATEST_CODE", uiState.value.userDetails.toString())
//        _uiState.update {
//            it.copy(
//                loadingStatus = LoadingStatus.LOADING
//            )
//        }
//        viewModelScope.launch {
//            try {
//                val response = apiRepository.getLatestTransactionCode(token = uiState.value.userDetails.token, userId = uiState.value.userDetails.userId)
//                if(response.isSuccessful) {
//                    _uiState.update {
//                        it.copy(
//                            existingTransactionCodes = response.body()?.data?.transaction!!
//                        )
//                    }
//                    fetchSmsMessages(context)
//                } else {
//                    _uiState.update {
//                        it.copy(
//                            errorCode = response.code()
//                        )
//                    }
//                    Log.e("GetLatestTransactionCodeErrorResponse", response.toString())
//                }
//            } catch (e: Exception) {
//                Log.e("GetLatestTransactionCodeErrorException", e.toString())
//            }
//        }
//    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL,
                errorCode = 0
            )
        }
    }

    fun resetTimer() {
        _uiState.update {
            it.copy(
                counterOn = false
            )
        }
    }

    init {
        getUserDetails()
        _uiState.update {
            it.copy(
                fromLogin = fromLogin
            )
        }
    }

}
