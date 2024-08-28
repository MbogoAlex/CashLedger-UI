package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.extensions.isNotNull
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserAccount
import com.records.pesa.models.user.UserLoginPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.userAccount.UserAccountService
import com.records.pesa.workers.WorkersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class LoginScreenUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val loginButtonEnabled: Boolean = false,
    val loginMessage: String = "",
    val exception: String = "",
    val loginStatus: LoginStatus = LoginStatus.INITIAL
)

class LoginScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val savedStateHandle: SavedStateHandle,
    private val workersRepository: WorkersRepository,
    private val userAccountService: UserAccountService
): ViewModel() {
    private val _uiState = MutableStateFlow(LoginScreenUiState())
    val uiState: StateFlow<LoginScreenUiState> = _uiState.asStateFlow()

    val phoneNumber: String? = savedStateHandle[LoginScreenDestination.phoneNumber]
    val password: String? = savedStateHandle[LoginScreenDestination.password]

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


    fun loginUser() {
        _uiState.update {
            it.copy(
                loginStatus = LoginStatus.LOADING
            )
        }
        val loginPayload = UserLoginPayload(
            phoneNumber = uiState.value.phoneNumber,
            password = uiState.value.password
        )
        viewModelScope.launch {
            try {
                val response = apiRepository.loginUser(uiState.value.password, loginPayload)
                if(response.isSuccessful) {
                    val userAccount = UserAccount(
                        id = response.body()?.data?.user?.userInfo?.id!!,
                        fname = null,
                        lname = null,
                        email = null,
                        phoneNumber = response.body()?.data?.user?.userInfo?.phoneNumber!!,
                        password = uiState.value.password,
                        createdAt = LocalDateTime.now()
                    )
                    try {
                        userAccountService.insertUserAccount(userAccount)
                    } catch (e: Exception) {
                        e.printStackTrace()
//                        Log.e("failedToSaveUserException", e.toString())
                    }

                    var users = dbRepository.getUsers().first()
                    when (users.isEmpty()) {
                        true -> {
                            users = dbRepository.getUsers().first()
                        }
                        false -> {
                            workersRepository.fetchAndPostMessages(
                                token = response.body()?.data?.user?.token!!,
                                userId = response.body()?.data?.user?.userInfo?.id!!
                            )
                            _uiState.update {
                                it.copy(
                                    loginStatus = LoginStatus.SUCCESS,
                                    loginMessage = "Login successful"
                                )
                            }
                        }
                    }
                } else {
                    Log.e("UserLoginResponseError", response.toString())
                    if(response.code() == 401) {
                        _uiState.update {
                            it.copy(
                                loginStatus = LoginStatus.FAIL,
                                loginMessage = "Invalid credentials"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UserLoginException", e.toString())
                _uiState.update {
                    it.copy(
                        loginStatus = LoginStatus.FAIL,
                        exception = e.toString(),
                        loginMessage = "Login failed. Check your internet or try later"
                    )
                }
            }
        }
    }

    fun resetLoginStatus() {
        _uiState.update {
            it.copy(
                loginStatus = LoginStatus.INITIAL
            )
        }
    }

    fun buttonEnabled() {
        _uiState.update {
            it.copy(
                loginButtonEnabled = uiState.value.phoneNumber.isNotEmpty() &&
                        uiState.value.password.isNotEmpty()
            )
        }
    }

    init {
        if(phoneNumber.isNotNull() && password.isNotNull()) {
            _uiState.update {
                it.copy(
                    phoneNumber = phoneNumber!!,
                    password = password!!
                )
            }
        }
    }
}