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
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.intasend.IntasendPaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusPayload
import com.records.pesa.models.payment.intasend.PaymentSavePayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
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
import java.time.LocalDateTime

data class SubscriptionScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val preferences: UserPreferences = userPreferences,
    val phoneNumber: String = "",
    val amount: String = "100",
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
                   paymentPayload = paymentPayload
               )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            invoice_id = response.body()?.data?.invoice?.invoice_id!!,
                            state = response.body()?.data?.invoice?.state!!
                        )
                    }

                    lipaStatus()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("paymentDetails", "exception: $e")
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
            }
        }
    }

    fun updateAmount(amount: String) {
        _uiState.update {
            it.copy(
                amount = amount
            )
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
                val expiredAt = when(uiState.value.amount) {
                    "100" -> paidAt.plusMonths(1)
                    "400" -> paidAt.plusMonths(6)
                    "700" -> paidAt.plusYears(1)
                    "2000" -> paidAt.plusYears(100)
                    else -> paidAt.plusMonths(1)
                }

                val permanent = uiState.value.amount == "2000"

                while(uiState.value.state.lowercase() == "starting" || uiState.value.state.lowercase() == "processing" || uiState.value.state.lowercase() == "pending") {
                    Log.d("CHECKING_STATUS, INVOICE ", uiState.value.invoice_id)
                    delay(2000)
                    try {
                        val response = apiRepository.lipaStatus(
                            paymentStatusPayload = lipaStatusPayload
                        )
                        if(response.isSuccessful) {
                            Log.d("STATUS_CHECKED:", uiState.value.state)
                            _uiState.update {
                                it.copy(
                                    failedReason = response.body()?.data!!.invoice.failed_reason,
                                    state = response.body()?.data!!.invoice.state
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("failedToCheckPaymentStatus", "Failed to check payment status")
                    }
                }

                if(!uiState.value.cancelled && uiState.value.state.lowercase() == "complete") {
                    lipaSave(
                        permanent = permanent,
                        paidAt = paidAt.toString(),
                        expiredAt = expiredAt.toString(),
                        month = paidAt.monthValue
                    )

                }

                if(uiState.value.state.lowercase() == "failed") {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                }
            }
        }
    }

    fun lipaSave(permanent: Boolean, paidAt: String, expiredAt: String, month: Int) {
        _uiState.update {
            it.copy(
                state = "SAVING"
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val paymentSavePayload = PaymentSavePayload(
                        userId = uiState.value.userDetails.userId.toString(),
                        amount = uiState.value.amount,
                        paidAt = paidAt,
                        expiredAt = expiredAt,
                        month = month,
                        permanent = permanent
                    )

                    val response = apiRepository.savePayment(
                        paymentSavePayload = paymentSavePayload
                    )

                    if(response.isSuccessful) {
                        dbRepository.updateUser(
                            _uiState.value.userDetails.copy(
                                paymentStatus = true,
                                paidAt = paidAt,
                                permanent = permanent,
                                expiredAt = expiredAt

                            )

                        )

                        dbRepository.updateUserPreferences(
                            _uiState.value.preferences.copy(
                                paid = true,
                                paidAt = LocalDateTime.parse(paidAt),
                                expiryDate = LocalDateTime.parse(expiredAt),
                                permanent = permanent
                            )
                        )

                        while(!uiState.value.preferences.paid) {
                            delay(1000)
                        }


                        _uiState.update {
                            it.copy(
                                paymentMessage = "Payment received",
                                loadingStatus = LoadingStatus.SUCCESS
                            )
                        }
                    }

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            paymentMessage = "Failed to save payment",
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
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

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    dbRepository.getUserPreferences().collect { preferences ->
                        _uiState.update {
                            it.copy(
                                preferences = preferences ?: userPreferences
                            )
                        }
                    }
                } catch (e: Exception) {

                }
            }
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
        getUserPreferences()
    }
}