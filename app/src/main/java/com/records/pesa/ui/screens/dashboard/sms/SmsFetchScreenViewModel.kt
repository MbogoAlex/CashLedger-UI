package com.records.pesa.ui.screens.dashboard.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.SmsMessage
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsFetchScreenUiState(
    val messagesSize: Float = 0.0f,
    val messagesSent: Float = 0.0f,
    val existingTransactionCodes: List<String> = emptyList(),
    val userDetails: UserDetails = UserDetails(),
    val errorCode: Int = 0,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class SmsFetchScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(value = SmsFetchScreenUiState())
    val uiState: StateFlow<SmsFetchScreenUiState> = _uiState.asStateFlow()

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
        if(messagesToSend.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    messagesSize = messagesToSend.size.toFloat()
                )
            }
            postMessagesInBatches(messagesToSend)
        } else {
            _uiState.update {
                it.copy(
                    messagesSize = 1.0f,
                    messagesSent = 1.0f,
                    loadingStatus = LoadingStatus.SUCCESS
                )
            }
        }
    }

    private fun filterMessagesToSend(messages: List<SmsMessage>): List<SmsMessage> {
        val existingTransactionCodes = uiState.value.existingTransactionCodes.map { it.trim().lowercase() }
        Log.d("EXISTING", existingTransactionCodes.toString())
        var i = 0
        val messagesToSend = mutableListOf<SmsMessage>()
        val newTransactionCodes = getNewTransactionCodes(messages);
        if(newTransactionCodes.isNotEmpty() && uiState.value.existingTransactionCodes.isNotEmpty()) {
            for(code in newTransactionCodes) {
                Log.d("COMPARISON", "${code["code"]} ${existingTransactionCodes[0]}")
                if(code["code"] == existingTransactionCodes[0]) {
                    Log.d("BREAK_LOOP", "BREAK")
                    break
                }
                messagesToSend.add(code["message"] as SmsMessage)
            }

        } else if(uiState.value.existingTransactionCodes.isEmpty() && newTransactionCodes.isNotEmpty()) {
            messagesToSend.addAll(newTransactionCodes.map { it["message"] as SmsMessage })
        }
        Log.d("ADDING", "$i messages")
        Log.d("MESSAGES_TO_SEND", "$i messages")
        return messagesToSend;
    }

    private fun getNewTransactionCodes(messages: List<SmsMessage>): List<Map<String, Any>> {
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

        val batchSize = 1000
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

    fun getLatestTransactionCodes(context: Context) {
        Log.d("USERS_WHEN_GETTING_LATEST_CODE", uiState.value.userDetails.toString())
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getLatestTransactionCode(token = uiState.value.userDetails.token, userId = uiState.value.userDetails.userId)
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            existingTransactionCodes = response.body()?.data?.transaction!!
                        )
                    }
                    fetchSmsMessages(context)
                } else {
                    _uiState.update {
                        it.copy(
                            errorCode = response.code()
                        )
                    }
                    Log.e("GetLatestTransactionCodeErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                Log.e("GetLatestTransactionCodeErrorException", e.toString())
            }
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL,
                errorCode = 0
            )
        }
    }

    init {
        getUserDetails()
    }

}
