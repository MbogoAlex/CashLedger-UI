package com.records.pesa.ui.screens.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.PaymentPayload
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
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("initiatePaymentResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("initiatePaymentException", e.toString())

            }
        }
    }



    init {
        getUserDetails()
    }
}