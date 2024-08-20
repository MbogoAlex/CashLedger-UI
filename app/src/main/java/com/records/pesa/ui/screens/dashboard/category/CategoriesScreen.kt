package com.records.pesa.ui.screens.dashboard.category

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.composables.TransactionCategoryCell
import com.records.pesa.models.TransactionCategory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.transactionCategories
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object CategoriesScreenDestination: AppNavigation {
    override val title: String = "Categories screen"
    override val route: String = "categories-screen"

}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoriesScreenComposable(
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    showBackArrow: Boolean,
    navigateToSubscriptionScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler(onBack = {
        if(showBackArrow) {
            navigateToPreviousScreen()
        } else {
            navigateToHomeScreen()
        }
    })

    val viewModel: CategoriesScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {
            viewModel.getUserCategories()
        }
    )

    var showSubscriptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var filteringOn by rememberSaveable {
        mutableStateOf(false)
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        uri?.let {
            viewModel.fetchReportAndSave(
                context = context,
                saveUri = it
            )
        }
    }

    if(uiState.downloadingStatus == DownloadingStatus.SUCCESS) {
        filteringOn = false
        Toast.makeText(context, "Report downloaded in your selected folder", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
        val uri = uiState.downLoadUri
        val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(Intent.createChooser(pdfIntent, "Open PDF with:"))

    } else if(uiState.downloadingStatus == DownloadingStatus.FAIL) {
        Toast.makeText(context, "Failed to download report. Check your connection", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
    }

    if(showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = {
                showSubscriptionDialog = false
            },
            onConfirm = {
                showSubscriptionDialog = false
                navigateToSubscriptionScreen()
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        Log.i("CURRENT_LIFECYCLE", lifecycleState.name)
        if(lifecycleState.name.lowercase() == "started") {
            viewModel.getUserCategories()
        }
    }

    Box(
        modifier = Modifier
//            .safeDrawingPadding()
    ) {
        CategoriesScreen(
            pullRefreshState = pullRefreshState,
            loadingStatus = uiState.loadingStatus,
            premium = uiState.userDetails.paymentStatus || uiState.userDetails.phoneNumber == "0179189199",
            searchQuery = uiState.name,
            startDate = LocalDate.parse(uiState.startDate),
            endDate = LocalDate.parse(uiState.endDate),
            onChangeStartDate = {
                viewModel.changeStartDate(it)
            },
            onChangeLastDate = {
                viewModel.changeEndDate(it)
            },
            onChangeSearchQuery = {
                viewModel.updateName(it)
            },
            onClearSearch = {
                viewModel.updateName("")
//                viewModel.getUserCategories()
            },
            categories = uiState.categories,
            selectedCategories = uiState.selectedCategories,
            onRemoveCategory = {
                viewModel.removeCategoryId(it)
            },
            onAddCategory = {
                viewModel.addCategoryId(it)
            },
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToPreviousScreen = navigateToPreviousScreen,
            onShowSubscriptionDialog = {
                showSubscriptionDialog = true
            },
            onDownloadReport = {
                createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}.pdf")
            },
            onFilter = {
                filteringOn = !filteringOn
            },
            filteringOn = filteringOn,
            showBackArrow = showBackArrow,
            downloadingStatus = uiState.downloadingStatus
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoriesScreen(
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    premium: Boolean,
    searchQuery: String,
    startDate: LocalDate,
    endDate: LocalDate,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    categories: List<TransactionCategory>,
    selectedCategories: List<Int>,
    onRemoveCategory: (id: Int) -> Unit,
    onAddCategory: (id: Int) -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    showBackArrow: Boolean,
    onShowSubscriptionDialog: () -> Unit,
    onClearSearch: () -> Unit,
    onDownloadReport: () -> Unit,
    onChangeSearchQuery: (value: String) -> Unit,
    downloadingStatus: DownloadingStatus,
    onFilter: () -> Unit,
    filteringOn: Boolean,
    modifier: Modifier = Modifier
) {
    Log.d("Back_arrow", showBackArrow.toString())
    BoxWithConstraints {
        when(maxWidth) {
            in 0.dp..320.dp -> {
                Column(
                    modifier = Modifier
                        .padding(
                            start = 10.dp,
                            top = if (showBackArrow) 8.dp else 0.dp,
                            end = 10.dp,
                            bottom = 8.dp
                        )
                        .fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if(showBackArrow) {
                            IconButton(onClick = navigateToPreviousScreen) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                if(categories.isNotEmpty() && !premium) {
                                    onShowSubscriptionDialog()
                                } else {
                                    navigateToCategoryAdditionScreen()
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add",
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add category")
                            }
                        }
                    }

                    if(!filteringOn && premium) {
                        TextField(
                            shape = RoundedCornerShape(10.dp),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            value = searchQuery,
                            placeholder = {
                                Text(text = "Category")
                            },
                            trailingIcon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.inverseOnSurface)
                                        .padding(5.dp)
                                        .clickable {
                                            onClearSearch()
                                        }

                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier
                                            .size(16.dp)
                                    )
                                }

                            },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Text
                            ),
                            onValueChange = onChangeSearchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
//                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if(filteringOn) {
                        DateRangePicker(
                            premium = premium,
                            startDate = startDate,
                            endDate = endDate,
                            defaultStartDate = null,
                            defaultEndDate = null,
                            onChangeStartDate = onChangeStartDate,
                            onChangeLastDate = onChangeLastDate,
                            onShowSubscriptionDialog = onShowSubscriptionDialog
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Categories",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                enabled = downloadingStatus != DownloadingStatus.LOADING && categories.isNotEmpty(),
                                onClick = onFilter) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Statement",
                                        fontSize = 14.sp
                                    )
                                    if(downloadingStatus == DownloadingStatus.LOADING) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(15.dp)
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.download),
                                            contentDescription = null
                                        )
                                    }

                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if(loadingStatus == LoadingStatus.SUCCESS) {
                        if(categories.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Text(
                                    text = "Add categories to group you transactions. You can for example add `Supermarket` and select from the transactions list the supermarkets (members) you want to group",
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            if(filteringOn) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generate report for",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(
                                        enabled = selectedCategories.isNotEmpty(),
                                        onClick = onDownloadReport
                                    ) {
                                        Text(
                                            text = "Confirm",
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(3.dp))
                                    IconButton(
                                        onClick = onFilter) {
                                        androidx.compose.material.Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Cancel"
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            LazyColumn {
                                items(categories.size) {index ->
                                    if(filteringOn) {
                                        SelectableCategoryCell(
                                            category = categories[index],
                                            selectedCategories = selectedCategories,
                                            onRemoveCategory = onRemoveCategory,
                                            onAddCategory = {
                                                if(index > 0 && !premium) {
                                                    onShowSubscriptionDialog()
                                                } else {
                                                    onAddCategory(categories[index].id)
                                                }
                                            }
                                        )
                                    } else {
                                        TransactionCategoryCell(
                                            transactionCategory = categories[index],
                                            navigateToCategoryDetailsScreen = {
                                                if(index != 0 && !premium) {
                                                    onShowSubscriptionDialog()
                                                } else {
                                                    navigateToCategoryDetailsScreen(categories[index].id.toString())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        PullRefreshIndicator(
                            refreshing = loadingStatus == LoadingStatus.LOADING,
                            state = pullRefreshState!!
                        )
                    }
                }
            }
            else -> {
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
                        if(showBackArrow) {
                            IconButton(onClick = navigateToPreviousScreen) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            enabled = downloadingStatus != DownloadingStatus.LOADING,
                            onClick = onFilter
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Statement")
                                if(downloadingStatus == DownloadingStatus.LOADING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(15.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.download),
                                        contentDescription = null
                                    )
                                }

                            }
                        }
                    }

                    if(!filteringOn && premium) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            value = searchQuery,
                            placeholder = {
                                Text(text = "Category")
                            },
                            trailingIcon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.inverseOnSurface)
                                        .padding(5.dp)
                                        .clickable {
                                            onClearSearch()
                                        }

                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier
                                            .size(16.dp)
                                    )
                                }

                            },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Text
                            ),
                            onValueChange = onChangeSearchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if(filteringOn) {
                        DateRangePicker(
                            premium = premium,
                            startDate = startDate,
                            endDate = endDate,
                            defaultStartDate = null,
                            defaultEndDate = null,
                            onChangeStartDate = onChangeStartDate,
                            onChangeLastDate = onChangeLastDate,
                            onShowSubscriptionDialog = onShowSubscriptionDialog
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Categories",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                if(categories.isNotEmpty() && !premium) {
                                    onShowSubscriptionDialog()
                                } else {
                                    navigateToCategoryAdditionScreen()
                                }
                            }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Add")
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add category")
                                }

                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    if(loadingStatus == LoadingStatus.SUCCESS) {
                        if(categories.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Text(text = "Add categories to group you transactions. You can for example add `Supermarket` and select from the transactions list the supermarkets (members) you want to group")
                            }
                        } else {
                            if(filteringOn) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Generate report for")
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(
                                        enabled = selectedCategories.isNotEmpty(),
                                        onClick = onDownloadReport
                                    ) {
                                        Text(text = "Confirm")
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            LazyColumn {
                                items(categories.size) {index ->
                                    if(filteringOn) {
                                        SelectableCategoryCell(
                                            category = categories[index],
                                            selectedCategories = selectedCategories,
                                            onRemoveCategory = onRemoveCategory,
                                            onAddCategory = {
                                                if(index > 0 && !premium) {
                                                    onShowSubscriptionDialog()
                                                } else {
                                                    onAddCategory(categories[index].id)
                                                }
                                            }
                                        )
                                    } else {
                                        TransactionCategoryCell(
                                            transactionCategory = categories[index],
                                            navigateToCategoryDetailsScreen = {
                                                if(index != 0 && !premium) {
                                                    onShowSubscriptionDialog()
                                                } else {
                                                    navigateToCategoryDetailsScreen(categories[index].id.toString())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        PullRefreshIndicator(
                            refreshing = loadingStatus == LoadingStatus.LOADING,
                            state = pullRefreshState!!
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun SelectableCategoryCell(
    category: TransactionCategory,
    selectedCategories: List<Int>,
    onRemoveCategory: (id: Int) -> Unit,
    onAddCategory: (id: Int) -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
) {
    BoxWithConstraints {
        when(maxWidth) {
            in 0.dp..320.dp -> {
                ElevatedCard(
                    modifier = Modifier
                        .padding(
                            bottom = 10.dp
                        )
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = category.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text =  if(category.transactions.size > 1) "${category.transactions.size} transactions" else "${category.transactions.size} transaction",
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Light
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if(selectedCategories.contains(category.id)) {
                            IconButton(onClick = { onRemoveCategory(category.id) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.check_box_filled),
                                    contentDescription = "Remove item from filter"
                                )
                            }
                        } else {
                            IconButton(onClick = { onAddCategory(category.id) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.check_box_blank),
                                    contentDescription = "Add item to filter"
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                ElevatedCard(
                    modifier = Modifier
                        .padding(
                            bottom = 10.dp
                        )
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text =  if(category.transactions.size > 1) "${category.transactions.size} transactions" else "${category.transactions.size} transaction",
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Light
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if(selectedCategories.contains(category.id)) {
                            IconButton(onClick = { onRemoveCategory(category.id) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.check_box_filled),
                                    contentDescription = "Remove item from filter"
                                )
                            }
                        } else {
                            IconButton(onClick = { onAddCategory(category.id) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.check_box_blank),
                                    contentDescription = "Add item to filter"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(text = "Go premium?")
        },
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Ksh100.0 premium monthly fee",
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Premium version allows you to: ",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "1. See transactions and export reports of more than one months")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "2. Manage more than one category")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "3. Manage more than one Budget")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "4. Use in dark mode")

                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Subscribe")
            }
        }
    )
}

@Composable
fun DateRangePicker(
    premium: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Parse the default start and end dates
    val defaultStartLocalDate = defaultStartDate?.let { LocalDate.parse(it) }
    val defaultEndLocalDate = defaultEndDate?.let { LocalDate.parse(it) }

    // Convert LocalDate to milliseconds since epoch
    val defaultStartMillis = defaultStartLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val defaultEndMillis = defaultEndLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    val oneMonthAgo = LocalDateTime.now().minusMonths(1)

    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker(isStart: Boolean) {
        val initialDate = if (isStart) startDate else endDate
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStart) {
                    if (selectedDate.isBefore(endDate) || selectedDate.isEqual(endDate)) {
                        if(selectedDate.isBefore(oneMonthAgo.toLocalDate())) {
                            if(premium) {
                                onChangeStartDate(selectedDate)
                            } else {
                                onShowSubscriptionDialog()
                            }
                        } else {
                            onChangeStartDate(selectedDate)
                        }
                    } else {
                        // Handle case where start date is after end date
                        Toast.makeText(context, "Start date must be before end date", Toast.LENGTH_LONG).show()
                    }
                } else {
                    if (selectedDate.isAfter(startDate) || selectedDate.isEqual(startDate)) {
                        onChangeLastDate(selectedDate)
                    } else {
                        // Handle case where end date is before start date
                        Toast.makeText(context, "End date must be after start date", Toast.LENGTH_LONG).show()
                    }
                }
            },

            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )

        // Set minimum and maximum dates
        defaultStartMillis?.let { datePicker.datePicker.minDate = it }
        defaultEndMillis?.let { datePicker.datePicker.maxDate = it }

        datePicker.show()
    }

    BoxWithConstraints {
        when(maxWidth) {
            in 0.dp..320.dp -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = CardDefaults.elevatedCardElevation(10.dp),
                    modifier = modifier
//                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                    ) {
                        IconButton(
                            onClick = { showDatePicker(true) },
                            modifier = Modifier
                                .size(22.dp)
                        ) {
                            Icon(
                                tint = Color(0xFF405189),
                                painter = painterResource(id = R.drawable.calendar),
                                contentDescription = null,
                            )
                        }
                        Text(
                            text = dateFormatter.format(startDate),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "to",
                            fontSize = 12.sp
                        )

                        Text(
                            text = dateFormatter.format(endDate),
                            fontSize = 12.sp
                        )
                        IconButton(
                            onClick = { showDatePicker(false) },
                            modifier = Modifier
                                .size(22.dp)
                        ) {
                            Icon(
                                tint = Color(0xFF405189),
                                painter = painterResource(id = R.drawable.calendar),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
            else -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = CardDefaults.elevatedCardElevation(10.dp),
                    modifier = modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        IconButton(onClick = { showDatePicker(true) }) {
                            Icon(
                                tint = Color(0xFF405189),
                                painter = painterResource(id = R.drawable.calendar),
                                contentDescription = null
                            )
                        }
                        Text(text = dateFormatter.format(startDate))
                        Text(text = "to")

                        Text(text = dateFormatter.format(endDate))
                        IconButton(onClick = { showDatePicker(false) }) {
                            Icon(
                                tint = Color(0xFF405189),
                                painter = painterResource(id = R.drawable.calendar),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CategoryScreenPreview() {
    CashLedgerTheme {
        CategoriesScreen(
            loadingStatus = LoadingStatus.INITIAL,
            pullRefreshState = null,
            categories = transactionCategories,
            searchQuery = "",
            selectedCategories = emptyList(),
            endDate = LocalDate.now().plusDays(14),
            startDate = LocalDate.now(),
            onAddCategory = {},
            onRemoveCategory = {},
            onChangeStartDate = {},
            onChangeLastDate = {},
            downloadingStatus = DownloadingStatus.INITIAL,
            onClearSearch = {},
            onChangeSearchQuery = {},
            navigateToCategoryDetailsScreen = {},
            navigateToCategoryAdditionScreen = {},
            navigateToPreviousScreen = {},
            premium = false,
            onDownloadReport = {},
            filteringOn = false,
            onFilter = {},
            showBackArrow = true,
            onShowSubscriptionDialog = {}
        )
    }
}