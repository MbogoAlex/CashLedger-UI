package com.records.pesa.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeScreenUiState(
    val darkMode: Boolean = false,
    val userDetails: UserDetails = UserDetails(),
    val user: UserDetails = UserDetails()
)

class HomeScreenViewModel(
    private val dbRepository: DBRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(value = HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    fun getUserDetails() {
        viewModelScope.launch {
            dbRepository.getUsers().collect() { userDetails ->
                _uiState.update {
                    it.copy(
                        userDetails = if(userDetails.isNotEmpty()) userDetails[0] else UserDetails()
                    )
                }
            }
        }

        viewModelScope.launch {
            val user = dbRepository.getUsers().first()[0]
            _uiState.update {
                it.copy(
                    user = user
                )
            }
        }
    }

    init {
        getUserDetails()
    }
}