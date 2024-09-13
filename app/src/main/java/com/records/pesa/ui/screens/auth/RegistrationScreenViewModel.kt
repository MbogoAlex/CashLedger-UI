package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.user.UserAccount
import com.records.pesa.models.user.UserRegistrationPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.SupabaseClient
import com.records.pesa.network.SupabaseClient.client
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.time.LocalDateTime

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
        val userAccount = UserAccount(
            phoneNumber = uiState.value.phoneNumber,
            password = uiState.value.password,
            month = LocalDateTime.now().monthValue,
            role = 0,
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val user = client.from("userAccount").insert(userAccount) {
                        select()
                    }.decodeSingle<UserAccount>()
                    Log.d("RgistrationDetails", user.toString())
                    dbRepository.deleteAllFromUser()
                    val userDetails = UserDetails(
                        userId = user.id ?: 0,
                        firstName = user.fname,
                        lastName = user.lname,
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                        password = uiState.value.password,
                        token = "",
                        paymentStatus = false
                    )
                    val appLaunchStatus = dbRepository.getAppLaunchStatus(1).first()
                    dbRepository.updateAppLaunchStatus(
                        appLaunchStatus.copy(
                            user_id = user.id ?: 0
                        )
                    )
                    dbRepository.insertUser(userDetails)
                    var users = emptyList<UserDetails>()

                    while (users.isEmpty()) {
                        delay(1000)
                        users = dbRepository.getUsers().first()
                    }
                    if(users.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                registrationStatus = RegistrationStatus.SUCCESS,
                                registrationMessage = "Registration Success"
                            )
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