package com.records.pesa.ui.screens.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.db.models.ManualTransaction
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.transaction.ItemCategory
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.ui.screens.components.EditManualTransactionDialog
import kotlin.math.abs

object TransactionDetailsScreenDestination : AppNavigation {
    override val title = "Transaction details screen"
    override val route = "transaction-details-screen"
    val transactionId = "transactionId"
    val routeWithArgs = "$route/{$transactionId}"
}

// ── helpers ───────────────────────────────────────────────────────────────────

private fun isPhoneLike(value: String): Boolean {
    val stripped = value.replace("\\s".toRegex(), "")
    return stripped.matches(Regex("^(07|01|254|\\+254)\\d+$"))
}

private fun buildShareText(tx: TransactionItem): String = buildString {
    appendLine("CASH LEDGER TRANSACTION")
    appendLine("═══════════════════════")
    appendLine("Code:    ${tx.transactionCode}")
    appendLine("Type:    ${tx.transactionType}")
    val sign = if (tx.transactionAmount >= 0) "+" else ""
    appendLine("Amount:  ${sign}${formatMoneyValue(tx.transactionAmount)}")
    appendLine("Entity:  ${tx.entity}")
    if (tx.transactionAmount < 0 && tx.transactionCost > 0)
        appendLine("Cost:    ${formatMoneyValue(tx.transactionCost)}")
    appendLine("Balance: ${formatMoneyValue(tx.balance)}")
    appendLine("Date:    ${tx.date}  ${tx.time}")
    if (!tx.categories.isNullOrEmpty())
        appendLine("Categories: ${tx.categories.joinToString { it.name }}")
    if (!tx.comment.isNullOrBlank())
        appendLine("Comment: ${tx.comment}")
}

private fun buildShareTextManual(tx: ManualTransaction): String = buildString {
    appendLine("CASH LEDGER TRANSACTION")
    appendLine("═══════════════════════")
    appendLine("Member: ${tx.memberName}")
    appendLine("Type:   ${tx.transactionTypeName}")
    val sign = if (!tx.isOutflow) "+" else "-"
    appendLine("Amount: ${sign}${formatMoneyValue(tx.amount)}")
    appendLine("Date:   ${tx.date}")
    if (tx.description.isNotBlank())
        appendLine("Note:   ${tx.description}")
}

private fun launchShare(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

private fun buildInvoiceHtml(
    tx: TransactionItem? = null,
    manualTx: ManualTransaction? = null,
    isManual: Boolean = false,
): String {
    val isOutflow = if (isManual) manualTx?.isOutflow == true else (tx?.transactionAmount ?: 0.0) < 0
    val amountColor = if (isOutflow) "#c0392b" else "#27ae60"
    val sign = if (isOutflow) "-" else "+"
    val sb = StringBuilder()
    sb.append("""<!DOCTYPE html><html><head><meta charset="UTF-8"><style>
body{font-family:Arial,sans-serif;background:#fff;color:#222;margin:0;padding:24px;max-width:480px;}
h1{font-size:22px;margin:0 0 2px;color:#1a237e;letter-spacing:1px;}
.sub{font-size:12px;color:#888;margin:0 0 12px;}
hr{border:none;border-top:2px solid #1a237e;margin:12px 0;}
.row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #eee;font-size:13px;}
.label{color:#666;flex:0 0 38%;}
.value{font-weight:600;flex:0 0 60%;text-align:right;}
.amt{font-size:26px;font-weight:700;color:""" + amountColor + """;text-align:center;padding:14px 0 6px;}
.entity{font-size:15px;font-weight:600;text-align:center;margin-bottom:4px;}
.footer{margin-top:24px;font-size:11px;color:#aaa;text-align:center;}
.badge{display:inline-block;background:#e8eaf6;color:#1a237e;border-radius:4px;padding:2px 10px;font-size:11px;font-weight:700;}
.chip{display:inline-block;background:#e3f2fd;color:#1565c0;border-radius:12px;padding:2px 10px;font-size:12px;margin:2px;}
</style></head><body>""")
    sb.append("<h1>Cash Ledger</h1><p class='sub'>Transaction Invoice</p><hr/>")
    if (!isManual && tx != null) {
        val entity = if (!tx.nickName.isNullOrBlank()) tx.nickName else tx.entity
        val amt = formatMoneyValue(abs(tx.transactionAmount))
        sb.append("<div class='entity'>$entity</div>")
        sb.append("<div class='amt'>${sign}KES $amt</div><hr/>")
        sb.append("<div class='row'><span class='label'>Code</span><span class='value'>${tx.transactionCode}</span></div>")
        sb.append("<div class='row'><span class='label'>Type</span><span class='value'><span class='badge'>${tx.transactionType}</span></span></div>")
        sb.append("<div class='row'><span class='label'>Entity</span><span class='value'>${tx.entity}</span></div>")
        sb.append("<div class='row'><span class='label'>Date</span><span class='value'>${tx.date}</span></div>")
        sb.append("<div class='row'><span class='label'>Time</span><span class='value'>${tx.time}</span></div>")
        sb.append("<div class='row'><span class='label'>Balance</span><span class='value'>KES ${formatMoneyValue(tx.balance)}</span></div>")
        if (isOutflow && tx.transactionCost > 0)
            sb.append("<div class='row'><span class='label'>Cost</span><span class='value'>KES ${formatMoneyValue(tx.transactionCost)}</span></div>")
        if (!tx.categories.isNullOrEmpty()) {
            val chips = tx.categories.joinToString("") { "<span class='chip'>${it.name}</span>" }
            sb.append("<div class='row'><span class='label'>Categories</span><span class='value'>$chips</span></div>")
        }
        if (!tx.comment.isNullOrBlank())
            sb.append("<div class='row'><span class='label'>Comment</span><span class='value'>${tx.comment}</span></div>")
    } else if (isManual && manualTx != null) {
        val amt = formatMoneyValue(manualTx.amount)
        sb.append("<div class='entity'>${manualTx.memberName}</div>")
        sb.append("<div class='amt'>${sign}KES $amt</div><hr/>")
        sb.append("<div class='row'><span class='label'>Type</span><span class='value'><span class='badge'>${manualTx.transactionTypeName}</span></span></div>")
        sb.append("<div class='row'><span class='label'>Member</span><span class='value'>${manualTx.memberName}</span></div>")
        sb.append("<div class='row'><span class='label'>Date</span><span class='value'>${manualTx.date}</span></div>")
        if (manualTx.time != null)
            sb.append("<div class='row'><span class='label'>Time</span><span class='value'>${manualTx.time}</span></div>")
        if (manualTx.description.isNotBlank())
            sb.append("<div class='row'><span class='label'>Note</span><span class='value'>${manualTx.description}</span></div>")
    }
    sb.append("<p class='footer'>Powered by Cash Ledger</p></body></html>")
    return sb.toString()
}

private fun printInvoice(context: Context, html: String) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = view.createPrintDocumentAdapter("Cash Ledger Invoice")
            printManager.print("Cash Ledger Invoice", printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}

// ── category chip palette ─────────────────────────────────────────────────────

private val chipPalette = listOf(
    Color(0xFF1565C0) to Color(0xFFE3F2FD),
    Color(0xFF2E7D32) to Color(0xFFE8F5E9),
    Color(0xFF6A1B9A) to Color(0xFFF3E5F5),
    Color(0xFFE65100) to Color(0xFFFFF3E0),
    Color(0xFF00695C) to Color(0xFFE0F2F1),
    Color(0xFF4527A0) to Color(0xFFEDE7F6),
    Color(0xFF558B2F) to Color(0xFFF1F8E9),
    Color(0xFF283593) to Color(0xFFE8EAF6),
)

private fun categoryChipColors(index: Int): Pair<Color, Color> = chipPalette[index % chipPalette.size]

// ── entry composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToCategoryScreen: (categoryId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: TransactionDetailsScreenViewModel =
        viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteTransactionDialog by rememberSaveable { mutableStateOf(false) }
    var deleteAllInstances by rememberSaveable { mutableStateOf(false) }
    var showDeleteCommentDialog by rememberSaveable { mutableStateOf(false) }
    var editAliasActive by rememberSaveable { mutableStateOf(false) }
    var editCommentActive by rememberSaveable { mutableStateOf(false) }
    var showAddToCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showEditManualTxDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteManualTxDialog by rememberSaveable { mutableStateOf(false) }
    var showUpgradeDialog by rememberSaveable { mutableStateOf(false) }
    var removeFromCatId by rememberSaveable { mutableStateOf(-1) }
    var removeKeywordId by rememberSaveable { mutableStateOf(-1) }
    var showRemoveFromCatConfirm by rememberSaveable { mutableStateOf(false) }

    val aliasStatus = uiState.updatingAliasStatus
    val commentStatus = uiState.updatingCommentStatus
    val deleteTxStatus = uiState.deletingTransactionStatus
    val deleteManualStatus = uiState.deletingManualTxStatus

    if (aliasStatus == UpdatingAliasStatus.SUCCESS) {
        editAliasActive = false
        Toast.makeText(context, "Alias updated", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    } else if (aliasStatus == UpdatingAliasStatus.FAIL) {
        Toast.makeText(context, "Failed to update alias", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    }

    if (commentStatus == UpdatingCommentStatus.SUCCESS) {
        editCommentActive = false
        Toast.makeText(context, "Comment updated", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    } else if (commentStatus == UpdatingCommentStatus.FAIL) {
        Toast.makeText(context, "Failed to update comment", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    }

    if (deleteTxStatus == DeletingTransactionStatus.SUCCESS) {
        Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
        navigateToPreviousScreen()
        viewModel.resetUpdatingStatus()
    } else if (deleteTxStatus == DeletingTransactionStatus.FAIL) {
        Toast.makeText(context, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    }

    if (deleteManualStatus == DeletingTransactionStatus.SUCCESS) {
        Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
        navigateToPreviousScreen()
        viewModel.resetUpdatingStatus()
    } else if (deleteManualStatus == DeletingTransactionStatus.FAIL) {
        Toast.makeText(context, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    }

    // upgrade dialog
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("Upgrade to Premium", fontWeight = FontWeight.Bold) },
            text = { Text("Viewing multiple categories requires a premium subscription.") },
            confirmButton = {
                Button(onClick = { showUpgradeDialog = false }) { Text("OK") }
            }
        )
    }

    // remove from category confirm dialog
    if (showRemoveFromCatConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveFromCatConfirm = false },
            title = { Text("Remove Category", fontWeight = FontWeight.Bold) },
            text = { Text("Remove this transaction from the selected category?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        if (removeFromCatId != -1 && removeKeywordId != -1)
                            viewModel.removeFromCategory(removeFromCatId, removeKeywordId)
                        showRemoveFromCatConfirm = false
                        removeFromCatId = -1
                        removeKeywordId = -1
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRemoveFromCatConfirm = false
                    removeFromCatId = -1
                    removeKeywordId = -1
                }) { Text("Cancel") }
            }
        )
    }

    // delete transaction dialog
    if (showDeleteTransactionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTransactionDialog = false },
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to delete this transaction?")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { deleteAllInstances = !deleteAllInstances }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (deleteAllInstances) R.drawable.check_box_filled
                                else R.drawable.check_box_blank
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Delete all transactions from this entity", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteTransaction(
                            uiState.transaction.transactionId!!,
                            deleteAllInstances
                        )
                        showDeleteTransactionDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTransactionDialog = false }) { Text("Cancel") }
            }
        )
    }

    // delete comment dialog
    if (showDeleteCommentDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCommentDialog = false },
            title = { Text("Delete Comment", fontWeight = FontWeight.Bold) },
            text = { Text("Remove the comment from this transaction?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.onDeleteComment()
                        showDeleteCommentDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCommentDialog = false }) { Text("Cancel") }
            }
        )
    }

    // delete manual tx dialog
    if (showDeleteManualTxDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteManualTxDialog = false },
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this manual transaction?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteManualTx()
                        showDeleteManualTxDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteManualTxDialog = false }) { Text("Cancel") }
            }
        )
    }

    // add-to-category dialog
    if (showAddToCategoryDialog) {
        var showCreateNew by rememberSaveable { mutableStateOf(false) }
        var categorySearch by rememberSaveable { mutableStateOf("") }
        val available = uiState.allCategories.filter { cat ->
            (uiState.transaction.categories?.none { it.id == cat.category.id } ?: true) &&
                (categorySearch.isBlank() || cat.category.name.contains(categorySearch, ignoreCase = true))
        }
        AlertDialog(
            onDismissRequest = { showAddToCategoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Add to Category", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = categorySearch,
                        onValueChange = { categorySearch = it },
                        placeholder = { Text("Search categories\u2026", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.search), null, Modifier.size(18.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    if (available.isEmpty() && !showCreateNew) {
                        Text(
                            "No matching categories.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(available) { cat ->
                            val allIdx = uiState.allCategories.indexOf(cat)
                            val (contentCol, _) = categoryChipColors(allIdx)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.addToCategory(cat.category.id)
                                        showAddToCategoryDialog = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .height(52.dp)
                                            .background(
                                                contentCol,
                                                RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                                            )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        cat.category.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(vertical = 14.dp)
                                    )
                                }
                            }
                        }
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                            if (showCreateNew) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    OutlinedTextField(
                                        value = uiState.newCategoryName,
                                        onValueChange = viewModel::onChangeNewCategoryName,
                                        label = { Text("New category name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { showCreateNew = false }) { Text("Cancel") }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            enabled = uiState.newCategoryName.isNotBlank(),
                                            onClick = {
                                                viewModel.createCategoryAndAdd()
                                                showAddToCategoryDialog = false
                                                showCreateNew = false
                                            }
                                        ) { Text("Create & Add") }
                                    }
                                }
                            } else {
                                TextButton(
                                    onClick = { showCreateNew = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_add),
                                        null,
                                        Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Create new category",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToCategoryDialog = false }) { Text("Close") }
            }
        )
    }

    // edit manual tx dialog
    val manualTxSnapshot = uiState.manualTransaction
    if (showEditManualTxDialog && manualTxSnapshot != null) {
        EditManualTransactionDialog(
            tx = manualTxSnapshot,
            members = uiState.members,
            onSave = { updated ->
                viewModel.updateManualTx(updated)
                showEditManualTxDialog = false
            },
            onDismiss = { showEditManualTxDialog = false }
        )
    }

    val onShare: () -> Unit = {
        val snap = uiState.manualTransaction
        if (uiState.isManualTransaction && snap != null)
            launchShare(context, buildShareTextManual(snap))
        else
            launchShare(context, buildShareText(uiState.transaction))
    }

    Box(modifier = Modifier.safeDrawingPadding()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (uiState.isManualTransaction) "Manual Transaction" else "Transaction",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = navigateToPreviousScreen) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onShare) {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = "Share",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .animateContentSize()
            ) {
                Spacer(Modifier.height(8.dp))

                if (uiState.isManualTransaction) {
                    ManualTransactionContent(
                        uiState = uiState,
                        onPrintInvoice = {
                            printInvoice(
                                context,
                                buildInvoiceHtml(manualTx = uiState.manualTransaction, isManual = true)
                            )
                        },
                        onEditTx = { showEditManualTxDialog = true },
                        onDeleteTx = { showDeleteManualTxDialog = true }
                    )
                } else {
                    MpesaTransactionContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        editAliasActive = editAliasActive,
                        editCommentActive = editCommentActive,
                        onToggleAlias = { editAliasActive = !editAliasActive },
                        onToggleComment = { editCommentActive = !editCommentActive },
                        onDeleteComment = { showDeleteCommentDialog = true },
                        onDeleteTransaction = { showDeleteTransactionDialog = true },
                        onAddToCategory = { showAddToCategoryDialog = true },
                        onPrintInvoice = {
                            printInvoice(context, buildInvoiceHtml(tx = uiState.transaction))
                        },
                        onCategoryClick = { cat, index ->
                            if (!uiState.isPremium && index > 0) showUpgradeDialog = true
                            else navigateToCategoryScreen(cat.id.toString())
                        },
                        onCategoryRemove = { cat ->
                            val kwId = uiState.allCategories
                                .find { it.category.id == cat.id }
                                ?.keyWords?.find { it.keyword == uiState.transaction.entity }?.id ?: -1
                            if (kwId != -1) {
                                removeFromCatId = cat.id
                                removeKeywordId = kwId
                                showRemoveFromCatConfirm = true
                            }
                        }
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── M-PESA content ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MpesaTransactionContent(
    uiState: TransactionDetailsScreenUiState,
    viewModel: TransactionDetailsScreenViewModel,
    editAliasActive: Boolean,
    editCommentActive: Boolean,
    onToggleAlias: () -> Unit,
    onToggleComment: () -> Unit,
    onDeleteComment: () -> Unit,
    onDeleteTransaction: () -> Unit,
    onAddToCategory: () -> Unit,
    onPrintInvoice: () -> Unit,
    onCategoryClick: (cat: ItemCategory, index: Int) -> Unit,
    onCategoryRemove: (cat: ItemCategory) -> Unit,
) {
    val context = LocalContext.current
    val tx = uiState.transaction
    val isOutflow = tx.transactionAmount < 0
    val phone = if (isOutflow) tx.recipient else tx.sender
    val phoneIsValid = isPhoneLike(phone)

    HeroCard(
        typeBadge = tx.transactionType,
        date = tx.date,
        amount = tx.transactionAmount,
        entityLabel = if (tx.nickName.isNullOrBlank()) tx.entity else "${tx.nickName} (${tx.entity})",
        time = tx.time,
        isOutflow = isOutflow
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        item {
            SuggestionChip(
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.10f)
                ),
                icon = { Icon(painterResource(R.drawable.copy), null, Modifier.size(15.dp), tint = Color(0xFF1565C0)) },
                label = { Text("Copy Code", fontSize = 12.sp, color = Color(0xFF1565C0)) },
                onClick = { copyToClipboard(context, "Transaction code", tx.transactionCode) }
            )
        }
        item {
            SuggestionChip(
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.10f)
                ),
                icon = { Icon(painterResource(R.drawable.copy), null, Modifier.size(15.dp), tint = Color(0xFF1565C0)) },
                label = { Text("Copy Entity", fontSize = 12.sp, color = Color(0xFF1565C0)) },
                onClick = { copyToClipboard(context, "Entity", tx.entity) }
            )
        }
        if (phoneIsValid) {
            item {
                SuggestionChip(
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.10f)
                    ),
                    icon = { Icon(painterResource(R.drawable.phone), null, Modifier.size(15.dp), tint = Color(0xFF2E7D32)) },
                    label = { Text("Copy Phone", fontSize = 12.sp, color = Color(0xFF2E7D32)) },
                    onClick = { copyToClipboard(context, "Phone", phone) }
                )
            }
        }
        if (phoneIsValid && isOutflow) {
            item {
                SuggestionChip(
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_send_money), null, Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    label = {
                        Text(
                            "Transact Again", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    onClick = {
                        val dialUri = Uri.parse("tel:*150*${phone.replace("+", "")}#")
                        context.startActivity(Intent(Intent.ACTION_DIAL, dialUri))
                    }
                )
            }
        }
    }

    SectionCard(title = "DETAILS") {
        DetailRow("Code", tx.transactionCode, onCopy = { copyToClipboard(context, "Code", tx.transactionCode) })
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Type", tx.transactionType)
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow(
            "Amount",
            if (isOutflow) "-${formatMoneyValue(abs(tx.transactionAmount))}"
            else "+${formatMoneyValue(tx.transactionAmount)}"
        )
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Entity", tx.entity, onCopy = { copyToClipboard(context, "Entity", tx.entity) })
        if (phoneIsValid) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            DetailRow("Phone", phone, onCopy = { copyToClipboard(context, "Phone", phone) })
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Balance", formatMoneyValue(tx.balance))
        if (isOutflow && tx.transactionCost > 0) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            DetailRow("Cost", formatMoneyValue(tx.transactionCost))
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Date", tx.date)
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Time", tx.time)
    }

    SectionCard(title = "NOTES") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                "Alias",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.35f)
            )
            Text(
                tx.nickName ?: "",
                fontSize = 14.sp,
                fontWeight = if (tx.nickName.isNullOrBlank()) FontWeight.Normal else FontWeight.Medium,
                color = if (tx.nickName.isNullOrBlank())
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleAlias, modifier = Modifier.size(32.dp)) {
                Icon(
                    painterResource(R.drawable.edit), "Edit alias", Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (editAliasActive) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = viewModel::onChangeNickname,
                label = { Text("Alias") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onToggleAlias) { Text("Discard") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = uiState.updatingAliasStatus != UpdatingAliasStatus.LOADING && uiState.nickname.isNotBlank(),
                    onClick = viewModel::updateEntityNickname
                ) {
                    if (uiState.updatingAliasStatus == UpdatingAliasStatus.LOADING)
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save")
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(
                "Comment",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.35f).padding(top = 2.dp)
            )
            Text(
                tx.comment ?: "",
                fontSize = 14.sp,
                color = if (tx.comment.isNullOrBlank())
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = onToggleComment, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painterResource(R.drawable.edit), "Edit comment", Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (!tx.comment.isNullOrBlank()) {
                    IconButton(onClick = onDeleteComment, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painterResource(R.drawable.baseline_clear_24), "Delete comment", Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        if (editCommentActive) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = viewModel::onChangeComment,
                label = { Text("Comment") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onToggleComment) { Text("Discard") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = uiState.updatingCommentStatus != UpdatingCommentStatus.LOADING,
                    onClick = viewModel::updateTransactionComment
                ) {
                    if (uiState.updatingCommentStatus == UpdatingCommentStatus.LOADING)
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save")
                }
            }
        }
    }

    // categories section
    ElevatedCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "CATEGORIES",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onAddToCategory,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (tx.categories.isNullOrEmpty()) {
                Text(
                    "No categories yet. Tap Add to categorise.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tx.categories.forEachIndexed { index, cat ->
                        val (contentCol, containerCol) = categoryChipColors(index)
                        val isLocked = !uiState.isPremium && index > 0
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .then(if (isLocked) Modifier.alpha(0.45f) else Modifier)
                                .clickable { onCategoryClick(cat, index) },
                            shape = RoundedCornerShape(20.dp),
                            color = containerCol,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp)
                            ) {
                                if (isLocked) {
                                    Icon(
                                        painterResource(R.drawable.lock),
                                        contentDescription = "Premium",
                                        modifier = Modifier.size(12.dp),
                                        tint = contentCol
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = contentCol)
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(contentCol.copy(alpha = 0.15f))
                                        .clickable { onCategoryRemove(cat) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.baseline_clear_24),
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(10.dp),
                                        tint = contentCol
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedButton(
            onClick = onPrintInvoice,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(painterResource(R.drawable.receipt), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Print Invoice")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            onClick = onDeleteTransaction,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (uiState.deletingTransactionStatus == DeletingTransactionStatus.LOADING) {
                CircularProgressIndicator(
                    Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onError
                )
            } else {
                Icon(painterResource(R.drawable.remove), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete Transaction")
            }
        }
    }
}

// ── manual content ────────────────────────────────────────────────────────────

@Composable
private fun ManualTransactionContent(
    uiState: TransactionDetailsScreenUiState,
    onPrintInvoice: () -> Unit,
    onEditTx: () -> Unit,
    onDeleteTx: () -> Unit,
) {
    val context = LocalContext.current
    val tx = uiState.manualTransaction ?: return
    val isOutflow = tx.isOutflow

    HeroCard(
        typeBadge = tx.transactionTypeName,
        date = tx.date.toString(),
        amount = if (isOutflow) -tx.amount else tx.amount,
        entityLabel = tx.memberName,
        time = tx.time?.toString() ?: "",
        isOutflow = isOutflow
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        item {
            SuggestionChip(
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.10f)
                ),
                icon = { Icon(painterResource(R.drawable.copy), null, Modifier.size(15.dp), tint = Color(0xFF1565C0)) },
                label = { Text("Copy Member", fontSize = 12.sp, color = Color(0xFF1565C0)) },
                onClick = { copyToClipboard(context, "Member", tx.memberName) }
            )
        }
        if (tx.description.isNotBlank()) {
            item {
                SuggestionChip(
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF1565C0).copy(alpha = 0.10f)
                    ),
                    icon = { Icon(painterResource(R.drawable.copy), null, Modifier.size(15.dp), tint = Color(0xFF1565C0)) },
                    label = { Text("Copy Note", fontSize = 12.sp, color = Color(0xFF1565C0)) },
                    onClick = { copyToClipboard(context, "Note", tx.description) }
                )
            }
        }
    }

    SectionCard(title = "DETAILS") {
        DetailRow("Member", tx.memberName, onCopy = { copyToClipboard(context, "Member", tx.memberName) })
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Type", tx.transactionTypeName)
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Amount", "${if (!isOutflow) "+" else "-"}${formatMoneyValue(tx.amount)}")
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Date", tx.date.toString())
        if (tx.time != null) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            DetailRow("Time", tx.time.toString())
        }
        if (tx.description.isNotBlank()) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            DetailRow("Note", tx.description)
        }
    }

    val catName = uiState.allCategories
        .find { it.category.id == tx.categoryId }
        ?.category?.name ?: ""
    if (catName.isNotEmpty()) {
        ElevatedCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "CATEGORY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))
                val (contentCol, containerCol) = categoryChipColors(0)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = containerCol,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        catName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentCol,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedButton(
            onClick = onEditTx,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(painterResource(R.drawable.edit), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit Transaction")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onPrintInvoice,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(painterResource(R.drawable.receipt), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Print Invoice")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            onClick = onDeleteTx,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (uiState.deletingManualTxStatus == DeletingTransactionStatus.LOADING) {
                CircularProgressIndicator(
                    Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onError
                )
            } else {
                Icon(painterResource(R.drawable.remove), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete Transaction")
            }
        }
    }
}

// ── shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    typeBadge: String,
    date: String,
    amount: Double,
    entityLabel: String,
    time: String,
    isOutflow: Boolean,
) {
    val gradientColors = if (isOutflow)
        listOf(Color(0xFFFFEBEE), Color(0xFFFCE4EC))
    else
        listOf(Color(0xFFE8F5E9), Color(0xFFE0F7FA))

    val amountColor = if (isOutflow) Color(0xFFC62828) else Color(0xFF2E7D32)
    val badgeContainer = if (isOutflow) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
    val badgeText = if (isOutflow) Color(0xFFB71C1C) else Color(0xFF1B5E20)
    val iconBg = if (isOutflow) Color(0xFFEF9A9A).copy(alpha = 0.35f) else Color(0xFF81C784).copy(alpha = 0.35f)

    ElevatedCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (isOutflow) R.drawable.out_transactions else R.drawable.in_transactions
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = amountColor.copy(alpha = 0.7f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(badgeContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(typeBadge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeText)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(date, fontSize = 12.sp, color = amountColor.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${if (isOutflow) "-" else "+"}${formatMoneyValue(abs(amount))}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(Modifier.height(6.dp))
                Text(entityLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = amountColor.copy(alpha = 0.85f))
                if (time.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(time, fontSize = 12.sp, color = amountColor.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(
                    painterResource(R.drawable.copy),
                    contentDescription = "Copy $label",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        } else {
            Spacer(Modifier.size(28.dp))
        }
    }
}
