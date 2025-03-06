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
                            expiryDate = null
                        )
                    )
                    val userDetailsList = dbRepository.getUsers().firstOrNull()
                    val userDetails: UserDetails? = userDetailsList?.firstOrNull()

                    // Check if the userPreferences table exists
                    val userPreferences = dbRepository.getUserPreferences().first()

                    dbRepository.updateUserPreferences(
                        userPreferences!!.copy(
                            loggedIn = userPreferences.loggedIn,
                            darkMode = userDetails?.darkThemeSet ?: false,
                            restoredData = true,
                            lastRestore = null,
                            paidAt = userDetails?.paidAt?.let { LocalDateTime.parse(it) },
                            expiryDate = userDetails?.expiredAt?.let { LocalDateTime.parse(it) },
                            paid = userDetails?.paymentStatus ?: false,
                            permanent = userDetails?.permanent ?: false
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



    fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUsers().collect(){userDetails->
                    _uiState.update {
                        it.copy(
                            userDetails = if(userDetails.isNotEmpty()) userDetails[0] else UserDetails()
                        )
                    }
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