package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.user.registration.UserRegistrationPayload
import com.records.pesa.network.ApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

data class RegistrationScreenUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val preferences: UserPreferences? = null,
    val passwordConfirmation: String = "",
    val registerButtonEnabled: Boolean = false,
    val registrationMessage: String = "",
    val registrationStatus: RegistrationStatus = RegistrationStatus.INITIAL
)

class RegistrationScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
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

        val userRegistrationPayload = UserRegistrationPayload(
            phoneNumber = uiState.value.phoneNumber.trim(),
            password = uiState.value.password.trim(),
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {

                    val response = apiRepository.registerUser(
                        userRegistrationPayload = userRegistrationPayload
                    )

                    if(response.isSuccessful) {

                        val userDetails = UserDetails(
                            userId = response.body()?.data?.user?.userId!!.toInt(),
                            firstName = response.body()?.data?.user?.firstName,
                            lastName = response.body()?.data?.user?.lastName,
                            email = response.body()?.data?.user?.email,
                            phoneNumber = response.body()?.data?.user?.phoneNumber!!,
                            password = uiState.value.password,
                            token = "",
                            paymentStatus = false
                        )
                        val appLaunchStatus = dbRepository.getAppLaunchStatus(1)?.first()

                        if(appLaunchStatus != null) {
                            dbRepository.updateAppLaunchStatus(
                                appLaunchStatus.copy(
                                    user_id = response.body()?.data?.user?.userId!!.toInt()
                                )
                            )
                        }

                        dbRepository.insertUser(userDetails)

                        _uiState.update {
                            it.copy(
                                registrationStatus = RegistrationStatus.SUCCESS,
                                registrationMessage = "Registration Successful"
                            )
                        }

                    } else {
                        if(response.code() == 400) {
                            _uiState.update {
                                it.copy(
                                    registrationMessage = "User with phone number ${uiState.value.phoneNumber} already exists"
                                )
                            }
                        }

                        _uiState.update {
                            it.copy(
                                registrationStatus = RegistrationStatus.FAIL,
                            )
                        }
                    }


                } catch (e: Exception) {
                    Log.e("UserRegistrationException", e.message.toString())
                    _uiState.update {
                        it.copy(
                            registrationStatus = RegistrationStatus.FAIL,
                            registrationMessage = if(e.message.toString().contains("duplicate")) "User already exists" else "Registration failed. Check your internet or try later"
                        )
                    }
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