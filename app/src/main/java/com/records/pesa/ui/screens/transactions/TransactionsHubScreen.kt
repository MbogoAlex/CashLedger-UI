package com.records.pesa.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.TransactionTypeSummary
import com.records.pesa.models.transaction.IndividualSortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.composables.RecentTransactionsSection
import kotlin.math.abs

// ─── Transaction type config ──────────────────────────────────────────────────

private data class TxTypeConfig(val label: String, val icon: Int, val accent: Color)

private val txTypeConfigs = listOf(
    TxTypeConfig("Send Money",        R.drawable.ic_send_money,    Color(0xFF006A65)),
    TxTypeConfig("Pay Bill",          R.drawable.ic_paybill,       Color(0xFF48607B)),
    TxTypeConfig("Buy Goods (Till)",  R.drawable.ic_shopping_cart, Color(0xFF7B5EA7)),
    TxTypeConfig("Withdraw Cash",     R.drawable.ic_withdraw,      Color(0xFFB5542D)),
    TxTypeConfig("Airtime & Bundles", R.drawable.ic_airtime,       Color(0xFF2D6A8A)),
    TxTypeConfig("Fuliza",            R.drawable.ic_bank,          Color(0xFF8A3D4A)),
    TxTypeConfig("M-Shwari",          R.drawable.ic_bank,          Color(0xFF5A7A2B)),
    TxTypeConfig("Deposit",           R.drawable.ic_deposit,       Color(0xFF4A6361)),
)

// ─── Entry point ─────────────────────────────────────────────────────────────

@Composable
fun TransactionsHubScreenComposable(
    navigateToAllTransactions: () -> Unit,
    navigateToSortedTransactions: () -> Unit,
    navigateToTransactionTypes: (startDate: String, endDate: String) -> Unit,
    navigateToTransactionDetails: (transactionId: String) -> Unit,
    navigateToEntityTransactions: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyDirection: String) -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: TransactionsHubScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    TransactionsHubScreen(
        recentTransactions = uiState.recentTransactions,
        latestTransactions = uiState.latestTransactions,
        topMoneyIn = uiState.topMoneyIn,
        topMoneyOut = uiState.topMoneyOut,
        totalMoneyIn = uiState.totalMoneyIn,
        totalMoneyOut = uiState.totalMoneyOut,
        totalTransactionCost = uiState.totalTransactionCost,
        costByType = uiState.costByType,
        selectedTimePeriod = uiState.selectedTimePeriod,
        moneyInCategories = uiState.moneyInCategories,
        moneyOutCategories = uiState.moneyOutCategories,
        startDate = uiState.startDate,
        endDate = uiState.endDate,
        userId = uiState.userDetails.id.toString(),
        isPremium = uiState.isPremium,
        onShowSubscriptionDialog = navigateToSubscriptionScreen,
        onPeriodSelected = { viewModel.updateSelectedPeriod(it) },
        navigateToAllTransactions = navigateToAllTransactions,
        navigateToSortedTransactions = navigateToSortedTransactions,
        navigateToTransactionTypes = navigateToTransactionTypes,
        navigateToTransactionDetails = navigateToTransactionDetails,
        navigateToEntityTransactions = navigateToEntityTransactions,
        modifier = modifier
    )
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun TransactionsHubScreen(
    recentTransactions: List<TransactionItem>,
    latestTransactions: List<TransactionItem> = emptyList(),
    topMoneyIn: List<IndividualSortedTransactionItem>,
    topMoneyOut: List<IndividualSortedTransactionItem>,
    totalMoneyIn: Double,
    totalMoneyOut: Double,
    totalTransactionCost: Double,
    costByType: List<TransactionTypeSummary>,
    selectedTimePeriod: TimePeriod,
    moneyInCategories: List<TransactionTypeSummary>,
    moneyOutCategories: List<TransactionTypeSummary>,
    startDate: String = "",
    endDate: String = "",
    userId: String = "",
    isPremium: Boolean = false,
    onShowSubscriptionDialog: () -> Unit = {},
    onPeriodSelected: (TimePeriod) -> Unit,
    navigateToAllTransactions: () -> Unit,
    navigateToSortedTransactions: () -> Unit,
    navigateToTransactionTypes: (startDate: String, endDate: String) -> Unit,
    navigateToTransactionDetails: (transactionId: String) -> Unit,
    navigateToEntityTransactions: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyDirection: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var topContactsShowIn by remember { mutableStateOf(true) }
    val contacts = if (topContactsShowIn) topMoneyIn else topMoneyOut
    val listState = rememberLazyListState()
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    // Show sticky bar once the hero card (item index 1) scrolls out of view
    val showStickyBar by remember { derivedStateOf { listState.firstVisibleItemIndex >= 2 } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ───────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {

        // ── Hero card ─────────────────────────────────────────────────────────
        item {
            HubHeroCard(
                totalIn = totalMoneyIn,
                totalOut = totalMoneyOut,
                selectedTimePeriod = selectedTimePeriod,
                isPremium = isPremium,
                onShowSubscriptionDialog = onShowSubscriptionDialog,
                onPeriodSelected = onPeriodSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ── Recent Activity ───────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            RecentTransactionsSection(
                transactions = latestTransactions,
                onSeeAllClick = navigateToAllTransactions,
                onTransactionClick = { id -> navigateToTransactionDetails(id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Top Contacts ──────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    primary.copy(alpha = 0.12f),
                                    tertiary.copy(alpha = 0.06f),
                                    primary.copy(alpha = 0.04f)
                                )
                            )
                        )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.account_info),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Top Contacts",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                ContactToggleChip(
                                    label = "Received",
                                    selected = topContactsShowIn,
                                    selectedBg = MaterialTheme.colorScheme.tertiary,
                                    onClick = { topContactsShowIn = true }
                                )
                                ContactToggleChip(
                                    label = "Sent",
                                    selected = !topContactsShowIn,
                                    selectedBg = MaterialTheme.colorScheme.error,
                                    onClick = { topContactsShowIn = false }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (contacts.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.account_info),
                                    contentDescription = null,
                                    tint = primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "No contact data",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Top contacts will appear here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(contacts) { contact ->
                                    ContactAvatarCard(
                                        item = contact,
                                        moneyIn = topContactsShowIn,
                                        onClick = {
                                            navigateToEntityTransactions(
                                                userId,
                                                contact.transactionType,
                                                contact.name,
                                                startDate,
                                                endDate,
                                                contact.times.toString(),
                                                if (topContactsShowIn) "in" else "out"
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Transaction Types ──────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                onClick = { navigateToTransactionTypes(startDate, endDate) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    primary.copy(alpha = 0.12f),
                                    tertiary.copy(alpha = 0.06f),
                                    primary.copy(alpha = 0.04f)
                                )
                            )
                        )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.chart),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Transaction Types",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            TextButton(
                                onClick = { navigateToTransactionTypes(startDate, endDate) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "View All",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = primary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (moneyInCategories.isEmpty() && moneyOutCategories.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.chart),
                                    contentDescription = null,
                                    tint = primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "No transaction data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            if (moneyInCategories.isNotEmpty()) {
                                Text(
                                    text = "MONEY IN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                moneyInCategories.take(3).forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(6.dp).background(tertiary.copy(alpha = 0.8f), CircleShape))
                                            Spacer(Modifier.width(8.dp))
                                            Text(item.transactionType, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text("+Ksh ${String.format("%,.0f", item.totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                            if (moneyInCategories.isNotEmpty() && moneyOutCategories.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                Spacer(Modifier.height(12.dp))
                            }
                            if (moneyOutCategories.isNotEmpty()) {
                                Text(
                                    text = "MONEY OUT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                moneyOutCategories.take(3).forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), CircleShape))
                                            Spacer(Modifier.width(8.dp))
                                            Text(item.transactionType, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text("-Ksh ${String.format("%,.0f", item.totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (totalTransactionCost > 0) {
            item {
                TransactionCostCard(
                    totalCost = totalTransactionCost,
                    costByType = costByType,
                    periodLabel = selectedTimePeriod.getDisplayName(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        } // end LazyColumn
        } // end Column

        // ── Sticky period bar — appears when hero card scrolls out of view ────
        AnimatedVisibility(
            visible = showStickyBar,
            enter = slideInVertically { -it } + fadeIn(tween(200)),
            exit = slideOutVertically { -it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Period dropdown chip
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selectedTimePeriod.getDisplayName(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_downward),
                                contentDescription = "Select period",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        val periodOptions = remember {
                            listOf(
                                TimePeriod.TODAY, TimePeriod.YESTERDAY,
                                TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK,
                                TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
                                TimePeriod.THIS_YEAR, TimePeriod.ENTIRE
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            periodOptions.forEach { period ->
                                val requiresPremium = !isPremium && (
                                    period == TimePeriod.LAST_MONTH ||
                                    period == TimePeriod.THIS_YEAR ||
                                    period == TimePeriod.ENTIRE
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = period.getDisplayName(),
                                                fontSize = 14.sp,
                                                fontWeight = if (period == selectedTimePeriod) FontWeight.Bold
                                                             else FontWeight.Normal,
                                                color = if (period == selectedTimePeriod) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (requiresPremium) {
                                                Icon(
                                                    painter = painterResource(R.drawable.lock),
                                                    contentDescription = "Premium",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (requiresPremium) onShowSubscriptionDialog()
                                        else onPeriodSelected(period)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    } // end Box
}

// ─── Hub hero card — mirrors HeroBalanceCard style ────────────────────────────

@Composable
private fun HubHeroCard(
    totalIn: Double,
    totalOut: Double,
    selectedTimePeriod: TimePeriod,
    isPremium: Boolean = false,
    onShowSubscriptionDialog: () -> Unit = {},
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val net = totalIn - totalOut
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "hubGradient")
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
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            Column(modifier = Modifier.fillMaxWidth()) {
                // Clickable period label with dropdown
                Box {
                    var showPeriodMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.08f))
                            .clickable { showPeriodMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedTimePeriod.getDisplayName().uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            painter = painterResource(R.drawable.arrow_downward),
                            contentDescription = "Select period",
                            tint = primaryColor,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    val periodOptions = remember {
                        listOf(
                            TimePeriod.TODAY, TimePeriod.YESTERDAY,
                            TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK,
                            TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
                            TimePeriod.THIS_YEAR, TimePeriod.ENTIRE
                        )
                    }
                    DropdownMenu(
                        expanded = showPeriodMenu,
                        onDismissRequest = { showPeriodMenu = false }
                    ) {
                        periodOptions.forEach { period ->
                            val requiresPremium = !isPremium && (
                                period == TimePeriod.LAST_MONTH ||
                                period == TimePeriod.THIS_YEAR ||
                                period == TimePeriod.ENTIRE
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = period.getDisplayName(),
                                            fontSize = 14.sp,
                                            fontWeight = if (period == selectedTimePeriod) FontWeight.Bold else FontWeight.Normal,
                                            color = if (period == selectedTimePeriod) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (requiresPremium) {
                                            Icon(
                                                painter = painterResource(R.drawable.lock),
                                                contentDescription = "Premium",
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showPeriodMenu = false
                                    if (requiresPremium) onShowSubscriptionDialog()
                                    else onPeriodSelected(period)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Net balance
                Text(
                    text = "Net Flow",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${if (net >= 0) "+" else "-"}Ksh ${String.format("%,.0f", kotlin.math.abs(net))}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // In / Out / Net row — full amounts, equal thirds, wrapping allowed
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "In",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ksh ${String.format("%,.0f", totalIn)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Out",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ksh ${String.format("%,.0f", totalOut)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Net",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val netStr = if (net >= 0) "Ksh ${String.format("%,.0f", net)}"
                                     else "-Ksh ${String.format("%,.0f", kotlin.math.abs(net))}"
                        Text(
                            text = netStr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatCompactAmount(amount: Double): String {
    return when {
        amount >= 1_000_000 -> "Ksh ${String.format("%.1fM", amount / 1_000_000)}"
        amount >= 1_000 -> "Ksh ${String.format("%.1fK", amount / 1_000)}"
        else -> "Ksh ${String.format("%.0f", amount)}"
    }
}

// ─── Contact avatar card ──────────────────────────────────────────────────────

private val avatarPalette = listOf(
    Color(0xFF006A65), Color(0xFF4A6361), Color(0xFF48607B),
    Color(0xFF7B5EA7), Color(0xFFB5542D), Color(0xFF2D6A8A),
    Color(0xFF5A7A2B), Color(0xFF8A3D4A),
)
private fun avatarColor(name: String): Color =
    avatarPalette[(name.firstOrNull()?.code ?: 0) % avatarPalette.size]

@Composable
private fun ContactAvatarCard(
    item: IndividualSortedTransactionItem,
    moneyIn: Boolean,
    onClick: () -> Unit
) {
    val displayName = item.nickName?.ifEmpty { item.name } ?: item.name
    val initials = displayName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("").ifEmpty { displayName.take(2).uppercase() }
    val accent = avatarColor(item.name)
    val amountColor = if (moneyIn) MaterialTheme.colorScheme.tertiary
                      else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .width(96.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = initials,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
            Text(
                text = displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Ksh ${String.format("%,.0f", abs(item.amount))}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${item.times}×",
                    fontSize = 10.sp,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ─── Transaction type grid ────────────────────────────────────────────────────

@Composable
private fun TransactionTypeGrid(
    onTypeClick: (String) -> Unit,
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
        Column(modifier = Modifier.padding(8.dp)) {
            txTypeConfigs.chunked(2).forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    row.forEach { config ->
                        TypeRow(
                            config = config,
                            onClick = { onTypeClick(config.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                if (rowIdx < txTypeConfigs.chunked(2).lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeRow(
    config: TxTypeConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = config.accent.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(config.icon),
                    contentDescription = config.label,
                    tint = config.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = config.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── Toggle chip ──────────────────────────────────────────────────────────────

@Composable
private fun ContactToggleChip(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) selectedBg else Color.Transparent,
        animationSpec = tween(200), label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200), label = "chipText"
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

// ─── Transaction Cost Card ────────────────────────────────────────────────────

@Composable
fun TransactionCostCard(
    totalCost: Double,
    costByType: List<TransactionTypeSummary>,
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.transactions),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TRANSACTION FEES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = periodLabel.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Fees Paid",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ksh ${String.format("%,.2f", totalCost)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (costByType.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "BY TYPE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                costByType.take(4).forEach { item ->
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
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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
                            text = "Ksh ${String.format("%,.2f", item.totalAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
