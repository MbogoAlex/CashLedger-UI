package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.extensions.isNotNull
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserAccount
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.supabase.PaymentData
import com.records.pesa.models.user.UserLoginPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.SupabaseClient
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.service.userAccount.UserAccountService
import com.records.pesa.workers.WorkersRepository
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
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class LoginScreenUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val loginButtonEnabled: Boolean = false,
    val loginMessage: String = "",
    val exception: String = "",
    val userDetails: UserDetails = UserDetails(),
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
            withContext(Dispatchers.IO) {

                try {
                    val user = client.postgrest["userAccount"]
                        .select {
                            filter {
                                eq("phoneNumber", loginPayload.phoneNumber)
                            }
                        }.decodeSingle<com.records.pesa.models.user.UserAccount>()

                    if(user.isNotNull()) {
                        if(BCrypt.checkpw(uiState.value.password, user.password)) {
                            val userAccount = UserAccount(
                                id = user.id ?: 0,
                                fname = user.fname,
                                lname = user.lname,
                                email = user.email,
                                phoneNumber = user.phoneNumber,
                                password = uiState.value.password,
                                createdAt = user.createdAt?.let {
                                    LocalDateTime.parse(it)
                                } ?: LocalDateTime.now(),
                            )

                            userAccountService.insertUserAccount(userAccount)
                            dbRepository.deleteAllFromUser()
                            Log.d("loggedInUser", user.toString())
                            val userDetails = UserDetails(
                                userId = user.id ?: 0,
                                firstName = user.fname,
                                lastName = user.lname,
                                email = user.email,
                                phoneNumber = user.phoneNumber,
                                password = uiState.value.password,
                                token = "",
                                paymentStatus = user.permanent,
                                permanent = user.permanent,
                                supabaseLogin = true,
                                backupSet = user.backupSet,
                                lastBackup = if(user.lastBackup != null) LocalDateTime.parse(user.lastBackup) else null,
                                backedUpItemsSize = user.backedUpItemsSize,
                                transactions = user.transactions,
                                categories = user.categories,
                                categoryKeywords = user.categoryKeywords,
                                categoryMappings = user.categoryMappings
                            )
                            Log.d("loggedInUserDetails", userDetails.toString())
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

                            val userData = dbRepository.getUser(userId = userAccount.id).first()
                            val payments = client.postgrest["payment"]
                                .select {
                                    filter{
                                        eq("userId", userAccount.id)
                                    }
                                }.decodeList<PaymentData>()

                            if(user.permanent) {
                                dbRepository.updateUser(
                                    userData.copy(
                                        paymentStatus = true,
                                    )
                                )
                            } else if(payments.isNotEmpty()) {
                                val sortedPayments = payments.sortedByDescending {
                                    it.id
                                }
                                Log.d("sortedPayments", sortedPayments.toString())
                                val payment = sortedPayments.first()
                                val paidAt = if(payment.paidAt != null) LocalDateTime.parse(payment.paidAt) else null
                                val expiredAt = if(payment.expiredAt != null) LocalDateTime.parse(payment.expiredAt) else LocalDateTime.parse(payment.freeTrialEndedOn)
                                if(expiredAt!!.isBefore(LocalDateTime.now())) {
                                    Log.d("paymentStatus", expiredAt!!.isBefore(LocalDateTime.now()).toString())
                                    dbRepository.updateUser(
                                        userData.copy(
                                            paymentStatus = false,
                                            paidAt = payment.paidAt,
                                            expiredAt = payment.expiredAt
                                        )
                                    )
                                } else {
                                    dbRepository.updateUser(
                                        userData.copy(
                                            paymentStatus = true,
                                            paidAt = payment.paidAt,
                                            expiredAt = payment.expiredAt
                                        )
                                    )
                                }
                            } else {
                                dbRepository.updateUser(
                                    userData.copy(
                                        paymentStatus = false
                                    )
                                )
                            }

                            if(userData.backupSet) {
                                workersRepository.fetchAndBackupTransactions(
                                    token = "dala",
                                    userId = userData.userId
                                )
                            }

                            if(users.isNotEmpty()) {
                                _uiState.update {
                                    it.copy(
                                        userDetails = dbRepository.getUser(userId = user.id!!).first(),
                                        loginStatus = LoginStatus.SUCCESS,
                                        loginMessage = "Login Successfully"
                                    )
                                }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    loginStatus = LoginStatus.FAIL,
                                    loginMessage = "Invalid credentials"
                                )
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                loginStatus = LoginStatus.FAIL,
                                loginMessage = "Invalid credentials"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UserLoginException", e.toString())
                    _uiState.update {
                        it.copy(
                            loginStatus = LoginStatus.FAIL,
                            exception = e.toString(),
                            loginMessage = "Failed. Ensure you are connected to the internet"
                        )
                    }
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