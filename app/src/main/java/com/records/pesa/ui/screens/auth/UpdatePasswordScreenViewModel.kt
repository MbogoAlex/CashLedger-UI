package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.user.PasswordUpdatePayload
import com.records.pesa.models.user.UserAccount
import com.records.pesa.models.user.UserRegistrationPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.reusables.LoadingStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

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
                    val user = client.postgrest["userAccount"]
                        .select {
                            filter {
                                eq("phoneNumber", uiState.value.phoneNumber)
                            }
                        }.decodeSingle<UserAccount>()
                    val userAccount = UserAccount(
                        phoneNumber = uiState.value.phoneNumber,
                        password = hashedPassword,
                        month = user.month,
                        role = 0,
                    )
                    client.postgrest["userAccount"]
                        .update(userAccount) {
                            filter {
                                eq("phoneNumber", uiState.value.phoneNumber)
                            }
                        }
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.SUCCESS,
                            resetMessage = "Password reset successfully"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ResetPasswordException", e.message.toString())
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