package com.records.pesa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainActivityUiState(
    val userDetails: UserDetails? = null,
    val navigate: Boolean = false,
    val preferences: UserPreferences? = null,
    val launchStatus: LoadingStatus = LoadingStatus.INITIAL
)

class MainActivityViewModel(
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(value = MainActivityUiState())
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()



    fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUser()?.collect(){userDetails->
                    _uiState.update {
                        it.copy(
                            userDetails = userDetails
                        )
                    }
                }
            }
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

    fun checkSubscriptionStatus() {
        _uiState.update {
            it.copy(
                launchStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val preferences = dbRepository.getUserPreferences()?.firstOrNull()

                if(preferences != null) {
                    dbRepository.updateUserPreferences(
                        preferences.copy(
                            darkMode = preferences.darkMode && preferences.paid,
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        launchStatus = LoadingStatus.SUCCESS,
                        navigate = true
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        launchStatus = LoadingStatus.SUCCESS
                    )
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
        getUserPreferences()
        checkSubscriptionStatus()
    }
}