package com.records.pesa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.workers.WorkersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class MainActivityUiState(
    val userDetails: UserDetails? = null,
    val navigate: Boolean = false,
    val preferences: UserPreferences? = null,
    val launchStatus: LoadingStatus = LoadingStatus.INITIAL
)

class MainActivityViewModel(
    private val dbRepository: DBRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val workersRepository: WorkersRepository
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
                dataStoreRepository.getUserPreferences().collect() { preferences ->
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
                    val paid = preferences.expiryDate?.isAfter(LocalDateTime.now()) == true

                    dataStoreRepository.saveUserPreferences(
                        preferences.copy(
                            darkMode = preferences.darkMode && paid,
                            paid = paid,
                        )
                    )

                    dbRepository.updateUserPreferences(
                        preferences.copy(
                            darkMode = preferences.darkMode && paid,
                            paid = paid,
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
                dataStoreRepository.saveUserPreferences(
                    _uiState.value.preferences!!.copy(
                        darkMode = !uiState.value.preferences!!.darkMode
                    )
                )
                dbRepository.updateUserPreferences(
                    _uiState.value.preferences!!.copy(
                        darkMode = !uiState.value.preferences!!.darkMode
                    )
                )
            }
        }
    }
    
    /**
     * Check if Safaricom migration is needed and trigger it
     * 
     * Migration is triggered once on first launch after update to version 153
     * to fix transactions misclassified due to masked phone numbers
     */
    private fun checkAndRunSafaricomMigration() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val preferences = dataStoreRepository.getUserPreferences().firstOrNull()
                
                // Only run if not already completed
                if (preferences?.safaricomMigrationCompleted == false) {
                    // Trigger migration worker
                    workersRepository.runSafaricomMigration()
                    
                    // Mark as completed so it doesn't run again
                    dataStoreRepository.saveUserPreferences(
                        preferences.copy(safaricomMigrationCompleted = true)
                    )
                    
                    // Update database as well
                    dbRepository.updateUserPreferences(
                        preferences.copy(safaricomMigrationCompleted = true)
                    )
                }
            } catch (e: Exception) {
                // Migration is optional, don't fail app launch if it errors
                android.util.Log.e("MainActivityVM", "Safaricom migration check failed: ${e.message}")
            }
        }
    }

    init {
        getUserDetails()
        getUserPreferences()
        checkSubscriptionStatus()
        checkAndRunSafaricomMigration()
    }
}