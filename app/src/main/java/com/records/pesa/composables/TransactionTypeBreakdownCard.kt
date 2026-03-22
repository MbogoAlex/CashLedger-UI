package com.records.pesa.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.TransactionTypeSummary
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth

/**
 * Displays a breakdown of transactions by type with visual indicators
 */
@Composable
fun TransactionTypeBreakdownCard(
    breakdown: List<TransactionTypeSummary>,
    selectedPeriod: TimePeriod,
    modifier: Modifier = Modifier
) {
    if (breakdown.isEmpty()) return
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = screenHeight(x = 2.0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(screenWidth(x = 16.0))
        ) {
            // Header
            Text(
                text = "Transaction Breakdown",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = selectedPeriod.getDetailedLabel(),
                fontSize = screenFontSize(x = 12.0).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = screenHeight(x = 4.0))
            )
            
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            
            // Transaction type items
            breakdown.take(7).forEach { summary ->
                TransactionTypeItem(
                    summary = summary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 12.0)))
            }
        }
    }
}

@Composable
private fun TransactionTypeItem(
    summary: TransactionTypeSummary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon and Type Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = getTransactionTypeIcon(summary.transactionType)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(screenWidth(x = 20.0))
                )
                Spacer(modifier = Modifier.width(screenWidth(x = 8.0)))
                Text(
                    text = summary.transactionType,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Count and Percentage
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${summary.transactionCount} txns",
                    fontSize = screenFontSize(x = 12.0).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format("%.1f", summary.percentageOfTotal)}%",
                    fontSize = screenFontSize(x = 11.0).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(screenHeight(x = 6.0)))
        
        // Progress Bar
        LinearProgressIndicator(
            progress = { summary.percentageOfTotal / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight(x = 4.0)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Returns an appropriate drawable resource ID for each transaction type
 */
private fun getTransactionTypeIcon(transactionType: String): Int {
    return when {
        transactionType.contains("Send Money", ignoreCase = true) -> R.drawable.ic_send_money
        transactionType.contains("Pay Bill", ignoreCase = true) -> R.drawable.ic_paybill
        transactionType.contains("Till", ignoreCase = true) || 
            transactionType.contains("Buy Goods", ignoreCase = true) -> R.drawable.ic_shopping_cart
        transactionType.contains("Pochi", ignoreCase = true) -> R.drawable.ic_shopping_cart
        transactionType.contains("Airtime", ignoreCase = true) || 
            transactionType.contains("Bundles", ignoreCase = true) -> R.drawable.ic_airtime
        transactionType.contains("Withdraw", ignoreCase = true) -> R.drawable.ic_withdraw
        transactionType.contains("Deposit", ignoreCase = true) -> R.drawable.ic_deposit
        transactionType.contains("Reversal", ignoreCase = true) -> R.drawable.ic_reversal
        transactionType.contains("Hustler", ignoreCase = true) -> R.drawable.ic_wheelbarrow
        transactionType.contains("Mshwari", ignoreCase = true) || 
            transactionType.contains("KCB", ignoreCase = true) ||
            transactionType.contains("Fuliza", ignoreCase = true) -> R.drawable.ic_bank
        else -> R.drawable.star
    }
}
