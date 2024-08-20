package com.records.pesa.ui.screens.transactionTypes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate

@Composable
fun TransactionTypesScreenComposable(
    navigateToTransactionsScreen: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        TransactionTypesScreen(
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(7),
            onSelectDateRange = {},
            navigateToTransactionsScreen = navigateToTransactionsScreen
        )
    }
}

@Composable
fun TransactionTypesScreen(
    startDate: LocalDate,
    endDate: LocalDate,
    onSelectDateRange: () -> Unit,
    navigateToTransactionsScreen: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints {
        when(maxWidth) {
            in 0.dp..320.dp -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 16.dp
                        )
                ) {
                    Text(
                        text = "Transaction types",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                            )
                    ) {
                        Text(
                            text = "${dateFormatter.format(startDate)} to ${dateFormatter.format(endDate)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
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
                                .size(20.dp)
                                .clickable {
                                    onSelectDateRange()
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Money in",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "All money in",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "From Send Money",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Mshwari",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "KCB",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "M-PESA Deposit",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Reversal",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Hustler fund",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Money out",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "All money out",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Send to mobile",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Airtime & Bundles",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Till Accounts",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Paybill Account",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Pochi la Biashara",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Mshwari",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "KCB",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Withdrawal",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Fuliza",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Reversal",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = true,
                            type = "Hustler fund",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                    }
                }
            } 
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 16.dp
                        )
                ) {
                    Text(
                        text = "Transaction types",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                            )
                    ) {
                        Text(
                            text = "${dateFormatter.format(startDate)} to ${dateFormatter.format(endDate)}",
                            fontWeight = FontWeight.Bold,
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
                                .size(20.dp)
                                .clickable {
                                    onSelectDateRange()
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Money in",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "All money in",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "From Send Money",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Mshwari",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "KCB",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "M-PESA Deposit",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Reversal",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Hustler fund",
                            moneyIn = true,
                            amount = "Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Money out",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "All money out",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Send to mobile",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Till Accounts",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Paybill Account",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Pochi la Biashara",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Mshwari",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "KCB",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Withdrawal",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Fuliza",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Reversal",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TransactionTypeItem(
                            smallScreen = false,
                            type = "Hustler fund",
                            moneyIn = false,
                            amount = "- Ksh500,000",
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            navigateToTransactionsScreen = navigateToTransactionsScreen
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun TransactionTypeItem(
    smallScreen: Boolean,
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
        else -> null
    }

    val moneyDirection = if(moneyIn) "in" else "out"

    ElevatedCard(
        modifier = modifier
            .clickable {
                navigateToTransactionsScreen(transactionType, moneyDirection, startDate, endDate)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = type,
                fontWeight = FontWeight.Bold,
                fontSize = if(smallScreen) 14.sp else 16.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = amount,
                color = if(moneyIn) MaterialTheme.colorScheme.surfaceTint else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = if(smallScreen) 14.sp else 16.sp,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "See $type transactions"
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TransactionTypesScreenPreview() {
    CashLedgerTheme {
        TransactionTypesScreen(
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(7),
            onSelectDateRange = {},
            navigateToTransactionsScreen = {transactionType, moneyDirection, startDate, endDate ->  }
        )
    }
}