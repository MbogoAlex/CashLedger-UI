package com.records.pesa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.models.dbModel.UserDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class MainActivityUiState(
    val userDetails: UserDetails = UserDetails(),
)

class MainActivityViewModel(
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(value = MainActivityUiState())
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

    private fun setUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                var userDetailsList = dbRepository.getUsers().first()
                var userDetails: UserDetails? = null
                if(userDetailsList.isNotEmpty()) {
                    userDetails = userDetailsList[0]

                    val userPreferences = dbRepository.getUserPreferences().first()

                    if(userPreferences == null) {
                        val newUserPreferences = UserPreferences(
                            loggedIn = userDetails.id != 0,
                            darkMode = userDetails.darkThemeSet,
                            paidAt = if(userDetails.paidAt != null) LocalDateTime.parse(userDetails.paidAt) else null,
                            expiryDate = if(userDetails.expiredAt != null) LocalDateTime.parse(userDetails.expiredAt) else null,
                            paid = userDetails.paymentStatus,
                            permanent = userDetails.permanent
                        )
                        dbRepository.insertUserPreferences(newUserPreferences)
                    }
                } else {
                    val userPreferences = dbRepository.getUserPreferences().first()
                    if(userPreferences == null) {
                        val newUserPreferences = UserPreferences(
                            loggedIn = false,
                            darkMode = false,
                            paidAt = null,
                            expiryDate = null,
                            paid = false,
                            permanent = false
                        )

                        dbRepository.insertUserPreferences(newUserPreferences)
                    }
                }

            }
        }
    }

    fun getUserDetails() {
        viewModelScope.launch {
            dbRepository.getUsers().collect(){userDetails->
                _uiState.update {
                    it.copy(
                        userDetails = if(userDetails.isNotEmpty()) userDetails[0] else UserDetails()
                    )
                }
            }
        }
    }

    fun switchDarkTheme() {
        viewModelScope.launch {
            dbRepository.updateUser(
                _uiState.value.userDetails.copy(
                    darkThemeSet = !uiState.value.userDetails.darkThemeSet
                )
            )
        }
    }

    init {
        getUserDetails()
        setUserPreferences()
    }
}