package com.records.pesa.ui.screens.payment

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay

object SubscriptionScreenDestination: AppNavigation {
    override val title: String = "Subscription screen"
    override val route: String = "subscription-screen"

}
@Composable
fun SubscriptionScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SubscriptionScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkConnectivity(context)
    }

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, uiState.paymentMessage, Toast.LENGTH_SHORT).show()
        navigateToHomeScreen()
        viewModel.resetPaymentStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, uiState.failedReason, Toast.LENGTH_SHORT).show()
        navigateToHomeScreen()
        viewModel.resetPaymentStatus()
    }

    var showPage by rememberSaveable {
        mutableStateOf(true)
    }

    var showPaymentScreen by rememberSaveable {
        mutableStateOf(false)
    }

    var monthly by rememberSaveable {
        mutableStateOf(false)
    }

    val isConnected by viewModel.isConnected.observeAsState(false)



    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        if(showPaymentScreen) {
            PaymentScreen(
                monthly = monthly,
                isConnected = isConnected,
                status = uiState.state,
                phoneNumber = uiState.phoneNumber,
                onAmountChange = {},
                onPhoneNumberChange = {
                    viewModel.updatePhoneNumber(it)
                },
                buttonEnabled = uiState.phoneNumber.isNotEmpty(),
                onPay = {
                    viewModel.lipa()
                },
                navigateToPreviousScreen = {
                    showPaymentScreen = !showPaymentScreen
                    viewModel.cancel()
                },
                loadingStatus = uiState.loadingStatus,
            )
        } else {
            SubscriptionScreen(
                navigateToPreviousScreen = navigateToPreviousScreen,
                navigateToPaymentScreen = {
                    monthly = it
                    showPaymentScreen = !showPaymentScreen
                }
            )
        }
    }
}

@Composable
fun SubscriptionScreen(
    navigateToPaymentScreen: (monthly: Boolean) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isLoading = remember { mutableStateOf(false) }  // State to track loading

    Column(
        modifier = Modifier
            .padding(horizontal = screenWidth(x = 16.0), vertical = screenHeight(x = 8.0))
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous screen"
                )
            }
            Text(
                text = "Upgrade to Pro",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        Image(
            painter = painterResource(id = R.drawable.analyze_transactions),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Text(
            text = "Get unlimited access to all features",
            fontSize = screenFontSize(x = 14.0).sp
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 24.0)))
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(screenWidth(x = 16.0))
            ) {
                Text(
                    text = "Monthly Plan +",
                    fontWeight = FontWeight.Bold,
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Ksh50",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 24.0).sp
                    )
                    Text(
                        text = "/month",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Button(onClick = {
                    navigateToPaymentScreen(true)
                }) {
                    Text(
                        text = "Subscribe",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }

            }
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(screenWidth(x = 16.0))
            ) {
                Text(
                    text = "Lifetime Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Ksh1000",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 24.0).sp
                    )
                    Text(
                        text = "one-time purchase",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Button(onClick = { navigateToPaymentScreen(false) }) {
                    Text(
                        text = "Subscribe",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }

            }
        }


        // Display loading indicator while loading
        if (isLoading.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}


@Composable
fun PaymentScreen(
    monthly: Boolean,
    isConnected: Boolean,
    status: String,
    phoneNumber: String,
    onAmountChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    buttonEnabled: Boolean,
    navigateToPreviousScreen: () -> Unit,
    loadingStatus: LoadingStatus,
    onPay: () -> Unit
) {
    BackHandler(onBack = navigateToPreviousScreen)
    var payButtonClicked by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .padding(
                start = 20.dp,
                end = 20.dp,
                bottom = 20.dp
            )
            .fillMaxSize()

    ) {
        Row {
            IconButton(
                onClick = navigateToPreviousScreen
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
            }
        }
        Text(
            text = if(monthly) "Monthly subscription fee" else "Lifetime subscription fee",
            fontSize = screenFontSize(x = 22.0).sp,
            fontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Deposit Amount",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Amount",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = if(monthly) "50" else "1000",
                readOnly = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                onValueChange = onAmountChange,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Payment method",
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp,
                modifier = Modifier
                    .padding(
                        top = 8.dp,
                        bottom = 8.dp
                    )
            )
            Row(
                modifier = Modifier
                    .padding(
                        vertical = 8.dp
                    )
            ) {
                Card {
                    Text(
                        text = "Mpesa",
                        fontSize = screenFontSize(x = 14.0).sp,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Phone number",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = phoneNumber,
                placeholder = {
                    Text(
                        text = "Enter phone number",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                onValueChange = onPhoneNumberChange,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.weight(1f))
            if(!isConnected) {
                Text(
                    text = "Connect to the internet",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            }
            if (payButtonClicked) {
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Text(
                    text = "Payment request will appear in a few seconds...",
                    fontWeight = FontWeight.Bold,
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            }
            Button(
                enabled = buttonEnabled && loadingStatus != LoadingStatus.LOADING && isConnected,
                onClick = {
                    payButtonClicked = !payButtonClicked
                    onPay()
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if(loadingStatus == LoadingStatus.LOADING) {
                    Text(
                        text = "$status...",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                } else {
                    Text(
                        text = "Pay now",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }

            }
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            if(payButtonClicked) {
                Text(
                    text = "Don't exit the screen or the transaction will be cancelled. You will be redirected from the screen when the transaction is complete",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }

    }
}




@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SubscriptionScreenPreview() {
    CashLedgerTheme {
        SubscriptionScreen(
            navigateToPaymentScreen = {},
            navigateToPreviousScreen = { /*TODO*/ }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaymentScreenPreview() {
    CashLedgerTheme {
        PaymentScreen(
            monthly = true,
            isConnected = false,
            status = "PROCESSING",
            phoneNumber = "254794649026",
            onAmountChange = {},
            onPhoneNumberChange = {},
            buttonEnabled = false,
            navigateToPreviousScreen = {},
            loadingStatus = LoadingStatus.INITIAL
        ) {
            
        }
    }
}