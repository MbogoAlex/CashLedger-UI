package com.records.pesa.service.auth

import android.util.Log
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserAccount
import com.records.pesa.db.models.UserSession
import com.records.pesa.models.user.TokenRefreshPayload
import com.records.pesa.models.user.login.AuthResponseBody
import com.records.pesa.models.user.login.UserLoginPayload
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.userAccount.UserAccountService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Centralized authentication manager for handling token refresh and re-login
 * This class provides a clean, reusable solution for automatic token management
 */
class AuthenticationManager(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
) {
    companion object {
        private const val TAG = "AuthenticationManager"
    }

    /**
     * Sealed class to represent authentication results
     */
    sealed class AuthResult {
        data class Success(val token: String) : AuthResult()
        data class Failed(val error: String) : AuthResult()
        object RequiresReLogin : AuthResult()
    }

    /**
     * Main method to ensure valid authentication
     * This handles the complete authentication flow:
     * 1. Check current token
     * 2. Refresh if expired (401)
     * 3. Re-login if refresh token expired (401)
     */
    suspend fun ensureValidAuthentication(): AuthResult {
        return try {
            val userSession = dbRepository.getSession()?.firstOrNull()

            if (userSession?.accessToken.isNullOrEmpty()) {
                Log.w(TAG, "No access token found")
                return AuthResult.RequiresReLogin
            }

            // Return current token (will be validated by API call)
            AuthResult.Success(userSession!!.accessToken!!)

        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring authentication: ${e.message}")

            // Check if this is a table-not-found error (migration not completed)
            if (e.message?.contains("no such table", ignoreCase = true) == true ||
                e.message?.contains("userSession", ignoreCase = true) == true) {
                Log.w(TAG, "UserSession table not available - migration may not have completed")
                AuthResult.Failed("UserSession table not available")
            } else {
                AuthResult.Failed("Authentication error: ${e.message}")
            }
        }
    }

    /**
     * Handle 401 response by attempting token refresh, then re-login if needed
     */
    suspend fun handle401Response(): AuthResult {
        Log.d(TAG, "Handling 401 response - attempting token refresh")

        return try {
            val refreshResult = attemptTokenRefresh()

            when (refreshResult) {
                is AuthResult.Success -> {
                    Log.d(TAG, "Token refresh successful")
                    refreshResult
                }
                is AuthResult.RequiresReLogin -> {
                    Log.d(TAG, "Token refresh failed - attempting re-login")
                    attemptReLogin()
                }
                is AuthResult.Failed -> {
                    Log.e(TAG, "Token refresh failed: ${refreshResult.error}")
                    refreshResult
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling 401 response", e)
            AuthResult.Failed("Authentication handling error: ${e.message}")
        }
    }

    /**
     * Attempt to refresh the access token using the refresh token
     */
    private suspend fun attemptTokenRefresh(): AuthResult {
        return try {
            val userSession = try {
                dbRepository.getSession()?.firstOrNull()
            } catch (e: Exception) {
                Log.w(TAG, "Cannot access UserSession table for refresh: ${e.message}")
                return AuthResult.Failed("UserSession table not available")
            }

            if (userSession?.refreshToken.isNullOrEmpty()) {
                Log.w(TAG, "No refresh token available")
                return AuthResult.RequiresReLogin
            }

            val refreshPayload = TokenRefreshPayload(
                refreshToken = userSession!!.refreshToken!!
            )

            val response = apiRepository.refreshToken(refreshPayload)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse?.data?.user != null) {
                    // Update session with new tokens
                    updateUserSession(authResponse, userSession)
                    Log.d(TAG, "Token refresh successful")
                    return AuthResult.Success(authResponse.data.user.accessToken ?: "")
                } else {
                    Log.e(TAG, "Token refresh response body is null")
                    return AuthResult.Failed("Invalid refresh response")
                }
            } else if (response.code() == 401) {
                // Refresh token is expired - need to re-login
                Log.w(TAG, "Refresh token expired (401)")
                return AuthResult.RequiresReLogin
            } else {
                Log.e(TAG, "Token refresh failed with code: ${response.code()}")
                return AuthResult.Failed("Token refresh failed: ${response.code()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh", e)
            AuthResult.Failed("Token refresh exception: ${e.message}")
        }
    }

    /**
     * Attempt to re-login using stored credentials
     */
    private suspend fun attemptReLogin(): AuthResult {
        return try {
            val userDetails = dbRepository.getUsers().first()

            if (userDetails.isEmpty()) {
                Log.w(TAG, "No user details found for re-login")
                return AuthResult.Failed("No stored credentials")
            }

            val user = userDetails[0]
            if (user.phoneNumber.isBlank() || user.password.isBlank()) {
                Log.w(TAG, "Incomplete credentials for re-login")
                return AuthResult.Failed("Incomplete stored credentials")
            }

            val loginPayload = UserLoginPayload(
                phoneNumber = user.phoneNumber,
                password = user.password
            )

            val response = apiRepository.login(loginPayload)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse?.data?.user != null) {
                    // Update session with new tokens from login
                    val existingSession = try {
                        dbRepository.getSession()?.firstOrNull()
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot access UserSession table for update: ${e.message}")
                        null // Will create new session
                    }
                    updateUserSession(authResponse, existingSession)

                    Log.d(TAG, "Re-login successful")
                    return AuthResult.Success(authResponse.data.user.accessToken ?: "")
                } else {
                    Log.e(TAG, "Re-login response body is null")
                    return AuthResult.Failed("Invalid login response")
                }
            } else {
                Log.e(TAG, "Re-login failed with code: ${response.code()}")
                return AuthResult.Failed("Re-login failed: ${response.code()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during re-login", e)
            AuthResult.Failed("Re-login exception: ${e.message}")
        }
    }

    /**
     * Update the user session in the database with new token information
     */
    private suspend fun updateUserSession(authResponse: AuthResponseBody, existingSession: UserSession?) {
        try {
            val userData = authResponse.data?.user
            if (userData != null) {
                val updatedSession = if (existingSession != null) {
                    existingSession.copy(
                        userId = userData.userId,
                        accessToken = userData.accessToken,
                        refreshToken = userData.refreshToken,
                        tokenType = userData.tokenType,
                        accessTokenExpiresIn = userData.accessTokenExpiresIn,
                        refreshTokenExpiresIn = userData.refreshTokenExpiresIn
                    )
                } else {
                    UserSession(
                        userId = userData.userId,
                        accessToken = userData.accessToken,
                        refreshToken = userData.refreshToken,
                        tokenType = userData.tokenType,
                        accessTokenExpiresIn = userData.accessTokenExpiresIn,
                        refreshTokenExpiresIn = userData.refreshTokenExpiresIn
                    )
                }

                try {
                    if (existingSession != null) {
                        dbRepository.updateSession(updatedSession)
                    } else {
                        dbRepository.insertSession(updatedSession)
                    }
                    Log.d(TAG, "User session updated successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update UserSession table: ${e.message}")
                    // Don't fail the authentication flow, just log the error
                    // The token is still valid for this session
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user session", e)
        }
    }

    /**
     * Helper method to execute API calls with automatic authentication handling
     * This method can be used throughout the app for any authenticated API call
     */
    suspend fun <T> executeWithAuth(
        apiCall: suspend (token: String) -> Response<T>
    ): Response<T>? {
        // First, ensure we have valid authentication
        val authResult = ensureValidAuthentication()

        when (authResult) {
            is AuthResult.Success -> {
                // Try the API call with current token
                val response = apiCall(authResult.token)

                // If we get 401, handle it and retry
                if (response.code() == 401) {
                    Log.d(TAG, "Got 401, handling authentication...")

                    val retryAuthResult = handle401Response()
                    when (retryAuthResult) {
                        is AuthResult.Success -> {
                            // Retry the API call with new token
                            Log.d(TAG, "Retrying API call with new token")
                            return apiCall(retryAuthResult.token)
                        }
                        is AuthResult.Failed -> {
                            Log.e(TAG, "Authentication failed: ${retryAuthResult.error}")
                            return null
                        }
                        is AuthResult.RequiresReLogin -> {
                            Log.w(TAG, "Re-login required - cannot proceed with API call")
                            return null
                        }
                    }
                } else {
                    // API call succeeded or failed for non-auth reasons
                    return response
                }
            }
            is AuthResult.Failed -> {
                Log.e(TAG, "Authentication failed: ${authResult.error}")
                return null
            }
            is AuthResult.RequiresReLogin -> {
                Log.w(TAG, "Re-login required - cannot proceed with API call")
                return null
            }
        }
    }
}