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
import com.records.pesa.models.transaction.CurrentBalanceResponseBody
import com.records.pesa.models.transaction.GroupedTransactionsResponseBody
import com.records.pesa.models.MessagesResponseBody
import com.records.pesa.models.SingleBudgetResponseBody
import com.records.pesa.models.SmsMessage
import com.records.pesa.models.dashboard.DashboardDetailsResponseBody
import com.records.pesa.models.transaction.SortedTransactionsResponseBody
import com.records.pesa.models.transaction.TransactionCodesResponseBody
import com.records.pesa.models.transaction.TransactionEditPayload
import com.records.pesa.models.transaction.TransactionEditResponseBody
import com.records.pesa.models.transaction.TransactionResponseBody
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.payment.PaymentPayload
import com.records.pesa.models.payment.PaymentResponseBody
import com.records.pesa.models.payment.SubscriptionPaymentStatusPayload
import com.records.pesa.models.payment.SubscriptionStatusResponseBody
import com.records.pesa.models.transaction.MonthlyTransactionsResponseBody
import com.records.pesa.models.transaction.SingleTransactionResponseBody
import com.records.pesa.models.user.PasswordUpdatePayload
import com.records.pesa.models.user.UserLoginPayload
import com.records.pesa.models.user.UserLoginResponseBody
import com.records.pesa.models.user.UserRegistrationPayload
import com.records.pesa.models.user.UserRegistrationResponseBody
import com.records.pesa.models.version.AppVersionCheckResponseBody
import kotlinx.coroutines.flow.first
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiRepository {
    suspend fun postMessages(token: String, id: Int, messages: List<SmsMessage>): Response<MessagesResponseBody>
    suspend fun getTransactions(token: String, userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, latest: Boolean, startDate: String?, endDate: String?): Response<TransactionResponseBody>
    suspend fun getMoneyIn(token: String, userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>
    suspend fun getMoneyOut(token: String, userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>

    suspend fun getMoneyInSortedTransactions(token: String, userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, orderByAmount: Boolean, ascendingOrder: Boolean, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>
    suspend fun getMoneyOutSortedTransactions(token: String, userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, orderByAmount: Boolean, ascendingOrder: Boolean, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>

    suspend fun getCurrentBalance(token: String, userId: Int): Response<CurrentBalanceResponseBody>

    suspend fun getUserCategories(token: String, userId: Int, categoryId: Int?, name: String?, orderBy: String?): Response<CategoriesResponseBody>

    suspend fun getCategoryDetails(token: String, categoryId: Int): Response<CategoryResponseBody>
    suspend fun createCategory(token: String, userId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun updateCategoryName(token: String, categoryId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun addCategoryMembers(token: String, categoryId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun updateCategoryKeyword(token: String, keyword: CategoryKeywordEditPayload): Response<CategoryKeywordEditResponseBody>

    suspend fun deleteCategoryKeyword(token: String, categoryId: Int, keywordId: Int): Response<CategoryKeywordDeleteResponseBody>

    suspend fun deleteCategory(token: String, categoryId: Int): Response<CategoryDeleteResponseBody>

    suspend fun updateTransaction(token: String, transactionEditPayload: TransactionEditPayload): Response<TransactionEditResponseBody>

    suspend fun getUserBudgets(token: String, userId: Int, name: String?): Response<BudgetResponseBody>

    suspend fun getCategoryBudgets(token: String, categoryId: Int, name: String?): Response<BudgetResponseBody>

    suspend fun getBudget(token: String, budgetId: Int): Response<SingleBudgetResponseBody>

    suspend fun createBudget(token: String, userId: Int, categoryId: Int, budget: BudgetEditPayLoad): Response<SingleBudgetResponseBody>

    suspend fun updateBudget(token: String, budgetId: Int, budget: BudgetEditPayLoad): Response<SingleBudgetResponseBody>
    suspend fun deleteBudget(token: String, budgetId: Int): Response<BudgetDeleteResponseBody>

    suspend fun getGroupedTransactions(token: String, userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, startDate: String, endDate: String): Response<GroupedTransactionsResponseBody>

    suspend fun getGroupedByEntityTransactions(token: String, userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>

    suspend fun getLatestTransactionCode(token: String, userId: Int): Response<TransactionCodesResponseBody>

    suspend fun registerUser(user: UserRegistrationPayload): Response<UserRegistrationResponseBody>

    suspend fun loginUser(password: String, user: UserLoginPayload): Response<UserLoginResponseBody>

    suspend fun getSubscriptionStatus(userId: Int): Response<SubscriptionStatusResponseBody>

    suspend fun getGroupedByMonthTransactions(token: String, userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, month: String, year: String): Response<MonthlyTransactionsResponseBody>
    suspend fun updateUserDetails(token: String, userId: Int, user: UserRegistrationPayload): Response<UserRegistrationResponseBody>
    suspend fun updateUserPassword(password: PasswordUpdatePayload): Response<UserRegistrationResponseBody>
    suspend fun paySubscriptionFee(token: String, paymentPayload: PaymentPayload): Response<PaymentResponseBody>
    suspend fun subscriptionPaymentStatus(token: String, subscriptionPaymentStatusPayload: SubscriptionPaymentStatusPayload): Response<SubscriptionStatusResponseBody>
    suspend fun getDashboardDetails(token: String, userId: Int, date: String): Response<DashboardDetailsResponseBody>
    suspend fun getAllTransactionsReport(userId: Int, token: String, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, startDate: String, endDate: String): Response<ResponseBody>
    suspend fun getSingleTransaction(token: String, transactionId: Int): Response<SingleTransactionResponseBody>
    suspend fun checkAppVersion(): Response<AppVersionCheckResponseBody>

}

class ApiRepositoryImpl(private val apiService: ApiService, private val dbRepository: DBRepository): ApiRepository {
    override suspend fun postMessages(
        token: String,
        id: Int,
        messages: List<SmsMessage>
    ): Response<MessagesResponseBody> = apiService.postMessages(
        token = "Bearer $token",
        id = id,
        messages = messages
    )

    override suspend fun getTransactions(
        token: String,
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        startDate: String?,
        endDate: String?
    ): Response<TransactionResponseBody> = apiService.getTransactions(
        token = "Bearer $token",
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        latest = latest,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getMoneyIn(
        token: String,
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
        token = "Bearer $token",
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
        token: String,
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
        token = "Bearer $token",
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
        token: String,
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
        token = "Bearer $token",
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
        token: String,
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
        token = "Bearer $token",
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

    override suspend fun getCurrentBalance(token: String, userId: Int): Response<CurrentBalanceResponseBody> = apiService.getCurrentBalance(
        token = "Bearer $token",
        userId = userId
    )

    override suspend fun getUserCategories(
        token: String,
        userId: Int,
        categoryId: Int?,
        name: String?,
        orderBy: String?
    ): Response<CategoriesResponseBody> = apiService.getUserCategories(
        token = "Bearer $token",
        userId = userId,
        categoryId = categoryId,
        name = name,
        orderBy = orderBy
    )

    override suspend fun getCategoryDetails(token: String, categoryId: Int): Response<CategoryResponseBody> = apiService.getCategoryDetails(
        token = "Bearer $token",
        categoryId = categoryId
    )

    override suspend fun createCategory(
        token: String,
        userId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.createCategory(
        token = "Bearer $token",
        userId = userId,
        category = category
    )

    override suspend fun updateCategoryName(
        token: String,
        categoryId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.updateCategoryName(
        token = "Bearer $token",
        categoryId = categoryId,
        category = category
    )

    override suspend fun addCategoryMembers(
        token: String,
        categoryId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.addCategoryMembers(
        token = "Bearer $token",
        categoryId = categoryId,
        category = category
    )

    override suspend fun updateCategoryKeyword(token: String, keyword: CategoryKeywordEditPayload): Response<CategoryKeywordEditResponseBody> = apiService.updateCategoryKeyword(
        token = "Bearer $token",
        keyword = keyword
    )

    override suspend fun deleteCategoryKeyword(token: String, categoryId: Int, keywordId: Int): Response<CategoryKeywordDeleteResponseBody> = apiService.deleteCategoryKeyword(
        token = "Bearer $token",
        categoryId = categoryId,
        keywordId = keywordId
    )

    override suspend fun deleteCategory(token: String, categoryId: Int): Response<CategoryDeleteResponseBody> = apiService.deleteCategory(
        token = "Bearer $token",
        categoryId = categoryId
    )

    override suspend fun updateTransaction(token: String, transactionEditPayload: TransactionEditPayload): Response<TransactionEditResponseBody> = apiService.updateTransaction(
        token = "Bearer $token",
        transactionEditPayload = transactionEditPayload
    )

    override suspend fun getUserBudgets(token: String, userId: Int, name: String?): Response<BudgetResponseBody> = apiService.getUserBudgets(
        token = "Bearer $token",
        userId = userId,
        name = name
    )

    override suspend fun getCategoryBudgets(
        token: String,
        categoryId: Int,
        name: String?
    ): Response<BudgetResponseBody> = apiService.getCategoryBudgets(
        token = "Bearer $token",
        categoryId = categoryId,
        name = name
    )

    override suspend fun getBudget(token: String, budgetId: Int): Response<SingleBudgetResponseBody> = apiService.getBudget(
        token = "Bearer $token",
        budgetId = budgetId
    )

    override suspend fun createBudget(
        token: String,
        userId: Int,
        categoryId: Int,
        budget: BudgetEditPayLoad
    ): Response<SingleBudgetResponseBody> = apiService.createBudget(
        token = "Bearer $token",
        userId = userId,
        categoryId = categoryId,
        budget = budget
    )

    override suspend fun updateBudget(
        token: String,
        budgetId: Int,
        budget: BudgetEditPayLoad
    ): Response<SingleBudgetResponseBody> = apiService.updateBudget(
        token = "Bearer $token",
        budgetId = budgetId,
        budget = budget
    )

    override suspend fun deleteBudget(token: String, budgetId: Int): Response<BudgetDeleteResponseBody> = apiService.deleteBudget(
        token = "Bearer $token",
        budgetId = budgetId
    )

    override suspend fun getGroupedTransactions(
        token: String,
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        startDate: String,
        endDate: String
    ): Response<GroupedTransactionsResponseBody> = apiService.getGroupedTransactions(
        token = "Bearer $token",
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getGroupedByEntityTransactions(
        token: String,
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        startDate: String,
        endDate: String
    ): Response<SortedTransactionsResponseBody> = apiService.getGroupedByEntityTransactions(
        token = "Bearer $token",
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getLatestTransactionCode(token: String, userId: Int): Response<TransactionCodesResponseBody> = apiService.getLatestTransactionCode(
        token = "Bearer $token",
        userId = userId
    )
    override suspend fun registerUser(user: UserRegistrationPayload): Response<UserRegistrationResponseBody> = apiService.registerUser(
        user = user
    )

    override  suspend fun getSubscriptionStatus(userId: Int): Response<SubscriptionStatusResponseBody> {
        val response = apiService.getSubscriptionStatus(userId = userId)
        if(response.isSuccessful) {
            val userDetails = dbRepository.getUser(userId).first()
            if(response.body()?.data?.payment != userDetails.paymentStatus) {
                dbRepository.updateUser(
                    userDetails.copy(
                        paymentStatus = response.body()?.data?.payment ?: false
                    )
                )
            }
        }
        return response;
    }

    override suspend fun getGroupedByMonthTransactions(
        token: String,
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        month: String,
        year: String
    ): Response<MonthlyTransactionsResponseBody> = apiService.getGroupedByMonthTransactions(
        token = "Bearer $token",
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        month = month,
        year = year
    )

    override suspend fun updateUserDetails(
        token: String,
        userId: Int,
        user: UserRegistrationPayload
    ): Response<UserRegistrationResponseBody> {
        val response = apiService.updateUserDetails(
            token = "Bearer $token",
            userId = userId,
            user = user
        )
        if(response.isSuccessful) {
            val userDetails = dbRepository.getUser(userId).first()
            dbRepository.updateUser(
                userDetails.copy(
                    firstName = response.body()?.data?.user?.fname,
                    lastName = response.body()?.data?.user?.lname,
                    email = response.body()?.data?.user?.email,
                )
            )
        }
        return response;
    }

    override suspend fun updateUserPassword(password: PasswordUpdatePayload): Response<UserRegistrationResponseBody> = apiService.updateUserPassword(
        password = password
    )

    override suspend fun paySubscriptionFee(
        token: String,
        paymentPayload: PaymentPayload
    ): Response<PaymentResponseBody> = apiService.paySubscriptionFee(
        token = "Bearer $token",
        paymentPayload = paymentPayload
    )

    override suspend fun subscriptionPaymentStatus(
        token: String,
        subscriptionPaymentStatusPayload: SubscriptionPaymentStatusPayload
    ): Response<SubscriptionStatusResponseBody> {
        val response = apiService.subscriptionPaymentStatus(
            token = "Bearer $token",
            subscriptionPaymentStatusPayload = subscriptionPaymentStatusPayload
        )
        if(response.isSuccessful) {
            if(response.body()?.data?.payment!!) {
                val userDetails = dbRepository.getUser(subscriptionPaymentStatusPayload.userId).first()
                dbRepository.updateUser(
                    userDetails.copy(
                        paymentStatus = true
                    )
                )
            }
        }
        return response
    }

    override suspend fun getDashboardDetails(
        token: String,
        userId: Int,
        date: String
    ): Response<DashboardDetailsResponseBody> = apiService.getDashboardDetails(
        token = "Bearer $token",
        userId = userId,
        date = date
    )

    override suspend fun getAllTransactionsReport(
        userId: Int,
        token: String,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        startDate: String,
        endDate: String
    ): Response<ResponseBody> = apiService.getAllTransactionsReport(
        token = "Bearer $token",
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getSingleTransaction(
        token: String,
        transactionId: Int
    ): Response<SingleTransactionResponseBody> = apiService.getSingleTransaction(
        token = "Bearer $token",
        transactionId = transactionId
    )

    override suspend fun checkAppVersion(): Response<AppVersionCheckResponseBody> = apiService.checkAppVersion()

    override suspend fun loginUser(password: String, user: UserLoginPayload): Response<UserLoginResponseBody> {
        val response = apiService.loginUser(user = user)

        if(response.isSuccessful) {
            dbRepository.deleteAllFromUser()
            val userDetails = UserDetails(
                userId = response.body()?.data?.user?.userInfo?.id!!,
                firstName = response.body()?.data?.user?.userInfo?.fname,
                lastName = response.body()?.data?.user?.userInfo?.lname,
                email = response.body()?.data?.user?.userInfo?.email,
                phoneNumber = response.body()?.data?.user?.userInfo?.phoneNumber!!,
                password = password,
                token = response.body()?.data?.user?.token!!,
                paymentStatus = false
            )
            val appLaunchStatus = dbRepository.getAppLaunchStatus(1).first()
            dbRepository.updateAppLaunchStatus(
                appLaunchStatus.copy(
                    user_id = response.body()?.data?.user?.userInfo?.id!!
                )
            )
            dbRepository.insertUser(userDetails)
        }
        return response
    }


}