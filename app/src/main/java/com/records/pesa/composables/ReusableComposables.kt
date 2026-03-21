package com.records.pesa.composables

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.functions.formatDate
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.mapper.toTransaction
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.IndividualSortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import kotlin.math.abs
import kotlin.math.absoluteValue

@Composable
fun TransactionItemCell(
    transaction: TransactionItem,
    modifier: Modifier = Modifier
) {
    Log.d("single_transaction", transaction.toString())
    Log.d("single_transaction_entity", transaction.entity)
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
            .padding(
                top = screenHeight(x = 10.0),
                bottom = screenHeight(x = 10.0)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
//                    .background(Color.Red)
                        .padding(screenWidth(x = 16.0))
                ) {
                    Text(
                        text = if(transaction.nickName.isNullOrEmpty()) transaction.entity.take(2) else transaction.nickName.take(2),
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
            Column {
                Text(
                    text = if(transaction.nickName.isNullOrEmpty()) if(transaction.entity.length > 20) "${transaction.entity.take(20).uppercase()}..." else transaction.entity.uppercase() else if(transaction.nickName.length > 20) "${transaction.nickName.take(20).uppercase()}..." else transaction.nickName.uppercase(),
                    fontSize = screenFontSize(x = 12.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
                Text(
                    text = transaction.transactionType,
                    fontSize = screenFontSize(x = 12.0).sp,
//                    fontWeight = FontWeight.Bold
                )



            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if(transaction.transactionAmount > 0) {
                    Text(
                        text = "+ ${formatMoneyValue(transaction.transactionAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.surfaceTint
                    )
                } else if(transaction.transactionAmount < 0) {
                    Text(
                        text = "- ${formatMoneyValue(transaction.transactionAmount.absoluteValue)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
                if(transaction.transactionAmount < 0) {
                    Text(
                        text = "Cost: - ${formatMoneyValue(transaction.transactionCost.absoluteValue)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 12.0).sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

        }
        Text(
            text = formatDate("${transaction.date} ${transaction.time}"),
            fontWeight = FontWeight.Light,
            fontSize = screenFontSize(x = 12.0).sp,
            style = TextStyle(
                fontStyle = FontStyle.Italic
            )
        )
    }

}

@Composable
fun SortedTransactionItemCell(
    transaction: SortedTransactionItem,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
            .padding(
                top = screenHeight(x = 10.0),
                bottom = screenHeight(x = 10.0)
            )
    ) {
        Row {
            Card {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        //                    .background(Color.Red)
                        .padding(screenWidth(x = 16.0))
                ) {
                    Text(
                        text = if(transaction.nickName.isNullOrEmpty()) transaction.entity.take(2) else transaction.nickName.take(2),
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
            Column {
                Text(
                    text = if(transaction.nickName.isNullOrEmpty()) if(transaction.entity.length > 20) "${transaction.entity.take(20).uppercase()}..." else transaction.entity.uppercase() else if(transaction.nickName.length > 20) "${transaction.nickName.take(20).uppercase()}..." else transaction.nickName.uppercase(),
                    fontSize = screenFontSize(x = 12.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
                Text(
                    text = transaction.transactionType,
                    fontSize = screenFontSize(x = 12.0).sp,
                    //                    fontWeight = FontWeight.Bold
                )


            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "+ ${formatMoneyValue(transaction.totalIn)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.surfaceTint
                )
                Text(
                    text = " - ${formatMoneyValue(transaction.totalOut.absoluteValue)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.error
                )
                if (transaction.totalOut != 0.0) {
                    Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
                    Text(
                        text = "Cost: - ${formatMoneyValue(transaction.transactionCost.absoluteValue)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 12.0).sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.error
                    )
                }

            }

        }
    }
}

@Composable
fun TransactionCategoryCell(
    transactionCategory: TransactionCategory,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(
                bottom = screenHeight(x = 10.0)
            )
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(screenWidth(x = 10.0))
        ) {
            Column {
                Text(
                    text = transactionCategory.name,
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text =  if(transactionCategory.transactions.size > 1) "${transactionCategory.transactions.size} transactions" else "${transactionCategory.transactions.size} transaction",
                    fontStyle = FontStyle.Italic,
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Light
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {navigateToCategoryDetailsScreen(transactionCategory.id.toString()) }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = transactionCategory.name,
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
        }
    }

}


@Composable
fun IndividualSortedTransactionItemCell(
    transaction: IndividualSortedTransactionItem,
    moneyIn: Boolean,
    modifier: Modifier = Modifier
) {
    Log.d("individualSortedTransaction", transaction.toString())
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
            .padding(
                top = screenHeight(x = 10.0),
                bottom = screenHeight(x = 10.0)
            )
    ) {
        Row {
            Card {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        //                    .background(Color.Red)
                        .padding(screenWidth(x = 16.0))
                ) {
                    Text(
                        text = if(transaction.nickName.isNullOrEmpty()) transaction.name.take(2) else transaction.nickName.take(2),
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
            Column {
                Text(
                    text = if(transaction.nickName.isNullOrEmpty())
                        transaction.name.take(20)
                    else
                        transaction.nickName.take(20),
                    fontSize = screenFontSize(x = 12.0).sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
                Text(
                    text = transaction.transactionType,
                    fontSize = screenFontSize(x = 12.0).sp,
                    //                    fontWeight = FontWeight.Bold
                )


            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if(moneyIn) {
                    Text(
                        text = "+ ${formatMoneyValue(transaction.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.surfaceTint
                    )
                } else {
                    Text(
                        text = " - ${formatMoneyValue(abs(transaction.amount))}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (transaction.amount.toDouble() != 0.0) {
                        Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
                        Text(
                            text = "Cost: - ${formatMoneyValue(transaction.transactionCost.absoluteValue)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 12.0).sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = "${transaction.times} times",
                    fontSize = screenFontSize(x = 12.0).sp,
                    fontWeight = FontWeight.Bold
                )

            }

        }
    }
}

