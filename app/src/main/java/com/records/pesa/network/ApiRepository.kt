package com.records.pesa.network

import com.records.pesa.db.DBRepository
import com.records.pesa.models.BudgetDeleteResponseBody
import com.records.pesa.models.BudgetEditPayLoad
import com.records.pesa.models.BudgetResponseBody
import com.records.pesa.models.CategoriesResponseBody
import com.records.pesa.models.CategoryDeleteResponseBody
import com.records.pesa.models.CategoryEditPayload
import com.records.pesa.models.CategoryKeywordDeleteResponseBody
import com.records.pesa.models.CategoryKeywordEditPayload
import com.records.pesa.models.CategoryKeywordEditResponseBody
import com.records.pesa.models.CategoryResponseBody
import com.records.pesa.models.CurrentBalanceResponseBody
import com.records.pesa.models.GroupedTransactionsResponseBody
import com.records.pesa.models.MessagesResponseBody
import com.records.pesa.models.SingleBudgetResponseBody
import com.records.pesa.models.SmsMessage
import com.records.pesa.models.SortedTransactionsResponseBody
import com.records.pesa.models.TransactionCodesResponseBody
import com.records.pesa.models.TransactionEditPayload
import com.records.pesa.models.TransactionEditResponseBody
import com.records.pesa.models.TransactionResponseBody
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.SubscriptionStatusResponseBody
import com.records.pesa.models.user.UserLoginPayload
import com.records.pesa.models.user.UserLoginResponseBody
import com.records.pesa.models.user.UserRegistrationPayload
import com.records.pesa.models.user.UserRegistrationResponseBody
import kotlinx.coroutines.flow.first
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiRepository {
    suspend fun postMessages(id: Int, messages: List<SmsMessage>): Response<MessagesResponseBody>
    suspend fun getTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, latest: Boolean, startDate: String?, endDate: String?): Response<TransactionResponseBody>
    suspend fun getMoneyIn(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>
    suspend fun getMoneyOut(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>

    suspend fun getMoneyInSortedTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, orderByAmount: Boolean, ascendingOrder: Boolean, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>
    suspend fun getMoneyOutSortedTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, orderByAmount: Boolean, ascendingOrder: Boolean, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>

    suspend fun getCurrentBalance(userId: Int): Response<CurrentBalanceResponseBody>

    suspend fun getUserCategories(userId: Int, categoryId: Int?, name: String?, orderBy: String?): Response<CategoriesResponseBody>

    suspend fun getCategoryDetails(categoryId: Int): Response<CategoryResponseBody>
    suspend fun createCategory(userId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun updateCategoryName(categoryId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun addCategoryMembers(categoryId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun updateCategoryKeyword(keyword: CategoryKeywordEditPayload): Response<CategoryKeywordEditResponseBody>

    suspend fun deleteCategoryKeyword(categoryId: Int, keywordId: Int): Response<CategoryKeywordDeleteResponseBody>

    suspend fun deleteCategory(categoryId: Int): Response<CategoryDeleteResponseBody>

    suspend fun updateTransaction(transactionEditPayload: TransactionEditPayload): Response<TransactionEditResponseBody>

    suspend fun getUserBudgets(userId: Int, name: String?): Response<BudgetResponseBody>

    suspend fun getCategoryBudgets(categoryId: Int, name: String?): Response<BudgetResponseBody>

    suspend fun getBudget(budgetId: Int): Response<SingleBudgetResponseBody>

    suspend fun createBudget(userId: Int, categoryId: Int, budget: BudgetEditPayLoad): Response<SingleBudgetResponseBody>

    suspend fun updateBudget(budgetId: Int, budget: BudgetEditPayLoad): Response<SingleBudgetResponseBody>
    suspend fun deleteBudget(budgetId: Int): Response<BudgetDeleteResponseBody>

    suspend fun getGroupedTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, startDate: String, endDate: String): Response<GroupedTransactionsResponseBody>

    suspend fun getGroupedByEntityTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>

    suspend fun getLatestTransactionCode(userId: Int): Response<TransactionCodesResponseBody>

    suspend fun registerUser(user: UserRegistrationPayload): Response<UserRegistrationResponseBody>

    suspend fun loginUser(password: String, user: UserLoginPayload): Response<UserLoginResponseBody>

    suspend fun getSubscriptionStatus(userId: Int): Response<SubscriptionStatusResponseBody>

}

class ApiRepositoryImpl(private val apiService: ApiService, private val dbRepository: DBRepository): ApiRepository {
    override suspend fun postMessages(
        id: Int,
        messages: List<SmsMessage>
    ): Response<MessagesResponseBody> = apiService.postMessages(
        id = id,
        messages = messages
    )

    override suspend fun getTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        startDate: String?,
        endDate: String?
    ): Response<TransactionResponseBody> = apiService.getTransactions(userId, entity, categoryId, budgetId, transactionType, latest, startDate, endDate)

    override suspend fun getMoneyIn(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        latest: Boolean,
        startDate: String,
        endDate: String
    ): Response<TransactionResponseBody> = apiService.getMoneyIn(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        moneyIn = moneyIn,
        latest = latest,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getMoneyOut(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        latest: Boolean,
        startDate: String,
        endDate: String
    ): Response<TransactionResponseBody> = apiService.getMoneyOut(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        moneyIn = moneyIn,
        latest = latest,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getMoneyInSortedTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        orderByAmount: Boolean,
        ascendingOrder: Boolean,
        startDate: String,
        endDate: String
    ): Response<SortedTransactionsResponseBody> = apiService.getMoneyInSortedTransactions(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        moneyIn = moneyIn,
        orderByAmount = orderByAmount,
        ascendingOrder = ascendingOrder,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getMoneyOutSortedTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        orderByAmount: Boolean,
        ascendingOrder: Boolean,
        startDate: String,
        endDate: String
    ): Response<SortedTransactionsResponseBody> = apiService.getMoneyOutSortedTransactions(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        moneyIn = moneyIn,
        orderByAmount = orderByAmount,
        ascendingOrder = ascendingOrder,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getCurrentBalance(userId: Int): Response<CurrentBalanceResponseBody> = apiService.getCurrentBalance(
        userId = userId
    )

    override suspend fun getUserCategories(
        userId: Int,
        categoryId: Int?,
        name: String?,
        orderBy: String?
    ): Response<CategoriesResponseBody> = apiService.getUserCategories(
        userId = userId,
        categoryId = categoryId,
        name = name,
        orderBy = orderBy
    )

    override suspend fun getCategoryDetails(categoryId: Int): Response<CategoryResponseBody> = apiService.getCategoryDetails(
        categoryId = categoryId
    )

    override suspend fun createCategory(
        userId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.createCategory(
        userId = userId,
        category = category
    )

    override suspend fun updateCategoryName(
        categoryId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.updateCategoryName(
        categoryId = categoryId,
        category = category
    )

    override suspend fun addCategoryMembers(
        categoryId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.addCategoryMembers(
        categoryId = categoryId,
        category = category
    )

    override suspend fun updateCategoryKeyword(keyword: CategoryKeywordEditPayload): Response<CategoryKeywordEditResponseBody> = apiService.updateCategoryKeyword(
        keyword = keyword
    )

    override suspend fun deleteCategoryKeyword(categoryId: Int, keywordId: Int): Response<CategoryKeywordDeleteResponseBody> = apiService.deleteCategoryKeyword(
        categoryId = categoryId,
        keywordId = keywordId
    )

    override suspend fun deleteCategory(categoryId: Int): Response<CategoryDeleteResponseBody> = apiService.deleteCategory(
        categoryId = categoryId
    )

    override suspend fun updateTransaction(transactionEditPayload: TransactionEditPayload): Response<TransactionEditResponseBody> = apiService.updateTransaction(
        transactionEditPayload = transactionEditPayload
    )

    override suspend fun getUserBudgets(userId: Int, name: String?): Response<BudgetResponseBody> = apiService.getUserBudgets(
        userId = userId,
        name = name
    )

    override suspend fun getCategoryBudgets(
        categoryId: Int,
        name: String?
    ): Response<BudgetResponseBody> = apiService.getCategoryBudgets(
        categoryId = categoryId,
        name = name
    )

    override suspend fun getBudget(budgetId: Int): Response<SingleBudgetResponseBody> = apiService.getBudget(
        budgetId = budgetId
    )

    override suspend fun createBudget(
        userId: Int,
        categoryId: Int,
        budget: BudgetEditPayLoad
    ): Response<SingleBudgetResponseBody> = apiService.createBudget(
        userId = userId,
        categoryId = categoryId,
        budget = budget
    )

    override suspend fun updateBudget(
        budgetId: Int,
        budget: BudgetEditPayLoad
    ): Response<SingleBudgetResponseBody> = apiService.updateBudget(
        budgetId = budgetId,
        budget = budget
    )

    override suspend fun deleteBudget(budgetId: Int): Response<BudgetDeleteResponseBody> = apiService.deleteBudget(
        budgetId = budgetId
    )

    override suspend fun getGroupedTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        startDate: String,
        endDate: String
    ): Response<GroupedTransactionsResponseBody> = apiService.getGroupedTransactions(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getGroupedByEntityTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        startDate: String,
        endDate: String
    ): Response<SortedTransactionsResponseBody> = apiService.getGroupedByEntityTransactions(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getLatestTransactionCode(userId: Int): Response<TransactionCodesResponseBody> = apiService.getLatestTransactionCode(userId = userId)
    override suspend fun registerUser(user: UserRegistrationPayload): Response<UserRegistrationResponseBody> = apiService.registerUser(
        user = user
    )

    override  suspend fun getSubscriptionStatus(userId: Int): Response<SubscriptionStatusResponseBody> {
        val response = apiService.getSubscriptionStatus(userId = userId)
        if(response.isSuccessful) {
            val userDetails = dbRepository.getUser(userId).first()
            if(response.body()?.data?.payment!!) {
                dbRepository.updateUser(
                    userDetails.copy(
                        paymentStatus = true
                    )
                )
            } else {
                dbRepository.updateUser(
                    userDetails.copy(
                        paymentStatus = false
                    )
                )
            }
        }
        return response;
    }

    override suspend fun loginUser(password: String, user: UserLoginPayload): Response<UserLoginResponseBody> {
        val response = apiService.loginUser(user = user)

        if(response.isSuccessful) {
            val userDetails = UserDetails(
                userId = response.body()?.data?.user?.userInfo?.id!!,
                firstName = response.body()?.data?.user?.userInfo?.fname,
                lastName = response.body()?.data?.user?.userInfo?.lname,
                email = response.body()?.data?.user?.userInfo?.email,
                phoneNumber = response.body()?.data?.user?.userInfo?.phoneNumber!!,
                password = password,
                token = response.body()?.data?.token!!,
                paymentStatus = false
            )
            val appLaunchStatus = dbRepository.getAppLaunchStatus(1).first()
            dbRepository.updateAppLaunchStatus(
                appLaunchStatus.copy(
                    userId = response.body()?.data?.user?.userInfo?.id!!
                )
            )
            dbRepository.insertUser(userDetails)
        }
        return response
    }


}