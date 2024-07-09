package com.records.pesa.reusables

import android.os.Build
import androidx.annotation.RequiresApi
import com.records.pesa.models.ItemCategory
import com.records.pesa.models.SortedTransactionItem
import com.records.pesa.models.TransactionItem
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
    transactionCode = "SFP2KN50C2",
    transactionType = "Send Money",
    transactionAmount = 1000.0,
    transactionCost = 0.0,
    date = "2024-06-21",
    time = "18:21:05",
    sender = "JOSPHAT  KAROKI 0720929489",
    recipient = "You",
    balance = 1000.0,
    categories = itemCategories
)

val transactions = listOf(
    TransactionItem(
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        categories = itemCategories
    ),
    TransactionItem(
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        categories = itemCategories
    ),
    TransactionItem(
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        categories = itemCategories
    ),
    TransactionItem(
        transactionCode = "SFP2KN50C2",
        transactionType = "Pay Bill",
        transactionAmount = -1550.0,
        transactionCost = -23.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "You",
        recipient = "PesaPal for account 0772364458",
        balance = 1000.0,
        categories = itemCategories
    ),
    TransactionItem(
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        categories = itemCategories
    ),
    TransactionItem(
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        categories = itemCategories
    ),
    TransactionItem(
        transactionCode = "SFP2KN50C2",
        transactionType = "Send Money",
        transactionAmount = 1000.0,
        transactionCost = 0.0,
        date = "2024-06-21",
        time = "18:21:05",
        sender = "JOSPHAT  KAROKI 0720929489",
        recipient = "You",
        balance = 1000.0,
        categories = itemCategories
    )
)

val moneyInSortedTransactionItem = SortedTransactionItem(
    transactionType = "Pay Bill",
    times = 103,
    amount = 11070.0,
    transactionCost = 310.0,
    name = "PesaPal for account 0772364458"
)

val moneyInSortedTransactionItems = listOf(
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = 11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = 11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = 11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = 11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = 11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    )

)

val moneyOutSortedTransactionItems = listOf(
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = -11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = -11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = -11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = -11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
    ),
    SortedTransactionItem(
        transactionType = "Pay Bill",
        times = 103,
        amount = -11070.0,
        transactionCost = 310.0,
        name = "PesaPal for account 0772364458"
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

