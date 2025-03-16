package com.records.pesa

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.models.dbModel.UserDetails
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
import java.time.LocalDateTime

data class MainActivityUiState(
    val userDetails: UserDetails = UserDetails(),
    val navigate: Boolean = false,
    val preferences: UserPreferences? = userPreferences
)

class MainActivityViewModel(
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(value = MainActivityUiState())
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

    private fun setUserPreferences() {
        Log.d("MainActivityViewModel", "setUserPreferences called")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    dbRepository.insertUserPreferences(
                        UserPreferences(
                            darkMode = false,
                            restoredData = false,
                            lastRestore = null,
                            loggedIn = false,
                            paid = false,
                            permanent = false,
                            paidAt = null,
                            expiryDate = null,
                            showBalance = true
                        )
                    )
                    val userDetailsList = dbRepository.getUsers().firstOrNull()
                    val userDetails: UserDetails? = userDetailsList?.firstOrNull()

                    // Check if the userPreferences table exists
                    val userPreferences = dbRepository.getUserPreferences().first()

                    dbRepository.updateUserPreferences(
                        userPreferences!!.copy(
                            loggedIn = userPreferences.loggedIn,
                            darkMode = userPreferences.darkMode,
                            restoredData = true,
                            lastRestore = null,
                            paidAt = userDetails?.paidAt?.let { LocalDateTime.parse(it.replace(" ", "T")) },
                            expiryDate = userDetails?.expiredAt?.let { LocalDateTime.parse(it.replace(" ", "T")) },
                            paid = userDetails?.paymentStatus ?: false,
                            permanent = userDetails?.permanent ?: false,
                            showBalance = userPreferences.showBalance
                        )
                    )

                    var userPrefs: UserPreferences? = null

                    while(userPrefs == null) {
                        delay(1000)
                        userPrefs = try {
                            dbRepository.getUserPreferences().first()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    getUserPreferences()
                    getSubscriptionStatus(userPrefs)

                    _uiState.update {
                        it.copy(
                            navigate = true
                        )
                    }

                } catch (e: Exception) {
                    Log.e("Database Error", "Table might not exist: ${e.message}")
                }
            }
        }
    }

    fun resetNavigation() {
        _uiState.update {
            it.copy(
                navigate = false
            )
        }
    }

    private fun getSubscriptionStatus(userPrefs: UserPreferences) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if(userPrefs.paidAt != null) {
                    val paidAt = userPrefs.paidAt
                    val expiredAt = userPrefs.expiryDate
                    val permanent = userPrefs.permanent

                    if(!permanent) {
                        if(expiredAt!!.isAfter(LocalDateTime.now())) {
                            dbRepository.updateUserPreferences(
                                userPrefs.copy(
                                    paid = true
                                )
                            )
                        } else if(expiredAt.isBefore(LocalDateTime.now())) {
                            dbRepository.updateUserPreferences(
                                userPrefs.copy(
                                    paid = false
                                )
                            )
                        }
                    } else {
                        dbRepository.updateUserPreferences(
                            userPrefs.copy(
                                paid = true,
                                permanent = true
                            )
                        )
                    }

                } else {
                    dbRepository.updateUserPreferences(
                        userPrefs.copy(
                            paid = false
                        )
                    )
                }
            }
        }
    }



    fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    dbRepository.getUsers().collect(){userDetails->
                        _uiState.update {
                            it.copy(
                                userDetails = if(userDetails.isNotEmpty()) userDetails[0] else UserDetails()
                            )
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
    }

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUserPreferences().collect() { preferences ->
                    _uiState.update {
                        it.copy(
                            preferences = preferences
                        )
                    }
                }
            }
        }
    }

    fun switchDarkTheme() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.updateUserPreferences(
                    _uiState.value.preferences!!.copy(
                        darkMode = !uiState.value.preferences!!.darkMode
                    )
                )
            }
        }
    }

    init {
        getUserDetails()
        setUserPreferences()
    }
}