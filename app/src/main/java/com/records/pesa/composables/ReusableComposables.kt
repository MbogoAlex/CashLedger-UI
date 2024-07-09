package com.records.pesa.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.functions.formatDate
import com.records.pesa.models.SortedTransactionItem
import com.records.pesa.models.TransactionItem
import com.records.pesa.reusables.TransactionScreenTab
import com.records.pesa.reusables.TransactionScreenTabItem
import kotlin.math.absoluteValue

@Composable
fun TransactionItemCell(
    transaction: TransactionItem,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .padding(
                top = 10.dp,
                bottom = 10.dp
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
                        .padding(16.dp)
                ) {
                    if(transaction.transactionAmount < 0) {
                        Text(text = transaction.recipient.substring(0, 2).uppercase())
                    } else {
                        Text(text = transaction.sender.substring(0, 2).uppercase())
                    }
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                Text(
                    text = transaction.transactionType.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                if(transaction.transactionAmount < 0) {
                    Text(
                        text = if(transaction.recipient.length > 12) "${transaction.recipient.substring(0, 12)}..." else transaction.recipient,
                        fontSize = 12.sp
//                    fontWeight = FontWeight.Bold
                    )
                } else if(transaction.transactionAmount > 0) {
                    Text(
                        text = if(transaction.sender.length > 12) "${transaction.sender.substring(0, 12)}..." else transaction.sender,
                        fontSize = 12.sp
//                    fontWeight = FontWeight.Bold
                    )
                }

            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if(transaction.transactionAmount > 0) {
                    Text(
                        text = "+ ${transaction.transactionAmount}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                } else if(transaction.transactionAmount < 0) {
                    Text(
                        text = "- ${transaction.transactionAmount.absoluteValue}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                if(transaction.transactionAmount < 0) {
                    Text(
                        text = "Cost: - ${transaction.transactionCost.absoluteValue}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.Red
                    )
                }
            }

        }
        Text(
            text = formatDate("${transaction.date} ${transaction.time}"),
            fontWeight = FontWeight.Light,
            fontSize = 12.sp,
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
        modifier = Modifier
            .padding(
                top = 10.dp,
                bottom = 10.dp
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
                        .padding(16.dp)
                ) {
                    Text(text = transaction.name.substring(0, 2).uppercase())
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                Text(
                    text = transaction.transactionType.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = if(transaction.name.length > 12) "${transaction.name.substring(0, 12)}..." else transaction.name,
                    fontSize = 12.sp
//                    fontWeight = FontWeight.Bold
                )

            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if(transaction.amount > 0) {
                    Text(
                        text = "+ ${transaction.amount}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                } else if(transaction.amount < 0) {
                    Text(
                        text = " - ${transaction.amount.absoluteValue}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                if(transaction.amount < 0) {
                    Text(
                        text = "Cost: - ${transaction.transactionCost.absoluteValue}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.Red
                    )
                } else {
                    Text(
                        text = "${transaction.times} times",
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        style = TextStyle(
                            fontStyle = FontStyle.Italic
                        )
                    )
                }

            }

        }
        if(transaction.amount < 0) {
            Text(
                text = "${transaction.times} times",
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                style = TextStyle(
                    fontStyle = FontStyle.Italic
                )
            )
        }
    }
}

