package com.records.pesa.network

import com.records.pesa.models.FinancialMessagePayload
import com.records.pesa.models.FinancialMessagesResponseBody
import com.records.pesa.models.GeneralResponseBody
import com.records.pesa.models.backup.FilesUploadResponseBody
import com.records.pesa.models.payment.PaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentResponseBody
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusPayload
import com.records.pesa.models.payment.intasend.IntasendPaymentStatusResponseBody
import com.records.pesa.models.payment.intasend.PaymentSavePayload
import com.records.pesa.models.payment.intasend.PaymentSaveResponseBody
import com.records.pesa.models.payment.intasend.PaymentsResponseBody
import com.records.pesa.models.subscription.SubscriptionContainerResponseBody
import com.records.pesa.models.subscription.SubscriptionTransactionResponseBody
import com.records.pesa.models.subscription.UserSubscriptionResponseBody
import com.records.pesa.models.user.TokenRefreshPayload
import com.records.pesa.models.user.UserProfileResponseBody
import com.records.pesa.models.user.login.AuthResponseBody
import com.records.pesa.models.user.login.UserLoginPayload
import com.records.pesa.models.user.registration.UserRegistrationPayload
import com.records.pesa.models.user.update.PasswordResetPayload
import com.records.pesa.models.user.update.UserBackupDataUpdatePayload
import com.records.pesa.models.user.update.UserProfileUpdatePayload
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("auth/register")
    suspend fun registerUser(@Body userRegistrationPayload: UserRegistrationPayload): Response<AuthResponseBody>

    @POST("auth/login")
    suspend fun login(
        @Body userLoginPayload: UserLoginPayload
    ): Response<AuthResponseBody>

    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Body refreshToken: TokenRefreshPayload
    ): Response<AuthResponseBody>

    @GET("user/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<UserProfileResponseBody>

    @PUT("auth/password-reset")
    suspend fun resetPassword(
        @Body passwordResetPayload: PasswordResetPayload
    ): Response<GeneralResponseBody>

    @PUT("user-profile-update")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body userProfileUpdatePayload: UserProfileUpdatePayload
    ): Response<GeneralResponseBody>

    @PUT("user-backup-update")
    suspend fun updateUserProfileBackupData(
        @Header("Authorization") token: String,
        @Body userBackupDataUpdatePayload: UserBackupDataUpdatePayload
    ): Response<UserProfileResponseBody>

    @GET("user/uid/{id}")
    suspend fun getUserByUserId(@Path("id") id: String): Response<UserProfileResponseBody>

    @GET("user/phone/{phoneNumber}")
    suspend fun getUserByPhoneNumber(@Path("phoneNumber") phoneNumber: String): Response<UserProfileResponseBody>

    @POST("payment/lipa")
    suspend fun lipa(
        @Header("Authorization") token: String,
        @Body paymentPayload: PaymentPayload
    ): Response<SubscriptionTransactionResponseBody>

    @GET("transaction/id/{id}")
    suspend fun getTransaction(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<SubscriptionTransactionResponseBody>

    @GET("subscription/subscription-package-container/id/{id}")
    suspend fun getSubscriptionPackageContainer(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<SubscriptionContainerResponseBody>

    @POST("lipa-status")
    suspend fun lipaStatus(
        @Body paymentStatusPayload: IntasendPaymentStatusPayload
    ): Response<IntasendPaymentStatusResponseBody>

    @POST("lipa-save")
    suspend fun savePayment(@Body paymentSavePayload: PaymentSavePayload): Response<PaymentSaveResponseBody>

    @GET("payments/{userId}")
    suspend fun getUserPayments(@Path("userId") userId: String): Response<PaymentsResponseBody>

    @Multipart
    @POST("storage/files/upload")
    suspend fun uploadFiles(
        @Header("Authorization") token: String,
        @Part files: List<MultipartBody.Part>
    ): Response<FilesUploadResponseBody>

    @GET("storage/file/{fileName}")
    suspend fun getFile(
        @Header("Authorization") token: String,
        @Path("fileName") fileName: String
    ): Response<ResponseBody>

    @POST("messages/submit-batch")
    suspend fun submitMessages(
        @Header("Authorization") token: String,
        @Body messages: List<FinancialMessagePayload>
    ): Response<FinancialMessagesResponseBody>

    @GET("subscription/user-subscription/{containerId}")
    suspend fun getUserSubscription(
        @Header("Authorization") token: String,
        @Path("containerId") containerId: Int
    ): Response<UserSubscriptionResponseBody>

}