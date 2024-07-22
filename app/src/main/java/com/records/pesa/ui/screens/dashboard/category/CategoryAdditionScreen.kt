package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.theme.CashLedgerTheme

object CategoryAdditionScreenDestination: AppNavigation {
    override val title = "Category addition screen"
    override val route = "category-addition-screen"
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryAdditionScreenComposable(
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: CategoryAdditionScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Category created. You can now add members", Toast.LENGTH_SHORT).show()
        navigateToCategoryDetailsScreen(uiState.categoryId.toString())
        viewModel.resetLoadingStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed to create category. Check connection or try later", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        CategoryAdditionScreen(
            categoryName = uiState.categoryName,
            onChangeCategoryName = {
                viewModel.updateCategoryName(it)
            },
            onCreateCategory = {
                viewModel.createCategory()
            },
            navigateToPreviousScreen = navigateToPreviousScreen,
            loadingStatus = uiState.loadingStatus
        )
    }
}

@Composable
fun CategoryAdditionScreen(
    categoryName: String,
    onChangeCategoryName: (name: String) -> Unit,
    onCreateCategory: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Add category",
                fontWeight = FontWeight.Bold
            )
        }
        OutlinedTextField(
            label = {
                Text(text = "Category name")
            },
            value = categoryName,
            onValueChange = onChangeCategoryName,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            enabled = categoryName.isNotEmpty() && loadingStatus != LoadingStatus.LOADING,
            onClick = onCreateCategory,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(loadingStatus == LoadingStatus.LOADING) {
                Text(text = "Loading...")
            } else {
                Text(text = "Next")
            }
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CategoryAdditionScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        CategoryAdditionScreen(
            categoryName = "",
            onChangeCategoryName = {},
            onCreateCategory = {},
            navigateToPreviousScreen = {},
            loadingStatus = LoadingStatus.INITIAL
        )
    }
}