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
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.TransactionItem
import kotlin.math.absoluteValue

@Composable
fun TransactionItemCell(
    transaction: TransactionItem,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints {
        Log.d("WIDTH", maxWidth.toString())
        when(maxWidth) {
            in 0.dp..320.dp -> {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = modifier
                        .padding(
                            top = 10.dp,
                            bottom = 10.dp
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
//                        Card {
//                            Box(
//                                contentAlignment = Alignment.Center,
//                                modifier = Modifier
////                    .background(Color.Red)
//                                    .padding(8.dp)
//                            ) {
//                                Text(text = transaction.entity.substring(0, 2).uppercase())
//                            }
//                        }
//                        Spacer(modifier = Modifier.width(5.dp))
                        Column {
                            Text(
                                text = transaction.nickName?.let { if(it.length > 15) "${it.substring(0, 15).uppercase()}..." else it.uppercase() }  ?: if(transaction.entity.length > 15) "${transaction.entity.substring(0, 15).uppercase()}..." else transaction.entity.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
//                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = transaction.transactionType,
                                fontSize = 12.sp,
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
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.surfaceTint
                                )
                            } else if(transaction.transactionAmount < 0) {
                                Text(
                                    text = "- ${formatMoneyValue(transaction.transactionAmount.absoluteValue)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
//                            Spacer(modifier = Modifier.height(5.dp))
                            if(transaction.transactionAmount < 0) {
                                Text(
                                    text = "Cost: - ${formatMoneyValue(transaction.transactionCost.absoluteValue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.error
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
            else -> {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = modifier
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
                                Text(text = transaction.entity.substring(0, 2).uppercase())
                            }
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Column {
                            Text(
                                text = transaction.nickName?.let { if(it.length > 20) "${it.substring(0, 20).uppercase()}..." else it.uppercase() }  ?: if(transaction.entity.length > 20) "${transaction.entity.substring(0, 20).uppercase()}..." else transaction.entity.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = transaction.transactionType,
                                fontSize = 12.sp,
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
                                    color = MaterialTheme.colorScheme.surfaceTint
                                )
                            } else if(transaction.transactionAmount < 0) {
                                Text(
                                    text = "- ${formatMoneyValue(transaction.transactionAmount.absoluteValue)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            if(transaction.transactionAmount < 0) {
                                Text(
                                    text = "Cost: - ${formatMoneyValue(transaction.transactionCost.absoluteValue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.error
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
        }

    }

}

@Composable
fun SortedTransactionItemCell(
    transaction: SortedTransactionItem,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints {
        when(maxWidth) {
            in 0.dp..320.dp -> {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = modifier
                        .padding(
                            top = 10.dp,
                            bottom = 10.dp
                        )
                ) {
                    Row {
                        Column {
                            Text(
                                text = if(transaction.nickName != null) if(transaction.nickName.length > 15) "${transaction.nickName.substring(0, 15).uppercase()}..." else transaction.nickName else if  (transaction.entity.length > 15) "${
                                    transaction.entity.substring(
                                        0,
                                        15
                                    ).uppercase()
                                }..." else transaction.entity.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
//                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = transaction.transactionType,
                                fontSize = 12.sp,
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
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.surfaceTint
                            )
                            Text(
                                text = " - ${formatMoneyValue(transaction.totalOut.absoluteValue)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            if (transaction.totalOut != 0.0) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "Cost: - ${formatMoneyValue(transaction.transactionCost.absoluteValue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_downward),
                                    contentDescription = null
                                )
                                Text(
                                    text = "${transaction.timesIn} times",
                                    fontWeight = FontWeight.Light,
                                    fontSize = 12.sp,
                                    style = TextStyle(
                                        fontStyle = FontStyle.Italic
                                    )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_upward),
                                    contentDescription = null
                                )
                                Text(
                                    text = "${transaction.timesOut} times",
                                    fontWeight = FontWeight.Light,
                                    fontSize = 12.sp,
                                    style = TextStyle(
                                        fontStyle = FontStyle.Italic
                                    )
                                )

                            }


                        }

                    }
                }
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = modifier
                        .padding(
                            top = 10.dp,
                            bottom = 10.dp
                        )
                ) {
                    Row {
                        Card {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    //                    .background(Color.Red)
                                    .padding(16.dp)
                            ) {
                                Text(text = transaction.nickName?.let { it.substring(0, 2).uppercase() }
                                    ?: transaction.entity.substring(0, 2).uppercase())
                            }
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Column {
                            Text(
                                text = transaction.nickName?.let { if(it.length > 20) "${it.substring(0, 20).uppercase()}..." else it.uppercase() } ?: if(transaction.entity.length > 20) "${transaction.entity.substring(0, 20).uppercase()}..." else transaction.entity.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = transaction.transactionType,
                                fontSize = 12.sp,
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
                                color = MaterialTheme.colorScheme.surfaceTint
                            )
                            Text(
                                text = " - ${formatMoneyValue(transaction.totalOut.absoluteValue)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            if (transaction.totalOut != 0.0) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "Cost: - ${formatMoneyValue(transaction.transactionCost.absoluteValue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_downward),
                                    contentDescription = null
                                )
                                Text(
                                    text = "${transaction.timesIn} times",
                                    fontWeight = FontWeight.Light,
                                    fontSize = 12.sp,
                                    style = TextStyle(
                                        fontStyle = FontStyle.Italic
                                    )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_upward),
                                    contentDescription = null
                                )
                                Text(
                                    text = "${transaction.timesOut} times",
                                    fontWeight = FontWeight.Light,
                                    fontSize = 12.sp,
                                    style = TextStyle(
                                        fontStyle = FontStyle.Italic
                                    )
                                )

                            }


                        }

                    }
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
    BoxWithConstraints {
        when(maxWidth) {
            in 0.dp..320.dp -> {
                Card(
                    modifier = Modifier
                        .padding(
                            bottom = 10.dp
                        )
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = transactionCategory.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text =  if(transactionCategory.transactions.size > 1) "${transactionCategory.transactions.size} transactions" else "${transactionCategory.transactions.size} transaction",
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {navigateToCategoryDetailsScreen(transactionCategory.id.toString()) }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = transactionCategory.name
                            )
                        }
                    }
                }
            }
            else -> {
                Card(
                    modifier = Modifier
                        .padding(
                            bottom = 10.dp
                        )
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = transactionCategory.name,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text =  if(transactionCategory.transactions.size > 1) "${transactionCategory.transactions.size} transactions" else "${transactionCategory.transactions.size} transaction",
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {navigateToCategoryDetailsScreen(transactionCategory.id.toString()) }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = transactionCategory.name
                            )
                        }
                    }
                }
            }
        }
    }

}

