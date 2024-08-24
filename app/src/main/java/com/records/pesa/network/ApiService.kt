package com.records.pesa.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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
import com.records.pesa.models.payment.FreeTrialStatusResponseBody
import com.records.pesa.models.payment.PaymentPayload
import com.records.pesa.models.payment.PaymentResponseBody
import com.records.pesa.models.payment.SubscriptionPaymentStatusPayload
import com.records.pesa.models.transaction.SortedTransactionsResponseBody
import com.records.pesa.models.transaction.TransactionCodesResponseBody
import com.records.pesa.models.transaction.TransactionEditPayload
import com.records.pesa.models.transaction.TransactionEditResponseBody
import com.records.pesa.models.transaction.TransactionResponseBody
import com.records.pesa.models.payment.SubscriptionStatusResponseBody
import com.records.pesa.models.transaction.IndividualSortedTransactionsResponseBody
import com.records.pesa.models.transaction.MonthlyTransactionsResponseBody
import com.records.pesa.models.transaction.SingleTransactionResponseBody
import com.records.pesa.models.transaction.TransactionTypeResponseBody
import com.records.pesa.models.user.PasswordUpdatePayload
import com.records.pesa.models.user.UserLoginPayload
import com.records.pesa.models.user.UserLoginResponseBody
import com.records.pesa.models.user.UserRegistrationPayload
import com.records.pesa.models.user.UserRegistrationResponseBody
import com.records.pesa.models.version.AppVersionCheckResponseBody
import com.records.pesa.ui.screens.dashboard.category.CategoryReportPayload
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Year

interface ApiService {
    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val baseUrl = "http://192.168.0.106:8080/api/"
        val instance by lazy {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ApiService::class.java)
        }
    }
    @POST("message/{id}")
    suspend fun postMessages(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body messages: List<SmsMessage>
    ): Response<MessagesResponseBody>

    @GET("transaction/{userId}")
    suspend fun getTransactions(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("latest") latest: Boolean,
        @Query("moneyDirection") moneyDirection: String?,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?
    ): Response<TransactionResponseBody>

    @GET("transaction/outandin/{userId}")
    suspend fun getMoneyIn(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("latest") latest: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<TransactionResponseBody>

    @GET("transaction/outandin/{userId}")
    suspend fun getMoneyOut(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("latest") latest: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<TransactionResponseBody>

    @GET("transaction/sorted/{userId}")
    suspend fun getMoneyInSortedTransactions(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("orderByAmount") orderByAmount: Boolean,
        @Query("ascendingOrder") ascendingOrder: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<IndividualSortedTransactionsResponseBody>

    @GET("transaction/sorted/{userId}")
    suspend fun getMoneyOutSortedTransactions(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("orderByAmount") orderByAmount: Boolean,
        @Query("ascendingOrder") ascendingOrder: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<IndividualSortedTransactionsResponseBody>

    @GET("transaction/balance/{userId}")
    suspend fun getCurrentBalance(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): Response<CurrentBalanceResponseBody>

    @GET("category/{userId}")
    suspend fun getUserCategories(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("categoryId") categoryId: Int?,
        @Query("name") name: String?,
        @Query("orderBy") orderBy: String?
    ): Response<CategoriesResponseBody>

    @GET("category/details/{categoryId}")
    suspend fun getCategoryDetails(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Int
    ): Response<CategoryResponseBody>

    @POST("category/{userId}")
    suspend fun createCategory(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Body category: CategoryEditPayload
    ): Response<CategoryResponseBody>

    @PUT("category/name/{categoryId}")
    suspend fun updateCategoryName(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Int,
        @Body category: CategoryEditPayload
    ): Response<CategoryResponseBody>

    @PUT("category/{categoryId}")
    suspend fun addCategoryMembers(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Int,
        @Body category: CategoryEditPayload
    ): Response<CategoryResponseBody>

    @PUT("category/keyword")
    suspend fun updateCategoryKeyword(
        @Header("Authorization") token: String,
        @Body keyword: CategoryKeywordEditPayload
    ): Response<CategoryKeywordEditResponseBody>

    @DELETE("category/keyword/{categoryId}/{keywordId}")
    suspend fun deleteCategoryKeyword(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Int,
        @Path("keywordId") keywordId: Int
    ): Response<CategoryKeywordDeleteResponseBody>

    @DELETE("category/{categoryId}")
    suspend fun deleteCategory(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Int
    ): Response<CategoryDeleteResponseBody>

    @PUT("transaction/update")
    suspend fun updateTransaction(
        @Header("Authorization") token: String,
        @Body transactionEditPayload: TransactionEditPayload
    ): Response<TransactionEditResponseBody>

    @GET("budget/{userId}")
    suspend fun getUserBudgets(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("name") name: String?,
    ): Response<BudgetResponseBody>

    @GET("budget/category/{categoryId}")
    suspend fun getCategoryBudgets(
        @Header("Authorization") token: String,
        @Path("categoryId") categoryId: Int,
        @Query("name") name: String?
    ): Response<BudgetResponseBody>

    @GET("budget/single/{budgetId}")
    suspend fun getBudget(
        @Header("Authorization") token: String,
        @Path("budgetId") budgetId: Int
    ): Response<SingleBudgetResponseBody>

    @POST("budget/{userId}/{categoryId}")
    suspend fun createBudget(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Path("categoryId") categoryId: Int,
        @Body budget: BudgetEditPayLoad
    ): Response<SingleBudgetResponseBody>

    @PUT("budget/{budgetId}")
    suspend fun updateBudget(
        @Header("Authorization") token: String,
        @Path("budgetId") budgetId: Int,
        @Body budget: BudgetEditPayLoad
    ): Response<SingleBudgetResponseBody>
    @DELETE("budget/{budgetId}")
    suspend fun deleteBudget(
        @Header("Authorization") token: String,
        @Path("budgetId") budgetId: Int
    ): Response<BudgetDeleteResponseBody>
    @GET("transaction/grouped/{userId}")
    suspend fun getGroupedTransactions(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<GroupedTransactionsResponseBody>

    @GET("transaction/grouped/entity/{userId}")
    suspend fun getGroupedByEntityTransactions(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyDirection") moneyDirection: String?,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<SortedTransactionsResponseBody>

    @GET("transaction/latest-code/{userId}")
    suspend fun getLatestTransactionCode(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): Response<TransactionCodesResponseBody>

    @POST("auth/register")
    suspend fun registerUser(
        @Body user: UserRegistrationPayload
    ): Response<UserRegistrationResponseBody>

    @POST("auth/login")
    suspend fun loginUser(
        @Body user: UserLoginPayload
    ): Response<UserLoginResponseBody>

    @GET("subscription/status/{userId}")
    suspend fun getSubscriptionStatus(
        @Path("userId") userId: Int
    ): Response<SubscriptionStatusResponseBody>

    @GET("transaction/grouped/month/year/{userId}")
    suspend fun getGroupedByMonthTransactions(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("month") month: String,
        @Query("year") year: String
    ): Response<MonthlyTransactionsResponseBody>

    @PUT("user/{userId}")
    suspend fun updateUserDetails(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Body user: UserRegistrationPayload
    ): Response<UserRegistrationResponseBody>

    @PUT("auth/update/password")
    suspend fun updateUserPassword(
        @Body password: PasswordUpdatePayload
    ): Response<UserRegistrationResponseBody>

    @POST("subscription/pay")
    suspend fun paySubscriptionFee(
        @Header("Authorization") token: String,
        @Body paymentPayload: PaymentPayload
    ): Response<PaymentResponseBody>

    @POST("subpayment/status")
    suspend fun subscriptionPaymentStatus(
        @Header("Authorization") token: String,
        @Body subscriptionPaymentStatusPayload: SubscriptionPaymentStatusPayload
    ): Response<SubscriptionStatusResponseBody>

    @GET("transaction/dashboard/{userId}")
    suspend fun getDashboardDetails(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("date") date: String
    ): Response<DashboardDetailsResponseBody>

    @GET("transaction/report/{userId}")
    suspend fun getAllTransactionsReport(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyDirection") moneyDirection: String?,
        @Query("reportType") reportType: String?,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ResponseBody>

    @GET("transaction/single/{transactionId}")
    suspend fun getSingleTransaction(
        @Header("Authorization") token: String,
        @Path("transactionId") transactionId: Int
    ): Response<SingleTransactionResponseBody>

    @GET("version")
    suspend fun checkAppVersion(): Response<AppVersionCheckResponseBody>

    @POST("category/report")
    suspend fun generateReportForMultipleCategories(
        @Header("Authorization") token: String,
        @Body categoryReportPayload: CategoryReportPayload
    ): Response<ResponseBody>

    @GET("transaction/transactiontype/{userId}")
    suspend fun getTransactionTypesDashboard(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): Response<TransactionTypeResponseBody>

    @GET("payment/freetrialstatus/{userId}")
    suspend fun getFreeTrialStatus(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): Response<FreeTrialStatusResponseBody>
}