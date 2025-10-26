package com.records.pesa.ui.screens.auth

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.extensions.isNotNull
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserAccount
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.UserSession
import com.records.pesa.db.models.userPreferences
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.user.login.UserLoginPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
import com.records.pesa.workers.WorkersRepository
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LoginScreenUiState(
    val phoneNumber: String = "",
    val preferences: UserPreferences? = null,
    val appLaunchStatus: AppLaunchStatus? = null,
    val transactions: List<TransactionItem> = emptyList(),
    val password: String = "",
    val loginButtonEnabled: Boolean = false,
    val loginMessage: String = "",
    val exception: String = "",
    val userDetails: UserDetails? = null,
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

        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                try {

                    val loginPayload = UserLoginPayload(
                        phoneNumber = uiState.value.phoneNumber.trim(),
                        password = uiState.value.password.trim()
                    )

                    val response = apiRepository.login(
                        userLoginPayload = loginPayload
                    )

                    if(response.isSuccessful) {

                        val session = UserSession(
                            userId = response.body()?.data?.user?.userId,
                            accessToken = response.body()?.data?.user?.accessToken,
                            refreshToken = response.body()?.data?.user?.refreshToken,
                            tokenType = response.body()?.data?.user?.tokenType,
                            accessTokenExpiresIn = response.body()?.data?.user?.accessTokenExpiresIn,
                            refreshTokenExpiresIn = response.body()?.data?.user?.refreshTokenExpiresIn
                        )
                        dbRepository.insertSession(session)

                        getUserProfile(
                            token = response.body()?.data?.user?.accessToken!!
                        )

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

    private fun getUserProfile(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiRepository.getMe(
                    token = token
                )

                if(response.isSuccessful) {
                    val user = response.body()?.data!!

                    var userAccount = try {
                        userAccountService.getUserAccount(user.userId.toInt()).firstOrNull()
                    } catch (e: Exception) {
                        null
                    }

                    if(userAccount == null) {
                        userAccount = UserAccount(
                            id = user.userId.toInt(),
                            backupUserId = user.dynamoUserId ?: user.phoneNumber.replaceFirstChar { "" }.toLong(),
                            fname = user.fname,
                            lname = user.lname,
                            email = user.email,
                            phoneNumber = user.phoneNumber,
                            password = uiState.value.password,
                            createdAt = user.createdAt?.let {
                                LocalDateTime.parse(it.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            } ?: LocalDateTime.now(),
                        )
                        userAccountService.insertUserAccount(userAccount)
                    } else {
                        userAccountService.updateUserAccount(
                            userAccount.copy(
                                id = user.userId.toInt(),
                                backupUserId = user.dynamoUserId ?: user.phoneNumber.replaceFirstChar { "" }.toLong(),
                                fname = user.fname,
                                lname = user.lname,
                                email = user.email,
                                phoneNumber = user.phoneNumber,
                                password = uiState.value.password,
                                createdAt = user.createdAt?.let {
                                    LocalDateTime.parse(it.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                } ?: LocalDateTime.now(),
                            )
                        )
                    }

                    var userDetails: UserDetails? = null

                    if(uiState.value.userDetails == null) {
                        userDetails = UserDetails(
                            userId = user.userId.toInt(),
                            dynamoUserId = user.dynamoUserId,
                            backUpUserId = user.dynamoUserId ?: user.phoneNumber.replaceFirstChar { "" }.toLong(),
                            firstName = user.fname,
                            lastName = user.lname,
                            email = user.email,
                            phoneNumber = user.phoneNumber,
                            password = uiState.value.password,
                            token = "",
                            paymentStatus = user.permanent,
                            permanent = user.permanent,
                            supabaseLogin = true,
                            backupSet = true,
                            lastBackup =  null,
                            backedUpItemsSize = 0,
                            transactions = 0,
                            categories = 0,
                            categoryKeywords = 0,
                            categoryMappings = 0
                        )
                        getUserSubscription(
                            token = token,
                            userDetails = userDetails,
                            permanent = user.permanent,
                            isInsert = true
                        )
                    } else {
                        userDetails = uiState.value.userDetails!!.copy(
                            userId = user.userId.toInt(),
                            dynamoUserId = user.dynamoUserId,
                            backUpUserId = user.dynamoUserId ?: user.phoneNumber.replaceFirstChar { "" }.toLong(),
                            firstName = user.fname,
                            lastName = user.lname,
                            email = user.email,
                            phoneNumber = user.phoneNumber,
                            password = uiState.value.password,
                            token = "",
                            paymentStatus = user.permanent,
                            permanent = user.permanent,
                            supabaseLogin = true,
                            backupSet = true,
                            lastBackup =  null,
                            backedUpItemsSize = 0,
                            transactions = 0,
                            categories = 0,
                            categoryKeywords = 0,
                            categoryMappings = 0
                        )
                        Log.d("updatingUser", userDetails.toString())
                        getUserSubscription(
                            token = token,
                            userDetails = userDetails,
                            permanent = user.permanent,
                            isInsert = false
                        )
                    }

                }

                Log.d("UserProfile", response.toString())
            } catch (e: Exception) {
                Log.e("UserProfileException", e.toString())
            }
        }
    }

    private fun getUserSubscription(
        token: String,
        userDetails: UserDetails,
        permanent: Boolean,
        isInsert: Boolean
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = apiRepository.getUserSubscription(
                        token = token,
                        containerId = 1
                    )

                    if(response.isSuccessful) {
                        val subscription = response.body()?.data
                        Log.d("UserSubscription", subscription.toString())
                        val user = userDetails.copy(
                            permanent = subscription?.subscriptionPackageId == 7,
                            paymentStatus = subscription != null,
                            paidAt = subscription?.paidAt,
                            expiredAt = subscription?.expiredAt
                        )

                        if(isInsert) {
                            dbRepository.insertUser(user)
                        } else {
                            dbRepository.updateUser(user)
                        }

                        if(uiState.value.preferences != null) {
                            val prefs = uiState.value.preferences!!.copy(
                                paid = subscription != null,
                                paidAt = if (subscription?.paidAt != null) LocalDateTime.parse(
                                    subscription?.paidAt
                                ) else null,
                                expiryDate = if (subscription?.expiredAt != null) LocalDateTime.parse(
                                    subscription?.expiredAt
                                ) else null,
                                permanent = permanent,
                                showBalance = true,
                                loggedIn = true,
                                hasSubmittedMessages = false
                            )
                            dbRepository.updateUserPreferences(prefs)
                        } else {
                            val prefs = UserPreferences(
                                paid = subscription != null,
                                paidAt = if (subscription?.paidAt != null) LocalDateTime.parse(
                                    subscription?.paidAt
                                ) else null,
                                expiryDate = if (subscription?.expiredAt != null) LocalDateTime.parse(
                                    subscription?.expiredAt
                                ) else null,
                                loggedIn = true,
                                darkMode = false,
                                restoredData = false,
                                lastRestore = null,
                                permanent = permanent,
                                showBalance = true,
                                hasSubmittedMessages = false
                            )

                            dbRepository.insertUserPreferences(prefs)
                        }



                        dbRepository.updateAppLaunchStatus(
                            uiState.value.appLaunchStatus!!.copy(
                                user_id = user.userId.toInt()
                            )
                        )


                        _uiState.update {
                            it.copy(
                                userDetails = user,
                                loginStatus = LoginStatus.SUCCESS,
                                loginMessage = "Logged in successfully"
                            )
                        }

                    }

                } catch (e: Exception) {
                    Log.e("UserSubscriptionsException", e.toString())
                }
            }
        }
    }

    private fun getTransactions() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                while(uiState.value.userDetails == null) {
                    delay(1000)
                }

                Log.d("gettingTransactions", "Transactions are being fetched at login screen")

                val query = transactionService.createUserTransactionQuery(
                    userId = uiState.value.userDetails!!.backUpUserId.toInt(),
                    entity = null,
                    categoryId = null,
                    budgetId = null,
                    transactionType = null,
                    moneyDirection = null,
                    startDate = LocalDate.now().minusYears(100),
                    endDate = LocalDate.now(),
                    latest = true
                )

                try {
                    val transactions = transactionService.getUserTransactions(query).firstOrNull()
                    if (transactions != null) {
                        _uiState.update {
                            it.copy(
                                transactions = transactions.map { transactionWithCategories ->  transactionWithCategories.toTransactionItem() },
                            )
                        }
                        Log.d("TRANSACTIONS_SIZE", transactions.size.toString())
                        Log.d("gettingTransactions", "transactions: $transactions")
                    }

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
                dbRepository.getUserPreferences()?.collect() { preferences ->
                    _uiState.update {
                        it.copy(
                            preferences = preferences
                        )
                    }
                }
            }
        }
    }

    fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUser()?.collect { user ->
                    _uiState.update {
                        it.copy(
                            userDetails = user
                        )
                    }
                }
            }
        }
    }

    fun getAppLaunchStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getAppLaunchStatus(id = 1)?.collect { appLaunchStatus ->
                    _uiState.update {
                        it.copy(
                            appLaunchStatus = appLaunchStatus
                        )
                    }
                }
            }
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
        getUserDetails()
        getTransactions()
        getAppLaunchStatus()
        getUserPreferences()
    }
}