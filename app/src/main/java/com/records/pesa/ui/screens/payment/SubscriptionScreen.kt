package com.records.pesa.ui.screens.payment

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate

object SubscriptionScreenDestination: AppNavigation {
    override val title: String = "Subscription screen"
    override val route: String = "subscription-screen"

}
@Composable
fun SubscriptionScreenComposable(
    navigateToHomeScreen: () -> Unit,
    navigateToSmsFetchScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SubscriptionScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkConnectivity(context)
    }

    var lipaStatusCheck by rememberSaveable {
        mutableStateOf(false)
    }

    var showSuccessDialogue by rememberSaveable {
        mutableStateOf(false)
    }

    if(showSuccessDialogue) {
        PaymentSuccessDialogue(
            paymentPlan = when(uiState.amount) {
                "100" -> "Monthly"
                "400" -> "6 Months"
                "700" -> "12 Months"
                "2000" -> "Lifetime"
                else -> "Monthly"
            },
            onConfirm = {
                showSuccessDialogue = !showSuccessDialogue
                navigateToSmsFetchScreen()
            },
            onDismiss = {
                showSuccessDialogue = !showSuccessDialogue
                navigateToSmsFetchScreen()
            }
        )
    }

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, uiState.paymentMessage, Toast.LENGTH_SHORT).show()
        lipaStatusCheck = false
        showSuccessDialogue = true
        viewModel.resetPaymentStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, uiState.failedReason, Toast.LENGTH_SHORT).show()
        lipaStatusCheck = false
//        navigateToHomeScreen()
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


    BackHandler(onBack = {
        if(uiState.loadingStatus != LoadingStatus.LOADING) {
            if(showPaymentScreen) {
                showPaymentScreen = !showPaymentScreen
            } else {
                navigateToHomeScreen()
            }
            viewModel.cancel()
        } else {
            Toast.makeText(context, "Payment in progress", Toast.LENGTH_SHORT).show()
        }
    })

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        if(showPaymentScreen) {
            PaymentScreen(
                firstTransactionDate = uiState.firstTransactionDate,
                amount = uiState.amount,
                isConnected = isConnected,
                status = uiState.state,
                phoneNumber = uiState.phoneNumber,
                onAmountChange = {
                    viewModel.updateAmount(it)
                },
                onPhoneNumberChange = {
                    viewModel.updatePhoneNumber(it)
                },
                buttonEnabled = uiState.phoneNumber.isNotEmpty(),
                onPay = {
                    viewModel.updateAmount(it)
                    viewModel.lipa()
                },
                navigateToPreviousScreen = {
                    if(uiState.loadingStatus != LoadingStatus.LOADING) {
                        if(showPaymentScreen) {
                            showPaymentScreen = !showPaymentScreen
                            viewModel.cancel()
                        } else {
                            navigateToPreviousScreen()
                        }

                    } else {
                        Toast.makeText(context, "Payment in progress", Toast.LENGTH_SHORT).show()
                    }
                },
                lipaStatus = {
                    viewModel.lipaStatus()
                },
                onChangeLipaStatusCheck = {
                    lipaStatusCheck = !lipaStatusCheck
                },
                lipaStatusCheck = lipaStatusCheck,
                loadingStatus = uiState.loadingStatus,
            )
        } else {
            SubscriptionScreen(
                firstTransactionDate = uiState.firstTransactionDate,
                navigateToPreviousScreen = navigateToPreviousScreen,
                navigateToPaymentScreen = {
                    viewModel.updateAmount(it)
                    showPaymentScreen = !showPaymentScreen
                }
            )
        }
    }
}

@Composable
fun SubscriptionScreen(
    firstTransactionDate: String,
    navigateToPaymentScreen: (amount: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isLoading = remember { mutableStateOf(false) }  // State to track loading

    val subscriptionTypes = listOf("Monthly", "6 Months", "12 Months", "Lifetime")

    Column(
        modifier = Modifier
            .padding(horizontal = screenWidth(x = 16.0), vertical = screenHeight(x = 8.0))
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
//        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Text(
            text = "Note: Transactions fetched are from $firstTransactionDate",
            fontSize = screenFontSize(x = 14.0).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Column(
            modifier = Modifier
//                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(id = R.drawable.analyze_transactions),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            Text(
                text = "Get unlimited access to all features",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 24.0)))
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(screenHeight(x = 8.0)),
                horizontalArrangement = Arrangement.spacedBy(screenWidth(x = 8.0)),
            ) {
                items(subscriptionTypes) {
                    SubscriptionOptionCard(
                        subscriptionType = it,
                        navigateToPaymentScreen = navigateToPaymentScreen
                    )
                }
            }



            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))



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
}

@Composable
fun SubscriptionOptionCard(
    subscriptionType: String,
    navigateToPaymentScreen: (amount: String) -> Unit,
) {
    when(subscriptionType) {
        "Monthly" -> {
            Card(
                onClick = {
                    navigateToPaymentScreen("100")
                },
                modifier = Modifier
                    .fillMaxWidth()
//                    .heightIn(min = screenHeight(x = 150.0))
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
                    Text(
                        text = "Ksh100",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 24.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Text(
                            text = "Ksh0",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                    }
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Button(onClick = {
                        navigateToPaymentScreen("100")
                    }) {
                        Text(
                            text = "Subscribe",
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                    }

                }
            }
        }

        "6 Months" -> {
            Card(
                onClick = {
                    navigateToPaymentScreen("400")
                },
                modifier = Modifier
                    .fillMaxWidth()
//                    .heightIn(min = screenHeight(x = 150.0))
            ) {
                Column(
                    modifier = Modifier
                        .padding(screenWidth(x = 16.0))
                ) {
                    Text(
                        text = "6 months Plan +",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Text(
                        text = "Ksh400",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 24.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Text(
                            text = "Ksh200",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                    }
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Button(onClick = {
                        navigateToPaymentScreen("400")
                    }) {
                        Text(
                            text = "Subscribe",
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                    }

                }
            }
        }

        "12 Months" -> {
            Card(
                onClick = {
                    navigateToPaymentScreen("700")
                },
                modifier = Modifier
                    .fillMaxWidth()
//                    .heightIn(min = screenHeight(x = 150.0))
            ) {
                Column(
                    modifier = Modifier
                        .padding(screenWidth(x = 16.0))
                ) {
                    Text(
                        text = "Annual Plan +",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Text(
                        text = "Ksh700",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 24.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Text(
                            text = "Ksh500",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                    }
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Button(onClick = {
                        navigateToPaymentScreen("700")
                    }) {
                        Text(
                            text = "Subscribe",
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                    }

                }
            }
        }

        "Lifetime" -> {
            Card(
                onClick = {
                    navigateToPaymentScreen("2000")
                },
                modifier = Modifier
                    .fillMaxWidth()
//                    .heightIn(min = screenHeight(x = 150.0))
            ) {
                Column(
                    modifier = Modifier
                        .padding(screenWidth(x = 16.0))
                ) {
                    Text(
                        text = "Lifetime Access +",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Text(
                        text = "Ksh2000",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 24.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Text(
                        text = "Save a lot!",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Button(onClick = { navigateToPaymentScreen("2000") }) {
                        Text(
                            text = "Purchase",
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                    }

                }
            }
        }
    }
}


@Composable
fun PaymentScreen(
    firstTransactionDate: String,
    amount: String,
    isConnected: Boolean,
    status: String,
    phoneNumber: String,
    onAmountChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    buttonEnabled: Boolean,
    lipaStatusCheck: Boolean,
    onChangeLipaStatusCheck: () -> Unit,
    lipaStatus: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    loadingStatus: LoadingStatus,
    onPay: (amount: String) -> Unit
) {
    var understood by rememberSaveable {
        mutableStateOf(false)
    }

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
            text = when(amount) {
                "100" -> "Monthly subscription fee"
                "400" -> "6 months subscription fee"
                "700" -> "12 months subscription fee"
                "2000" -> "Lifetime subscription fee"
                else -> "Monthly subscription fee"
            },
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
                text = "Fee payment",
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
                value = amount,
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
            Text(
                text = "Note: Transactions fetched are from $firstTransactionDate",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
            Row(
                verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier
//                        .padding(screenWidth(x = 10.0))
            ) {
                if(understood) {
                    IconButton(
                        enabled = loadingStatus != LoadingStatus.LOADING,
                        onClick = {
                        understood = !understood
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.check_box_filled),
                            contentDescription = "I understand",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                } else {
                    IconButton(
                        enabled = loadingStatus != LoadingStatus.LOADING,
                        onClick = {
                        understood = !understood
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.check_box_blank),
                            contentDescription = "I understand",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }
//                    Spacer(modifier = Modifier.width(screenWidth(x = 4.0)))
                Text(
                    text = "I understand",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
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
                enabled = buttonEnabled && loadingStatus != LoadingStatus.LOADING && isConnected && understood,
                onClick = {
                    payButtonClicked = !payButtonClicked
                    onPay(amount)

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
            if(lipaStatusCheck) {
                Button(
                    enabled = false,
                    onClick = {
                        onChangeLipaStatusCheck()
                        lipaStatus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Checking payment...",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
            if(loadingStatus == LoadingStatus.FAIL) {
                Text(
                    text = "Already paid? ",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Button(
                    enabled = loadingStatus != LoadingStatus.LOADING,
                    onClick = {
                        onChangeLipaStatusCheck()
                        lipaStatus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "I have already paid",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
        }

    }
}

@Composable
fun PaymentSuccessDialogue(
    paymentPlan: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Payment Successful",
                fontSize = screenFontSize(x = 16.0).sp
            )
        },
        text = {
            Text(
                text = "You have successfully subscribed to $paymentPlan",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Exit",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SubscriptionScreenPreview() {
    CashLedgerTheme {
        SubscriptionScreen(
            firstTransactionDate = formatLocalDate(LocalDate.now().minusMonths(6)),
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
            firstTransactionDate = "",
            isConnected = false,
            status = "PROCESSING",
            phoneNumber = "254794649026",
            onAmountChange = {},
            onPhoneNumberChange = {},
            buttonEnabled = false,
            lipaStatus = {},
            onChangeLipaStatusCheck = {},
            lipaStatusCheck = false,
            navigateToPreviousScreen = {},
            amount = "",
            loadingStatus = LoadingStatus.INITIAL
        ) {
            
        }
    }
}