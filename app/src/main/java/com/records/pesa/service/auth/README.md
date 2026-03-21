# Authentication Management System

This package provides a centralized, reusable authentication management system for handling token refresh and automatic re-login throughout the MLedger application.

## Overview

The authentication system automatically handles:
- ✅ **Token Refresh**: When access tokens expire (401 response)
- ✅ **Automatic Re-login**: When refresh tokens expire (401 on refresh)
- ✅ **Session Management**: Updates UserSession table automatically
- ✅ **Error Handling**: Comprehensive error handling and logging
- ✅ **Reusable**: Can be used throughout the app for any authenticated API call

## Architecture

### Core Components

1. **`AuthenticationManager`** - Main service for authentication handling
2. **`AuthenticatedApiExecutor`** - Extension functions for easy usage
3. **Integration with `AppContainer`** - Available throughout the app via DI

### Flow Diagram

```
API Call (401)
    ↓
AuthenticationManager.handle401Response()
    ↓
Try Token Refresh
    ├─ Success → Return new token
    ├─ 401 → Try Re-login
    │   ├─ Success → Return new token
    │   └─ Failed → Return error
    └─ Failed → Return error
```

## Usage Examples

### 1. Simple API Call (Recommended)

```kotlin
class MyViewModel(
    private val authenticationManager: AuthenticationManager,
    private val apiRepository: ApiRepository
) : ViewModel() {

    fun loadUserData() {
        viewModelScope.launch {
            val result = authenticationManager.executeAuthenticated { token ->
                apiRepository.getMe(token)
            }

            when (result) {
                is AuthenticatedResult.Success -> {
                    val userData = result.response.body()
                    // Update UI with user data
                }
                is AuthenticatedResult.Failed -> {
                    // Show error message to user
                    Log.e("MyViewModel", "API failed: ${result.error}")
                }
                is AuthenticatedResult.AuthenticationError -> {
                    // Redirect to login screen
                    // This means both token refresh and re-login failed
                }
            }
        }
    }
}
```

### 2. Data-Only API Call (Simpler)

```kotlin
fun loadPayments() {
    viewModelScope.launch {
        val payments = authenticationManager.executeAuthenticatedForData { token ->
            apiRepository.getUserPayments(userId.toString())
        }

        if (payments != null) {
            // Update UI with payments
            _uiState.update { it.copy(payments = payments.data) }
        } else {
            // Handle error (already logged by AuthenticationManager)
            _uiState.update { it.copy(error = "Failed to load payments") }
        }
    }
}
```

### 3. Background Worker Usage (Already Implemented)

```kotlin
class SmsSubmissionWorker : CoroutineWorker() {

    override suspend fun doWork(): Result {
        val authManager = appContext.container.authenticationManager

        // Submit messages with automatic auth handling
        val response = authManager.executeWithAuth { token ->
            apiRepository.submitMessages(token, messages)
        }

        return if (response?.isSuccessful == true) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
```

## Authentication Flow Details

### 1. Token Refresh Process

When an API call returns 401:

1. **Extract Refresh Token** from UserSession table
2. **Call `/auth/refresh-token`** with refresh token
3. **If Successful (200)**:
   - Update UserSession with new tokens
   - Retry original API call with new access token
4. **If Failed (401)**:
   - Refresh token is expired
   - Proceed to re-login process

### 2. Re-login Process

When token refresh fails with 401:

1. **Extract Credentials** from UserDetails table (phoneNumber, password)
2. **Call `/auth/login`** with stored credentials
3. **If Successful (200)**:
   - Update UserSession with new tokens
   - Retry original API call with new access token
4. **If Failed**:
   - Return authentication error
   - App should redirect user to login screen

### 3. Session Management

The system automatically updates the `UserSession` table with:
- `userId` - User ID from auth response
- `accessToken` - New access token
- `refreshToken` - New refresh token
- `tokenType` - Token type (usually "Bearer")
- `accessTokenExpiresIn` - Access token expiry time
- `refreshTokenExpiresIn` - Refresh token expiry time

## Integration with Existing Code

### Adding to New ViewModels

1. **Inject AuthenticationManager** via AppContainer:
```kotlin
class MyViewModel(
    private val authenticationManager: AuthenticationManager,
    // ... other dependencies
) : ViewModel()
```

2. **Use in ViewModel Factory** (if using manual DI):
```kotlin
class MyViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MyViewModel(
            authenticationManager = appContainer.authenticationManager,
            // ... other dependencies
        ) as T
    }
}
```

### Adding to Existing API Calls

**Before (Manual Token Handling):**
```kotlin
fun loadUserProfile() {
    viewModelScope.launch {
        try {
            val session = dbRepository.getUserSession().first()
            val response = apiRepository.getMe("Bearer ${session?.accessToken}")

            if (response.isSuccessful) {
                // Handle success
            } else if (response.code() == 401) {
                // Manual token refresh logic here...
            }
        } catch (e: Exception) {
            // Handle exception
        }
    }
}
```

**After (Automatic Handling):**
```kotlin
fun loadUserProfile() {
    viewModelScope.launch {
        val userData = authenticationManager.executeAuthenticatedForData { token ->
            apiRepository.getMe(token)
        }

        if (userData != null) {
            // Handle success
        }
        // Errors are automatically handled and logged
    }
}
```

## Error Handling

### Authentication Errors

When `AuthenticatedResult.AuthenticationError` is returned:
- Both token refresh and re-login have failed
- User credentials may be invalid
- Network issues preventing authentication
- **Action**: Redirect user to login screen

### API Errors

When `AuthenticatedResult.Failed` is returned:
- Authentication succeeded, but API call failed for other reasons
- Could be network issues, server errors, validation errors
- **Action**: Show appropriate error message to user

### Logging

The system provides comprehensive logging:
- `AuthenticationManager`: Token refresh and re-login attempts
- `AuthenticatedApiExecutor`: API call results
- **Tag Pattern**: Use descriptive tags for easy filtering in Logcat

## Configuration

### Backend Requirements

Ensure your backend APIs return:
- **401 status** when tokens are expired
- **AuthResponseBody** format for both login and refresh-token endpoints
- **Consistent token format** for Bearer authentication

### Database Requirements

The system requires these database tables:
- **UserSession**: For storing authentication tokens
- **UserDetails**: For storing user credentials (phoneNumber, password)

## Security Considerations

### Password Storage
- Passwords are stored in local database for automatic re-login
- Consider encrypting passwords in UserDetails table for additional security
- Passwords are only used for automatic re-authentication

### Token Security
- Access tokens are stored temporarily in UserSession
- Refresh tokens are used only for token renewal
- All tokens are cleared when user logs out

### Network Security
- All API calls use HTTPS in production
- Tokens are sent via Authorization header
- No tokens are logged in production builds

## Testing

### Unit Tests
Test authentication flows:
```kotlin
@Test
fun `when access token expired, should refresh token and retry`() {
    // Test token refresh flow
}

@Test
fun `when refresh token expired, should re-login and retry`() {
    // Test re-login flow
}
```

### Integration Tests
Test with actual API endpoints:
- Verify 401 handling
- Verify token refresh
- Verify re-login process

## Migration Guide

### From Manual Token Handling

1. **Replace manual token management** with `AuthenticationManager`
2. **Remove custom 401 handling** logic
3. **Update API calls** to use `executeAuthenticated()`
4. **Remove manual session updates**
5. **Simplify error handling**

### From Old Authentication System

1. **Add AuthenticationManager** to AppContainer
2. **Update dependency injection**
3. **Migrate API calls** one by one
4. **Test thoroughly** with expired tokens
5. **Remove old authentication code**

## Troubleshooting

### Common Issues

1. **"No refresh token available"**
   - Check UserSession table has valid refresh token
   - Verify login flow properly stores tokens

2. **"No stored credentials"**
   - Check UserDetails table has phoneNumber and password
   - Verify login flow properly stores credentials

3. **"Re-login failed"**
   - Verify stored credentials are correct
   - Check network connectivity
   - Verify backend login endpoint is working

### Debugging

Enable detailed logging:
```kotlin
Log.d("AuthenticationManager", "Token refresh attempt")
Log.d("AuthenticatedAPI", "API call with automatic auth")
```

Monitor database state:
- Check UserSession table for token values
- Check UserDetails table for stored credentials
- Verify token expiry times

---

## Summary

This authentication system provides:
- **🔒 Automatic token management** - No manual token handling needed
- **🔄 Seamless user experience** - Transparent authentication renewal
- **🛡️ Robust error handling** - Comprehensive error scenarios covered
- **🔧 Easy integration** - Simple API for existing and new code
- **📝 Comprehensive logging** - Detailed logs for debugging
- **🚀 Production ready** - Used in SMS submission and ready for all APIs

Use `authenticationManager.executeAuthenticated()` for all your authenticated API calls and never worry about token management again! 🎉