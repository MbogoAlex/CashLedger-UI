package com.records.pesa.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.user.update.UserProfileUpdatePayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AccountInformationScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val preferences: UserPreferences = userPreferences,
    val appLaunchStatus: AppLaunchStatus = AppLaunchStatus(),
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val successMessage: String = "",
    val saveButtonEnabled: Boolean = false,
    val clearLoginDetails: Boolean = false,
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

    fun updateClearLoginDetails() {
        _uiState.update {
            it.copy(
                clearLoginDetails = !uiState.value.clearLoginDetails
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

        val userProfileUpdatePayload = UserProfileUpdatePayload(
            userId = uiState.value.userDetails.userId.toString(),
            fname = uiState.value.firstName.ifEmpty { null },
            lname = uiState.value.lastName.ifEmpty { null },
            email = uiState.value.email.ifEmpty { null },
            phoneNumber = null,
            password = null
        )

        viewModelScope.launch {
            try {

                val response = apiRepository.updateUserProfile(
                    userProfileUpdatePayload = userProfileUpdatePayload
                )

                if(response.isSuccessful) {
                    dbRepository.updateUser(
                        uiState.value.userDetails.copy(
                            firstName = uiState.value.firstName.ifEmpty { uiState.value.userDetails.firstName },
                            lastName = uiState.value.lastName.ifEmpty { uiState.value.userDetails.lastName },
                            email = uiState.value.email.ifEmpty { uiState.value.userDetails.email }
                        )
                    )
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
                            successMessage = "Failed to update details. Check your connection or try later",
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                }


            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        successMessage = "Failed to update details. Check your connection or try later",
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

            if(uiState.value.clearLoginDetails) {
                dbRepository.updateUser(
                    uiState.value.userDetails.copy(
                        phoneNumber = "",
                        password = ""
                    )
                )

                dbRepository.updateUserPreferences(
                    uiState.value.preferences.copy(
                        loggedIn = false
                    )
                )
            } else {
                dbRepository.updateUserPreferences(
                    uiState.value.preferences.copy(
                        loggedIn = false
                    )
                )
            }
        }
    }

    private fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val appLaunchStatus = dbRepository.getAppLaunchStatus(1).first()

                dbRepository.getUsers().collect {userDetails ->
                    _uiState.update {
                        it.copy(
                            userDetails = userDetails[0],
                            appLaunchStatus = appLaunchStatus,
                            firstName = userDetails[0].firstName ?: "",
                            lastName = userDetails[0].lastName ?: "",
                            email = userDetails[0].email ?: ""
                        )
                    }
                }
            }
        }
    }

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUserPreferences().collect() { preferences ->
                    _uiState.update {
                        it.copy(
                            preferences = preferences!!
                        )
                    }
                }
            }
        }
    }

    init {
        getUserDetails()
        getUserPreferences()
    }

}