package com.records.pesa.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import com.records.pesa.container.AppContainerImpl
import com.records.pesa.db.DBRepository
import com.records.pesa.models.user.UserAccount
import com.records.pesa.network.SupabaseClient.client
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

class BackupWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
): CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val token = inputData.getString("token")
        val userId = inputData.getInt("userId", -1)

        val appContext = context.applicationContext as CashLedger
        appContext.container = AppContainerImpl(appContext)

        val dbRepository = appContext.container.dbRepository
        val transactionService = appContext.container.transactionService
        val categoryService = appContext.container.categoryService
        val userAccountService = appContext.container.userAccountService

        if(userId == -1) {
            return Result.failure()
        }

        backup(
            dbRepository = dbRepository,
            userId = userId,
            transactionService = transactionService,
            categoryService = categoryService,
        )

        return Result.success()
    }

}

suspend fun backup(
    dbRepository: DBRepository,
    userId: Int,
    transactionService: TransactionService,
    categoryService: CategoryService
) {

    val query = transactionService.createUserTransactionQuery(
        userId = userId,
        entity = null,
        categoryId = null,
        budgetId = null,
        transactionType = null,
        moneyDirection = null,
        startDate = LocalDate.now().minusYears(10),
        endDate = LocalDate.now(),
        latest = true
    )

    Log.d("backingUpInProcess", "LOADING...")
    val userDetails = dbRepository.getUser(userId = userId).first()
    val transactions = transactionService.getUserTransactions(query).first()
    val categories = categoryService.getAllCategories().first()
    val categoryKeywords = categoryService.getAllCategoryKeywords()
    val transactionCategoryMappings = categoryService.getTransactionCategoryCrossRefs().first()

    val transactionsToBackup = transactions.subList(userDetails.transactions, transactions.size)
    val categoriesToBackup = categories.subList(userDetails.categories, categories.size)
    val categoryKeywordsToBackup = categoryKeywords.subList(userDetails.categoryKeywords, categoryKeywords.size)
    val categoryMappingsToBackup = transactionCategoryMappings.subList(userDetails.categoryMappings, transactionCategoryMappings.size)

    var totalItems = 0
    var insertedItems = 0

    val batchSize = 2000
    val totalTransactionsBatches = (transactionsToBackup.size + batchSize - 1) / batchSize
    val totalCategoriesBatches = (categoriesToBackup.size + batchSize - 1) / batchSize
    val totalCategoryKeywordsBatches = (categoryKeywordsToBackup.size + batchSize - 1) / batchSize
    val totalTransactionWithCategoryMappingsBatches = (categoryMappingsToBackup.size + batchSize - 1) / batchSize

    totalItems += transactionsToBackup.size
    totalItems += categoriesToBackup.size
    totalItems += categoryKeywordsToBackup.size
    totalItems += categoryMappingsToBackup.size

    var totalItems2 = 0
    totalItems2 += transactions.size
    totalItems2 += categories.size
    totalItems2 += categoryKeywords.size
    totalItems2 += categoryMappingsToBackup.size

    Log.d("TOTAL_ITEMS", totalItems2.toString())
    Log.d("TOTAL_ITEMS_NOT_BACKED_UP", "${totalItems2 - userDetails.backedUpItemsSize}")
    val itemsNotBackedUp = totalItems2 - userDetails.backedUpItemsSize
    val transactionsNotBackedUp = transactions.size - userDetails.transactions
    val categoriesNotBackedUp = categories.size - userDetails.categories
    val categoryKeywordsNotBackedUp = categoryKeywords.size - userDetails.categoryKeywords
    val categoryMappingsNotBackedUp = categoryMappingsToBackup.size - userDetails.categoryMappings

    try {
        val user = client.postgrest["userAccount"].select {
            filter {
                eq("id", userDetails.userId)
            }
        }.decodeSingle<UserAccount>()

        client.postgrest["userAccount"].update(user.copy(
            backupSet = true
        )) {
            filter {
                eq("id", userDetails.userId)
            }
        }

        dbRepository.updateUser(
            userDetails.copy(backupSet = true)
        )

        for(i in 0 until totalTransactionsBatches) {
            val fromIndex = i * batchSize
            val toIndex = minOf(fromIndex + batchSize, transactionsToBackup.size)
            val batch = transactionsToBackup.subList(fromIndex, toIndex)
            // backup transactions
            client.postgrest["transaction"].upsert(batch, onConflict = "transactionCode")
        }

        for(i in 0 until totalCategoriesBatches) {
            val fromIndex = i * batchSize
            val toIndex = minOf(fromIndex + batchSize, categoriesToBackup.size)
            val batch = categoriesToBackup.subList(fromIndex, toIndex)
            // backup categories
            client.postgrest["transactionCategory"].upsert(batch, onConflict = "id")
        }

        for(i in 0 until totalCategoryKeywordsBatches) {
            val fromIndex = i * batchSize
            val toIndex = minOf(fromIndex + batchSize, categoryKeywordsToBackup.size)
            val batch = categoryKeywordsToBackup.subList(fromIndex, toIndex)
            // backup category keywords
            client.postgrest["categoryKeyword"].upsert(batch, onConflict = "id")
        }

        for(i in 0 until totalTransactionWithCategoryMappingsBatches) {
            val fromIndex = i * batchSize
            val toIndex = minOf(fromIndex + batchSize, categoryMappingsToBackup.size)
            val batch = categoryMappingsToBackup.subList(fromIndex, toIndex)
            // backup category keywords
            // backup transactionCategoryCrossRef
            client.postgrest["transactionCategoryCrossRef"].upsert(
                batch,
                onConflict = "transactionId, categoryId"
            )
        }

        val lastBackup = LocalDateTime.now()

        client.postgrest["userAccount"].update(user.copy(
            lastBackup = lastBackup.toString(),
            backedUpItemsSize = user.backedUpItemsSize + totalItems,
            transactions = user.transactions + transactionsNotBackedUp,
            categories = user.categories + categoriesNotBackedUp,
            categoryKeywords = user.categoryKeywords + categoryKeywordsNotBackedUp,
            categoryMappings = user.categoryMappings + categoryMappingsNotBackedUp
        )) {
            filter {
                eq("id", userDetails.userId)
            }
        }

        dbRepository.updateUser(
            user = userDetails.copy(
                lastBackup = lastBackup,
                backedUpItemsSize = user.backedUpItemsSize + totalItems,
                transactions = user.transactions + transactionsNotBackedUp,
                categories = user.categories + categoriesNotBackedUp,
                categoryKeywords = user.categoryKeywords + categoryKeywordsNotBackedUp,
                categoryMappings = user.categoryMappings + categoryMappingsNotBackedUp
            )
        )

    } catch (e: Exception) {

        Log.e("backUpException", e.toString())
    }
}
