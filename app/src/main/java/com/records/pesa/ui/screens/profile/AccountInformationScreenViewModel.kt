package com.records.pesa.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.UserSession
import com.records.pesa.db.models.userPreferences
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.user.update.UserProfileUpdatePayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.auth.AuthenticationManager
import com.records.pesa.service.auth.AuthenticatedResult
import com.records.pesa.service.auth.executeAuthenticated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AccountInformationScreenUiState(
    val userDetails: UserDetails? = null,
    val userSession: UserSession? = null,
    val preferences: UserPreferences = userPreferences,
    val appLaunchStatus: AppLaunchStatus = AppLaunchStatus(),
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val successMessage: String = "",
    val saveButtonEnabled: Boolean = false,
    val clearLoginDetails: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class AccountInformationScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val authenticationManager: AuthenticationManager
): ViewModel() {
    private val _uiState = MutableStateFlow(AccountInformationScreenUiState())
    val uiState: StateFlow<AccountInformationScreenUiState> = _uiState.asStateFlow()

    fun updateFirstName(firstName: String) {
        _uiState.update {
            it.copy(
                firstName = firstName,
                saveButtonEnabled = isValidForUpdate(firstName, uiState.value.lastName, uiState.value.email)
            )
        }
    }

    fun updateLastName(lastName: String) {
        _uiState.update {
            it.copy(
                lastName = lastName,
                saveButtonEnabled = isValidForUpdate(uiState.value.firstName, lastName, uiState.value.email)
            )
        }
    }

    fun updateClearLoginDetails() {
        _uiState.update {
            it.copy(
                clearLoginDetails = !uiState.value.clearLoginDetails
            )
        }
    }

    fun updateEmail(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                saveButtonEnabled = isValidForUpdate(uiState.value.firstName, uiState.value.lastName, email)
            )
        }
    }

    fun updateUserDetails() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING,
                successMessage = ""
            )
        }

        viewModelScope.launch {
            try {
                // Validate user details exist
                val currentUserDetails = uiState.value.userDetails
                if (currentUserDetails == null) {
                    _uiState.update {
                        it.copy(
                            successMessage = "User details not found. Please try logging in again.",
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    return@launch
                }

                // Create update payload with only non-empty values
                val userProfileUpdatePayload = UserProfileUpdatePayload(
                    fname = uiState.value.firstName.takeIf { it.isNotBlank() },
                    lname = uiState.value.lastName.takeIf { it.isNotBlank() },
                    email = uiState.value.email.takeIf { it.isNotBlank() }
                )

                // Check if there are any changes to update
                if (userProfileUpdatePayload.fname == null &&
                    userProfileUpdatePayload.lname == null &&
                    userProfileUpdatePayload.email == null) {
                    _uiState.update {
                        it.copy(
                            successMessage = "No changes to update",
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                    return@launch
                }

                Log.d("AccountInfoVM", "Updating user profile: $userProfileUpdatePayload")

                // Execute API call with automatic authentication handling
                val result = authenticationManager.executeAuthenticated { token ->
                    apiRepository.updateUserProfile(
                        token = token,
                        userProfileUpdatePayload = userProfileUpdatePayload
                    )
                }

                when (result) {
                    is AuthenticatedResult.Success -> {
                        val response = result.response
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            Log.d("AccountInfoVM", "Profile update successful: ${responseBody}")

                            // Update local database with new values
                            val updatedUserDetails = currentUserDetails.copy(
                                firstName = userProfileUpdatePayload.fname ?: currentUserDetails.firstName,
                                lastName = userProfileUpdatePayload.lname ?: currentUserDetails.lastName,
                                email = userProfileUpdatePayload.email ?: currentUserDetails.email
                            )

                            try {
                                dbRepository.updateUser(updatedUserDetails)
                                Log.d("AccountInfoVM", "Local database updated successfully")

                                _uiState.update {
                                    it.copy(
                                        successMessage = "Profile updated successfully!",
                                        loadingStatus = LoadingStatus.SUCCESS,
                                        userDetails = updatedUserDetails
                                    )
                                }
                            } catch (dbException: Exception) {
                                Log.e("AccountInfoVM", "Failed to update local database: ${dbException.message}")
                                _uiState.update {
                                    it.copy(
                                        successMessage = "Profile updated on server but failed to save locally. Changes may be lost.",
                                        loadingStatus = LoadingStatus.SUCCESS
                                    )
                                }
                            }
                        } else {
                            Log.e("AccountInfoVM", "API call failed with code: ${response.code()}")
                            val errorMessage = when (response.code()) {
                                400 -> "Invalid profile data. Please check your input."
                                403 -> "You don't have permission to update this profile."
                                404 -> "User profile not found."
                                422 -> "Invalid email format or other validation error."
                                else -> "Failed to update profile. Server returned error ${response.code()}."
                            }

                            _uiState.update {
                                it.copy(
                                    successMessage = errorMessage,
                                    loadingStatus = LoadingStatus.FAIL
                                )
                            }
                        }
                    }

                    is AuthenticatedResult.Failed -> {
                        Log.e("AccountInfoVM", "API call failed: ${result.error}")
                        _uiState.update {
                            it.copy(
                                successMessage = "Failed to update profile: ${result.error}",
                                loadingStatus = LoadingStatus.FAIL
                            )
                        }
                    }

                    is AuthenticatedResult.AuthenticationError -> {
                        Log.e("AccountInfoVM", "Authentication failed: ${result.error}")
                        _uiState.update {
                            it.copy(
                                successMessage = "Authentication failed. Please login again.",
                                loadingStatus = LoadingStatus.FAIL
                            )
                        }
                        // Could trigger logout or redirect to login here
                    }
                }

            } catch (e: Exception) {
                Log.e("AccountInfoVM", "Unexpected error updating user details", e)
                _uiState.update {
                    it.copy(
                        successMessage = "An unexpected error occurred: ${e.message}",
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
            }
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL
            )
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            dbRepository.deleteSessions()
            if(uiState.value.clearLoginDetails) {
                dbRepository.updateUser(
                    uiState.value.userDetails!!.copy(
                        phoneNumber = "",
                        password = ""
                    )
                )

                dbRepository.updateUserPreferences(
                    uiState.value.preferences.copy(
                        loggedIn = false
                    )
                )
            } else {
                dbRepository.updateUserPreferences(
                    uiState.value.preferences.copy(
                        loggedIn = false
                    )
                )
            }
        }
    }

    private fun getUserDetails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUser()?.collect { userDetails ->
                    _uiState.update {
                        it.copy(
                            userDetails = userDetails,
                        )
                    }

                    // Initialize form fields with current user data
                    if (userDetails != null) {
                        initializeFormFields(userDetails)
                    }
                }
            }
        }
    }

    private fun getUserSession() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dbRepository.getSession()?.collect { session ->
                        _uiState.update {
                            it.copy(
                                userSession = session,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AccountInfoVM", "UserSession table not available: ${e.message}")
                Log.d("AccountInfoVM", "Profile updates will still work using AuthenticationManager")
                // Continue without session - AuthenticationManager will handle token management
            }
        }
    }

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbRepository.getUserPreferences()?.collect() { preferences ->
                    _uiState.update {
                        it.copy(
                            preferences = preferences!!
                        )
                    }
                }
            }
        }
    }

    /**
     * Check if there are valid changes to update
     */
    private fun isValidForUpdate(firstName: String, lastName: String, email: String): Boolean {
        return firstName.isNotBlank() || lastName.isNotBlank() ||
               (email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
    }

    /**
     * Initialize UI state with current user data when user details are loaded
     */
    private fun initializeFormFields(userDetails: UserDetails) {
        _uiState.update {
            it.copy(
                firstName = userDetails.firstName ?: "",
                lastName = userDetails.lastName ?: "",
                email = userDetails.email ?: "",
                saveButtonEnabled = false // Start with save disabled
            )
        }
    }

    init {
        getUserDetails()
        getUserSession()
        getUserPreferences()
    }

}