package com.records.pesa.reusables

import android.os.Build
import androidx.annotation.RequiresApi
import com.records.pesa.models.BudgetCategory
import com.records.pesa.models.BudgetDt
import com.records.pesa.models.BudgetOwner
import com.records.pesa.models.CategoryBudget
import com.records.pesa.models.CategoryKeyword
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.transaction.ItemCategory
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.TransactionItem
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
val dateFormatter = DateTimeFormatter.ofPattern("dd MMM, yyyy")

val itemCategories = listOf(
    ItemCategory(
        id = 1,
        name = "Send money"
    ),
    ItemCategory(
        id = 2,
        name = "Family"
    ),
)

val transaction = TransactionItem(
    transactionId = 1,
    nickName = "Josphat  Karoki",
    transactionCode = "SFP2KN50C2",
    transactionType = "Send Money",
    transactionAmount = 1000.0,
    transactionCost = 0.0,
    date = "2024-06-21",
    time = "18:21:05",
    sender = "JOSPHAT  KAROKI 0720929489",
    recipient = "You",
    balance = 1000.0,
    entity = "JOSPHAT  KAROKI 0720929489",
    categories = itemCategories,
    comment = "This is a comment"
)

val transactions = listOf(
    TransactionItem(
        transactionId = 1,
        nickName = "Josphat  Karoki",
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        entity = "JOSPHAT  KAROKI 0720929489",
        categories = itemCategories,
        comment = "This is a comment"
    ),
    TransactionItem(
        transactionId = 1,
        nickName = "Josphat  Karoki",
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        entity = "JOSPHAT  KAROKI 0720929489",
        categories = itemCategories,
        comment = "This is a comment"
    ),
    TransactionItem(
        transactionId = 1,
        nickName = "Josphat  Karoki",
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        entity = "JOSPHAT  KAROKI 0720929489",
        categories = itemCategories,
        comment = "This is a comment"
    ),
    TransactionItem(
        transactionId = 1,
        nickName = "Josphat  Karoki",
        transactionCode = "SFP2KN50C2",
        transactionType = "Pay Bill",
        transactionAmount = -1550.0,
        transactionCost = -23.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "You",
        recipient = "PesaPal for account 0772364458",
        balance = 1000.0,
        entity = "JOSPHAT  KAROKI 0720929489",
        categories = itemCategories,
        comment = "This is a comment"
    ),
    TransactionItem(
        transactionId = 1,
        nickName = "Josphat  Karoki",
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        entity = "JOSPHAT  KAROKI 0720929489",
        categories = itemCategories,
        comment = "This is a comment"
    ),
    TransactionItem(
        transactionId = 1,
        nickName = "Josphat  Karoki",
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        entity = "JOSPHAT  KAROKI 0720929489",
        categories = itemCategories,
        comment = "This is a comment"
    ),
    TransactionItem(
        transactionId = 1,
        nickName = "Josphat  Karoki",
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        entity = "JOSPHAT  KAROKI 0720929489",
        categories = itemCategories,
        comment = "This is a comment"
    )
)

val moneyInSortedTransactionItem = SortedTransactionItem(
    transactionType = "Pay Bill",
    times = 103,
    timesOut = 51,
    timesIn = 52,
    totalOut = 500.0,
    totalIn = 1240.0,
    transactionCost = 310.0,
    entity = "PesaPal for account 0772364458",
    nickName = "Pesapal",
)

val moneyInSortedTransactionItems = listOf(
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    )

)

val moneyOutSortedTransactionItems = listOf(
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        timesOut = 51,
        timesIn = 52,
        totalOut = 500.0,
        totalIn = 1240.0,
        transactionCost = 310.0,
        entity = "PesaPal for account 0772364458",
        nickName = "Pesapal",
    )

)

val transactionTypes = listOf(
    "All types",
    "Send Money",
    "Pay Bill",
    "Buy goods and Services",
    "Mshwari",
    "Fuliza",
    "Hustler fund",
    "Reversal",
    "KBC Mpesa account",
    "Lock savings",
    "Deposit",
    "Withdrawal"
)

val sortTypes = listOf(
    "Amount",
    "Times"
)

val categoryKeyword = CategoryKeyword(
    id = 1,
    keyWord = "Josphat  Karoki",
    nickName = "Josphat  Karoki"
)

val categoryKeywords = listOf(
    CategoryKeyword(
        id = 1,
        keyWord = "Josphat  Karoki",
        nickName = "Josphat  Karoki"
    ),
    CategoryKeyword(
        id = 1,
        keyWord = "Josphat  Karoki",
        nickName = "Josphat  Karoki"
    )
)

val categoryBudget = CategoryBudget(
    id = 1,
    name = "Grocery",
    budgetLimit = 1000.0,
    createdAt = "2024-07-18T18:45:25.671011928",
    limitDate = "2024-08-10",
    limitReached = false,
    exceededBy = 0.0
)

val categoryBudgets = listOf(
    CategoryBudget(
        id = 1,
        name = "Grocery",
        budgetLimit = 1000.0,
        createdAt = "2024-07-18T18:45:25.671011928",
        limitDate = "2024-08-10",
        limitReached = false,
        exceededBy = 0.0
    ),
    CategoryBudget(
        id = 1,
        name = "Grocery",
        budgetLimit = 1000.0,
        createdAt = "2024-07-18T18:45:25.671011928",
        limitDate = "2024-08-10",
        limitReached = false,
        exceededBy = 0.0
    )
)

val transactionCategory = TransactionCategory(
    id = 1,
    name = "",
    createdAt = "2024-07-08T19:06:12.563465",
    transactions = transactions,
    keywords = categoryKeywords,
    budgets = categoryBudgets
)

val transactionCategories = listOf(
    TransactionCategory(
        id = 1,
        name = "Grocery",
        createdAt = "2024-07-08T19:06:12.563465",
        transactions = emptyList(),
        keywords = emptyList(),
        budgets = emptyList()
    ),
    TransactionCategory(
        id = 1,
        name = "Grocery",
        createdAt = "2024-07-08T19:06:12.563465",
        transactions = emptyList(),
        keywords = emptyList(),
        budgets = emptyList()
    )
)

val budgetCategory = BudgetCategory(
    id = 1,
    name = "Grocery"
)

val budgetOwner = BudgetOwner(
    id = 1,
    name = "Alex"
)

val budget = BudgetDt(
    id = 1,
    name = "Picnic",
    active = true,
    expenditure = 0.0,
    budgetLimit = 1000.0,
    createdAt = "2024-07-08T19:06:12.563465",
    limitDate = "2024-08-10",
    limitReached = false,
    limitReachedAt = null,
    exceededBy = 0.0,
    category = budgetCategory,
    user = budgetOwner
)

val budgets = listOf(
    BudgetDt(
        id = 1,
        name = "Picnic",
        active = true,
        expenditure = 0.0,
        budgetLimit = 1000.0,
        createdAt = "2024-07-08T19:06:12.563465",
        limitDate = "2024-08-10",
        limitReached = false,
        limitReachedAt = null,
        exceededBy = 0.0,
        category = budgetCategory,
        user = budgetOwner
    ),
    BudgetDt(
        id = 1,
        name = "Picnic",
        active = true,
        expenditure = 0.0,
        budgetLimit = 1000.0,
        createdAt = "2024-07-08T19:06:12.563465",
        limitDate = "2024-08-10",
        limitReached = false,
        limitReachedAt = null,
        exceededBy = 0.0,
        category = budgetCategory,
        user = budgetOwner
    ),
    BudgetDt(
        id = 1,
        name = "Picnic",
        active = true,
        expenditure = 0.0,
        budgetLimit = 1000.0,
        createdAt = "2024-07-08T19:06:12.563465",
        limitDate = "2024-08-10",
        limitReached = false,
        limitReachedAt = null,
        exceededBy = 0.0,
        category = budgetCategory,
        user = budgetOwner
    ),
    BudgetDt(
        id = 1,
        name = "Picnic",
        active = true,
        expenditure = 0.0,
        budgetLimit = 1000.0,
        createdAt = "2024-07-08T19:06:12.563465",
        limitDate = "2024-08-10",
        limitReached = false,
        limitReachedAt = null,
        exceededBy = 0.0,
        category = budgetCategory,
        user = budgetOwner
    ),
    BudgetDt(
        id = 1,
        name = "Picnic",
        active = true,
        expenditure = 0.0,
        budgetLimit = 1000.0,
        createdAt = "2024-07-08T19:06:12.563465",
        limitDate = "2024-08-10",
        limitReached = false,
        limitReachedAt = null,
        exceededBy = 0.0,
        category = budgetCategory,
        user = budgetOwner
    )
)

val groupedTransactions = listOf(
    GroupedTransactionData("2024-06-25", 9, 5.0f, 5.0f, 0.0f),
    GroupedTransactionData("2024-06-24", 8, 200.0f, 545.0f,  3.0f),
    GroupedTransactionData("2024-06-23", 7, 0.0f, 229.0f,  0.0f),
    GroupedTransactionData("2024-06-22", 8, 800.0f, 338.0f, 0.0f),
    GroupedTransactionData("2024-06-21", 28, 1191.0f, 944.37f,  5.0f),
    GroupedTransactionData("2024-06-20", 6, 0.0f, 152.0f, 2.0f),
    GroupedTransactionData("2024-06-19", 5, 0.0f, 349.0f,  3.0f),
    GroupedTransactionData("2024-06-18", 4, 800.0f, 542.55f,  0.0f),
    GroupedTransactionData("2024-06-17", 4, 0.0f, 555.0f,  10.0f),
    GroupedTransactionData("2024-06-16", 4, 0.0f, 1410.0f,  16.0f),
    GroupedTransactionData("2024-06-15", 7, 1500.0f, 830.0f,  7.0f),
    GroupedTransactionData("2024-06-14", 11, 0.0f, 595.0f,  5.0f),
    GroupedTransactionData("2024-06-13", 7, 50.0f, 401.0f,  6.0f),
)


