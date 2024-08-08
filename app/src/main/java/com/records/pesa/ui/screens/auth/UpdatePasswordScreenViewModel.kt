package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.user.PasswordUpdatePayload
import com.records.pesa.models.user.UserRegistrationPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdatePasswordScreenUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val resetMessage: String = "",
    val resetButtonEnabled: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class UpdatePasswordScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(value = UpdatePasswordScreenUiState())
    val uiState: StateFlow<UpdatePasswordScreenUiState> = _uiState.asStateFlow()

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.update {
            it.copy(
                phoneNumber = phoneNumber
            )
        }
    }

    fun updatePassword(password: String) {
        _uiState.update {
            it.copy(
                password = password
            )
        }
    }

    fun updatePasswordConfirmation(password: String) {
        _uiState.update {
            it.copy(
                passwordConfirmation = password
            )
        }
    }

    fun resetPassword() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        val passwordUpdatePayload = PasswordUpdatePayload(
            phoneNumber = uiState.value.phoneNumber,
            newPassword = uiState.value.password
        )
        viewModelScope.launch {
            try {
                val response = apiRepository.updateUserPassword(passwordUpdatePayload)
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.SUCCESS,
                            resetMessage = "New password set successfully"
                        )
                    }
                } else {
                    Log.e("ResetPasswordResponseError", response.toString())
                    if(response.code() == 401) {
                        _uiState.update {
                            it.copy(
                                loadingStatus = LoadingStatus.FAIL,
                                resetMessage = "No account with the phone number exists"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ResetPasswordException", e.toString())
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL,
                        resetMessage = "Password reset failed. Check your internet or try later"
                    )
                }
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

    fun buttonEnabled() {
        _uiState.update {
            it.copy(
                resetButtonEnabled = uiState.value.phoneNumber.isNotEmpty() &&
                        uiState.value.password.isNotEmpty() &&
                        uiState.value.passwordConfirmation.isNotEmpty()
            )
        }
    }
}