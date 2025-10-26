package com.records.pesa.service.auth

import android.util.Log
import retrofit2.Response

/**
 * Extension functions and utilities for executing authenticated API calls
 * Use these throughout your app for consistent authentication handling
 */

/**
 * Execute an authenticated API call with automatic token refresh and re-login
 *
 * Usage example in any ViewModel or Service:
 * ```
 * val result = authenticationManager.executeAuthenticated { token ->
 *     apiRepository.getUserProfile(token)
 * }
 *
 * when (result) {
 *     is AuthenticatedResult.Success -> {
 *         // Handle successful response
 *         val data = result.response.body()
 *     }
 *     is AuthenticatedResult.Failed -> {
 *         // Handle failure
 *         Log.e("API", "Failed: ${result.error}")
 *     }
 *     is AuthenticatedResult.AuthenticationError -> {
 *         // Handle authentication issues (redirect to login)
 *         Log.e("API", "Auth error: ${result.error}")
 *     }
 * }
 * ```
 */
suspend fun <T> AuthenticationManager.executeAuthenticated(
    apiCall: suspend (token: String) -> Response<T>
): AuthenticatedResult<T> {
    return try {
        val response = executeWithAuth(apiCall)

        when {
            response == null -> {
                Log.e("AuthenticatedAPI", "Authentication failed - no response received")
                AuthenticatedResult.AuthenticationError("Authentication failed")
            }
            response.isSuccessful -> {
                Log.d("AuthenticatedAPI", "API call successful")
                AuthenticatedResult.Success(response)
            }
            else -> {
                Log.e("AuthenticatedAPI", "API call failed with code: ${response.code()}")
                AuthenticatedResult.Failed("API call failed: ${response.code()}", response)
            }
        }
    } catch (e: Exception) {
        Log.e("AuthenticatedAPI", "Exception during authenticated API call", e)
        AuthenticatedResult.Failed("Exception: ${e.message}", null)
    }
}

/**
 * Sealed class for authenticated API call results
 */
sealed class AuthenticatedResult<T> {
    data class Success<T>(val response: Response<T>) : AuthenticatedResult<T>()
    data class Failed<T>(val error: String, val response: Response<T>?) : AuthenticatedResult<T>()
    data class AuthenticationError<T>(val error: String) : AuthenticatedResult<T>()
}

/**
 * Convenience method for API calls that don't need the full response
 * Returns just the response body or null
 */
suspend fun <T> AuthenticationManager.executeAuthenticatedForData(
    apiCall: suspend (token: String) -> Response<T>
): T? {
    return when (val result = executeAuthenticated(apiCall)) {
        is AuthenticatedResult.Success -> result.response.body()
        is AuthenticatedResult.Failed -> {
            Log.e("AuthenticatedAPI", "API call failed: ${result.error}")
            null
        }
        is AuthenticatedResult.AuthenticationError -> {
            Log.e("AuthenticatedAPI", "Authentication error: ${result.error}")
            null
        }
    }
}

/**
 * Example usage in a ViewModel:
 *
 * class SomeViewModel(
 *     private val authenticationManager: AuthenticationManager,
 *     private val apiRepository: ApiRepository
 * ) : ViewModel() {
 *
 *     fun loadUserProfile() {
 *         viewModelScope.launch {
 *             val result = authenticationManager.executeAuthenticated { token ->
 *                 apiRepository.getMe(token)
 *             }
 *
 *             when (result) {
 *                 is AuthenticatedResult.Success -> {
 *                     val userProfile = result.response.body()
 *                     // Update UI state with user profile
 *                 }
 *                 is AuthenticatedResult.Failed -> {
 *                     // Show error message to user
 *                 }
 *                 is AuthenticatedResult.AuthenticationError -> {
 *                     // Redirect to login screen
 *                 }
 *             }
 *         }
 *     }
 *
 *     // Or for simpler cases where you just need the data:
 *     fun loadUserPayments() {
 *         viewModelScope.launch {
 *             val payments = authenticationManager.executeAuthenticatedForData { token ->
 *                 apiRepository.getUserPayments("123")
 *             }
 *
 *             if (payments != null) {
 *                 // Update UI with payments
 *             } else {
 *                 // Handle error (already logged)
 *             }
 *         }
 *     }
 * }
 */