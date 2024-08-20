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
import androidx.compose.material.Divider
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
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
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
                type = "All money in",
                moneyIn = true,
                amount = "Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "From Send Money",
                moneyIn = true,
                amount = "Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Mshwari",
                moneyIn = true,
                amount = "Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "KCB",
                moneyIn = true,
                amount = "Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "M-PESA Deposit",
                moneyIn = true,
                amount = "Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Reversal",
                moneyIn = true,
                amount = "Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Hustler fund",
                moneyIn = true,
                amount = "Ksh500,000",
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
                type = "All money out",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Send to mobile",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Till Accounts",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Paybill Account",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Pochi la Biashara",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Mshwari",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "KCB",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Withdrawal",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Fuliza",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
                type = "Reversal",
                moneyIn = false,
                amount = "- Ksh500,000",
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                navigateToTransactionsScreen = navigateToTransactionsScreen
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
            TransactionTypeItem(
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

@Composable
fun TransactionTypeItem(
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

    Box(
        modifier = Modifier
            .clickable {
                navigateToTransactionsScreen(transactionType, moneyDirection, startDate, endDate)
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