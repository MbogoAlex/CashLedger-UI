package com.records.pesa.ui.screens.dashboard.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.UserSession
import com.records.pesa.db.models.userPreferences
import com.records.pesa.mapper.toTransactionCategory
import com.records.pesa.models.MessageData
import com.records.pesa.models.SmsMessage
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.SmsProviders
import com.records.pesa.workers.WorkersRepository
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    val preferences: UserPreferences? = null,
    val messagesSize: Float = 0.0f,
    val messagesSent: Float = 0.0f,
    val existingTransactionCodes: List<String> = emptyList(),
    val userDetails: UserDetails = UserDetails(),
    val userSession: UserSession? = null,
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
    private val workersRepository: WorkersRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(value = SmsFetchScreenUiState())
    val uiState: StateFlow<SmsFetchScreenUiState> = _uiState.asStateFlow()

    private val fromLogin: String? = savedStateHandle[SMSFetchScreenDestination.fromLogin]

    private var allMessagesSent = mutableStateOf(false)

    fun getUserDetails() {
        viewModelScope.launch {
            dbRepository.getUser()?.collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            userDetails = user
                        )
                    }
                }
            }
            Log.d("USER", uiState.value.userDetails.toString())
        }
    }

    private fun getUserSession() {
        viewModelScope.launch {
            try {
                dbRepository.getSession()?.collect { session ->
                    if (session != null) {
                        _uiState.update {
                            it.copy(
                                userSession = session
                            )
                        }
                    }
                }
                Log.d("USER", uiState.value.userDetails.toString())
            } catch (e: Exception) {
                // Handle cases where UserSession table doesn't exist yet
                Log.w("SmsFetchScreenViewModel", "UserSession table not available: ${e.message}")
                Log.d("SmsFetchScreenViewModel", "This is normal if migration hasn't run yet or failed")

                // Continue without session - app will still work with local processing
                // Background SMS submission will be skipped until session is available
            }
        }
    }


    fun fetchSmsMessages(context: Context) {
        val messages = mutableListOf<SmsMessage>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.SUBSCRIPTION_ID
        )

        // Use comprehensive provider patterns for filtering
        val (whereClause, whereArgs) = SmsProviders.createSmsFilterQuery()
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        // Get SIM card information
        val simInfoMap = getSimCardInfo(context)

        val cursor = context.contentResolver.query(uri, projection, whereClause, whereArgs, sortOrder)

        cursor?.use {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (it.moveToNext()) {
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val dateMillis = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val senderAddress = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val subscriptionId = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))
                
                val date = Date(dateMillis)
                val formattedDate = dateFormat.format(date)
                val formattedTime = timeFormat.format(date)
                
                // Get SIM info for this subscription ID
                val simInfo = simInfoMap[subscriptionId]

                // Verify this is a financial provider before adding
                if (SmsProviders.isFinancialProvider(senderAddress)) {
                    messages.add(
                        SmsMessage(
                            body = body,
                            date = formattedDate,
                            time = formattedTime,
                            receivingPhoneNumber = simInfo?.phoneNumber,
                            carrierName = simInfo?.carrierName,
                            simSlotIndex = simInfo?.simSlotIndex ?: -1,
                            subscriptionId = subscriptionId,
                            senderAddress = senderAddress
                        )
                    )
                }
            }
        }


        val messagesToSend = filterMessagesToSend(messages)
        Log.d("MESSAGES_ADDITION", "ADDED ${messagesToSend.size} M-PESA MESSAGES")

        // Always trigger comprehensive SMS submission for ALL financial providers
        // This is independent of M-PESA transaction processing above
        if(uiState.value.preferences?.hasSubmittedMessages == false) {
            startBackgroundSmsSubmissionWithRetry()
        }

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
            }
            allMessagesSent.value = true
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun getSimCardInfo(context: Context): Map<Int, SimCardInfo> {
        val simInfoMap = mutableMapOf<Int, SimCardInfo>()
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.w("SmsFetchViewModel", "READ_PHONE_STATE permission not granted")
            return simInfoMap
        }
        
        try {
            val subscriptionManager = SubscriptionManager.from(context)
            val subscriptions = subscriptionManager.activeSubscriptionInfoList
            
            subscriptions?.forEach { subInfo ->
                simInfoMap[subInfo.subscriptionId] = SimCardInfo(
                    subscriptionId = subInfo.subscriptionId,
                    phoneNumber = subInfo.number,
                    carrierName = subInfo.carrierName?.toString(),
                    displayName = subInfo.displayName?.toString(),
                    simSlotIndex = subInfo.simSlotIndex
                )
                
                Log.d("SimInfo", "SIM ${subInfo.simSlotIndex + 1}: ${subInfo.carrierName} - ${subInfo.number}")
            }
        } catch (e: Exception) {
            Log.e("SmsFetchViewModel", "Error getting SIM info: ${e.message}")
        }
        
        return simInfoMap
    }
    
    data class SimCardInfo(
        val subscriptionId: Int,
        val phoneNumber: String?,
        val carrierName: String?,
        val displayName: String?,
        val simSlotIndex: Int
    )

    /**
     * Start background SMS submission using WorkManager
     * This ensures SMS submission continues even if the UI is destroyed
     */
    private fun startBackgroundSmsSubmission() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userDetails = uiState.value.userDetails

                // Check if we have valid user details
                if (userDetails.userId <= 0) {
                    Log.w("SMS_SUBMISSION", "Cannot start SMS submission: invalid user ID")
                    return@launch
                }

                // Check if UserSession is available (migration completed successfully)
                try {
                    val userSession = dbRepository.getSession()?.first()
                    if (userSession?.accessToken.isNullOrEmpty()) {
                        Log.w("SMS_SUBMISSION", "Cannot start SMS submission: no access token available")
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.w("SMS_SUBMISSION", "UserSession table not available, skipping background submission: ${e.message}")
                    return@launch
                }

                Log.d("SMS_SUBMISSION", "Starting background SMS submission for user ${userDetails.userId}")

                workersRepository.submitSmsMessages(
                    userId = userDetails.dynamoUserId ?: userDetails.phoneNumber.replaceFirstChar { "" }.toLong(),
                    userPhone = userDetails.phoneNumber
                )

                if(uiState.value.preferences != null) {
                    dbRepository.updateUserPreferences(
                        uiState.value.preferences!!.copy(
                            hasSubmittedMessages = true
                        )
                    )
                }

                Log.d("SMS_SUBMISSION", "Background SMS submission work enqueued successfully")
            } catch (e: Exception) {
                Log.e("SMS_SUBMISSION", "Error starting background SMS submission", e)
            }
        }
    }

    /**
     * Start background SMS submission with retry logic for userSession loading
     * Waits for userSession to become available with exponential backoff
     */
    private fun startBackgroundSmsSubmissionWithRetry() {
        viewModelScope.launch {
            val maxRetries = 5
            val baseDelayMs = 500L
            var currentRetry = 0

            Log.d("SMS_SUBMISSION_RETRY", "Starting SMS submission with retry logic")

            while (currentRetry < maxRetries) {
                if (uiState.value.userSession != null) {
                    Log.d("SMS_SUBMISSION_RETRY", "UserSession found on attempt ${currentRetry + 1}, starting submission")
                    startBackgroundSmsSubmission()
                    return@launch
                }

                currentRetry++
                val delayMs = baseDelayMs * (1L shl (currentRetry - 1)) // Exponential backoff: 500ms, 1s, 2s, 4s, 8s

                Log.d("SMS_SUBMISSION_RETRY", "UserSession still null, retry $currentRetry/$maxRetries in ${delayMs}ms")
                delay(delayMs)
            }

            // Final attempt - try submission anyway in case session loading is delayed
            Log.w("SMS_SUBMISSION_RETRY", "Max retries reached, attempting submission without userSession check")
            startBackgroundSmsSubmission()
        }
    }

    /**
     * Start comprehensive SMS processing including both local processing and API submission
     */
    fun startComprehensiveSmsProcessing(context: Context) {
        _uiState.update {
            it.copy(loadingStatus = LoadingStatus.LOADING)
        }

        viewModelScope.launch {
            try {
                // First, fetch and process SMS messages locally
                fetchSmsMessages(context)

                // Background submission is triggered automatically in fetchSmsMessages
                // if there are new messages to process

                _uiState.update {
                    it.copy(loadingStatus = LoadingStatus.SUCCESS)
                }

                Log.d("SMS_PROCESSING", "Comprehensive SMS processing completed")

            } catch (e: Exception) {
                Log.e("SMS_PROCESSING", "Error in comprehensive SMS processing", e)
                _uiState.update {
                    it.copy(loadingStatus = LoadingStatus.FAIL)
                }
            }
        }
    }

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUserPreferences()?.collect() { preferences ->
                    _uiState.update {
                        it.copy(
                            preferences = preferences
                        )
                    }
                }
            }
        }
    }

    init {
        getUserDetails()
        getUserSession()
        getUserPreferences()
        _uiState.update {
            it.copy(
                fromLogin = fromLogin
            )
        }
    }

}
