package com.records.pesa.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
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
                Text(text = "Login failure message")
            },
            text = {
                Text(text = uiState.exception)
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
            if(uiState.transactions.isEmpty()) {
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
    var passwordVisibility by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
//        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(
                horizontal = screenWidth(x = 16.0),
                vertical = screenHeight(x = 16.0)
            )
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(id = R.drawable.mpesa_ledge_playstore_logo_no_bg),
            contentDescription = null,
            modifier = Modifier.aspectRatio(16f / 9f)
        )
        Text(
            text = "Welcome back!",
            fontWeight = FontWeight.Bold,
            fontSize = screenFontSize(x = 14.0).sp,
            modifier = Modifier
                .align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        TextField(
            label = {
                Text(
                    text = "Safaricom phone number",
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.scrim,
                )
            },
            value = phoneNumber,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.phone),
                    contentDescription = null,
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Phone
            ),
            onValueChange = onChangePhoneNumber,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
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
            modifier = Modifier
                .align(Alignment.Start)
        ) {
            Text(
                text = "Forgot password?",
                fontSize = screenFontSize(x = 14.0).sp
            )
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        Row {
            Text(
                text = "Don't have an account? ",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Text(
                text = "Register",
                fontSize = screenFontSize(x = 14.0).sp,
                color = MaterialTheme.colorScheme.surfaceTint,
                modifier = Modifier
                    .clickable { navigateToRegistrationScreen() }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            enabled = buttonEnabled && loginStatus != LoginStatus.LOADING,
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(loginStatus == LoginStatus.LOADING) {
                Text(
                    text = "Loading...",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            } else {
                Text(
                    text = "Login",
                    fontSize = screenFontSize(x = 14.0).sp
                )
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