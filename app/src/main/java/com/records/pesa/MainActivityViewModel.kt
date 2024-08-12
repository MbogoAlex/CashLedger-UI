package com.records.pesa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainActivityUiState(
    val userDetails: UserDetails = UserDetails(),
)

class MainActivityViewModel(
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(value = MainActivityUiState())
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

    fun getUserDetails() {
        viewModelScope.launch {
            dbRepository.getUsers().collect(){userDetails->
                _uiState.update {
                    it.copy(
                        userDetails = userDetails[0]
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
    }
}