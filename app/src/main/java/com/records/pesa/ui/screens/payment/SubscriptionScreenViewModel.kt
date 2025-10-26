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
import com.records.pesa.db.models.UserSession
import com.records.pesa.db.models.userPreferences
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.PaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusPayload
import com.records.pesa.models.payment.intasend.PaymentSavePayload
import com.records.pesa.models.subscription.SubscriptionPackageDt
import com.records.pesa.models.user.login.UserLoginPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.auth.AuthenticationManager
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.ui.screens.auth.LoginStatus
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
    val subscriptionPackages: List<SubscriptionPackageDt> = emptyList(),
    val selectedPackageId: Int? = null,
    val phoneNumber: String = "",
    val amount: String = "100",
    val invoice_id: String = "",
    val transactionId: Long? = null,
    val state: String = "",
    val failedReason: String? = "",
    val paymentStatusSaved: Boolean = false,
    val paymentToken: String = "",
    val orderTrackingId: String = "",
    val redirectUrl: String = "",
    val paymentMessage: String = "",
    val firstTransactionDate: String = "",
    val cancelled: Boolean = false,
    val fetchingStatus: LoadingStatus = LoadingStatus.LOADING,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class SubscriptionScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService,
    private val authenticationManager: AuthenticationManager
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
        val paymentPayload = PaymentPayload(
            packageId = uiState.value.selectedPackageId!!,
            phoneNumber = phoneNumber,
            transactionMethodId = 1,
            transactionTypeId = 1
        )

        viewModelScope.launch {
            try {
                Log.d("paymentDetails", "lipa: $paymentPayload")
               val response = authenticationManager.executeWithAuth { token ->
                   apiRepository.lipa(
                       token = token,
                       paymentPayload = paymentPayload
                   )}
                if(response?.isSuccessful == true) {
                    Log.d("paymentDetails", "lipa response: ${response.body()?.data}")
                    _uiState.update {
                        it.copy(
                            transactionId = response.body()?.data?.id,
                            state = response.body()?.data?.status!!
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

    fun updatePackageId(packageId: Int) {
        _uiState.update {
            it.copy(
                selectedPackageId = packageId,
                amount = when(packageId) {
                    1 -> "100"
                    2 -> "400"
                    3 -> "700"
                    4 -> "2000"
                    else -> "100"
                }
            )
        }
    }

    fun getSubscriptionContainer() {
        _uiState.update {
            it.copy(
                fetchingStatus = LoadingStatus.LOADING
            )
        }

        viewModelScope.launch {
            try {
               val response = authenticationManager.executeWithAuth { token ->
                   apiRepository.getSubscriptionPackageContainer(
                       token = token,
                       id = 1
                   )
               }

               if (response?.isSuccessful == true) {
                   val subscriptionPackages = response.body()?.data?.subscriptionPackages ?: emptyList()
                   _uiState.update {
                       it.copy(
                           subscriptionPackages = subscriptionPackages,
                           fetchingStatus = LoadingStatus.SUCCESS
                       )
                   }
                   Log.d("SubscriptionContainer", "Successfully fetched ${subscriptionPackages.size} subscription packages")
               } else {
                   Log.e("SubscriptionContainer", "API call failed: ${response?.code()}")
                   _uiState.update {
                       it.copy(
                           fetchingStatus = LoadingStatus.FAIL
                       )
                   }
               }

            } catch (e: Exception) {
                Log.e("SubscriptionContainer", "Exception fetching subscription packages", e)
                _uiState.update {
                    it.copy(
                        fetchingStatus = LoadingStatus.FAIL
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
                        val response = authenticationManager.executeWithAuth { token ->
                            apiRepository.getTransaction(
                                token = token,
                                id = uiState.value.transactionId!!,
                            )
                        }
                        if(response?.isSuccessful == true) {
                            Log.d("STATUS_CHECKED:", uiState.value.state)
                            _uiState.update {
                                it.copy(
                                    failedReason = response.body()?.data!!.failedReason,
                                    state = response.body()?.data!!.status
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("failedToCheckPaymentStatus", "Failed to check payment status")
                    }
                }

                if(!uiState.value.cancelled && uiState.value.state.lowercase() == "completed") {
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
                    dbRepository.getUserPreferences()?.collect { preferences ->
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
        getSubscriptionContainer()
    }
}