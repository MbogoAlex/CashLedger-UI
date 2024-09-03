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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
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

    var addMembersThatContainsEntity by rememberSaveable {
        mutableStateOf(false)
    }

    if(showReviewScreen) {
        Box(
            modifier = Modifier
                .safeDrawingPadding()
        ) {
            MembersReviewScreen(
                searchText = uiState.entity,
                newMembers = uiState.membersToAdd,
                loadingStatus = uiState.loadingStatus,
                onRemoveMember = {
                    viewModel.removeMember(it)
                },
                onConfirm = {
                    viewModel.addMembersToCategory()
                },
                onAddMembersThatContainKeyword ={
                    addMembersThatContainsEntity = !addMembersThatContainsEntity
                    viewModel.addMembersThatContainsEntity(addMembersThatContainsEntity)
                },
                addAllMembersThatContainEntity = uiState.addAllMembersThatContainEntity,
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
                onAddMembersThatContainKeyword ={
                    addMembersThatContainsEntity = !addMembersThatContainsEntity
                    viewModel.addMembersThatContainsEntity(addMembersThatContainsEntity)
                },
                addAllMembersThatContainEntity = uiState.addAllMembersThatContainEntity,
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
    searchText: String,
    addAllMembersThatContainEntity: Boolean,
    onAddMembersThatContainKeyword: () -> Unit,
    selectableMembers: List<TransactionItem>,
    onChangeSearchText: (value: String) -> Unit,
    onAddMember: (member: TransactionItem) -> Unit,
    navigateToReviewScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = Modifier
            .padding(
                horizontal = screenWidth(x = 16.0),
                vertical = screenHeight(x = 8.0)
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
        Text(
            text = "Members will be added from your transactions list. To add a member, search and select from your transactions list",
            fontSize = screenFontSize(x = 14.0).sp
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        OutlinedTextField(
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            label = { Text(
                text = "name / phone number",
                fontSize = screenFontSize(x = 14.0).sp
            ) },
            value = searchText,
            onValueChange = onChangeSearchText,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            items(selectableMembers) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = it.nickName ?: it.entity,
                        fontSize = screenFontSize(x = 14.0).sp,
                        modifier = Modifier
                            .weight(0.8f)
                    )
                    IconButton(
                        onClick = { onAddMember(it) },
                        modifier = Modifier
                            .weight(0.2f)
                    ) {
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            imageVector = Icons.Default.Add, contentDescription = "Add ${if(it.transactionAmount > 0) it.sender else it.recipient}",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }
            }
        }
        if(searchText.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add all members that contain `$searchText`",
                    fontSize = screenFontSize(x = 14.0).sp,
                    modifier = Modifier
                        .weight(0.8f)
                )
                IconButton(
                    onClick = onAddMembersThatContainKeyword,
                    modifier = Modifier
                        .weight(0.2f)
                ) {
                    if(addAllMembersThatContainEntity) {
                        Icon(
                            tint = MaterialTheme.colorScheme.error,
                            painter = painterResource(id = R.drawable.check_box_filled), contentDescription = "No",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    } else {
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            painter = painterResource(id = R.drawable.check_box_blank), contentDescription = "Yes",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }

                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
        }
        Button(
            onClick = navigateToReviewScreen,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Review",
                fontSize = screenFontSize(x = 14.0).sp
            )
        }
    }
}

@Composable
fun MembersReviewScreen(
    searchText: String,
    addAllMembersThatContainEntity: Boolean,
    onAddMembersThatContainKeyword: () -> Unit,
    newMembers: List<TransactionItem>,
    onRemoveMember: (transaction: TransactionItem) -> Unit,
    onConfirm: () -> Unit,
    navigateToMembersAdditionScreen: () -> Unit,
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = navigateToMembersAdditionScreen)
    Column(
        modifier = Modifier
            .padding(
                horizontal = screenWidth(x = 16.0),
                vertical = screenHeight(x = 8.0)
            )
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateToMembersAdditionScreen) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous screen",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )

            }
            Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
            Text(
                text = "Continue",
                fontSize = screenFontSize(x = 14.0).sp
            )
        }
        Text(
            text = "You are about to add the following members:",
            fontSize = screenFontSize(x = 16.0).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            items(newMembers.distinct()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = it.entity,
                        fontSize = screenFontSize(x = 14.0).sp,
                        modifier = Modifier
                            .weight(0.8f)
                    )
                    IconButton(
                        onClick = {
                            if(addAllMembersThatContainEntity) {
                                Toast.makeText(context, "Unselect add all members that contain `$searchText`", Toast.LENGTH_SHORT).show()
                            } else {
                                onRemoveMember(it)
                            }
                        },
                        modifier = Modifier
                            .weight(0.2f)
                    ) {
                        Icon(
                            tint = MaterialTheme.colorScheme.error,
                            painter = painterResource(id = R.drawable.remove), contentDescription = "Remove ${if(it.transactionAmount > 0) it.sender else it.recipient}",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }
            }
        }
        if(searchText.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add all members that contain `$searchText`",
                    fontSize = screenFontSize(x = 14.0).sp,
                    modifier = Modifier
                        .weight(0.8f)
                )
                IconButton(
                    onClick = onAddMembersThatContainKeyword,
                    modifier = Modifier
                        .weight(0.2f)
                ) {
                    if(addAllMembersThatContainEntity) {
                        Icon(
                            tint = MaterialTheme.colorScheme.error,
                            painter = painterResource(id = R.drawable.check_box_filled), contentDescription = "No",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    } else {
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            painter = painterResource(id = R.drawable.check_box_blank), contentDescription = "Yes",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                    
                }
            }
            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
        }
        Button(
            enabled = newMembers.isNotEmpty() && loadingStatus != LoadingStatus.LOADING,
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(loadingStatus == LoadingStatus.LOADING) {
                Text(
                    text = "Loading...",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            } else {
                Text(
                    text = "Confirm",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MembersReviewScreenPreview() {
    CashLedgerTheme {
        MembersReviewScreen(
            searchText = "",
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MembersAdditionScreenPreview() {
    CashLedgerTheme {
        MembersAdditionScreen(
            searchText = "",
            addAllMembersThatContainEntity = false,
            onAddMembersThatContainKeyword = {},
            selectableMembers = transactions,
            onChangeSearchText = {},
            onAddMember = {},
            navigateToReviewScreen = {},
            navigateToPreviousScreen = {},
        )
    }
}