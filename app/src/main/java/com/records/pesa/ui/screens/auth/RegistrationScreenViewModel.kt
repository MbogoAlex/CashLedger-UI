package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.user.UserRegistrationPayload
import com.records.pesa.network.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrationScreenUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val registerButtonEnabled: Boolean = false,
    val registrationMessage: String = "",
    val registrationStatus: RegistrationStatus = RegistrationStatus.INITIAL
)

class RegistrationScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(RegistrationScreenUiState())
    val uiState: StateFlow<RegistrationScreenUiState> = _uiState.asStateFlow()

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

    fun registerUser() {
        _uiState.update {
            it.copy(
                registrationStatus = RegistrationStatus.LOADING
            )
        }
        val registrationPayload = UserRegistrationPayload(
            fname = null,
            lname = null,
            email = null,
            phoneNumber = uiState.value.phoneNumber,
            password = uiState.value.password
        )
        viewModelScope.launch {
            try {
                val response = apiRepository.registerUser(registrationPayload)
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            registrationStatus = RegistrationStatus.SUCCESS,
                            registrationMessage = "Registration successful"
                        )
                    }
                } else {
                    Log.e("UserRegistrationResponseError", response.toString())
                    if(response.code() == 409) {
                        _uiState.update {
                            it.copy(
                                registrationStatus = RegistrationStatus.FAIL,
                                registrationMessage = "User already exists"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UserRegistrationException", e.toString())
                _uiState.update {
                    it.copy(
                        registrationStatus = RegistrationStatus.FAIL,
                        registrationMessage = "Registration failed. Check your internet or try later"
                    )
                }
            }
        }
    }

    fun resetRegistrationStatus() {
        _uiState.update {
            it.copy(
                registrationStatus = RegistrationStatus.INITIAL
            )
        }
    }

    fun buttonEnabled() {
        _uiState.update {
            it.copy(
                registerButtonEnabled = uiState.value.phoneNumber.isNotEmpty() &&
                        uiState.value.password.isNotEmpty() &&
                        uiState.value.passwordConfirmation.isNotEmpty()
            )
        }
    }

}