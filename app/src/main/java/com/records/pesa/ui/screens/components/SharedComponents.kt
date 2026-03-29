package com.records.pesa.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import kotlin.math.absoluteValue
import com.records.pesa.models.TransactionTypeSummary
import com.records.pesa.models.transaction.TransactionItem
import android.app.DatePickerDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.records.pesa.db.models.ManualTransaction
import com.records.pesa.models.TimePeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Standard transaction card — matches DashboardScreen's TransactionCard exactly.
 */
@Composable
fun TransactionCard(
    transaction: TransactionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (transaction.transactionAmount > 0)
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(
                                if (transaction.transactionAmount > 0) R.drawable.arrow_downward
                                else R.drawable.arrow_upward
                            ),
                            contentDescription = null,
                            tint = if (transaction.transactionAmount > 0)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.transactionType,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = transaction.nickName?.ifEmpty { transaction.entity }
                            ?: transaction.entity,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (transaction.transactionAmount > 0)
                        "+Ksh ${String.format("%,.0f", transaction.transactionAmount)}"
                    else
                        "-Ksh ${String.format("%,.0f", kotlin.math.abs(transaction.transactionAmount))}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.transactionAmount > 0)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.error
                )
                Text(
                    text = transaction.time,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Standard empty state card — matches DashboardScreen's EmptyStateCard exactly.
 */
@Composable
fun EmptyStateCard(
    icon: Int,
    message: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Standard section header — title on left, labelled action button on right.
 * Matches DashboardScreen's "Recent Activity" header style.
 */
@Composable
fun SectionHeader(
    title: String,
    actionLabel: String = "See All",
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = actionLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Top senders / receivers card computed from raw transaction list.
 * Matches DashboardScreen's TopPeopleSection exactly.
 */
@Composable
fun TopPeopleSection(
    transactions: List<TransactionItem>,
    periodLabel: String = "All Time",
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) return

    val topSenders = transactions
        .filter { it.transactionAmount > 0 }
        .groupBy { it.entity }
        .mapValues { it.value.sumOf { tx -> tx.transactionAmount } }
        .toList().sortedByDescending { it.second }.take(3)

    val topReceivers = transactions
        .filter { it.transactionAmount < 0 }
        .groupBy { it.entity }
        .mapValues { it.value.sumOf { tx -> Math.abs(tx.transactionAmount) } }
        .toList().sortedByDescending { it.second }.take(3)

    if (topSenders.isEmpty() && topReceivers.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            if (topSenders.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOP SENDERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = periodLabel.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                topSenders.forEach { (entity, amount) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entity.take(25),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        Text(
                            text = "+Ksh ${String.format("%,.0f", amount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (topSenders.isNotEmpty() && topReceivers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (topReceivers.isNotEmpty()) {
                Text(
                    text = "TOP RECEIVERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                topReceivers.forEach { (entity, amount) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entity.take(25),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        Text(
                            text = "-Ksh ${String.format("%,.0f", amount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top Categories breakdown — shows top transaction types for money in and out.
 */
@Composable
fun CompactTransactionBreakdown(
    moneyInCategories: List<TransactionTypeSummary>,
    moneyOutCategories: List<TransactionTypeSummary>,
    periodLabel: String = "All Time",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (moneyInCategories.isEmpty() && moneyOutCategories.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTION TYPES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = periodLabel.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (moneyInCategories.isNotEmpty()) {
                Text(
                    text = "MONEY IN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                moneyInCategories.take(3).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.transactionType,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "+Ksh ${String.format("%,.0f", item.totalAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (moneyInCategories.isNotEmpty() && moneyOutCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (moneyOutCategories.isNotEmpty()) {
                Text(
                    text = "MONEY OUT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                moneyOutCategories.take(3).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.transactionType,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "-Ksh ${String.format("%,.0f", item.totalAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// ─── Avatar palette ───────────────────────────────────────────────────────────

val txAvatarPalette = listOf(
    Color(0xFF006A65), Color(0xFF4A6361), Color(0xFF48607B),
    Color(0xFF7B5EA7), Color(0xFFB5542D), Color(0xFF2D6A8A),
    Color(0xFF5A7A2B), Color(0xFF8A3D4A),
)

fun txAvatarColor(name: String): Color =
    txAvatarPalette[(name.firstOrNull()?.code ?: 0) % txAvatarPalette.size]

// ─── Format helpers ───────────────────────────────────────────────────────────

fun formatTxShortDate(dateStr: String): String = try {
    java.time.LocalDate.parse(dateStr).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
} catch (e: Exception) { dateStr }

fun formatTxShortTime(timeStr: String): String = try {
    val t = java.time.LocalTime.parse(timeStr)
    t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
} catch (e: Exception) { timeStr }

fun formatTxDateHeader(dateStr: String): String = try {
    val date = java.time.LocalDate.parse(dateStr)
    val today = java.time.LocalDate.now()
    when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
    }
} catch (e: Exception) { dateStr }

// ─── Date header composable ───────────────────────────────────────────────────

@Composable
fun TxDateHeader(date: String) {
    Text(
        text = formatTxDateHeader(date),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
        letterSpacing = 0.4.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─── Transaction item row ─────────────────────────────────────────────────────

@Composable
fun TxItemRow(
    transaction: TransactionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIn = transaction.transactionAmount > 0
    val displayName = (transaction.nickName?.ifEmpty { null } ?: transaction.entity)
        .replaceFirstChar { it.uppercase() }
    val initials = displayName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("").ifEmpty { displayName.take(2).uppercase() }
    val avatarColor = txAvatarColor(transaction.entity)
    val amountColor = if (isIn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val hasFee = !isIn && transaction.transactionCost.absoluteValue > 0

    Row(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.15f)))
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
                Text(initials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(text = transaction.transactionType, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(text = "· ${formatTxShortDate(transaction.date)}  ${formatTxShortTime(transaction.time)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), maxLines = 1, softWrap = false)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(amountColor))
                Text(text = "${if (isIn) "+" else "-"}Ksh ${String.format("%,.0f", transaction.transactionAmount.absoluteValue)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = amountColor)
            }
            if (hasFee) {
                Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text = "Fee", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    Text(text = "Ksh ${String.format("%,.2f", transaction.transactionCost.absoluteValue)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun TxEmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(R.drawable.transactions), contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
        }
    }
}

// ─── Subscription / Upgrade dialog ───────────────────────────────────────────
@Composable
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFF7B5EA7)
    val accentColor  = Color(0xFFFFB300)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(primaryColor, primaryColor.copy(alpha = 0.75f))
                            )
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("★", fontSize = 26.sp, color = accentColor)
                        }
                        Text(
                            "Cash Ledger Premium",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.3.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = accentColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "KES 100 / month",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "Full transaction history — no 30-day cap",
                        "Advanced AI insights & spend projections",
                        "Unusual activity alerts",
                        "Backup & restore your data",
                        "Unlimited categories",
                        "Dark mode support",
                        "Downloadable reports (PDF & CSV)"
                    ).forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", fontSize = 11.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                            Text(feature, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text(
                        "Subscribe Now  →",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        "Maybe later",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        },
        dismissButton = null
    )
}

/**
 * Generic permission explanation dialog.
 * Shows WHY a permission is needed before the system OS prompt appears.
 * Use this for every permission request in the app.
 */
@Composable
fun PermissionExplanationDialog(
    icon: Int,
    title: String,
    explanation: String,
    confirmLabel: String = "Allow",
    dismissLabel: String = "Not now",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header gradient banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(primary.copy(alpha = 0.15f), tertiary.copy(alpha = 0.08f))
                            )
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                painter = androidx.compose.ui.res.painterResource(icon),
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(confirmLabel, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        dismissLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditManualTransactionDialog(
    tx: ManualTransaction,
    members: List<String>,
    onSave: (ManualTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMember by remember { mutableStateOf(tx.memberName) }
    var memberExpanded by remember { mutableStateOf(false) }
    var isOutflow by remember { mutableStateOf(tx.isOutflow) }
    var amountText by remember { mutableStateOf(tx.amount.toString()) }
    var description by remember { mutableStateOf(tx.description) }
    var selectedDate by remember { mutableStateOf(tx.date) }
    var selectedTime by remember { mutableStateOf(tx.time) }
    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val timeFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("hh:mm a") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = memberExpanded, onExpandedChange = { memberExpanded = it }) {
                    OutlinedTextField(
                        value = selectedMember,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Member") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false }) {
                        members.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { selectedMember = m; memberExpanded = false })
                        }
                    }
                }

                Text("Transaction type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isOutflow, onClick = { isOutflow = true }, label = { Text("Expense") })
                    FilterChip(selected = !isOutflow, onClick = { isOutflow = false }, label = { Text("Income") })
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (KES)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = selectedDate.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> selectedDate = java.time.LocalDate.of(y, m + 1, d) },
                                selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
                            ).show()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.calendar),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = selectedTime?.format(timeFormatter) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time (optional)") },
                    placeholder = { Text("Tap to set time") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedTime != null) {
                                IconButton(onClick = { selectedTime = null }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_clear_24),
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                val now = selectedTime ?: java.time.LocalTime.now()
                                android.app.TimePickerDialog(context, { _, h, min -> selectedTime = java.time.LocalTime.of(h, min) }, now.hour, now.minute, false).show()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.calendar),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    if (selectedMember.isNotBlank() && amount > 0) {
                        onSave(
                            tx.copy(
                                memberName = selectedMember,
                                isOutflow = isOutflow,
                                transactionTypeName = if (isOutflow) "Expense" else "Income",
                                amount = amount,
                                description = description,
                                date = selectedDate,
                                time = selectedTime
                            )
                        )
                    }
                },
                enabled = selectedMember.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TxSummaryBar(
    totalIn: Double,
    totalOut: Double,
    modifier: Modifier = Modifier
) {
    val net = totalIn - totalOut
    val netColor = if (net >= 0) Color(0xFF388E3C) else MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // IN
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C).copy(alpha = 0.08f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text("IN", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF388E3C), letterSpacing = 0.5.sp)
                Text(
                    "KES ${String.format("%,.0f", totalIn)}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C), maxLines = 1
                )
            }
        }
        // OUT
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text("OUT", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error, letterSpacing = 0.5.sp)
                Text(
                    "KES ${String.format("%,.0f", totalOut)}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error, maxLines = 1
                )
            }
        }
        // NET
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = netColor.copy(alpha = 0.08f)),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text("NET", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = netColor, letterSpacing = 0.5.sp)
                Text(
                    "${if (net >= 0) "+" else ""}KES ${String.format("%,.0f", net)}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = netColor, maxLines = 1
                )
            }
        }
    }
}

// ─── Shared download report dialog ───────────────────────────────────────────

/**
 * Reusable download-report dialog used across CategoriesScreen,
 * CategoryAllTransactionsScreen, and BudgetAllTransactionsScreen.
 *
 * Mirrors the period-chip style from TransactionsScreen.
 * Free users are blocked from selecting ranges longer than one month.
 */
@Composable
fun DownloadReportDialog(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (type: String, startDate: LocalDate, endDate: LocalDate) -> Unit,
) {
    val context = LocalContext.current
    val fmtDisplay = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    val periodOptions = remember {
        listOf(
            TimePeriod.TODAY, TimePeriod.YESTERDAY,
            TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK,
            TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
            TimePeriod.THIS_YEAR, TimePeriod.ENTIRE
        )
    }

    var selectedPeriod by remember { mutableStateOf<TimePeriod>(TimePeriod.THIS_MONTH) }
    var showPeriodMenu by remember { mutableStateOf(false) }
    var isCustom by remember { mutableStateOf(false) }

    val defaultRange = TimePeriod.THIS_MONTH.getDateRange()
    var customStart by remember { mutableStateOf(defaultRange.first) }
    var customEnd by remember { mutableStateOf(defaultRange.second) }

    val startDate = if (isCustom) customStart else selectedPeriod.getDateRange().first
    val endDate = if (isCustom) customEnd else selectedPeriod.getDateRange().second

    val oneMonthAgo = LocalDate.now().minusMonths(1)
    val rangeExceedsOneMonth = startDate.isBefore(oneMonthAgo)
    val premiumBlocked = !isPremium && rangeExceedsOneMonth

    val formats = listOf("PDF", "CSV")
    var selectedFormat by remember { mutableStateOf("PDF") }
    var showFormatMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Download Report",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Period picker ──────────────────────────────────────────
                Text(
                    text = "Period",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .clickable { showPeriodMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isCustom) "Custom" else selectedPeriod.getDisplayName(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            painter = painterResource(R.drawable.arrow_downward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showPeriodMenu,
                        onDismissRequest = { showPeriodMenu = false }
                    ) {
                        periodOptions.forEach { period ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = period.getDisplayName(),
                                        fontSize = 14.sp,
                                        fontWeight = if (!isCustom && period == selectedPeriod) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isCustom && period == selectedPeriod)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedPeriod = period
                                    isCustom = false
                                    showPeriodMenu = false
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.calendar),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Custom",
                                        fontSize = 14.sp,
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                isCustom = true
                                showPeriodMenu = false
                            }
                        )
                    }
                }

                // ── Custom date pickers ────────────────────────────────────
                if (isCustom) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start date
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "From",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d -> customStart = LocalDate.of(y, m + 1, d) },
                                            customStart.year,
                                            customStart.monthValue - 1,
                                            customStart.dayOfMonth
                                        ).show()
                                    }
                            ) {
                                Text(
                                    text = customStart.format(fmtDisplay),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        // End date
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "To",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d -> customEnd = LocalDate.of(y, m + 1, d) },
                                            customEnd.year,
                                            customEnd.monthValue - 1,
                                            customEnd.dayOfMonth
                                        ).show()
                                    }
                            ) {
                                Text(
                                    text = customEnd.format(fmtDisplay),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // ── Date range summary ─────────────────────────────────────
                Text(
                    text = "${startDate.format(fmtDisplay)}  →  ${endDate.format(fmtDisplay)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── Premium lock warning ───────────────────────────────────
                if (premiumBlocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔒 Premium required to download reports beyond 1 month. Upgrade to unlock.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // ── Format picker ──────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Format:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                .clickable { showFormatMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedFormat,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_downward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showFormatMenu,
                            onDismissRequest = { showFormatMenu = false }
                        ) {
                            formats.forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt, fontSize = 14.sp) },
                                    onClick = {
                                        selectedFormat = fmt
                                        showFormatMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedFormat, startDate, endDate) },
                enabled = !premiumBlocked
            ) {
                Text("Download")
            }
        }
    )
}
