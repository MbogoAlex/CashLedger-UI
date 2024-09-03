package com.records.pesa.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.userAccount.UserAccountService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplashScreenUiState(
    val appLaunchStatus: AppLaunchStatus = AppLaunchStatus(),
    val userDetails: UserDetails? = null,
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
                val userAccount = userAccountService.getUserAccount(uiState.value.appLaunchStatus.user_id!!).first()
                Log.d("USER_ACCOUNT", userAccount.toString())
                getSubscriptionStatus()
            }
        }
    }


    fun getSubscriptionStatus() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getSubscriptionStatus(uiState.value.appLaunchStatus.user_id!!)
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            subscriptionStatus = response.body()?.data?.payment!!
                        )
                    }
                } else {
                    Log.e("SubscriptionStatusCheckError", response.toString())
                }
            } catch (e: Exception) {
                Log.e("SubscriptionStatusCheckException", e.toString())
            }
        }
    }

    init {
        getAppLaunchState()
    }
}