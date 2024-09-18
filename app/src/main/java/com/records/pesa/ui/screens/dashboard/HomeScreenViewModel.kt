package com.records.pesa.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeScreenUiState(
    val darkMode: Boolean = false,
    val userDetails: UserDetails = UserDetails(),
    val freeTrialDays: Int = 0,
    val screen: String? = null,
    val user: UserDetails = UserDetails()
)

class HomeScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(value = HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    val screen: String? = savedStateHandle[HomeScreenDestination.route]

    fun getUserDetails() {
        viewModelScope.launch {
            dbRepository.getUsers().collect() { userDetails ->
                Log.d("DB_USER", userDetails.toString())
                _uiState.update {
                    it.copy(
                        userDetails = if(userDetails.isNotEmpty()) userDetails[0] else UserDetails()
                    )
                }
            }
        }

        viewModelScope.launch {
            val user = dbRepository.getUsers().first()[0]
            Log.d("DB_USER", user.toString())
            _uiState.update {
                it.copy(
                    user = user
                )
            }
            while (user.userId == 0) {
                delay(1000)
            }
            try {
                val response = apiRepository.getFreeTrialStatus(
                    token = user.token,
                    userId = user.userId
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            freeTrialDays = response.body()!!.data.days
                        )
                    }
                } else {
                    Log.e("geFreeTrialErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                Log.e("geFreeTrialError", e.toString())
            }
        }
    }

    fun resetNavigationScreen() {
        _uiState.update {
            it.copy(
                screen = null
            )
        }
    }

    init {
        getUserDetails()
        _uiState.update {
            it.copy(
                screen = screen
            )
        }
    }
}