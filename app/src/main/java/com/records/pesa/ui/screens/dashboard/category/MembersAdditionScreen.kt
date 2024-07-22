package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.models.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.theme.CashLedgerTheme

object MembersAdditionScreenDestination: AppNavigation {
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

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Members added", Toast.LENGTH_SHORT).show()
        navigateToPreviousScreen()
        viewModel.resetLoadingStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed. Check your connection or try later", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
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
                newMembers = uiState.membersToAdd,
                loadingStatus = uiState.loadingStatus,
                onRemoveMember = {
                    viewModel.removeMember(it)
                },
                onConfirm = {
                    viewModel.addMembersToCategory()
                },
                navigateToMembersAdditionScreen = {
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
                selectableMembers = uiState.membersToDisplay,
                searchText = uiState.entity,
                onChangeSearchText = {
                    viewModel.updateSearchText(it)
                },
                onAddMember = {
                    viewModel.addMember(it)
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
    searchText: String = "",
    onChangeSearchText: (value: String) -> Unit,
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
            value = searchText,
            onValueChange = onChangeSearchText,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
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
                    Text(text = it.nickName ?: if(it.transactionAmount > 0) if(it.sender.length > 25) "${it.sender.substring(0, 25)}..." else it.sender else if(it.recipient.length > 25) "${it.recipient.substring(0, 25)}..." else it.recipient)
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
    onRemoveMember: (transaction: TransactionItem) -> Unit,
    onConfirm: () -> Unit,
    navigateToMembersAdditionScreen: () -> Unit,
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = navigateToMembersAdditionScreen)
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
            IconButton(onClick = navigateToMembersAdditionScreen) {
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
                    Text(text = if(it.transactionAmount > 0) if(it.sender.length > 25) "${it.sender.substring(0, 25)}..." else it.sender else if(it.recipient.length > 25) "${it.recipient.substring(0, 25)}..." else it.recipient)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemoveMember(it) }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.error,
                            painter = painterResource(id = R.drawable.remove), contentDescription = "Remove ${if(it.transactionAmount > 0) it.sender else it.recipient}"
                        )
                    }
                }
            }
        }
        Button(
            enabled = newMembers.isNotEmpty() && loadingStatus != LoadingStatus.LOADING,
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(loadingStatus == LoadingStatus.LOADING) {
                Text(text = "Loading...")
            } else {
                Text(text = "Confirm")
            }

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MembersReviewScreenPreview() {
    CashLedgerTheme {
        MembersReviewScreen(
            newMembers = transactions,
            loadingStatus = LoadingStatus.INITIAL,
            onRemoveMember = {},
            onConfirm = {},
            navigateToMembersAdditionScreen = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MembersAdditionScreenPreview() {
    CashLedgerTheme {
        MembersAdditionScreen(
            selectableMembers = transactions,
            searchText = "",
            onChangeSearchText = {},
            onAddMember = {},
            navigateToReviewScreen = {},
            navigateToPreviousScreen = {},
        )
    }
}