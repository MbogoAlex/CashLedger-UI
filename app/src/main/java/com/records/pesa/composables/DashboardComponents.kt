package com.records.pesa.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.common.model.Point
import com.records.pesa.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Hero Balance Card - Premium gradient card showcasing account balance
 */
@Composable
fun HeroBalanceCard(
    balance: String,
    showBalance: Boolean,
    onToggleVisibility: () -> Unit,
    firstTransactionDate: String,
    periodLabel: String,
    moneyIn: String,
    moneyOut: String,
    selectedTimePeriod: TimePeriod = TimePeriod.TODAY,
    availableYears: List<Int> = emptyList(),
    onPeriodSelected: (TimePeriod) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    // Animated gradient
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                spotColor = primaryColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            tertiaryColor.copy(alpha = 0.10f),
                            primaryColor.copy(alpha = 0.05f)
                        ),
                        start = Offset(animatedOffset, animatedOffset),
                        end = Offset(animatedOffset + 500f, animatedOffset + 500f)
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with tracking info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(primaryColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "M-PESA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "Since $firstTransactionDate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Balance label
                Text(
                    text = "Account Balance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Balance amount with toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedCounter(
                        targetValue = balance,
                        showValue = showBalance,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    // Visibility toggle
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            painter = if (showBalance) {
                                painterResource(R.drawable.visibility_off)
                            } else {
                                painterResource(R.drawable.visibility_on)
                            },
                            contentDescription = if (showBalance) "Hide balance" else "Show balance",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Clickable period label with dropdown
                Box {
                    var showPeriodMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable { showPeriodMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = periodLabel.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            painter = painterResource(R.drawable.arrow_downward),
                            contentDescription = "Select period",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    val allPeriods = remember(availableYears) {
                        buildList<TimePeriod> {
                            add(TimePeriod.TODAY); add(TimePeriod.YESTERDAY)
                            add(TimePeriod.THIS_WEEK); add(TimePeriod.LAST_WEEK)
                            add(TimePeriod.THIS_MONTH); add(TimePeriod.LAST_MONTH)
                            add(TimePeriod.THIS_YEAR)
                            val currentYear = java.time.LocalDate.now().year
                            availableYears.filter { it < currentYear }
                                .forEach { add(TimePeriod.SPECIFIC_YEAR(it)) }
                        }
                    }
                    DropdownMenu(
                        expanded = showPeriodMenu,
                        onDismissRequest = { showPeriodMenu = false }
                    ) {
                        allPeriods.forEach { period ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = period.getDisplayName(),
                                        fontSize = 14.sp,
                                        fontWeight = if (period == selectedTimePeriod) FontWeight.Bold
                                                     else FontWeight.Normal
                                    )
                                },
                                onClick = { onPeriodSelected(period); showPeriodMenu = false }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Period stats in compact row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Money In
                    Column {
                        Text(
                            text = "In",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = moneyIn.replace("Ksh ", ""),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    // Money Out
                    Column {
                        Text(
                            text = "Out",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = moneyOut.replace("Ksh ", ""),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    // Net
                    Column {
                        Text(
                            text = "Net",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val net = calculateNetFlow(moneyIn, moneyOut)
                        Text(
                            text = net,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (net.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated counter for numbers
 */
@Composable
fun AnimatedCounter(
    targetValue: String,
    showValue: Boolean,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val displayValue = if (showValue) targetValue else "*".repeat(targetValue.length.coerceAtMost(15))
    
    Text(
        text = displayValue,
        style = style,
        modifier = modifier
    )
}

/**
 * Quick Stats Grid - Clean, compact period metrics
 */
@Composable
fun QuickStatsGrid(
    periodLabel: String,
    moneyIn: String,
    moneyOut: String,
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Period label
            Text(
                text = periodLabel.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Money In
                StatItem(
                    label = "In",
                    value = moneyIn.replace("Ksh ", ""),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                
                // Money Out
                StatItem(
                    label = "Out",
                    value = moneyOut.replace("Ksh ", ""),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                
                // Net
                val net = calculateNetFlow(moneyIn, moneyOut)
                StatItem(
                    label = "Net",
                    value = net,
                    color = if (net.startsWith("-")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Transaction count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$transactionCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "transactions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Individual stat item - clean and compact
 */
@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

/**
 * Calculate net flow from money in and out
 */
private fun calculateNetFlow(moneyIn: String, moneyOut: String): String {
    return try {
        // Extract only numeric values and handle any format
        val inValue = moneyIn.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        val outValue = moneyOut.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        val net = inValue - outValue
        
        if (net >= 0) {
            "+${String.format("%,.0f", net)}"
        } else {
            "${String.format("%,.0f", net)}"
        }
    } catch (e: Exception) {
        "0"
    }
}

/**
 * Visual Chart Section with proper context and prominence
 */
@Composable
fun VisualChartSection(
    moneyInPoints: List<Point>,
    moneyOutPoints: List<Point>,
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Money Flow Trends",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Daily breakdown for $periodLabel",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Chart card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                MoneyFlowLineChart(
                    moneyInPoints = moneyInPoints,
                    moneyOutPoints = moneyOutPoints,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Redesigned Recent Transactions Section with card layout
 */
@Composable
fun RecentTransactionsSection(
    transactions: List<TransactionItem>,
    onSeeAllClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "See All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (transactions.isEmpty()) {
            // Empty state
            EmptyStateCard(
                icon = R.drawable.list,
                message = "No transactions yet",
                subtitle = "Your recent transactions will appear here"
            )
        } else {
            // Transaction cards
            transactions.take(3).forEach { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction.transactionId.toString()) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

/**
 * Modern transaction card — delegates to shared component.
 */
@Composable
fun TransactionCard(
    transaction: TransactionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = com.records.pesa.ui.screens.components.TransactionCard(transaction, onClick, modifier)

/**
 * Empty state card — delegates to shared component.
 */
@Composable
fun EmptyStateCard(
    icon: Int,
    message: String,
    subtitle: String,
    modifier: Modifier = Modifier
) = com.records.pesa.ui.screens.components.EmptyStateCard(icon, message, subtitle, modifier)

/**
 * Determine if a transaction type is money in or out based on type name
 */
private fun isMoneyInType(transactionType: String): Boolean {
    val lowerType = transactionType.lowercase()
    return when {
        // Money IN types
        lowerType.contains("reversal") -> true
        lowerType.contains("deposit") -> true
        lowerType.contains("mshwari") -> true
        lowerType.contains("kcb") -> true
        lowerType.contains("hustler") -> true
        lowerType.contains("from") -> true  // "From Send Money", etc.
        
        // Money OUT types
        lowerType.contains("send") && !lowerType.contains("from") -> false
        lowerType.contains("airtime") -> false
        lowerType.contains("bundle") -> false
        lowerType.contains("buy") -> false
        lowerType.contains("till") -> false
        lowerType.contains("paybill") -> false
        lowerType.contains("pay bill") -> false
        lowerType.contains("withdraw") -> false
        lowerType.contains("pochi") -> false
        lowerType.contains("fuliza") -> false
        
        // Default: check amount sign
        else -> true
    }
}

/**
 * Compact Transaction Breakdown — delegates to shared component.
 */
@Composable
fun CompactTransactionBreakdown(
    moneyInCategories: List<com.records.pesa.models.TransactionTypeSummary>,
    moneyOutCategories: List<com.records.pesa.models.TransactionTypeSummary>,
    periodLabel: String = "All Time",
    modifier: Modifier = Modifier
) = com.records.pesa.ui.screens.components.CompactTransactionBreakdown(
    moneyInCategories, moneyOutCategories, periodLabel, modifier
)

/**
 * Top Senders/Receivers Section — delegates to shared component.
 */
@Composable
fun TopPeopleSection(
    transactions: List<com.records.pesa.models.transaction.TransactionItem>,
    periodLabel: String = "All Time",
    modifier: Modifier = Modifier
) = com.records.pesa.ui.screens.components.TopPeopleSection(transactions, periodLabel, modifier)
