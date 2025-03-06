package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.extensions.isNotNull
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserAccount
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.supabase.PaymentData
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.user.UserLoginPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.transaction.TransactionService
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
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.absoluteValue

data class LoginScreenUiState(
    val phoneNumber: String = "",
    val preferences: UserPreferences = userPreferences,
    val transactions: List<TransactionItem> = emptyList(),
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
    private val userAccountService: UserAccountService,
    private val transactionService: TransactionService,
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

                var userPreferences = dbRepository.getUserPreferences().first()


                try {
                    val user = client.postgrest["userAccount"]
                        .select {
                            filter {
                                eq("phoneNumber", loginPayload.phoneNumber)
                            }
                        }.decodeSingle<com.records.pesa.models.user.UserAccount>()

                    if(user.isNotNull()) {
                        if(BCrypt.checkpw(uiState.value.password, user.password)) {
                            var userAccount = try {
                                userAccountService.getUserAccount(user.id!!).first()
                            } catch (e: Exception) {
                                null
                            }

                            if(userAccount == null) {
                                userAccount = UserAccount(
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
                            } else {
                                userAccountService.updateUserAccount(
                                    userAccount.copy(
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
                                )
                            }

                            var userDetails = try {
                                dbRepository.getUser(user.id!!).first()
                            } catch (e: Exception) {
                                Log.e("gettingUser", e.toString())
                                null
                            }

                            if(userDetails == null) {
                                userDetails = UserDetails(
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
                                dbRepository.insertUser(userDetails)
                            } else {
                                userDetails = userDetails.copy(
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
                                Log.d("updatingUser", userDetails.toString())
                                dbRepository.updateUser(userDetails)
                            }

                            val appLaunchStatus = dbRepository.getAppLaunchStatus(1).first()
                            dbRepository.updateAppLaunchStatus(
                                appLaunchStatus.copy(
                                    user_id = user.id ?: 0
                                )
                            )
                            dbRepository.insertUser(userDetails)
                            var users = emptyList<UserDetails>()

                            userPreferences = userPreferences!!.copy(
                                loggedIn = true
                            )

                            while (users.isEmpty() || userDetails!!.phoneNumber.isEmpty() || userDetails.password.isEmpty()) {
                                delay(1000)
                                userDetails = dbRepository.getUser(userId = user.id!!).first()
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
                                val sortedPayments = payments.sortedByDescending {
                                    it.id
                                }
                                userPreferences = userPreferences.copy(
                                    paid = true,
                                    permanent = true,
                                    paidAt = LocalDateTime.parse(sortedPayments[0].paidAt),
                                    expiryDate = LocalDateTime.parse(sortedPayments[0].expiredAt)
                                )
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

                                    userPreferences = userPreferences.copy(
                                        paid = false,
                                        paidAt = LocalDateTime.parse(sortedPayments[0].paidAt),
                                        expiryDate = LocalDateTime.parse(sortedPayments[0].expiredAt)
                                    )

                                } else {
                                    userPreferences = userPreferences.copy(
                                        paid = true,
                                        paidAt = LocalDateTime.parse(sortedPayments[0].paidAt),
                                        expiryDate = LocalDateTime.parse(sortedPayments[0].expiredAt)
                                    )
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

                            dbRepository.updateUserPreferences(userPreferences)

                            if(users.isNotEmpty()) {
                                _uiState.update {
                                    it.copy(
                                        userDetails = dbRepository.getUser(userId = user.id!!).first(),
                                        loginStatus = LoginStatus.SUCCESS,
                                        loginMessage = "Logged in successfully"
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

    private fun getTransactions() {

        Log.d("gettingTransactions", "Transactions are being fetched at login screen")

        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.userId,
            entity = null,
            categoryId = null,
            budgetId = null,
            transactionType = null,
            moneyDirection = null,
            startDate = LocalDate.now().minusYears(100),
            endDate = LocalDate.now(),
            latest = true
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val transactions = transactionService.getUserTransactions(query).first()
                    _uiState.update {
                        it.copy(
                            transactions = transactions.map { transactionWithCategories ->  transactionWithCategories.toTransactionItem() },
                        )
                    }
                    Log.d("TRANSACTIONS_SIZE", transactions.size.toString())
                    Log.d("gettingTransactions", "transactions: $transactions")
                } catch (e: Exception) {
                    Log.e("GetTransactionsException", e.toString())
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

    fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val users = dbRepository.getUsers().first()
                _uiState.update {
                    it.copy(
                        userDetails = if(users.isEmpty()) UserDetails() else users[0]
                    )
                }
            }
            while(uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getTransactions()
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
        getUserPreferences()
        getUserDetails()
    }
}