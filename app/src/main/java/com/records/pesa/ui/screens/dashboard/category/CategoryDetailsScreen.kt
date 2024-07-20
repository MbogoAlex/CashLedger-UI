package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.models.TransactionCategory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.transactionCategory
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDateTime
object CategoryDetailsScreenDestination: AppNavigation {
    override val title: String = "Category details screen"
    override val route: String = "category-details-screen"
    val categoryId: String = "categoryId"
    val routeWithArgs: String = "$route/{$categoryId}"

}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryDetailsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToMembersAdditionScreen: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CategoryDetailsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        CategoryDetailsScreen(
            category = uiState.category,
            navigateToCategoryBudgetListScreen = {},
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToMembersAdditionScreen = navigateToMembersAdditionScreen
        )
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryDetailsScreen(
    category: TransactionCategory,
    navigateToCategoryBudgetListScreen: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToMembersAdditionScreen: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = navigateToPreviousScreen
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                onClick = { /*TODO*/ }
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.error,
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete category"
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Previous screen")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Created on ${formatIsoDateTime(LocalDateTime.parse(category.createdAt))}")
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budgets (${if (category.budgets.isNotEmpty()) category.budgets.size else 0})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                enabled = category.budgets.isNotEmpty(),
                onClick = {
                    navigateToCategoryBudgetListScreen(category.id.toString())
                }
            ) {
                Text(text = "Explore")
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Members",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { navigateToMembersAdditionScreen(category.id.toString()) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Add")
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add members")
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            items(category.keywords) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = it.nickName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit member"
                        )
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.error,
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove member"
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Transactions")
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "See transactions")
            }
        }

    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CategoryDetailsScreenPreview() {
    CashLedgerTheme {
        CategoryDetailsScreen(
            category = transactionCategory,
            navigateToPreviousScreen = {},
            navigateToCategoryBudgetListScreen = {},
            navigateToMembersAdditionScreen = {}
        )
    }
}