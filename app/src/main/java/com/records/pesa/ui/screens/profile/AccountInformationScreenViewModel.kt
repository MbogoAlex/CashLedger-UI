package com.records.pesa.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.user.UserRegistrationPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountInformationScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val appLaunchStatus: AppLaunchStatus = AppLaunchStatus(),
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val successMessage: String = "",
    val saveButtonEnabled: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class AccountInformationScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(AccountInformationScreenUiState())
    val uiState: StateFlow<AccountInformationScreenUiState> = _uiState.asStateFlow()

    fun updateFirstName(firstName: String) {
        _uiState.update {
            it.copy(
                firstName = firstName
            )
        }
    }

    fun updateLastName(lastName: String) {
        _uiState.update {
            it.copy(
                lastName = lastName
            )
        }
    }

    fun updateEmail(email: String) {
        _uiState.update {
            it.copy(
                email = email
            )
        }
    }

    fun updateUserDetails() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        val user = UserRegistrationPayload(
            fname = uiState.value.firstName,
            lname = uiState.value.lastName,
            email = uiState.value.email,
            phoneNumber = uiState.value.userDetails.phoneNumber,
            password = uiState.value.userDetails.password,
        )
        viewModelScope.launch {
            try {
                val response = apiRepository.updateUserDetails(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    user = user
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            successMessage = "Successfully updated details",
                            userDetails = dbRepository.getUser(uiState.value.userDetails.userId).first(),
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            successMessage = "Failed to update details",
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("updateUserDetailsResponseError", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        successMessage = "Failed to update details",
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("updateUserDetailsException", e.toString())
            }
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            dbRepository.deleteUser(uiState.value.userDetails.userId)
            dbRepository.updateAppLaunchStatus(
                uiState.value.appLaunchStatus.copy(
                    user_id = null
                )
            )
        }
    }

    private fun getUserDetails() {
        viewModelScope.launch {
            val appLaunchStatus = dbRepository.getAppLaunchStatus(1).first()
            val user = dbRepository.getUsers().first()[0]
            _uiState.update {
                it.copy(
                    userDetails = user,
                    appLaunchStatus = appLaunchStatus,
                    firstName = user.firstName ?: "",
                    lastName = user.lastName ?: "",
                    email = user.email ?: ""
                )
            }
        }
    }

    init {
        getUserDetails()
    }

}