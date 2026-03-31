package com.records.pesa.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.ui.theme.CashLedgerTheme

object LoginScreenDestination: AppNavigation {
    override val title: String = "Login screen"
    override val route: String = "login-screen"
    val phoneNumber: String = "phoneNumber"
    val password: String = "password"
    val routeWithArgs: String = "$route/{$phoneNumber}/{$password}"

}

@Composable
fun LoginScreenComposable(
    navigateToRegistrationScreen: () -> Unit,
    navigateToSmsFetchScreenWithArgs: (arg: String) -> Unit,
    navigateToSmsFetchScreen: () -> Unit,
    navigateToUpdatePasswordScreen: () -> Unit,
    navigateToBackupRestoreScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current

    BackHandler(onBack = navigateToRegistrationScreen)

    val viewModel: LoginScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()



    LaunchedEffect(Unit) {
        viewModel.buttonEnabled()
    }
    
    var showAlert by rememberSaveable {
        mutableStateOf(false)
    }

    var showLoginFailDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if(uiState.loginStatus == LoginStatus.FAIL) {
        showLoginFailDialog = true
    }

    if(showLoginFailDialog) {
        AlertDialog(
            title = {
                Text(text = "Failed")
            },
            text = {
                Text(text = uiState.loginMessage)
            },
            onDismissRequest = { showLoginFailDialog = !showLoginFailDialog },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginFailDialog = !showLoginFailDialog
                    }
                ) {
                    Text(text = "Dismiss")
                }
            }
        )
    }

    LaunchedEffect(uiState.loginStatus) {
        if(uiState.loginStatus == LoginStatus.SUCCESS) {
            Toast.makeText(context, uiState.loginMessage, Toast.LENGTH_SHORT).show()
            // Only navigate to restore screen on first-ever login (user has never restored before)
            val neverRestored = uiState.preferences?.restoredData == false
            if(neverRestored && uiState.transactions.isEmpty()) {
                navigateToBackupRestoreScreen()
            } else {
                navigateToSmsFetchScreen()
            }
            viewModel.resetLoginStatus()
        } else if(uiState.loginStatus == LoginStatus.FAIL) {
            Toast.makeText(context, uiState.loginMessage, Toast.LENGTH_SHORT).show()
//        showAlert = true
            viewModel.resetLoginStatus()
        }
    }

    if(showAlert) {
        AlertDialog(
            text = {
                Text(text = uiState.exception)
            },
            onDismissRequest = {showAlert = !showAlert },
            confirmButton = { showAlert = !showAlert }
        )
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
       LoginScreen(
           phoneNumber = uiState.phoneNumber,
           onChangePhoneNumber = {
               viewModel.updatePhoneNumber(it)
               viewModel.buttonEnabled()
           },
           password = uiState.password,
           onChangePassword = {
               viewModel.updatePassword(it)
               viewModel.buttonEnabled()
           },
           buttonEnabled = uiState.loginButtonEnabled,
           onLogin = { viewModel.loginUser() },
           navigateToRegistrationScreen = navigateToRegistrationScreen,
           navigateToUpdatePasswordScreen = navigateToUpdatePasswordScreen,
           loginStatus = uiState.loginStatus
       )
    }

}

@Composable
fun LoginScreen(
    phoneNumber: String,
    onChangePhoneNumber: (value: String) -> Unit,
    password: String,
    onChangePassword: (value: String) -> Unit,
    buttonEnabled: Boolean,
    onLogin: () -> Unit,
    navigateToRegistrationScreen: () -> Unit,
    navigateToUpdatePasswordScreen: () -> Unit,
    loginStatus: LoginStatus,
    modifier: Modifier = Modifier
) {
    var passwordVisibility by rememberSaveable { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero gradient banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = 0.15f),
                                tertiary.copy(alpha = 0.08f),
                                primary.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(top = 48.dp, bottom = 28.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mpesa_ledge_playstore_logo_no_bg),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .aspectRatio(16f / 9f)
                    )
                    Text(
                        text = "Welcome Back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sign in to your account",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }

            // Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    label = {
                        Text(text = "Safaricom phone number", fontSize = 14.sp)
                    },
                    value = phoneNumber,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.phone),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Phone
                    ),
                    onValueChange = { if (it.length <= 10) onChangePhoneNumber(it) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PasswordInputField(
                    heading = "Password",
                    value = password,
                    trailingIcon = R.drawable.visibility_on,
                    onValueChange = onChangePassword,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    visibility = passwordVisibility,
                    onChangeVisibility = { passwordVisibility = !passwordVisibility }
                )

                TextButton(
                    onClick = navigateToUpdatePasswordScreen,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = "Forgot password?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    enabled = buttonEnabled && loginStatus != LoginStatus.LOADING,
                    onClick = onLogin,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (loginStatus == LoginStatus.LOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Signing in…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(text = "Sign In", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Don't have an account? ", fontSize = 14.sp)
                    TextButton(onClick = navigateToRegistrationScreen) {
                        Text(
                            text = "Register",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    CashLedgerTheme {
        LoginScreen(
            phoneNumber = "",
            onChangePhoneNumber = {},
            password = "",
            onChangePassword = {},
            buttonEnabled = false,
            onLogin = { /*TODO*/ },
            navigateToRegistrationScreen = { /*TODO*/ },
            navigateToUpdatePasswordScreen = {},
            loginStatus = LoginStatus.INITIAL
        )
    }
}