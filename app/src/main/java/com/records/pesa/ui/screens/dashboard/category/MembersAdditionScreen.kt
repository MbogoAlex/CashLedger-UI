package com.records.pesa.ui.screens.dashboard.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.records.pesa.R
import com.records.pesa.models.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.theme.CashLedgerTheme

object MembersAdditionScreenDestination: AppNavigation {
    override val title: String = "Membership addition screen"
    override val route: String = "membership-addition-screen"
    val categoryId: String = "categoryId"
    val routeWithArgs: String = "$route/{$categoryId}"
}
@Composable
fun MembersAdditionScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {

    val existingMembers by rememberSaveable {
        mutableStateOf(mutableListOf<TransactionItem>())
    }

    val newMembers by rememberSaveable {
        mutableStateOf(mutableListOf<TransactionItem>())
    }
    val selectableMembers by rememberSaveable {
        mutableStateOf(mutableListOf<TransactionItem>())
    }
    for(transaction in transactions) {
        if(!existingMembers.contains(transaction) && !newMembers.contains(transaction)) {
            selectableMembers.add(transaction)
        }
    }

    var showReviewScreen by rememberSaveable {
        mutableStateOf(false)
    }

    if(showReviewScreen) {
        Box(
            modifier = Modifier
                .safeDrawingPadding()
        ) {
            MembersReviewScreen(
                newMembers = newMembers,
                onRemoveMember = {
                    selectableMembers.add(newMembers[it])
                    newMembers.removeAt(it)
                },
                onConfirm = { /*TODO*/ },
                navigateToPreviousScreen = {
                    showReviewScreen = !showReviewScreen
                }
            )
        }
    } else {
        Box(
            modifier = Modifier
                .safeDrawingPadding()
        ) {
            MembersAdditionScreen(
                selectableMembers = selectableMembers,
                onAddMember = {
                    newMembers.add(it)
                    selectableMembers.remove((it))
                },
                navigateToReviewScreen = {
                    showReviewScreen = !showReviewScreen
                },
                navigateToPreviousScreen = navigateToPreviousScreen
            )
        }
    }
}

@Composable
fun MembersAdditionScreen(
    selectableMembers: List<TransactionItem>,
    onAddMember: (member: TransactionItem) -> Unit,
    navigateToReviewScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
            }
        }
        Text(text = "Members will be added from your transactions list. To add a member, search and select from your transactions list")
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            label = { Text(text = "name / phone number") },
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            items(selectableMembers.distinct()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if(it.transactionAmount > 0) it.sender else it.recipient)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onAddMember(it) }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            imageVector = Icons.Default.Add, contentDescription = "Add ${if(it.transactionAmount > 0) it.sender else it.recipient}"
                        )
                    }
                }
            }
        }
        Button(
            onClick = navigateToReviewScreen,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = "Review")
        }
    }
}

@Composable
fun MembersReviewScreen(
    newMembers: List<TransactionItem>,
    onRemoveMember: (index: Int) -> Unit,
    onConfirm: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = navigateToPreviousScreen)
    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")

            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = "Continue")
        }
        Text(
            text = "You are about to add the following members:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            items(newMembers.distinct()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if(it.transactionAmount > 0) it.sender else it.recipient)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemoveMember(newMembers.indexOf(it)) }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.error,
                            painter = painterResource(id = R.drawable.remove), contentDescription = "Remove ${if(it.transactionAmount > 0) it.sender else it.recipient}"
                        )
                    }
                }
            }
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = "Confirm")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MembersReviewScreenPreview() {
    CashLedgerTheme {
        MembersReviewScreen(
            newMembers = transactions,
            onRemoveMember = {},
            onConfirm = {},
            navigateToPreviousScreen = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MembersAdditionScreenPreview() {
    CashLedgerTheme {
        MembersAdditionScreen(
            selectableMembers = transactions,
            onAddMember = {},
            navigateToReviewScreen = {},
            navigateToPreviousScreen = {},
        )
    }
}