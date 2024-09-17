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
    val paymentToken: String = "",
    val orderTrackingId: String = "",
    val redirectUrl: String = "",
    val paymentMessage: String = "",
    val cancelled: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class SubscriptionScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
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
            paySubscriptionFee()
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
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
            }
        }
    }

    fun updatePhoneNumber(number: String) {
        _uiState.update {
            it.copy(
                phoneNumber = number
            )
        }
    }

    private fun lipaStatus() {
        val lipaStatusPayload = IntasendPaymentStatusPayload(
            invoice_id = uiState.value.invoice_id
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                delay(10000)
                while(uiState.value.state.lowercase() != "complete" && !uiState.value.cancelled && uiState.value.failedReason?.lowercase() != "request cancelled by user" && uiState.value.failedReason?.lowercase() != "ds timeout user cannot be reached") {
                    delay(5000)
                    try {
                        val response = apiRepository.lipaStatus(
                            payload = lipaStatusPayload
                        )
                        if(response.isSuccessful) {
                            _uiState.update {
                                it.copy(
                                    failedReason = response.body()?.invoice?.failed_reason,
                                    state = uiState.value.state
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("failedToCheckPaymentStatus", "Failed to check payment status")
                    }
                }

                if(uiState.value.amount == "1000" && !uiState.value.cancelled && uiState.value.state.lowercase() == "complete") {
                    try {
                        val user = client.postgrest["userAccount"]
                            .select {
                                filter {
                                    eq("phoneNumber", uiState.value.phoneNumber)
                                }
                            }.decodeSingle<UserAccount>()
                        val dbUser = dbRepository.getUser(userId = user.id!!).first()
                        dbRepository.updateUser(
                            dbUser.copy(
                                permanent = true
                            )
                        )
                        client.postgrest["userAccount"]
                            .update(user.copy(permanent = true)) {
                                filter {
                                    eq("phoneNumber", uiState.value.phoneNumber)
                                }
                            }
                    } catch (e: Exception) {
                        Log.d("userUpdateFailure", e.toString())
                    }
                } else if(uiState.value.amount == "50" && !uiState.value.cancelled && uiState.value.state.lowercase() == "complete"){
                    val payment = com.records.pesa.models.payment.supabase.PaymentData(
                        amount = uiState.value.amount.toDouble(),
                        expiredAt = LocalDateTime.now().plusMonths(1).toString(),
                        paidAt = LocalDateTime.now().toString(),
                        month = LocalDateTime.now().month.toString(),
                        userId = uiState.value.userDetails.userId,

                        )
                    val savedPayment = client.from("payment").insert(payment) {
                        select()
                    }.decodeSingle<com.records.pesa.models.payment.supabase.PaymentData>()
                }

                if(!uiState.value.cancelled && uiState.value.state.lowercase() == "complete") {
                    withContext(Dispatchers.IO) {
                        dbRepository.updateUser(
                            uiState.value.userDetails.copy(
                                paymentStatus = true
                            )
                        )

                        var user = dbRepository.getUser(userId = uiState.value.userDetails.userId).first()

                        while(!user.paymentStatus) {
                            user = dbRepository.getUser(userId = uiState.value.userDetails.userId).first()
                        }

                        _uiState.update {
                            it.copy(
                                loadingStatus = LoadingStatus.SUCCESS
                            )
                        }
                    }
                } else if(uiState.value.failedReason?.lowercase() == "request cancelled by user" || uiState.value.failedReason?.lowercase() == "ds timeout user cannot be reached") {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                }
            }
        }
    }

    fun paySubscriptionFee() {
        val paymentPayload = PaymentPayload(
            userId = uiState.value.userDetails.userId,
            phoneNumber = uiState.value.phoneNumber.takeIf { it.isNotEmpty() } ?: uiState.value.userDetails.phoneNumber,
        )
        viewModelScope.launch {
            try {
                Log.d("PAYING_WITH_TOKEN", uiState.value.userDetails.token)
                val response = apiRepository.paySubscriptionFee(
                    token = uiState.value.userDetails.token,
                    paymentPayload = paymentPayload
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            paymentToken = response.body()?.data?.payment?.token!!,
                            orderTrackingId = response.body()?.data?.payment?.order_tracking_id!!,
                            redirectUrl = response.body()?.data?.payment?.redirect_url!!,
//                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL,
                            paymentMessage = "Failed. Check your connection or try later"

                        )
                    }
                    Log.e("initiatePaymentResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL,
                        paymentMessage = "Failed. Check your connection or try later"
                    )
                }
                Log.e("initiatePaymentException", e.toString())

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