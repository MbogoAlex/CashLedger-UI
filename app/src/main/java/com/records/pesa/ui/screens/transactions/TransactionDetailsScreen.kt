package com.records.pesa.ui.screens.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.records.pesa.ui.screens.components.PermissionExplanationDialog
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


data class InvoiceData(
    val entity: String,
    val amount: String,
    val type: String,
    val isOutflow: Boolean,
    val rows: List<Pair<String, String>>,   // label → value
)

private fun buildAndOpenPdf(context: Context, data: InvoiceData) {
    val primaryColor  = android.graphics.Color.parseColor("#006A65")
    val primaryDark   = android.graphics.Color.parseColor("#004D49")
    val accentInflow  = android.graphics.Color.parseColor("#006A65")
    val accentOutflow = android.graphics.Color.parseColor("#C62828")
    val headerColor   = if (data.isOutflow) accentOutflow else accentInflow
    val headerDark    = if (data.isOutflow) android.graphics.Color.parseColor("#8E0000") else primaryDark
    val bgColor       = android.graphics.Color.parseColor("#F4FBF9")
    val white         = android.graphics.Color.WHITE
    val labelColor    = android.graphics.Color.parseColor("#6B9E9B")
    val valueColor    = android.graphics.Color.parseColor("#1A2E2D")
    val divColor      = android.graphics.Color.parseColor("#E0F5F3")

    // A5 page in points (72dpi)
    val pageW = 420f
    val pageH = 595f

    val pdfDoc = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), 1).create()
    val page = pdfDoc.startPage(pageInfo)
    val canvas: android.graphics.Canvas = page.canvas

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // ── Background ──────────────────────────────────────────────────────────────
    paint.color = bgColor
    canvas.drawRect(0f, 0f, pageW, pageH, paint)

    // ── Card shadow (approximate with a soft rect) ───────────────────────────
    val cardL = 24f; val cardR = pageW - 24f
    val cardT = 28f; val cardRadius = 18f
    paint.color = android.graphics.Color.argb(30, 0, 106, 101)
    canvas.drawRoundRect(android.graphics.RectF(cardL + 2f, cardT + 4f, cardR + 2f, pageH - 28f + 4f), cardRadius, cardRadius, paint)

    // ── Card background ───────────────────────────────────────────────────────
    paint.color = white
    canvas.drawRoundRect(android.graphics.RectF(cardL, cardT, cardR, pageH - 28f), cardRadius, cardRadius, paint)

    // ── Header gradient (approximate with two rects) ─────────────────────────
    val headerH = 148f
    val headerRect = android.graphics.RectF(cardL, cardT, cardR, cardT + headerH)
    val shader = android.graphics.LinearGradient(
        cardL, cardT, cardR, cardT + headerH,
        headerColor, headerDark,
        android.graphics.Shader.TileMode.CLAMP
    )
    paint.shader = shader
    // clip to card corners for top
    val path = android.graphics.Path().apply {
        addRoundRect(android.graphics.RectF(cardL, cardT, cardR, cardT + headerH + cardRadius), cardRadius, cardRadius, android.graphics.Path.Direction.CW)
        addRect(android.graphics.RectF(cardL, cardT + headerH - cardRadius, cardR, cardT + headerH + cardRadius), android.graphics.Path.Direction.CW)
    }
    canvas.save()
    canvas.clipRect(cardL, cardT, cardR, cardT + headerH)
    canvas.drawRoundRect(android.graphics.RectF(cardL, cardT, cardR, cardT + headerH + cardRadius * 2), cardRadius, cardRadius, paint)
    canvas.restore()
    paint.shader = null

    // ── Decorative circle in header ───────────────────────────────────────────
    paint.color = android.graphics.Color.argb(30, 255, 255, 255)
    canvas.drawCircle(cardR - 10f, cardT + headerH - 10f, 60f, paint)
    canvas.drawCircle(cardR + 10f, cardT + 20f, 30f, paint)

    // ── Header text ──────────────────────────────────────────────────────────
    paint.shader = null
    paint.color = android.graphics.Color.argb(180, 255, 255, 255)
    paint.textSize = 8f
    paint.letterSpacing = 0.15f
    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    canvas.drawText("CASH LEDGER  ·  RECEIPT", cardL + 18f, cardT + 22f, paint)

    paint.letterSpacing = 0f
    paint.color = white
    paint.textSize = 15f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    // Entity — truncate if too long
    val maxEntityW = cardR - cardL - 36f
    val entityText = if (paint.measureText(data.entity) > maxEntityW)
        data.entity.take(28) + "…" else data.entity
    canvas.drawText(entityText, cardL + 18f, cardT + 52f, paint)

    paint.textSize = 28f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    canvas.drawText(data.amount, cardL + 18f, cardT + 86f, paint)

    // Badge
    paint.textSize = 9f
    paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
    val badgeText = data.type.uppercase()
    val badgeW = paint.measureText(badgeText) + 20f
    val badgeRect = android.graphics.RectF(cardL + 18f, cardT + 100f, cardL + 18f + badgeW, cardT + 118f)
    paint.color = android.graphics.Color.argb(60, 255, 255, 255)
    canvas.drawRoundRect(badgeRect, 9f, 9f, paint)
    paint.color = white
    canvas.drawText(badgeText, cardL + 28f, cardT + 113f, paint)

    // ── Dashed divider ────────────────────────────────────────────────────────
    paint.color = divColor
    paint.strokeWidth = 1f
    paint.style = android.graphics.Paint.Style.STROKE
    val dashEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 6f), 0f)
    paint.pathEffect = dashEffect
    canvas.drawLine(cardL + 16f, cardT + headerH, cardR - 16f, cardT + headerH, paint)
    paint.pathEffect = null
    paint.style = android.graphics.Paint.Style.FILL

    // ── Data rows ─────────────────────────────────────────────────────────────
    var rowY = cardT + headerH + 14f
    val rowH = 26f
    val labelX = cardL + 18f
    val valueX = cardR - 18f

    data.rows.forEachIndexed { i, (label, value) ->
        if (i > 0) {
            paint.color = divColor
            canvas.drawLine(cardL + 16f, rowY - 4f, cardR - 16f, rowY - 4f, paint)
        }
        paint.color = labelColor
        paint.textSize = 10f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText(label, labelX, rowY + 12f, paint)

        paint.color = valueColor
        paint.textSize = 10.5f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        val valText = if (paint.measureText(value) > (cardR - cardL - 120f)) value.take(32) + "…" else value
        canvas.drawText(valText, valueX, rowY + 12f, paint)
        paint.textAlign = android.graphics.Paint.Align.LEFT

        rowY += rowH
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    val footerY = pageH - 28f - 22f
    paint.color = divColor
    canvas.drawLine(cardL + 16f, footerY, cardR - 16f, footerY, paint)
    paint.color = labelColor
    paint.textSize = 8f
    paint.typeface = android.graphics.Typeface.DEFAULT
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("POWERED BY CASH LEDGER", pageW / 2f, footerY + 14f, paint)
    paint.textAlign = android.graphics.Paint.Align.LEFT

    pdfDoc.finishPage(page)

    try {
        val invoiceDir = java.io.File(context.cacheDir, "invoices")
        invoiceDir.mkdirs()
        val file = java.io.File(invoiceDir, "invoice_${System.currentTimeMillis()}.pdf")
        java.io.FileOutputStream(file).use { pdfDoc.writeTo(it) }
        pdfDoc.close()

        val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.records.pesa.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        pdfDoc.close()
        Toast.makeText(context, "Could not open PDF viewer", Toast.LENGTH_SHORT).show()
    }
}


// ── category chip palette ─────────────────────────────────────────────────────

private fun TransactionItem.toInvoiceData(includeBalance: Boolean = true): InvoiceData {
    val isOutflow = transactionAmount < 0
    fun fmtDate(raw: String) = try {
        java.time.LocalDate.parse(raw)
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH))
    } catch (_: Exception) { raw }
    val rows = buildList {
        add("Code" to transactionCode)
        add("Entity" to entity)
        add("Date" to fmtDate(date))
        add("Time" to time.take(5))
        if (includeBalance) add("Balance after" to formatMoneyValue(balance))
        if (isOutflow && transactionCost > 0) add("Fee" to formatMoneyValue(transactionCost))
        if (!categories.isNullOrEmpty()) add("Categories" to categories.joinToString(" · ") { it.name })
        if (!comment.isNullOrBlank()) add("Note" to comment!!)
    }
    return InvoiceData(
        entity = nickName?.ifBlank { null } ?: entity,
        amount = "${if (isOutflow) "−" else "+"}${formatMoneyValue(abs(transactionAmount))}",
        type = transactionType,
        isOutflow = isOutflow,
        rows = rows
    )
}

private fun ManualTransaction.toInvoiceData(): InvoiceData {
    fun fmtDate(raw: String) = try {
        java.time.LocalDate.parse(raw)
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH))
    } catch (_: Exception) { raw }
    val rows = buildList {
        add("Member" to memberName)
        add("Date" to fmtDate(date.toString()))
        if (time != null) add("Time" to time.toString().take(5))
        if (description.isNotBlank()) add("Note" to description)
    }
    return InvoiceData(
        entity = memberName,
        amount = "${if (isOutflow) "−" else "+"}${formatMoneyValue(amount)}",
        type = transactionTypeName,
        isOutflow = isOutflow,
        rows = rows
    )
}

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
    var showDeleteAliasDialog by rememberSaveable { mutableStateOf(false) }
    var showInvoiceOptionsDialog by rememberSaveable { mutableStateOf(false) }

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
        PermissionExplanationDialog(
            icon = R.drawable.lock,
            title = "Premium Required",
            explanation = "Viewing multiple categories for a transaction requires a Cash Ledger Premium subscription.",
            confirmLabel = "Upgrade",
            dismissLabel = "Not now",
            onConfirm = { showUpgradeDialog = false },
            onDismiss = { showUpgradeDialog = false }
        )
    }

    // remove from category confirm dialog
    if (showRemoveFromCatConfirm) {
        PermissionExplanationDialog(
            icon = R.drawable.remove,
            title = "Remove from Category?",
            explanation = "This will remove this transaction's entity from the selected category.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            onConfirm = {
                if (removeFromCatId != -1 && removeKeywordId != -1)
                    viewModel.removeFromCategory(removeFromCatId, removeKeywordId)
                showRemoveFromCatConfirm = false
                removeFromCatId = -1
                removeKeywordId = -1
            },
            onDismiss = {
                showRemoveFromCatConfirm = false
                removeFromCatId = -1
                removeKeywordId = -1
            }
        )
    }

    // delete transaction dialog
    if (showDeleteTransactionDialog) {
        PermissionExplanationDialog(
            icon = R.drawable.remove,
            title = "Delete Transaction?",
            explanation = "This will permanently delete this transaction. This action cannot be undone.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                viewModel.deleteTransaction(
                    uiState.transaction.transactionId!!,
                    deleteAllInstances
                )
                showDeleteTransactionDialog = false
            },
            onDismiss = { showDeleteTransactionDialog = false }
        )
    }

    // delete alias dialog
    if (showDeleteAliasDialog) {
        PermissionExplanationDialog(
            icon = R.drawable.baseline_clear_24,
            title = "Remove Nickname?",
            explanation = "The nickname for this entity will be cleared for all transactions.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            onConfirm = {
                viewModel.deleteAlias()
                showDeleteAliasDialog = false
            },
            onDismiss = { showDeleteAliasDialog = false }
        )
    }

    // delete comment dialog
    if (showDeleteCommentDialog) {
        PermissionExplanationDialog(
            icon = R.drawable.baseline_clear_24,
            title = "Delete Comment?",
            explanation = "Remove the comment from this transaction?",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                viewModel.onDeleteComment()
                showDeleteCommentDialog = false
            },
            onDismiss = { showDeleteCommentDialog = false }
        )
    }

    // delete manual tx dialog
    if (showDeleteManualTxDialog) {
        PermissionExplanationDialog(
            icon = R.drawable.remove,
            title = "Delete Transaction?",
            explanation = "This manual transaction will be permanently deleted.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                viewModel.deleteManualTx()
                showDeleteManualTxDialog = false
            },
            onDismiss = { showDeleteManualTxDialog = false }
        )
    }

    // invoice options dialog — ask whether to include balance
    if (showInvoiceOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showInvoiceOptionsDialog = false },
            icon = { Icon(painterResource(R.drawable.receipt), null, Modifier.size(28.dp)) },
            title = { Text("Print Invoice", fontWeight = FontWeight.SemiBold) },
            text = { Text("Include the M-PESA balance in the printed invoice?") },
            confirmButton = {
                Button(onClick = {
                    showInvoiceOptionsDialog = false
                    buildAndOpenPdf(context, uiState.transaction.toInvoiceData(includeBalance = true))
                }) { Text("Yes, include") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showInvoiceOptionsDialog = false
                    buildAndOpenPdf(context, uiState.transaction.toInvoiceData(includeBalance = false))
                }) { Text("No, hide it") }
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

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ──────────────────────────────────────────────────────────────
        Surface(shadowElevation = 0.dp, tonalElevation = 0.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = navigateToPreviousScreen) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(22.dp)
                            .scale(scaleX = -1f, scaleY = 1f)
                    )
                }
                Text(
                    text = if (uiState.isManualTransaction) "Manual Transaction" else "Transaction",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
                IconButton(onClick = onShare) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        // ── Content ─────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .animateContentSize()
        ) {
            Spacer(Modifier.height(8.dp))

            if (uiState.isManualTransaction) {
                ManualTransactionContent(
                    uiState = uiState,
                    onPrintInvoice = {
                        uiState.manualTransaction?.let { buildAndOpenPdf(context, it.toInvoiceData()) }
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
                    onDeleteAlias = { showDeleteAliasDialog = true },
                    onToggleComment = { editCommentActive = !editCommentActive },
                    onDeleteComment = { showDeleteCommentDialog = true },
                    onDeleteTransaction = { showDeleteTransactionDialog = true },
                    onAddToCategory = { showAddToCategoryDialog = true },
                    onPrintInvoice = {
                        showInvoiceOptionsDialog = true
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

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(16.dp)
            )
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
    onDeleteAlias: () -> Unit,
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
        val fmtDate = remember(tx.date) {
            try { java.time.LocalDate.parse(tx.date)
                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")) }
            catch (_: Exception) { tx.date }
        }
        val fmtTime = remember(tx.time) { tx.time.take(5) }
        DetailRow("Code", tx.transactionCode, onCopy = { copyToClipboard(context, "Code", tx.transactionCode) })
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Type", tx.transactionType)
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
            DetailRow("Fee", formatMoneyValue(tx.transactionCost))
        }
        // Alias (entity nickname) belongs in Details, not Notes
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text(
                "Nickname",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.35f)
            )
            Text(
                tx.nickName?.ifBlank { "—" } ?: "—",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.55f)
            )
            IconButton(onClick = onToggleAlias, modifier = Modifier.size(28.dp)) {
                Icon(painterResource(R.drawable.edit), "Edit nickname", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            if (!tx.nickName.isNullOrBlank()) {
                IconButton(onClick = onDeleteAlias, modifier = Modifier.size(28.dp)) {
                    Icon(painterResource(R.drawable.baseline_clear_24), "Delete nickname", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (editAliasActive) {
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = viewModel::onChangeNickname,
                label = { Text("Nickname") },
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
    }

    SectionCard(title = "COMMENT") {
        // Actions row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (tx.comment.isNullOrBlank()) {
                Text(
                    "No comment yet",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    tx.comment,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            IconButton(onClick = onToggleComment, modifier = Modifier.size(32.dp)) {
                Icon(painterResource(R.drawable.edit), "Edit", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            if (!tx.comment.isNullOrBlank()) {
                IconButton(onClick = onDeleteComment, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(R.drawable.baseline_clear_24), "Delete", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "CATEGORIES",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
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
            Spacer(Modifier.height(6.dp))
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
                                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp)
                            ) {
                                if (isLocked) {
                                    Icon(
                                        painterResource(R.drawable.lock),
                                        contentDescription = "Premium",
                                        modifier = Modifier.size(14.dp),
                                        tint = contentCol
                                    )
                                    Spacer(Modifier.width(5.dp))
                                }
                                Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = contentCol)
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(contentCol.copy(alpha = 0.15f))
                                        .clickable { onCategoryRemove(cat) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.baseline_clear_24),
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(12.dp),
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

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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
        val fmtDate = remember(tx.date) {
            try { tx.date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")) }
            catch (_: Exception) { tx.date.toString() }
        }
        DetailRow("Member", tx.memberName, onCopy = { copyToClipboard(context, "Member", tx.memberName) })
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Type", tx.transactionTypeName)
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Amount", "${if (!isOutflow) "+" else "-"}${formatMoneyValue(tx.amount)}")
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
        DetailRow("Date", fmtDate)
        if (tx.time != null) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            DetailRow("Time", tx.time.toString().take(5))
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

    // Format date: "2026-03-24" → "24 Mar 2026"
    val displayDate = remember(date) {
        try {
            val ld = java.time.LocalDate.parse(date)
            ld.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
        } catch (_: Exception) { date }
    }
    // Format time: "12:24:15" → "12:24"
    val displayTime = remember(time) { time.take(5) }

    ElevatedCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
        ) {
            // Decorative icon — bottom-end so it doesn't clash with text
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (isOutflow) R.drawable.out_transactions else R.drawable.in_transactions
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = amountColor.copy(alpha = 0.6f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Type badge + date on same row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(badgeContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(typeBadge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeText)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            displayDate,
                            fontSize = 11.sp,
                            color = amountColor.copy(alpha = 0.65f)
                        )
                        if (displayTime.isNotBlank()) {
                            Text("·", fontSize = 11.sp, color = amountColor.copy(alpha = 0.4f))
                            Text(
                                displayTime,
                                fontSize = 11.sp,
                                color = amountColor.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Amount
                Text(
                    text = "${if (isOutflow) "-" else "+"}${formatMoneyValue(abs(amount))}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(Modifier.height(4.dp))
                // Entity
                Text(
                    entityLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor.copy(alpha = 0.85f)
                )
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
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
            .padding(vertical = 6.dp)
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
