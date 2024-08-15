package com.records.pesa.ui.screens.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.PaymentPayload
import com.records.pesa.models.payment.SubscriptionPaymentStatusPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriptionScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val phoneNumber: String = "",
    val paymentToken: String = "",
    val orderTrackingId: String = "",
    val redirectUrl: String = "",
    val paymentMessage: String = "",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class SubscriptionScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(SubscriptionScreenUiState())
    val uiState: StateFlow<SubscriptionScreenUiState> = _uiState.asStateFlow()

    fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            paySubscriptionFee()
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



    init {
        getUserDetails()
    }
}