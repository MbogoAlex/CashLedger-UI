package com.records.pesa.ui.screens.transactions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.composables.TransactionItemCell
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate

object SingleEntityTransactionsScreenDestination: AppNavigation {
    override val title: String = "Single entity transactions screen"
    override val route: String = "single-entity-transactions-screen"
    val userId: String = "userId"
    val transactionType: String = "transactionType"
    val entity: String = "entity"
    val categoryId: String = "categoryId"
    val budgetId: String = "categoryId"
    val startDate: String = "startDate"
    val endDate: String = "endDate"
    val times: String = "times"
    val moneyIn: String = "moneyIn"
    val routeWithArgs: String = "$route/{$userId}/{$transactionType}/{$entity}/{$startDate}/{$endDate}/{$times}/{$moneyIn}"
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SingleEntityTransactionsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SingleEntityTransactionsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        SingleEntityTransactionsScreen(
            transactions = uiState.transactions,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            totalMoneyIn = formatMoneyValue(uiState.totalMoneyIn),
            totalMoneyOut = formatMoneyValue(uiState.totalMoneyOut),
            navigateToPreviousScreen = navigateToPreviousScreen
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SingleEntityTransactionsScreen(
    transactions: List<TransactionItem>,
    startDate: String,
    endDate: String,
    totalMoneyIn: String,
    totalMoneyOut: String,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp
            )
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "From ${dateFormatter.format(LocalDate.parse(startDate))} to ${dateFormatter.format(LocalDate.parse(endDate))}")
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    horizontal = 16.dp
                )
        ) {
            Icon(painter = painterResource(id = R.drawable.arrow_downward), contentDescription = null)
            Text(
                text = totalMoneyIn,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(painter = painterResource(id = R.drawable.arrow_upward), contentDescription = null)
            Text(
                text = totalMoneyOut,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        }
        LazyColumn {
            items(transactions) {
                TransactionItemCell(transaction = it)
                Divider()
            }
        }

    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SingleEntityTransactionsScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        SingleEntityTransactionsScreen(
            transactions = transactions,
            totalMoneyIn = "Ksh 1000",
            totalMoneyOut = "Ksh 500",
            startDate = "2023-03-06",
            endDate = "2024-06-25",
            navigateToPreviousScreen = {}
        )
    }
}