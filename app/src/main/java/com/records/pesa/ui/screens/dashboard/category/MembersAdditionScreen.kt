package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.screens.components.txAvatarColor
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.launch

object MembersAdditionScreenDestination : AppNavigation {
    override val title: String = "Membership addition screen"
    override val route: String = "membership-addition-screen"
    val categoryId: String = "categoryId"
    val routeWithArgs: String = "$route/{$categoryId}"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MembersAdditionScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MembersAdditionScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Members added successfully", Toast.LENGTH_SHORT).show()
        navigateToPreviousScreen()
        viewModel.resetLoadingStatus()
    } else if (uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed to add members. Try again.", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    LaunchedEffect(uiState.manualMemberAdded) {
        if (uiState.manualMemberAdded) {
            viewModel.clearManualMemberAdded()
        }
    }

    var showAfterAddDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.manualMemberAdded) {
        if (uiState.manualMemberAdded) {
            showAfterAddDialog = true
            viewModel.clearManualMemberAdded()
        }
    }

    if (showAfterAddDialog) {
        AlertDialog(
            onDismissRequest = { showAfterAddDialog = false },
            title = { Text("Member added!", fontWeight = FontWeight.Bold) },
            text = {
                Text("The member has been added to this category. Would you like to add another non-M-PESA member, or go back to the category?")
            },
            confirmButton = {
                Button(onClick = { showAfterAddDialog = false }) {
                    Text("Add another")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAfterAddDialog = false; navigateToPreviousScreen() }) {
                    Text("Go to category")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    var showReviewScreen by rememberSaveable { mutableStateOf(false) }
    var addMembersThatContainsEntity by rememberSaveable { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
        if (showReviewScreen) {
            Box(modifier = Modifier.safeDrawingPadding()) {
                MembersReviewScreen(
                    categoryName = uiState.category.name,
                    searchText = uiState.entity,
                    newMembers = uiState.membersToAdd,
                    loadingStatus = uiState.loadingStatus,
                    addAllMembersThatContainEntity = uiState.addAllMembersThatContainEntity,
                    onRemoveMember = { viewModel.removeMember(it) },
                    onConfirm = { viewModel.addMembersToCategory() },
                    onAddMembersThatContainKeyword = {
                        addMembersThatContainsEntity = !addMembersThatContainsEntity
                        viewModel.addMembersThatContainsEntity(addMembersThatContainsEntity)
                    },
                    onSetLinkMode = { txId, isLinked -> viewModel.setMemberLinkMode(txId, isLinked) },
                    navigateToMembersAdditionScreen = { showReviewScreen = false }
                )
            }
        } else {
            Box(modifier = Modifier.safeDrawingPadding()) {
                MembersAdditionScreen(
                    categoryName = uiState.category.name,
                    selectableMembers = uiState.membersToDisplay,
                    selectedCount = uiState.membersToAdd.size,
                    searchText = uiState.entity,
                    addAllMembersThatContainEntity = uiState.addAllMembersThatContainEntity,
                    onAddMembersThatContainKeyword = {
                        addMembersThatContainsEntity = !addMembersThatContainsEntity
                        viewModel.addMembersThatContainsEntity(addMembersThatContainsEntity)
                    },
                    onChangeSearchText = { viewModel.updateSearchText(it) },
                    onAddMember = { viewModel.addMember(it) },
                    onAddManualMember = { viewModel.addManualMember(it) },
                    navigateToReviewScreen = { showReviewScreen = true },
                    navigateToPreviousScreen = navigateToPreviousScreen
                )
            }
        }
    }
}

// ── Search & Select screen ────────────────────────────────────────────────────

@Composable
fun MembersAdditionScreen(
    categoryName: String,
    searchText: String,
    selectedCount: Int,
    addAllMembersThatContainEntity: Boolean,
    onAddMembersThatContainKeyword: () -> Unit,
    selectableMembers: List<TransactionItem>,
    onChangeSearchText: (value: String) -> Unit,
    onAddMember: (member: TransactionItem) -> Unit,
    onAddManualMember: (String) -> Unit = {},
    navigateToReviewScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val categoryColor = txAvatarColor(categoryName.ifBlank { "C" })
    var customMemberName by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {

        // ── App bar ──────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Back",
                    modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = 180f },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Add Members",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            // Selected badge → tapping goes to review
            if (selectedCount > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = categoryColor,
                    modifier = Modifier.clickable { navigateToReviewScreen() }
                ) {
                    Text(
                        text = "$selectedCount selected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            // Step indicator
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Step 2 of 2",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Context banner ───────────────────────────────────────────────
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        categoryColor.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(categoryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = categoryName.take(2).uppercase().ifBlank { "?" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Adding to",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = categoryName.ifBlank { "…" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${selectableMembers.size} available",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Add non-M-PESA member card ───────────────────────────────────
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(
                            Brush.linearGradient(listOf(
                                Color(0xFF1565C0).copy(alpha = 0.08f),
                                Color(0xFF0288D1).copy(alpha = 0.04f)
                            ))
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.account_info),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Add non-M-PESA member", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Add someone who doesn't appear in your M-PESA history — e.g. a cash vendor, shop, or person you pay outside M-PESA.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customMemberName,
                                    onValueChange = { customMemberName = it },
                                    label = { Text("Name (e.g. Mama Mboga, Local Barber)", fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (customMemberName.isNotBlank()) {
                                            onAddManualMember(customMemberName.trim())
                                            customMemberName = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Add") }
                            }
                        }
                    }
                }
            }

            // ── Search field ─────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onChangeSearchText,
                    placeholder = {
                        Text(
                            "Search by name or phone number",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { onChangeSearchText("") }) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_clear_24),
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = categoryColor,
                        focusedLabelColor = categoryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── "Add all matching" toggle ────────────────────────────────────
            if (searchText.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (addAllMembersThatContainEntity)
                            categoryColor.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddMembersThatContainKeyword() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (addAllMembersThatContainEntity) R.drawable.check_box_filled
                                    else R.drawable.check_box_blank
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (addAllMembersThatContainEntity) categoryColor
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add all matching \"$searchText\"",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (addAllMembersThatContainEntity) categoryColor
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Any future transactions with matching entities will also be auto-tagged",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Empty/idle state ─────────────────────────────────────────────
            if (selectableMembers.isEmpty() && searchText.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            "Search to find members",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Type a name or phone number to find contacts from your transaction history",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (selectableMembers.isEmpty() && searchText.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("No results for \"$searchText\"", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "All matching contacts may already be members of this category",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Results list ─────────────────────────────────────────────────
            if (selectableMembers.isNotEmpty()) {
                item {
                    Text(
                        "M-PESA Contacts",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                item {
                    Text(
                        text = "${selectableMembers.size} result${if (selectableMembers.size != 1) "s" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(selectableMembers) { tx ->
                val displayName = tx.nickName?.takeIf { it.isNotBlank() } ?: tx.entity
                val color = txAvatarColor(displayName)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = color.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    tx.transactionType,
                                    fontSize = 10.sp,
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                "KES ${String.format("%,.0f", kotlin.math.abs(tx.transactionAmount))}",
                                fontSize = 11.sp,
                                color = if (tx.transactionAmount > 0) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = { onAddMember(tx) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(categoryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = "Add ${displayName}",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 52.dp)
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // ── Review button ────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Button(
                onClick = navigateToReviewScreen,
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = categoryColor),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    "Review $selectedCount member${if (selectedCount != 1) "s" else ""} →",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ── Review screen ─────────────────────────────────────────────────────────────

@Composable
fun MembersReviewScreen(
    categoryName: String,
    searchText: String,
    addAllMembersThatContainEntity: Boolean,
    onAddMembersThatContainKeyword: () -> Unit,
    newMembers: List<TransactionItem>,
    onRemoveMember: (transaction: TransactionItem) -> Unit,
    onConfirm: () -> Unit,
    navigateToMembersAdditionScreen: () -> Unit,
    loadingStatus: LoadingStatus,
    onSetLinkMode: (transactionId: Int, isLinked: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = navigateToMembersAdditionScreen)
    val categoryColor = txAvatarColor(categoryName.ifBlank { "C" })
    val distinctMembers = newMembers.distinct()
    // Track link mode per transaction (true=link all, false=this only)
    val linkModeState = remember(distinctMembers) {
        mutableMapOf(*distinctMembers.map { (it.transactionId ?: 0) to true }.toTypedArray())
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── App bar ──────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = navigateToMembersAdditionScreen) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Back",
                    modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = 180f },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Review Members",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = categoryColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${distinctMembers.size} to add",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Summary card ─────────────────────────────────────────────────
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(categoryColor.copy(alpha = 0.15f), MaterialTheme.colorScheme.surface)
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(categoryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(categoryName.take(2).uppercase().ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Adding to \"$categoryName\"", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${distinctMembers.size} member${if (distinctMembers.size != 1) "s" else ""} will be added",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (searchText.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (addAllMembersThatContainEntity) categoryColor.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().clickable { onAddMembersThatContainKeyword() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (addAllMembersThatContainEntity) R.drawable.check_box_filled else R.drawable.check_box_blank
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (addAllMembersThatContainEntity) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Also add all containing \"$searchText\"",
                                        fontSize = 12.sp,
                                        color = if (addAllMembersThatContainEntity) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Members list ─────────────────────────────────────────────────
            item {
                Text(
                    "Selected members",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(distinctMembers) { tx ->
                val displayName = tx.nickName?.takeIf { it.isNotBlank() } ?: tx.entity
                val color = txAvatarColor(displayName)
                val txId = tx.transactionId ?: 0
                var isLinked by remember(txId) { mutableStateOf(linkModeState[txId] ?: true) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                displayName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = color.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    tx.transactionType,
                                    fontSize = 10.sp,
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                if (addAllMembersThatContainEntity) {
                                    Toast.makeText(context, "Turn off 'Add all containing' first", Toast.LENGTH_SHORT).show()
                                } else {
                                    onRemoveMember(tx)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.remove),
                                contentDescription = "Remove $displayName",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // Link mode toggle chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 50.dp, bottom = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isLinked) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isLinked) androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.clickable {
                                isLinked = true
                                linkModeState[txId] = true
                                onSetLinkMode(txId, true)
                            }
                        ) {
                            Text(
                                "🔗 Link all",
                                fontSize = 10.sp,
                                fontWeight = if (isLinked) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isLinked) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (!isLinked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (!isLinked) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.clickable {
                                isLinked = false
                                linkModeState[txId] = false
                                onSetLinkMode(txId, false)
                            }
                        ) {
                            Text(
                                "📌 This only",
                                fontSize = 10.sp,
                                fontWeight = if (!isLinked) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isLinked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 52.dp)
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // ── Confirm button ───────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Button(
                enabled = distinctMembers.isNotEmpty() && loadingStatus != LoadingStatus.LOADING,
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = categoryColor),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (loadingStatus == LoadingStatus.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Adding members…", fontSize = 15.sp, color = Color.White)
                } else {
                    Text(
                        "Confirm & Add ${distinctMembers.size} Member${if (distinctMembers.size != 1) "s" else ""} ✓",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MembersAdditionScreenPreview() {
    CashLedgerTheme {
        MembersAdditionScreen(
            categoryName = "School Fees",
            searchText = "equity",
            selectedCount = 2,
            addAllMembersThatContainEntity = false,
            onAddMembersThatContainKeyword = {},
            selectableMembers = transactions,
            onChangeSearchText = {},
            onAddMember = {},
            navigateToReviewScreen = {},
            navigateToPreviousScreen = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MembersReviewScreenPreview() {
    CashLedgerTheme {
        MembersReviewScreen(
            categoryName = "School Fees",
            searchText = "equity",
            addAllMembersThatContainEntity = false,
            onAddMembersThatContainKeyword = {},
            newMembers = transactions,
            loadingStatus = LoadingStatus.INITIAL,
            onRemoveMember = {},
            onConfirm = {},
            navigateToMembersAdditionScreen = {}
        )
    }
}
