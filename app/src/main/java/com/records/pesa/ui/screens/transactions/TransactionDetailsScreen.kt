package com.records.pesa.ui.screens.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatDate
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.transaction
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlin.math.absoluteValue

object TransactionDetailsScreenDestination: AppNavigation {
    override val title: String = "Transaction details screen"
    override val route: String = "transaction-details-screen"
    val transactionId: String = "transactionId"
    val routeWithArgs: String = "$route/{$transactionId}"
}

@Composable
fun TransactionDetailsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val viewModel: TransactionDetailsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var deleteAllInstanceOffThisTransaction by rememberSaveable {
        mutableStateOf(false)
    }

    var editAliasActive by rememberSaveable {
        mutableStateOf(false)
    }

    var editCommentActive by rememberSaveable {
        mutableStateOf(false)
    }

    var showDeleteTransactionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if(showDeleteTransactionDialog) {
        DeleteTransactionDialog(
            deleteAllInstanceOffThisTransaction = deleteAllInstanceOffThisTransaction,
            onDeleteAllInstances = {
                deleteAllInstanceOffThisTransaction = !deleteAllInstanceOffThisTransaction
            },
            onDismiss = {
                showDeleteTransactionDialog = false
            },
            onConfirm = {
                viewModel.deleteTransaction(uiState.transaction.transactionId!!, deleteAllInstanceOffThisTransaction)
                showDeleteTransactionDialog = false
            }
        )
    }

    if(uiState.updatingAliasStatus == UpdatingAliasStatus.SUCCESS) {
        editAliasActive = !editAliasActive
        Toast.makeText(context, "Alias name updated", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    } else if(uiState.updatingAliasStatus == UpdatingAliasStatus.FAIL) {
        Toast.makeText(context, "Failed to update alias name. Ensure internet connection is active", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    }

    if(uiState.updatingCommentStatus == UpdatingCommentStatus.SUCCESS) {
        editCommentActive = !editCommentActive
        Toast.makeText(context, "Comment updated", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    } else if(uiState.updatingCommentStatus == UpdatingCommentStatus.FAIL) {
        editCommentActive = !editCommentActive
        Toast.makeText(context, "Failed to update comment. Ensure internet connection is active", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    }

    if(uiState.deletingTransactionStatus == DeletingTransactionStatus.SUCCESS) {
        Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
        navigateToPreviousScreen()
        viewModel.resetUpdatingStatus()
    } else if(uiState.deletingTransactionStatus == DeletingTransactionStatus.FAIL) {
        Toast.makeText(context, "Failed to delete transaction. Ensure internet connection is active", Toast.LENGTH_SHORT).show()
        viewModel.resetUpdatingStatus()
    }

    if(showDeleteDialog) {
        DeleteDialog(
            onDismiss = {
                showDeleteDialog = !showDeleteDialog
            },
            onConfirm = {
                showDeleteDialog = !showDeleteDialog
                viewModel.onDeleteComment()
            }
        )
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        TransactionDetailsScreen(
            transactionItem = uiState.transaction,
            alias = uiState.nickname,
            onChangeAlias = {
                viewModel.onChangeNickname(it)
            },
            onSaveAlias = {
                viewModel.updateEntityNickname()
            },
            comment = uiState.comment,
            onChangeComment = {
                viewModel.onChangeComment(it)
            },
            onSaveComment = {
                viewModel.updateTransactionComment()
            },
            editAliasActive = editAliasActive,
            editCommentActive = editCommentActive,
            onToggleEditComment = {
                editCommentActive = !editCommentActive
            },
            onToggleEditAlias = {
                editAliasActive = !editAliasActive
            },
            updatingAliasStatus = uiState.updatingAliasStatus,
            updatingCommentStatus = uiState.updatingCommentStatus,
            deletingTransactionStatus = uiState.deletingTransactionStatus,
            onDeleteComment = {
                showDeleteDialog = !showDeleteDialog
            },
            onDeleteTransaction = {
                showDeleteTransactionDialog = true
            },
            navigateToPreviousScreen = navigateToPreviousScreen
        )
    }
}

@Composable
fun TransactionDetailsScreen(
    editAliasActive: Boolean,
    editCommentActive: Boolean,
    transactionItem: TransactionItem,
    alias: String,
    onChangeAlias: (alias: String) -> Unit,
    onSaveAlias: () -> Unit,
    comment: String,
    onChangeComment: (comment: String) -> Unit,
    onSaveComment: () -> Unit,
    onToggleEditAlias: () -> Unit,
    onToggleEditComment: () -> Unit,
    updatingAliasStatus: UpdatingAliasStatus,
    updatingCommentStatus: UpdatingCommentStatus,
    onDeleteComment: () -> Unit,
    onDeleteTransaction: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    deletingTransactionStatus: DeletingTransactionStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val date = transactionItem.date
    val time = transactionItem.time

    var formattedDatetime = ""

    if(date.isNotEmpty() && time.isNotEmpty()) {
        formattedDatetime = formatDate("$date $time")
    }

    Column(
        modifier = Modifier
            .padding(
                vertical = screenHeight(x = 8.0),
                horizontal = screenWidth(x = 16.0)
            )
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                .verticalScroll(rememberScrollState())
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous screen",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Transaction code: ${transactionItem.transactionCode}",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
                    Text(
                        text = formattedDatetime,
                        fontWeight = FontWeight.Light,
                        fontSize = screenFontSize(x = 12.0).sp,
                        style = TextStyle(
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    onClick = {
                        val clip = ClipData.newPlainText("Transaction Code", transactionItem.transactionCode)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Transaction code copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        painter = painterResource(id = R.drawable.copy),
                        contentDescription = "Copy transaction code",
                        modifier = Modifier
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Row {
                Text(
                    text = "Type:",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = transactionItem.transactionType,
                    fontSize = screenFontSize(x = 14.0).sp,
                )
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Row {
                Text(
                    text = "Amount:",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if(transactionItem.transactionAmount > 0) {
                    Text(
                        text = "+ ${formatMoneyValue(transactionItem.transactionAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.surfaceTint
                    )
                } else if(transactionItem.transactionAmount < 0) {
                    Text(
                        text = "- ${formatMoneyValue(transactionItem.transactionAmount.absoluteValue)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Row {
                Text(
                    text = "Cost:",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if(transactionItem.transactionAmount < 0) {
                    Text(
                        text = "Cost: - ${formatMoneyValue(transactionItem.transactionCost.absoluteValue)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "N/A",
                        fontSize = screenFontSize(x = 14.0).sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Entity:",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(0.4f)
                )
//                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = transactionItem.entity,
                        fontSize = screenFontSize(x = 14.0).sp,
                        modifier = Modifier

                    )
                }

            }
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Alias:",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transactionItem.nickName.takeIf { it != null } ?: "",
                        fontSize = screenFontSize(x = 14.0).sp,
                    )
                    Spacer(modifier = Modifier.width(screenWidth(x = 3.0)))
                    Icon(
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit alias",
                        modifier = Modifier
                            .clickable {
                                onToggleEditAlias()
                            }
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
            if(editAliasActive) {
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                TextField(
                    label = {
                        Text(
                            text = "Alias",
                            fontSize = screenFontSize(x = 14.0).sp,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    value = alias,
                    onValueChange = onChangeAlias,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { onToggleEditAlias() }
                    ) {
                        Text(
                            text = "Discard",
                            fontSize = screenFontSize(x = 14.0).sp,
                        )
                    }
                    Button(
                        enabled = updatingAliasStatus != UpdatingAliasStatus.LOADING && alias.isNotEmpty(),
                        onClick = onSaveAlias
                    ) {
                        if(updatingAliasStatus == UpdatingAliasStatus.LOADING) {
                            Text(
                                text = "Loading...",
                                fontSize = screenFontSize(x = 14.0).sp,
                            )
                        } else {
                            Text(
                                text = "Save",
                                fontSize = screenFontSize(x = 14.0).sp,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Balance:",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatMoneyValue(transactionItem.balance),
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = if(transactionItem.balance > 0) MaterialTheme.colorScheme.surfaceTint else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Comment:",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit comment",
                    modifier = Modifier
                        .clickable {
                            onToggleEditComment()
                        }
                        .size(screenWidth(x = 24.0))
                )
                if(!transactionItem.comment.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
                    Icon(
                        tint = MaterialTheme.colorScheme.error,
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete comment",
                        modifier = Modifier
                            .clickable {
                                onDeleteComment()
                            }
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
            if(transactionItem.comment != null) {
                Text(
                    text = transactionItem.comment,
                    fontSize = screenFontSize(x = 14.0).sp,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if(editCommentActive) {
                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                TextField(
                    label = {
                        Text(
                            text = "Comment",
                            fontSize = screenFontSize(x = 14.0).sp,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    value = comment,
                    onValueChange = onChangeComment,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextButton(onClick = { onToggleEditComment() }) {
                        Text(
                            text = "Discard",
                            fontSize = screenFontSize(x = 14.0).sp,
                        )
                    }
                    Button(
                        enabled = updatingCommentStatus != UpdatingCommentStatus.LOADING && comment.isNotEmpty(),
                        onClick = onSaveComment
                    ) {
                        if(updatingCommentStatus == UpdatingCommentStatus.LOADING) {
                            Text(
                                text = "Loading...",
                                fontSize = screenFontSize(x = 14.0).sp,
                            )
                        } else {
                            Text(
                                text = "Save",
                                fontSize = screenFontSize(x = 14.0).sp,
                            )
                        }

                    }
                }
            }

        }
        Spacer(modifier = Modifier.height(screenHeight(x = 32.0)))
        Text(
            text = "Categories",
            fontWeight = FontWeight.Bold,
            fontSize = screenFontSize(x = 18.0).sp
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
        ) {
            transactionItem.categories?.let {
                items(it.size) {
                    Box(
                        contentAlignment = Alignment.Center,

                        ) {
                        Card() {
                            Text(
                                text = transactionItem.categories[it].name,
                                fontSize = screenFontSize(x = 14.0).sp,
                                modifier = Modifier
                                    .padding(screenWidth(x = 10.0))
                            )
                        }
                    }
                }
            }
        }
        Button(
            enabled = deletingTransactionStatus != DeletingTransactionStatus.LOADING,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            onClick = onDeleteTransaction,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(deletingTransactionStatus == DeletingTransactionStatus.LOADING) {
                Text(
                    text = "Deleting...",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            } else {
                Text(
                    text = "Delete transaction",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    }


}

@Composable
fun DeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = "Delete comment")
        },
        text = {
            Text(text = "Are you sure you want to delete this comment?")
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Confirm")
            }
        }
    )
}

@Composable
fun DeleteTransactionDialog(
    deleteAllInstanceOffThisTransaction: Boolean,
    onDeleteAllInstances: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(
                text = "Delete transaction",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Are you sure you want to delete this transaction? This action is permanent",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier
//                        .padding(screenWidth(x = 10.0))
                ) {
                    if(deleteAllInstanceOffThisTransaction) {
                        IconButton(onClick = onDeleteAllInstances) {
                            Icon(
                                painter = painterResource(id = R.drawable.check_box_filled),
                                contentDescription = "Remove item from filter?",
                                modifier = Modifier
                                    .size(screenWidth(x = 24.0))
                            )
                        }
                    } else {
                        IconButton(onClick = onDeleteAllInstances) {
                            Icon(
                                painter = painterResource(id = R.drawable.check_box_blank),
                                contentDescription = "Add item to filter",
                                modifier = Modifier
                                    .size(screenWidth(x = 24.0))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Delete all instances of this transaction",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                if(deleteAllInstanceOffThisTransaction) {
                    Text(
                        text = "Existing transactions with the same entity name will also be deleted",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                    Text(
                        text = "New transactions with the same entity name will not be added",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }

            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Confirm",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TransactionDetailsScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        TransactionDetailsScreen(
            transactionItem = transaction,
            comment = "",
            onChangeComment = {},
            onChangeAlias = {},
            alias = "",
            onSaveAlias = {},
            onSaveComment = {},
            updatingAliasStatus = UpdatingAliasStatus.INITIAL,
            updatingCommentStatus = UpdatingCommentStatus.INITIAL,
            deletingTransactionStatus = DeletingTransactionStatus.LOADING,
            onToggleEditAlias = {},
            editAliasActive = true,
            editCommentActive = true,
            onToggleEditComment = {},
            onDeleteComment = {},
            onDeleteTransaction = {},
            navigateToPreviousScreen = {}
        )
    }
}