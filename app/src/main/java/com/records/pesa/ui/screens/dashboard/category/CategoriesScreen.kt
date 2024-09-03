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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
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

    var showDownloadReportDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var reportType by rememberSaveable {
        mutableStateOf("PDF")
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        uri?.let {
            viewModel.fetchReportAndSave(
                context = context,
                saveUri = it,
                reportType = reportType
            )
        }
    }

    if(uiState.downloadingStatus == DownloadingStatus.SUCCESS) {
        filteringOn = false
        Toast.makeText(context, "Report downloaded in your selected folder", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
        val uri = uiState.downLoadUri
        if(reportType == "PDF") {
            val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(Intent.createChooser(pdfIntent, "Open PDF with:"))
        } else if(reportType == "CSV") {
            val csvIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(Intent.createChooser(csvIntent, "Open CSV with:"))
        }

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

    if(showDownloadReportDialog) {
        DownloadReportDialog(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onDismiss = { showDownloadReportDialog = !showDownloadReportDialog },
            onConfirm = { type ->
                reportType = type
                showDownloadReportDialog = !showDownloadReportDialog
                if(reportType == "PDF") {
                    createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}.pdf")
                } else if(reportType == "CSV") {
                    createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}.csv")
                }
            }
        )
    }

//    val lifecycleOwner = LocalLifecycleOwner.current
//    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

//    LaunchedEffect(lifecycleState) {
//        Log.i("CURRENT_LIFECYCLE", lifecycleState.name)
//        if(lifecycleState.name.lowercase() == "started") {
//            viewModel.getUserCategories()
//        }
//    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
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
                showDownloadReportDialog = !showDownloadReportDialog
            },
            onFilter = {
                filteringOn = !filteringOn
            },
            filteringOn = filteringOn,
            showBackArrow = showBackArrow,
            getCategories = {
                viewModel.getUserCategories()
            },
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
    getCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("Back_arrow", showBackArrow.toString())
    var searchingOn by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .padding(
                start = screenWidth(x = 16.0),
                end = screenWidth(x = 16.0),
                top = if (showBackArrow) screenHeight(x = 8.0) else 0.dp,
                bottom = screenHeight(x = 8.0)
            )
            .fillMaxSize()

    ) {
        

        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(showBackArrow) {
                IconButton(onClick = navigateToPreviousScreen) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous screen",
                        modifier = Modifier
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if(!filteringOn) {
                Box(
                    modifier = Modifier
                        .clickable {
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
                            color = MaterialTheme.colorScheme.surfaceTint,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Spacer(modifier = Modifier.width(screenWidth(x = 3.0)))
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add category",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }
            }

        }



        if(filteringOn) {
            Text(
                text = "Select report date range",
                fontSize = screenFontSize(x = 14.0).sp
            )
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
            TextButton(
                onClick = { onFilter() },
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                Text(text = "Cancel")
            }
        } else {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    enabled = downloadingStatus != DownloadingStatus.LOADING && categories.isNotEmpty(),
                    onClick = onFilter
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Statement",
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        if(downloadingStatus == DownloadingStatus.LOADING) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(screenWidth(x = 15.0))
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.download),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(screenWidth(x = 24.0))
                            )
                        }

                    }
                }
                if(premium && !searchingOn && categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(screenWidth(x = 3.0)))
                    IconButton(onClick = { searchingOn = !searchingOn }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search category",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }

            }
            if(!filteringOn && premium && searchingOn) {
//            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextField(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(screenWidth(x = 24.0))
                            )
                        },
                        value = searchQuery,
                        placeholder = {
                            Text(
                                text = "Category",
                                fontSize = screenFontSize(x = 14.0).sp
                            )
                        },
                        trailingIcon = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.inverseOnSurface)
                                    .padding(screenWidth(x = 5.0))
                                    .clickable {
                                        onClearSearch()
                                    }

                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier
                                        .size(screenWidth(x = 16.0))
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
                            .weight(0.9f)
                    )
//                    Spacer(modifier = Modifier.width(screenWidth(x = 3.0)))
                    IconButton(
                        onClick = { searchingOn = !searchingOn },
                        modifier = Modifier
                            .weight(0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop searching",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if(loadingStatus == LoadingStatus.FAIL) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                IconButton(onClick = getCategories) {
                    Icon(
                        painter = painterResource(id = R.drawable.refresh),
                        contentDescription = "Reload categories"
                    )
                }
            }
        }

//        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        if(loadingStatus == LoadingStatus.SUCCESS) {
            if(categories.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Add categories to group you transactions. You can for example add `Supermarket` and select from the transactions list the supermarkets (members) you want to group",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            } else {
                if(filteringOn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Generate report for",
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            enabled = selectedCategories.isNotEmpty(),
                            onClick = onDownloadReport
                        ) {
                            Text(
                                text = "Generate",
                                fontSize = screenFontSize(x = 14.0).sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
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
                                },
                                modifier = Modifier
                                    .clickable {
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

@Composable
fun SelectableCategoryCell(
    category: TransactionCategory,
    selectedCategories: List<Int>,
    onRemoveCategory: (id: Int) -> Unit,
    onAddCategory: (id: Int) -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
) {
    ElevatedCard(
        modifier = Modifier
            .padding(
                bottom = screenHeight(x = 10.0)
            )
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(screenWidth(x = 10.0))
        ) {
            Text(
                text = category.name,
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if(selectedCategories.contains(category.id)) {
                IconButton(onClick = { onRemoveCategory(category.id) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.check_box_filled),
                        contentDescription = "Remove item from filter",
                        modifier = Modifier
                            .size(screenWidth(x = 24.0))
                    )
                }
            } else {
                IconButton(onClick = { onAddCategory(category.id) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.check_box_blank),
                        contentDescription = "Add item to filter",
                        modifier = Modifier
                            .size(screenWidth(x = 24.0))
                    )
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

@Composable
fun DownloadReportDialog(
    startDate: String,
    endDate: String,
    onDismiss: () -> Unit,
    onConfirm: (type: String) -> Unit,
) {
    val types = listOf("PDF", "CSV")
    var selectedType by rememberSaveable {
        mutableStateOf("PDF")
    }

    var expanded by rememberSaveable {
        mutableStateOf(false)
    }

    AlertDialog(
        title = {
            Text(
                text = "Report for $startDate to $endDate",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            Row(
//                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Export to ",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                expanded = !expanded
                            }
                    ) {
                        Text(
                            text = selectedType,
                            color = MaterialTheme.colorScheme.surfaceTint,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select report type",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {expanded = !expanded},
                        modifier = Modifier
                    ) {
                        types.forEach {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = it,
                                        fontSize = screenFontSize(x = 14.0).sp
                                    )
                                },
                                onClick = {
                                    selectedType = it
                                    expanded = !expanded
                                }
                            )
                        }
                    }
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
            Button(onClick = { onConfirm(selectedType) }) {
                Text(
                    text = "Confirm",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
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
            getCategories = {},
            onShowSubscriptionDialog = {}
        )
    }
}