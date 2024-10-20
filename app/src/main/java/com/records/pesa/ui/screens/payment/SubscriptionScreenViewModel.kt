package com.records.pesa.ui.screens.payment

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.PaymentData
import com.records.pesa.models.payment.PaymentPayload
import com.records.pesa.models.payment.SubscriptionPaymentStatusPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusPayload
import com.records.pesa.models.user.UserAccount
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.transaction.TransactionService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
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

data class SubscriptionScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val phoneNumber: String = "",
    val amount: String = "50",
    val invoice_id: String = "",
    val state: String = "",
    val failedReason: String? = "",
    val paymentStatusSaved: Boolean = false,
    val paymentToken: String = "",
    val orderTrackingId: String = "",
    val redirectUrl: String = "",
    val paymentMessage: String = "",
    val firstTransactionDate: String = "",
    val cancelled: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class SubscriptionScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService
): ViewModel() {
    private val _uiState = MutableStateFlow(SubscriptionScreenUiState())
    val uiState: StateFlow<SubscriptionScreenUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected


    var conManager: ConnectivityManager? = null
    var netCallback: ConnectivityManager.NetworkCallback? = null
    fun checkConnectivity(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        conManager = connectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.postValue(true)
            }

            override fun onLost(network: Network) {
                _isConnected.postValue(false)
            }

        }

        netCallback = networkCallback

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onCleared() {
        super.onCleared()
        conManager!!.unregisterNetworkCallback(netCallback!!)
    }

    fun getUserDetails() {
        viewModelScope.launch {
            val user = dbRepository.getUsers().first()[0]
            _uiState.update {
                it.copy(
                    userDetails = user,
                    phoneNumber = user.phoneNumber,
                )
            }
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getFirstTransactionDate()
        }
    }

    fun updatePhoneNumber(number: String) {
        _uiState.update {
            it.copy(
                phoneNumber = number
            )
        }
    }

    fun getFirstTransactionDate() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val firstTransaction = transactionService.getFirstTransaction().first()
                _uiState.update {
                    it.copy(
                        firstTransactionDate = formatLocalDate(firstTransaction.date)
                    )
                }
            }
        }
    }

    fun lipa() {
        var phoneNumber = uiState.value.phoneNumber
        if(phoneNumber.startsWith("0")) {
            phoneNumber = phoneNumber.replaceFirst("0", "254")
        }
        _uiState.update {
            it.copy(
                cancelled = false,
                loadingStatus = LoadingStatus.LOADING,
                state = "STARTING..."
            )
        }
        val paymentPayload = IntasendPaymentPayload(
            amount = uiState.value.amount,
            phone_number = phoneNumber
        )

        viewModelScope.launch {
            try {
               val response = apiRepository.lipa(
                   payload = paymentPayload
               )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            invoice_id = response.body()?.invoice?.invoice_id!!,
                            state = response.body()?.invoice?.state!!
                        )
                    }
                    lipaStatus()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
            }
        }
    }

    fun lipaStatus() {
        if(uiState.value.loadingStatus != LoadingStatus.LOADING) {
            _uiState.update {
                it.copy(
                    loadingStatus = LoadingStatus.LOADING
                )
            }
        }
        val lipaStatusPayload = IntasendPaymentStatusPayload(
            invoice_id = uiState.value.invoice_id
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                delay(5000)
                val paidAt = LocalDateTime.now()
                val expiredAt = paidAt.plusMonths(1)
                while(uiState.value.state.lowercase() != "complete" && uiState.value.state.lowercase() != "redirecting" && !uiState.value.cancelled && uiState.value.failedReason?.lowercase() != "request cancelled by user" && uiState.value.failedReason?.lowercase() != "ds timeout user cannot be reached") {
                    Log.d("CHECKING_STATUS, INVOICE ", uiState.value.invoice_id)
                    delay(2000)
                    try {
                        val response = apiRepository.lipaStatus(
                            payload = lipaStatusPayload
                        )
                        if(response.isSuccessful) {
                            Log.d("STATUS_CHECKED:", uiState.value.state)
                            _uiState.update {
                                it.copy(
                                    failedReason = response.body()?.invoice?.failed_reason,
                                    state = response.body()?.invoice?.state ?: ""
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("failedToCheckPaymentStatus", "Failed to check payment status")
                    }
                }

                if(!uiState.value.cancelled && uiState.value.state.lowercase() == "complete") {
                    _uiState.update {
                        it.copy(
                            state = "REDIRECTING"
                        )
                    }
                    try {
                        if(uiState.value.amount == "1000") {
                            val payment = com.records.pesa.models.payment.supabase.PaymentData(
                                amount = uiState.value.amount.toDouble(),
                                expiredAt = LocalDateTime.now().plusMonths(1).toString(),
                                paidAt = LocalDateTime.now().toString(),
                                month = LocalDateTime.now().month.value,
                                userId = uiState.value.userDetails.userId,
                                )

                            client.from("payment").insert(payment)


                            client.from("userAccount").update(
                                {
                                    UserAccount::permanent setTo true
                                }
                            ) {
                                filter {
                                    UserAccount::id eq uiState.value.userDetails.userId
                                }
                            }
                        } else {
                            val payment = com.records.pesa.models.payment.supabase.PaymentData(
                                amount = uiState.value.amount.toDouble(),
                                expiredAt = expiredAt.toString(),
                                paidAt = paidAt.toString(),
                                month = LocalDateTime.now().month.value,
                                userId = uiState.value.userDetails.userId,

                                )

                            client.from("payment").insert(payment)
                        }
                    } catch (e: Exception) {
                        while (!uiState.value.paymentStatusSaved) {
                            delay(3000)
                            try {
                                if(uiState.value.amount == "1000") {
                                    val payment = com.records.pesa.models.payment.supabase.PaymentData(
                                        amount = uiState.value.amount.toDouble(),
                                        expiredAt = LocalDateTime.now().plusMonths(1).toString(),
                                        paidAt = LocalDateTime.now().toString(),
                                        month = LocalDateTime.now().month.value,
                                        userId = uiState.value.userDetails.userId,

                                        )

                                    client.from("payment").insert(payment)


                                    client.from("userAccount").update(
                                        {
                                            UserAccount::permanent setTo true
                                        }
                                    ) {
                                        filter {
                                            UserAccount::id eq uiState.value.userDetails.userId
                                        }
                                    }
                                } else {
                                    val payment = com.records.pesa.models.payment.supabase.PaymentData(
                                        amount = uiState.value.amount.toDouble(),
                                        expiredAt = expiredAt.toString(),
                                        paidAt = paidAt.toString(),
                                        month = LocalDateTime.now().month.value,
                                        userId = uiState.value.userDetails.userId,

                                        )

                                    client.from("payment").insert(payment)
                                }
                                _uiState.update {
                                    it.copy(
                                        paymentStatusSaved = true
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("userUpdateFailure", e.toString())
                            }
                        }
                        Log.e("userUpdateFailure", e.toString())
                    }
                }

                if(uiState.value.state.lowercase().contains("fail")) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                }

                // save to local db
                if(uiState.value.state.lowercase() == "redirecting") {
                    Log.d("SAVING_TO_DB", "SAVING")
                    if(uiState.value.amount == "50") {
                        Log.d("SAVING_TO_DB", "SAVING KES 50")
                        withContext(Dispatchers.IO) {
                            var user = dbRepository.getUser(userId = uiState.value.userDetails.userId).first()
                            Log.d("SAVING_TO_DB", user.toString())
                            dbRepository.updateUser(
                                user.copy(
                                    paymentStatus = true,
                                    paidAt = paidAt.toString(),
                                    expiredAt = expiredAt.toString()
                                )
                            )

                            while(!user.paymentStatus) {
                                user = dbRepository.getUser(userId = uiState.value.userDetails.userId).first()
                            }

                            Log.d("SAVING_TO_DB", "UPDATED USER: $user")

                            _uiState.update {
                                it.copy(
                                    paymentMessage = "Payment received",
                                    loadingStatus = LoadingStatus.SUCCESS
                                )
                            }
                        }
                    } else if(uiState.value.amount == "1000") {
                        withContext(Dispatchers.IO) {
                            var user = dbRepository.getUser(userId = uiState.value.userDetails.userId).first()
                            dbRepository.updateUser(
                                user.copy(
                                    paymentStatus = true,
                                    permanent = true,
                                    paidAt = paidAt.toString(),
                                    expiredAt = null
                                )
                            )

                            while(!user.paymentStatus) {
                                user = dbRepository.getUser(userId = uiState.value.userDetails.userId).first()
                            }

                            _uiState.update {
                                it.copy(
                                    paymentMessage = "Payment received",
                                    loadingStatus = LoadingStatus.SUCCESS
                                )
                            }
                        }
                    }
                } else if(uiState.value.failedReason?.lowercase() == "request cancelled by user" || uiState.value.failedReason?.lowercase() == "ds timeout user cannot be reached") {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            failedReason = "Something went wrong. Contact CashLedger team if this persists",
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                }
            }
        }
    }



    fun checkPaymentStatus() {
        Log.d("CKECK_STATUS", "CHECKING_PAYMENT_STATUS")
        val paymentStatusPayload = SubscriptionPaymentStatusPayload(
            token = uiState.value.paymentToken,
            userId = uiState.value.userDetails.userId,
            orderId = uiState.value.orderTrackingId
        )
        viewModelScope.launch {
            try {
               val response = apiRepository.subscriptionPaymentStatus(
                   token = uiState.value.userDetails.token,
                   subscriptionPaymentStatusPayload = paymentStatusPayload
               )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            paymentMessage = "Payment successful",
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            paymentMessage = "Payment not successful. Contact KIWITECH if you are sure you have paid",
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        paymentMessage = "Payment not successful. Try again later",
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
            }
        }
    }

    fun resetPaymentStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL
            )
        }
    }

    fun cancel() {
        _uiState.update {
            it.copy(
                cancelled = true
            )
        }
    }

    init {
        getUserDetails()
    }
}