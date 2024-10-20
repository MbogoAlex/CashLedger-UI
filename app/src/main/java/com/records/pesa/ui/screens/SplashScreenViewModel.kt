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
            Log.d("appLaunchStatus", appLaunchStatus.toString())

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
                Log.d("gettingUserData", "GETTING USER DATA")
                val userAccount = dbRepository.getUser(uiState.value.appLaunchStatus.user_id!!).first()
                _uiState.update {
                    it.copy(
                        userDetails = userAccount
                    )
                }
                Log.d("USER_ACCOUNT", userAccount.toString())
                getSubscriptionStatus(userAccount)
            }
        }
    }


    fun getSubscriptionStatus(userDetails: UserDetails) {
        Log.d("subscriptionStatusCheckMessage", "getting subscription status")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if(userDetails.permanent) {
                    dbRepository.updateUser(
                        userDetails.copy(
                            paymentStatus = true,
                        )
                    )
                    _uiState.update {
                        it.copy(
                            paymentStatus = true
                        )
                    }
                } else {
                    if(userDetails.paidAt == null) {
                        _uiState.update {
                            it.copy(
                                paymentStatus = false
                            )
                        }
                    } else {
                        val paidAt = LocalDateTime.parse(userDetails.paidAt)
                        val expiredAt = LocalDateTime.parse(userDetails.expiredAt)
                        if(expiredAt.isBefore(LocalDateTime.now())) {
                            dbRepository.updateUser(
                                userDetails.copy(
                                    paymentStatus = false,
                                    darkThemeSet = false
                                )
                            )
                            _uiState.update {
                                it.copy(
                                    paymentStatus = false,
                                )
                            }
                        } else if(expiredAt.isAfter(LocalDateTime.now())) {
                            dbRepository.updateUser(
                                userDetails.copy(
                                    paymentStatus = true,
                                )
                            )
                            _uiState.update {
                                it.copy(
                                    paymentStatus = false
                                )
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        paymentStatus = false
                    )
                }

            }
        }
    }

    init {
        getAppLaunchState()
    }
}