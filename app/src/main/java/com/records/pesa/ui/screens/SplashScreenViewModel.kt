package com.records.pesa.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserAccount
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.supabase.PaymentData
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.service.userAccount.UserAccountService
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class SplashScreenUiState(
    val appLaunchStatus: AppLaunchStatus = AppLaunchStatus(),
    val userDetails: UserDetails? = null,
    val paymentStatus: Boolean? = null,
    val subscriptionStatus: Boolean = false
)

class SplashScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val userAccountService: UserAccountService
): ViewModel() {
    private val _uiState = MutableStateFlow(SplashScreenUiState())
    val uiState: StateFlow<SplashScreenUiState> = _uiState.asStateFlow()

    fun getAppLaunchState() {
        viewModelScope.launch {
            // Fetch the app launch status or handle null by inserting a default value
            var appLaunchStatus = dbRepository.getAppLaunchStatus(1).first()

            if (appLaunchStatus == null) {
                // Create a default AppLaunchStatus if it doesn't exist
                val defaultAppLaunchStatus = AppLaunchStatus(id = 1, user_id = null, launched = 1)
                dbRepository.insertAppLaunchStatus(defaultAppLaunchStatus)
                appLaunchStatus = defaultAppLaunchStatus
            }

            val userId: Int? = appLaunchStatus.user_id
            var user: UserDetails? = null
            val users = dbRepository.getUsers().first()

            if (userId != null) {
                user = if (users.isNotEmpty()) users[0] else null
            }

            _uiState.update {
                it.copy(
                    appLaunchStatus = appLaunchStatus,
                    userDetails = user
                )
            }

            if (uiState.value.appLaunchStatus.launched == 0) {
                dbRepository.updateAppLaunchStatus(
                    uiState.value.appLaunchStatus.copy(
                        launched = 1
                    )
                )
            }

            if (uiState.value.appLaunchStatus.user_id != null) {
                val userAccount = dbRepository.getUser(uiState.value.appLaunchStatus.user_id!!).first()
                Log.d("USER_ACCOUNT", userAccount.toString())
                getSubscriptionStatus(userAccount)
            }
        }
    }


    fun getSubscriptionStatus(userDetails: UserDetails) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val user = client.postgrest["userAccount"]
                        .select {
                            filter {
                                eq("phoneNumber", uiState.value.userDetails!!.phoneNumber)
                            }
                        }.decodeSingle<com.records.pesa.models.user.UserAccount>()
                    val payments = client.postgrest["payment"]
                        .select {
                            filter{
                                eq("userId", uiState.value.appLaunchStatus.user_id!!)
                            }
                        }.decodeList<PaymentData>()
                        .sortedByDescending {
                            LocalDateTime.parse(it.paidAt)
                        }
                    val payment = payments.first()

                    if(user.permanent) {
                        dbRepository.updateUser(
                            userDetails.copy(
                                paymentStatus = true
                            )
                        )
                        _uiState.update {
                            it.copy(
                                paymentStatus = true
                            )
                        }
                    } else if(payments.isNotEmpty()) {
                        val paidAt = LocalDateTime.parse(payment.paidAt)
                        if(ChronoUnit.MONTHS.between(paidAt, LocalDateTime.now())  >= 1) {
                            dbRepository.updateUser(
                                userDetails.copy(
                                    paymentStatus = false
                                )
                            )
                            _uiState.update {
                                it.copy(
                                    paymentStatus = false
                                )
                            }
                        } else {
                            dbRepository.updateUser(
                                userDetails.copy(
                                    paymentStatus = true
                                )
                            )
                            _uiState.update {
                                it.copy(
                                    paymentStatus = true
                                )
                            }
                        }
                    }

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            paymentStatus = true
                        )
                    }
                    Log.e("SubscriptionStatusCheckException", e.toString())
                }
            }
        }
    }

    init {
        getAppLaunchState()
    }
}