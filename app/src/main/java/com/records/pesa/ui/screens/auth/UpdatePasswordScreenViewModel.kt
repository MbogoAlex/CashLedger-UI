package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.user.update.UserProfileUpdatePayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

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

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val hashedPassword = BCrypt.hashpw(uiState.value.password, BCrypt.gensalt())

                    val userAccountResponse = apiRepository.getUserByPhoneNumber(uiState.value.phoneNumber)

                    if(userAccountResponse.isSuccessful) {
                        val userAccount = userAccountResponse.body()?.data!!

                        val userProfileUpdatePayload = UserProfileUpdatePayload(
                            userId = userAccount.id,
                            fname = null,
                            lname = null,
                            email = null,
                            phoneNumber = null,
                            password = hashedPassword
                        )

                        val userProfileUpdateResponse = apiRepository.updateUserProfile(userProfileUpdatePayload)

                        if(userProfileUpdateResponse.isSuccessful) {
                            _uiState.update {
                                it.copy(
                                    loadingStatus = LoadingStatus.SUCCESS,
                                    resetMessage = "Password reset successfully"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    loadingStatus = LoadingStatus.FAIL,
                                    resetMessage = "Password reset failed. Check your internet or try later"
                                )
                            }
                        }

                    } else {
                        _uiState.update {
                            it.copy(
                                loadingStatus = LoadingStatus.FAIL,
                                resetMessage = "User with the phone number not found"
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e("ResetPasswordException", e.toString())
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL,
                            resetMessage = if(e.message.toString().contains("empty")) "User with this phone number does not exist" else "Password reset failed. Check your internet or try later"
                        )
                    }
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