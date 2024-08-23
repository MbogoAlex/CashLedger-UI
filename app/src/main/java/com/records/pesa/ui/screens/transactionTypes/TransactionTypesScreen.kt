package com.records.pesa.ui.screens.transactionTypes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.ui.screens.transactions.DateRangePicker
import com.records.pesa.ui.screens.transactions.SubscriptionDialog
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate

@Composable
fun TransactionTypesScreenComposable(
    navigateToTransactionsScreen: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {

    BackHandler(onBack = navigateToHomeScreen)

    val context = LocalContext.current
    
    val viewModel: TransactionTypesScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()
    
    var showDateRangePicker by rememberSaveable {
        mutableStateOf(false)
    }
    
    var showSubscriptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if(showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = {
                showSubscriptionDialog = !showSubscriptionDialog
            },
            onConfirm = {
                showSubscriptionDialog = !showSubscriptionDialog
                navigateToSubscriptionScreen()
            }
        )
    }
    
    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        TransactionTypesScreen(
            premium = uiState.userDetails.paymentStatus,
            allMoneyIn = formatMoneyValue(uiState.allMoneyIn),
            fromMshwari = formatMoneyValue(uiState.fromMshwari),
            fromSendMoney = formatMoneyValue(uiState.fromSendMoney),
            fromKcb = formatMoneyValue(uiState.fromKcbMpesa),
            deposit = formatMoneyValue(uiState.deposit),
            fromReversal = formatMoneyValue(uiState.fromReversal),
            fromHustler = formatMoneyValue(uiState.fromHustler),
            allMoneyOut = formatMoneyValue(uiState.allMoneyOut),
            toSendMoney = formatMoneyValue(uiState.toSendMoney),
            till = formatMoneyValue(uiState.till),
            payBill = formatMoneyValue(uiState.payBill),
            pochi = formatMoneyValue(uiState.pochi),
            withdrawal = formatMoneyValue(uiState.withdrawal),
            fuliza = formatMoneyValue(uiState.fuliza),
            toReversal = formatMoneyValue(uiState.toReversal),
            toHustler = formatMoneyValue(uiState.toHustler),
            startDate = LocalDate.parse(uiState.startDate),
            endDate = LocalDate.parse(uiState.endDate),
            onChangeStartDate = {
                viewModel.updateStartDate(it)
            },
            onChangeLastDate = {
                viewModel.updateEndDate(it)
            },
            onSelectDateRange = {
                showDateRangePicker = !showDateRangePicker
            },
            onConfirmDateRange = {
                showDateRangePicker = !showDateRangePicker
            },
            onDismissDatePicker = {
                showDateRangePicker = !showDateRangePicker
            },
            showDateRangePicker = showDateRangePicker,
            onShowSubscriptionDialog = {
                showSubscriptionDialog = !showSubscriptionDialog
            },
            navigateToTransactionsScreen = navigateToTransactionsScreen,
        )
    }
}

@Composable
fun TransactionTypesScreen(
    premium: Boolean,
    allMoneyIn: String,
    fromMshwari: String,
    fromSendMoney: String,
    fromKcb: String,
    deposit: String,
    fromReversal: String,
    fromHustler: String,
    allMoneyOut: String,
    toSendMoney: String,
    till: String,
    payBill: String,
    pochi: String,
    withdrawal: String,
    fuliza: String,
    toReversal: String,
    toHustler: String,
    startDate: LocalDate,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    endDate: LocalDate,
    onSelectDateRange: () -> Unit,
    showDateRangePicker: Boolean,
    onDismissDatePicker: () -> Unit,
    onConfirmDateRange: () -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    navigateToTransactionsScreen: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = screenWidth(x = 16.0),
                end = screenWidth(x = 16.0),
                bottom = screenWidth(x = 16.0)
            )
    ) {
        Text(
            text = "Transaction types",
            fontSize = screenFontSize(x = 18.0).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    horizontal = screenWidth(x = 16.0),
                )
        ) {
            Text(
                text = "${dateFormatter.format(startDate)} to ${dateFormatter.format(endDate)}",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp,
//                            fontSize = 14.sp,
//                        textAlign = TextAlign.Center,
                modifier = Modifier

//                            .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                tint = MaterialTheme.colorScheme.surfaceTint,
                painter = painterResource(id = R.drawable.calendar),
                contentDescription = "Select date range",
                modifier = Modifier
                    .size(screenWidth(x = 20.0))
                    .clickable {
                        onSelectDateRange()
                    }
            )
        }
        if(showDateRangePicker) {
           DateRangePickerDialog(
                premium = premium,
                startDate = startDate,
                endDate = endDate,
                defaultStartDate = null,
                defaultEndDate = null,
                onChangeStartDate = onChangeStartDate,
                onChangeLastDate = onChangeLastDate,
                onDismiss = onDismissDatePicker,
                onConfirm = onConfirmDateRange,
                onShowSubscriptionDialog = onShowSubscriptionDialog,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Money in:",
                fontSize = screenFontSize(x = 17.0).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surfaceTint,
                modifier = Modifier
                    .padding(start = screenWidth(x = 16.0))
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            Divider()
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "All money in",
                moneyIn = true,
                amount = allMoneyIn,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "From Send Money",
                moneyIn = true,
                amount = fromSendMoney,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Mshwari",
                moneyIn = true,
                amount = fromMshwari,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "KCB",
                moneyIn = true,
                amount = fromKcb,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "M-PESA Deposit",
                moneyIn = true,
                amount = deposit,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Reversal",
                moneyIn = true,
                amount = fromReversal,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Hustler fund",
                moneyIn = true,
                amount = fromHustler,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Text(
                text = "Money out:",
                fontSize = screenFontSize(x = 17.0).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(start = screenWidth(x = 16.0))
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            Divider()
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "All money out",
                moneyIn = false,
                amount = "- $allMoneyOut",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Send to mobile",
                moneyIn = false,
                amount = "- $toSendMoney",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Till Accounts",
                moneyIn = false,
                amount = "- $till",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Paybill Account",
                moneyIn = false,
                amount = "- $payBill",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Pochi la Biashara",
                moneyIn = false,
                amount = "- $pochi",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Mshwari",
                moneyIn = false,
                amount = "- $fromMshwari",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "KCB",
                moneyIn = false,
                amount = "- $fromKcb",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Withdrawal",
                moneyIn = false,
                amount = "- $withdrawal",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Fuliza",
                moneyIn = false,
                amount = "- $fuliza",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Reversal",
                moneyIn = false,
                amount = "- $toReversal",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                showDateRangePicker = showDateRangePicker,
                type = "Hustler fund",
                moneyIn = false,
                amount = "- $toHustler",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
        }
    }

}

@Composable
fun TransactionTypeItem(
    showDateRangePicker: Boolean,
    type: String,
    moneyIn: Boolean,
    amount: String,
    startDate: String,
    endDate: String,
    navigateToTransactionsScreen: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val transactionType = when(type) {
        "From Send Money" -> "Send Money"
        "Mshwari" -> "Mshwari"
        "KCB" -> "KCB Mpesa account"
        "M-PESA Deposit" -> "Deposit"
        "Send to mobile" -> "Send Money"
        "Airtime & Bundles" -> "Airtime & Bundles"
        "Till Accounts" -> "Buy Goods and Services (till)"
        "Paybill Account" -> "Pay Bill"
        "Pochi la Biashara" -> "Pochi la Biashara"
        "Withdrawal" -> "Withdraw Cash"
        "Fuliza" -> "Fuliza"
        "Reversal" -> "Reversal"
        "Hustler fund" -> "Hustler fund"
        "All money in" -> "All money in"
        "All money out" -> "All money out"
        else -> null
    }

    val moneyDirection = if(moneyIn) "in" else "out"

    Box(
        modifier = Modifier
            .clickable {
                if(!showDateRangePicker) {
                    navigateToTransactionsScreen(transactionType, moneyDirection, startDate, endDate)
                }

            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(screenWidth(x = 16.0))

        ) {
            Text(
                text = type,
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = amount,
                color = if(moneyIn) MaterialTheme.colorScheme.surfaceTint else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "See $type transactions",
                modifier = Modifier
                    .size(screenWidth(x = 24.0))
            )
        }
    }

}

@Composable
fun DateRangePickerDialog(
    premium: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Red
        ),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier

    ) {
        Popup(
            alignment = Alignment.TopEnd,
            properties = PopupProperties(
                excludeFromSystemGesture = true
            ),
            onDismissRequest = onDismiss,
        ) {
            Card(
                shape = RoundedCornerShape(0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Select date range",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 18.0).sp,
                        modifier = Modifier
                            .padding(
                                start = screenWidth(x = 16.0)
                            )
                    )
                    DateRangePicker(
                        premium = premium,
                        startDate = startDate,
                        endDate = endDate,
                        defaultStartDate = defaultStartDate,
                        defaultEndDate = defaultEndDate,
                        onChangeStartDate = onChangeStartDate,
                        onChangeLastDate = onChangeLastDate,
                        onShowSubscriptionDialog = onShowSubscriptionDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(
                                horizontal = screenWidth(x = 16.0)
                            )
                            .align(Alignment.End)
                    ) {
                        Text(text = "Dismiss")
                    }
                }
            }
        }
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TransactionTypesScreenPreview() {
    CashLedgerTheme {
        TransactionTypesScreen(
            premium = false,
            allMoneyIn = "0.0",
            fromMshwari = "0.0",
            fromSendMoney = "500.0",
            fromKcb = "1000.0",
            deposit = "1230",
            fromReversal = "4324",
            fromHustler = "76543",
            allMoneyOut = "8978",
            toSendMoney = "9878",
            till = "9876",
            payBill = "1234",
            pochi = "3212",
            withdrawal = "54456",
            fuliza = "87656",
            toReversal = "8765",
            toHustler = "5654",
            startDate = LocalDate.now(),
            onChangeStartDate = {},
            onChangeLastDate = {},
            endDate = LocalDate.now().plusDays(7),
            onSelectDateRange = { /*TODO*/ },
            showDateRangePicker = false,
            onDismissDatePicker = { /*TODO*/ },
            onConfirmDateRange = { /*TODO*/ },
            onShowSubscriptionDialog = { /*TODO*/ },
            navigateToTransactionsScreen = {transactionType, moneyDirection, startDate, endDate ->  }
        )
    }
}